package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/** Guards completed numeric edits while permitting an existing bound violation to improve. */
fun acceptsRestrictedNumericEdit(
    current: JsonElement?,
    proposed: JsonPrimitive,
    schema: JsonSchema,
    restrict: Boolean,
    rootSchema: JsonSchema? = null,
): Boolean {
    if (!restrict) return true
    val next = proposed.doubleOrNull ?: return false
    val before = (current as? JsonPrimitive)?.doubleOrNull
    if (before == next) return true

    val bounds = numericBounds(schema, rootSchema)
    val lower = bounds.lower
    if (lower != null) {
        val nextViolates = next < lower.value || (lower.exclusive && next == lower.value)
        val improving = before != null &&
            (before < lower.value || (lower.exclusive && before == lower.value)) && next > before
        if (nextViolates && !improving) return false
    }

    val upper = bounds.upper
    if (upper != null) {
        val nextViolates = next > upper.value || (upper.exclusive && next == upper.value)
        val improving = before != null &&
            (before > upper.value || (upper.exclusive && before == upper.value)) && next < before
        if (nextViolates && !improving) return false
    }
    return true
}

private data class NumericBound(val value: Double, val exclusive: Boolean)
private data class NumericBounds(val lower: NumericBound?, val upper: NumericBound?)

private fun numericBounds(schema: JsonSchema, rootSchema: JsonSchema?): NumericBounds {
    val branches = schema["allOf"] as? JsonArray
    val safeBranches = if (rootSchema != null && branches != null && branches.isNotEmpty()) {
        branches.map { element ->
            val branch = element as? JsonObject ?: return@map null
            val ref = (branch["\$ref"] as? JsonPrimitive)?.stringContentOrNull()
            if (ref != null) {
                if (branch.keys != setOf("\$ref")) null else resolveSchema(rootSchema, ref, rootSchema)
            } else JsonSchema(branch)
        }.takeIf { it.all { branch -> branch != null && branch.keys.all(NUMERIC_BRANCH_KEYWORDS::contains) &&
            (branch["type"] == null || (branch.typeValue as? JsonSchemaTypeValue.Single)?.value in
                setOf(JsonSchemaType.Integer, JsonSchemaType.Number)) } }
    } else null
    val parts = listOf(schema) + safeBranches.orEmpty().filterNotNull()
    val lowers = parts.flatMap { part -> buildList {
        part.minimum?.let { add(NumericBound(it, part.exclusiveMinimumBoolean == true)) }
        part.exclusiveMinimumNumber?.let { add(NumericBound(it, true)) }
    } }
    val uppers = parts.flatMap { part -> buildList {
        part.maximum?.let { add(NumericBound(it, part.exclusiveMaximumBoolean == true)) }
        part.exclusiveMaximumNumber?.let { add(NumericBound(it, true)) }
    } }
    return NumericBounds(
        lower = lowers.maxWithOrNull(compareBy<NumericBound> { it.value }.thenBy { it.exclusive }),
        upper = uppers.minWithOrNull(compareBy<NumericBound> { it.value }.thenBy { !it.exclusive }),
    )
}

private val NUMERIC_BRANCH_KEYWORDS = setOf(
    "type", "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
    "title", "description", "default", "examples", "readOnly", "writeOnly", "deprecated", "\$comment",
)
