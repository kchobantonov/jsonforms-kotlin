package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun hasType(
    schema: JsonSchema,
    expected: String,
): Boolean = deriveTypes(schema).contains(expected)

fun deriveTypes(schema: JsonSchema): List<String> {
    val typeElement = schema["type"]
    if (typeElement is JsonPrimitive && typeElement.isString) {
        return listOf(typeElement.content)
    }
    if (typeElement is JsonArray) {
        return typeElement.mapNotNull { (it as? JsonPrimitive)?.stringContentOrNull() }
    }
    if (schema["properties"] is JsonObject || schema.containsKey("additionalProperties")) {
        return listOf("object")
    }
    if (schema.containsKey("items")) {
        return listOf("array")
    }
    val enumElement = schema["enum"] as? JsonArray
    if (enumElement != null) {
        return enumElement.flatMap { enumValue ->
            when (enumValue) {
                is JsonPrimitive -> when {
                    enumValue.isString -> listOf("string")
                    enumValue.booleanValueOrNull() != null -> listOf("boolean")
                    enumValue.intValueOrNull() != null -> listOf("integer")
                    enumValue.doubleValueOrNull() != null -> listOf("number")
                    else -> emptyList()
                }

                is JsonObject -> deriveTypes(JsonSchema(enumValue))
                else -> emptyList()
            }
        }.distinct()
    }
    val allOf = schema["allOf"] as? JsonArray
    if (allOf != null) {
        return allOf
            .mapNotNull(JsonElement::asJsonSchemaOrNull)
            .map(::deriveTypes)
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
    }
    return emptyList()
}

fun isEnumSchema(schema: JsonSchema): Boolean =
    schema.containsKey("enum") || schema.containsKey("const")

fun isOneOfEnumSchema(schema: JsonSchema): Boolean {
    val oneOf = schema["oneOf"] as? JsonArray ?: return false
    return oneOf.all { (it as? JsonObject)?.containsKey("const") == true }
}

fun resolveSchema(
    schema: JsonSchema?,
    schemaPath: String?,
    rootSchema: JsonSchema?,
): JsonSchema? = resolveSchemaElement(schema, schemaPath, rootSchema).asJsonSchemaOrNull()

fun resolveSchemaElement(
    schema: JsonSchema?,
    schemaPath: String?,
    rootSchema: JsonSchema?,
): JsonElement? {
    val currentRoot = rootSchema ?: schema ?: return null
    val source = schema ?: currentRoot
    val path = schemaPath ?: ""
    val resolutionMap = mutableMapOf<Int, MutableSet<String>>()

    return doResolveSchemaElement(source.toJsonObject(), path, currentRoot.toJsonObject(), resolutionMap)
}

private fun doResolveSchemaElement(
    schema: JsonElement?,
    schemaPath: String,
    rootSchema: JsonObject,
    resolutionMap: MutableMap<Int, MutableSet<String>>,
): JsonElement? {
    if (schema == null) {
        return null
    }

    val ref = (schema as? JsonObject)?.get("\$ref") as? JsonPrimitive
    if (ref?.isString == true) {
        val resolvedRef = resolvePath(rootSchema, ref.content, rootSchema, resolutionMap)
        if (resolvedRef != null) {
            val resolvedSchema = resolvePath(resolvedRef, schemaPath, rootSchema, resolutionMap)
            if (resolvedSchema != null) {
                return resolvedSchema
            }
        }
    }

    return resolvePath(schema, schemaPath, rootSchema, resolutionMap)
}

private fun resolvePath(
    schema: JsonElement?,
    schemaPath: String?,
    rootSchema: JsonObject,
    resolutionMap: MutableMap<Int, MutableSet<String>>,
): JsonElement? {
    if (schema == null) {
        return null
    }

    val identity = schema.hashCode()
    val visitedPaths = resolutionMap.getOrPut(identity) { mutableSetOf() }
    val normalizedPath = schemaPath ?: ""
    if (!visitedPaths.add(normalizedPath)) {
        return null
    }

    val result = resolvePathSegmentsWithCombinatorFallback(
        schema,
        normalizedPath.split('/').map(JsonPointer::decode),
        rootSchema,
        resolutionMap,
    )

    visitedPaths.remove(normalizedPath)
    return result
}

private fun resolvePathSegmentsWithCombinatorFallback(
    schema: JsonElement?,
    pathSegments: List<String>,
    rootSchema: JsonObject,
    resolutionMap: MutableMap<Int, MutableSet<String>>,
): JsonElement? {
    if (schema == null) {
        return null
    }
    if (pathSegments.isEmpty()) {
        return schema
    }
    if ((schema as? JsonObject)?.isEmpty() == true) {
        return null
    }

    val directResult = resolvePathSegments(schema, pathSegments, rootSchema, resolutionMap)
    if (directResult != null) {
        return directResult
    }

    val schemaObject = schema as? JsonObject ?: return null
    val combinators = buildList {
        addAll((schemaObject["oneOf"] as? JsonArray).orEmpty())
        addAll((schemaObject["allOf"] as? JsonArray).orEmpty())
        addAll((schemaObject["anyOf"] as? JsonArray).orEmpty())
        schemaObject["then"]?.let(::add)
        schemaObject["else"]?.let(::add)
    }

    combinators.forEach { subSchema ->
        val candidate = if ((subSchema as? JsonObject)?.get("\$ref") is JsonPrimitive) {
            resolvePath(rootSchema, (subSchema["\$ref"] as JsonPrimitive).content, rootSchema, resolutionMap)
        } else {
            subSchema
        }
        val alternative = resolvePathSegmentsWithCombinatorFallback(candidate, pathSegments, rootSchema, resolutionMap)
        if (alternative != null) {
            return alternative
        }
    }

    return null
}

private fun resolvePathSegments(
    schema: JsonElement?,
    pathSegments: List<String>,
    rootSchema: JsonObject,
    resolutionMap: MutableMap<Int, MutableSet<String>>,
): JsonElement? {
    if (schema == null) {
        return null
    }
    if (pathSegments.isEmpty()) {
        return schema
    }
    if ((schema as? JsonObject)?.isEmpty() == true) {
        return null
    }

    val (nextSchema, remainingPathSegments) = resolveSingleStep(schema, pathSegments)
    val dereferenced = if ((nextSchema as? JsonObject)?.get("\$ref") is JsonPrimitive) {
        resolvePath(rootSchema, (nextSchema["\$ref"] as JsonPrimitive).content, rootSchema, resolutionMap)
    } else {
        nextSchema
    }

    return resolvePathSegmentsWithCombinatorFallback(dereferenced, remainingPathSegments, rootSchema, resolutionMap)
}

private fun resolveSingleStep(
    schema: JsonElement,
    pathSegments: List<String>,
): Pair<JsonElement?, List<String>> {
    if (pathSegments.isEmpty()) {
        return schema to emptyList()
    }

    val segment = pathSegments.first()
    val remaining = pathSegments.drop(1)
    if (segment.isBlank() || segment == "#") {
        return resolveSingleStep(schema, remaining)
    }

    val resolved = when (schema) {
        is JsonObject -> schema[segment]
        is JsonArray -> segment.toIntOrNull()?.let(schema::getOrNull)
        else -> null
    }
    return resolved to remaining
}

internal fun JsonPrimitive.stringContentOrNull(): String? =
    if (isString) {
        content
    } else {
        null
    }

internal fun JsonPrimitive.booleanValueOrNull(): Boolean? =
    content.lowercase().let {
        when (it) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

internal fun JsonPrimitive.intValueOrNull(): Int? = content.toIntOrNull()

internal fun JsonPrimitive.doubleValueOrNull(): Double? = content.toDoubleOrNull()
