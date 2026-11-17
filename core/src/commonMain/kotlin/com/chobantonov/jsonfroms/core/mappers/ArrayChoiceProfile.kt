package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.hasType
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedCountChange
import com.chobantonov.jsonfroms.core.util.resolveSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class StringArrayChoice(val value: JsonPrimitive, val label: String)

/** Finite string choices only; structured constants need a renderer with typed identity support. */
fun finiteStringArrayChoices(schema: JsonSchema, rootSchema: JsonSchema): List<StringArrayChoice>? {
    if (!hasType(schema, "array")) return null
    val items = resolveSchema(schema, "items", rootSchema) ?: return null
    items.enumValues?.let { values ->
        if (values.any { it !is JsonPrimitive || !it.isString }) return null
        return values.map { value ->
            val text = (value as JsonPrimitive).content
            StringArrayChoice(value, text)
        }
    }
    val branches = items.oneOf ?: return null
    if (branches.isEmpty() || branches.any { it.const !is JsonPrimitive || !(it.const as JsonPrimitive).isString }) {
        return null
    }
    return branches.map { branch ->
        val value = branch.const as JsonPrimitive
        StringArrayChoice(value, branch.title ?: value.content)
    }
}

fun supportsMultiSelectArray(schema: JsonSchema, rootSchema: JsonSchema): Boolean =
    schema.uniqueItems == true && finiteStringArrayChoices(schema, rootSchema) != null

/** Null means free string entry; an empty list means a finite schema with no choices. */
fun finiteChipsChoices(schema: JsonSchema, rootSchema: JsonSchema): List<StringArrayChoice>? {
    val items = resolveSchema(schema, "items", rootSchema) ?: return null
    val constant = items.const
    if (constant is JsonPrimitive && constant.isString) {
        return listOf(StringArrayChoice(constant, items.title ?: constant.content))
    }
    return finiteStringArrayChoices(schema, rootSchema)
}

fun supportsChipsArray(schema: JsonSchema, rootSchema: JsonSchema): Boolean {
    if (!hasType(schema, "array")) return false
    val items = resolveSchema(schema, "items", rootSchema) ?: return false
    return finiteChipsChoices(schema, rootSchema) != null ||
        (hasType(items, "string") && items.enumValues == null && items.oneOf == null && items.const == null)
}

/** A token is committed only on explicit action; schema validation still checks item constraints. */
fun addChipValue(
    current: JsonElement?,
    value: String,
    choices: List<StringArrayChoice>?,
    unique: Boolean,
    maxItems: Int?,
    restrict: Boolean,
): JsonElement? {
    if (value.isEmpty()) return current
    val token = JsonPrimitive(value)
    if (choices != null && choices.none { it.value == token }) return current
    val items = (current as? JsonArray)?.toList().orEmpty()
    if ((unique && token in items) ||
        !acceptsRestrictedCountChange(items.size, items.size + 1, null, maxItems, restrict)) return current
    return JsonArray(items + token)
}

/** Only an explicit true enables search; local false overrides the host setting. */
fun choiceAutocomplete(option: JsonElement?, config: Map<String, Any?>): Boolean =
    (option as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
        ?: when (val global = config["autocomplete"]) {
            is Boolean -> global
            is JsonPrimitive -> global.takeUnless { it.isString }?.booleanOrNull
            else -> null
        } ?: false

/** Rechecks the current array and permitted choice before appending once. */
fun addFiniteStringChoice(
    current: JsonElement?,
    value: JsonPrimitive,
    choices: List<StringArrayChoice>,
    maxItems: Int?,
    restrict: Boolean,
): JsonElement? {
    if (choices.none { it.value == value }) return current
    val items = (current as? JsonArray)?.toList().orEmpty()
    if (value in items || !acceptsRestrictedCountChange(items.size, items.size + 1, null, maxItems, restrict)) {
        return current
    }
    return JsonArray(items + value)
}

/** Removes only the still-matching occurrence, preserving order and unrelated updates. */
fun removeArrayChoiceOccurrence(
    current: JsonElement?,
    index: Int,
    expected: JsonElement,
    minItems: Int?,
    restrict: Boolean,
): JsonElement? {
    val items = current as? JsonArray ?: return current
    if (items.getOrNull(index) != expected ||
        !acceptsRestrictedCountChange(items.size, items.size - 1, minItems, null, restrict)) return current
    return JsonArray(items.filterIndexed { position, _ -> position != index })
}

/** Replace one still-matching choice without crossing a restricted count boundary. */
fun replaceArrayChoiceOccurrence(
    current: JsonElement?,
    index: Int,
    expected: JsonElement,
    replacement: JsonPrimitive,
    choices: List<StringArrayChoice>,
    unique: Boolean,
): JsonElement? {
    val items = current as? JsonArray ?: return current
    if (items.getOrNull(index) != expected || replacement == expected ||
        choices.none { it.value == replacement } ||
        (unique && items.withIndex().any { (position, value) ->
            position != index && value == replacement
        })) return current
    return JsonArray(items.mapIndexed { position, value ->
        if (position == index) replacement else value
    })
}
