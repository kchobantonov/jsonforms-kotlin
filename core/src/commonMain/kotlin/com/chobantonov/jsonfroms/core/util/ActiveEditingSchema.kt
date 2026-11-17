package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Data pointer for Controls composed solely of JSON Schema property steps. */
fun scopedPropertyDataPointer(scope: String): String? {
    val parts = scope.split('/')
    if (parts.size < 3 || parts.size % 2 == 0 || parts.first() != "#") return null
    val properties = mutableListOf<String>()
    for (index in 1 until parts.size step 2) {
        if (parts[index] != "properties" || parts[index + 1].isEmpty()) return null
        properties += parts[index + 1]
    }
    return properties.joinToString("/", prefix = "/")
}

/** Choose a branch from a unique stored type or required sibling const. */
fun activeEditingSchemaForScopedProperty(
    schema: JsonSchema,
    scope: String,
    rootSchema: JsonSchema,
    value: JsonElement?,
    contextData: JsonElement? = null,
): JsonSchema? {
    val dataPointer = scopedPropertyDataPointer(scope) ?: return null
    val firstProperty = JsonPointer.decode(dataPointer.removePrefix("/").substringBefore('/'))
    if ((schema["properties"] as? JsonObject)?.containsKey(firstProperty) == true ||
        (schema["required"] as? JsonArray)?.any { it == JsonPrimitive(firstProperty) } == true ||
        schema.containsKey("allOf") || schema.containsKey("then") || schema.containsKey("else")) return null
    val branches = (schema["oneOf"] as? JsonArray) ?: (schema["anyOf"] as? JsonArray) ?: return null
    val candidates = branches.mapNotNull { branch ->
        val parent = branch.asJsonSchemaOrNull() ?: return@mapNotNull null
        val resolved = resolveSchema(parent, scope, rootSchema) ?: return@mapNotNull null
        parent to resolved
    }
    if (candidates.isEmpty()) return null
    val parentPointer = dataPointer.substringBeforeLast('/')
    val objectData = JsonPointer.resolve(contextData, parentPointer) as? JsonObject
    val parentScope = scope.substringBeforeLast("/properties/").takeUnless { it == "#" }.orEmpty()
    val editedProperty = JsonPointer.decode(dataPointer.substringAfterLast('/'))
    if (candidates.size == 1) {
        // A field present in only one alternative needs branch evidence; its
        // presence or JSON type alone cannot establish which object is active.
        val selected = candidates.single()
        if (value != null && !matchesStoredType(selected.second, value)) return null
        val row = objectData ?: return null
        val decisions = branches.map { element ->
            val branch = element.asJsonSchemaOrNull() ?: return null
            val parent = resolveSchema(branch, parentScope, rootSchema) ?: return null
            requiredConstDiscriminatorMatches(parent, editedProperty, row) ?: return null
        }
        if (decisions.count { it } != 1) return null
        val selectedParent = resolveSchema(selected.first, parentScope, rootSchema) ?: return null
        return selected.first.takeIf {
            requiredConstDiscriminatorMatches(selectedParent, editedProperty, row) == true
        }
    }
    val matching = if (value == null) candidates else
        candidates.filter { (_, resolved) -> matchesStoredType(resolved, value) }
    if (value != null) matching.singleOrNull()?.let { return it.first }
    if (matching.size < 2) return null
    val row = objectData ?: return null
    val decisions = matching.map { (branch, _) ->
        val parent = resolveSchema(branch, parentScope, rootSchema)
        parent?.let { requiredConstDiscriminatorMatches(it, editedProperty, row) }
    }
    if (decisions.any { it == null } || decisions.count { it == true } != 1) return null
    return matching[decisions.indexOf(true)].first
}

private fun requiredConstDiscriminatorMatches(
    branch: JsonSchema,
    editedProperty: String,
    data: JsonObject,
): Boolean? {
    val required = (branch["required"] as? JsonArray)?.toSet() ?: return null
    val properties = branch["properties"] as? JsonObject ?: return null
    val predicates = properties.filter { (name, element) ->
        name != editedProperty && JsonPrimitive(name) in required &&
            (element as? JsonObject)?.containsKey("const") == true
    }
    if (predicates.isEmpty()) return null
    val matches: List<Boolean?> = predicates.map { (name, element) ->
        val stored = data[name] ?: return@map false
        schemaConstEquivalent(stored, (element as JsonObject)["const"]!!)
    }
    return when {
        false in matches -> false
        matches.any { it == null } -> null
        else -> true
    }
}

private val jsonIntegerPattern = Regex("-?(0|[1-9][0-9]*)")

private fun matchesStoredType(schema: JsonSchema, value: JsonElement): Boolean {
    val types = deriveTypes(schema)
    return when (value) {
        JsonNull -> "null" in types
        is JsonObject -> "object" in types
        is JsonArray -> "array" in types
        is JsonPrimitive -> when {
            value.isString -> "string" in types
            value.content == "true" || value.content == "false" -> "boolean" in types
            jsonIntegerPattern.matches(value.content) -> "integer" in types || "number" in types
            value.content.toDoubleOrNull() != null -> "number" in types
            else -> false
        }
    }
}
