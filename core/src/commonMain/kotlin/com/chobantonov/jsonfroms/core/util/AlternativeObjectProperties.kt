package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AlternativeObjectColumns(
    val properties: JsonObject,
    val branchSpecific: Set<String>,
)

data class SelectableAlternativeObjectBranch(
    val schema: JsonSchema,
    val discriminatorField: String,
    val discriminatorValue: JsonElement,
    val title: String?,
)

/** Flat alternatives whose required const values identify a new row's branch. */
fun selectableAlternativeObjectBranches(
    schema: JsonSchema,
    rootSchema: JsonSchema,
): List<SelectableAlternativeObjectBranch>? {
    val branches = alternativeObjectBranches(schema, rootSchema) ?: return null
    if (branches.any { branch ->
            (branch["properties"] as JsonObject).keys.any { field ->
                val resolved = resolveSchema(branch, "#/properties/${JsonPointer.encode(field)}", rootSchema)
                if (resolved == null) true else {
                    val types = deriveTypes(resolved)
                    types.any { it == "object" || it == "array" } ||
                        types.isEmpty() && resolved["const"] !is JsonPrimitive
                }
            }
        }) return null
    val discriminator = commonRequiredConstDiscriminator(branches) ?: return null
    return branches.map { branch ->
        val fieldSchema = (branch["properties"] as JsonObject)[discriminator] as JsonObject
        SelectableAlternativeObjectBranch(
            schema = branch,
            discriminatorField = discriminator,
            discriminatorValue = fieldSchema["const"]!!,
            title = (branch["title"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
        )
    }
}

/** Union of authored alternative fields for table columns. */
fun alternativeObjectColumns(schema: JsonSchema, rootSchema: JsonSchema): AlternativeObjectColumns? {
    val branches = alternativeObjectBranches(schema, rootSchema) ?: return null
    val properties = branches.map { it["properties"] as JsonObject }
    val merged = linkedMapOf<String, JsonElement>()
    properties.forEach { fields -> fields.forEach { (name, value) ->
        if (name !in merged) merged[name] = value
    } }
    val branchSpecific = merged.keys.filterTo(mutableSetOf()) { name ->
        properties.any { name !in it }
    }
    commonRequiredConstDiscriminator(branches)?.let { field ->
        val first = properties.first()[field] as JsonObject
        merged[field] = buildJsonObject {
            first["title"]?.let { put("title", it) }
            put("enum", buildJsonArray {
                properties.forEach { fields -> add((fields[field] as JsonObject)["const"]!!) }
            })
        }
    }
    return AlternativeObjectColumns(JsonObject(merged), branchSpecific)
}

private fun commonRequiredConstDiscriminator(branches: List<JsonSchema>): String? {
    val first = branches.first()["properties"] as JsonObject
    return first.keys.firstOrNull { field ->
        val values = branches.map { branch ->
            val required = branch["required"] as? JsonArray ?: return@firstOrNull false
            if (JsonPrimitive(field) !in required) return@firstOrNull false
            val properties = branch["properties"] as JsonObject
            (properties[field] as? JsonObject)?.get("const") ?: return@firstOrNull false
        }
        values.indices.all { index ->
            (index + 1 until values.size).none { other ->
                schemaConstEquivalent(values[index], values[other]) != false
            }
        }
    }
}

internal fun schemaConstEquivalent(first: JsonElement, second: JsonElement): Boolean? {
    if (first == second) return true
    if (first is JsonArray && second is JsonArray) {
        if (first.size != second.size) return false
        var uncertain = false
        first.indices.forEach { index ->
            when (schemaConstEquivalent(first[index], second[index])) {
                false -> return false
                null -> uncertain = true
                true -> Unit
            }
        }
        return if (uncertain) null else true
    }
    if (first is JsonObject && second is JsonObject) {
        if (first.keys != second.keys) return false
        var uncertain = false
        first.forEach { (key, value) ->
            when (schemaConstEquivalent(value, second[key]!!)) {
                false -> return false
                null -> uncertain = true
                true -> Unit
            }
        }
        return if (uncertain) null else true
    }
    val left = first as? JsonPrimitive ?: return false
    val right = second as? JsonPrimitive ?: return false
    if (left.isString || right.isString) return false
    if (left.content == "true" || left.content == "false" ||
        right.content == "true" || right.content == "false") return false
    val leftNumber = normalizedDecimal(left.content)
    val rightNumber = normalizedDecimal(right.content)
    // A numeric spelling outside our exact-decimal range cannot prove a match or uniqueness.
    if (leftNumber == null || rightNumber == null) return null
    return leftNumber.digits == rightNumber.digits &&
        leftNumber.exponent == rightNumber.exponent &&
        (leftNumber.digits == "0" || leftNumber.negative == rightNumber.negative)
}

/** Choice values follow JSON Schema numeric equality without rewriting stored JSON. */
fun jsonSchemaChoiceEquals(first: JsonElement, second: JsonElement): Boolean =
    schemaConstEquivalent(first, second) == true

/** Fields shared by every object alternative, preserving the first branch's order. */
fun commonAlternativeProperties(schema: JsonSchema, rootSchema: JsonSchema): JsonObject? {
    val branches = alternativeObjectBranches(schema, rootSchema) ?: return null
    val properties = branches.map { it["properties"] as JsonObject }
    val names = properties.first().keys
    if (names.isEmpty() || properties.any { it.keys != names }) return null
    return properties.first()
}

/** True only when the common fields can all be edited as scalar table cells. */
fun hasFlatAlternativeObjectProperties(schema: JsonSchema, rootSchema: JsonSchema): Boolean {
    if (selectableAlternativeObjectBranches(schema, rootSchema) != null) return true
    val fields = commonAlternativeProperties(schema, rootSchema) ?: return false
    val branches = alternativeObjectBranches(schema, rootSchema) ?: return false
    return branches.all { branch -> fields.keys.all { field ->
        val resolved = resolveSchema(branch, "#/properties/${JsonPointer.encode(field)}", rootSchema)
            ?: return@all false
        val types = deriveTypes(resolved)
        types.isNotEmpty() && types.none { it == "object" || it == "array" }
    } }
}

private fun alternativeObjectBranches(schema: JsonSchema, rootSchema: JsonSchema): List<JsonSchema>? {
    if (schema.containsKey("properties") || schema.containsKey("allOf") ||
        schema.containsKey("then") || schema.containsKey("else")) return null
    val source = (schema["oneOf"] as? JsonArray)
        ?: (schema["anyOf"] as? JsonArray) ?: return null
    if (source.size < 2) return null
    return source.map { element ->
        val branch = element.asJsonSchemaOrNull() ?: return null
        val resolved = resolveSchema(branch, "", rootSchema) ?: return null
        if (resolved["properties"] !is JsonObject) return null
        resolved
    }
}
