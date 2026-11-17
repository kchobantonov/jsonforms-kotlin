package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

data class CompositeDialogActions(
    val showEmpty: Boolean,
    val showRemove: Boolean,
    val okLabel: String?,
    val cancelLabel: String?,
    val emptyLabel: String?,
    val removeLabel: String?,
    val clearable: Boolean = true,
)

/** Explicit cell values, including false, take precedence over collection and form defaults. */
fun resolveCompositeDialogActions(
    cell: JsonObject?,
    collection: JsonOptions,
    config: Map<String, Any?>,
): CompositeDialogActions {
    fun selected(key: String): Any? = cell?.get(key) ?: collection[key] ?: config[key]
    fun boolean(key: String, default: Boolean = false): Boolean = when (val value = selected(key)) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) default else value.booleanOrNull ?: default
        else -> default
    }
    fun label(key: String): String? = when (val value = selected(key)) {
        is String -> value.takeIf(String::isNotBlank)
        is JsonPrimitive -> value.takeIf { it.isString }?.contentOrNull?.takeIf(String::isNotBlank)
        else -> null
    }
    return CompositeDialogActions(boolean("showEmptyButton"), boolean("showRemoveButton"),
        label("okLabel"), label("cancelLabel"), label("emptyLabel"), label("removeLabel"),
        boolean("clearable", default = true))
}

/** Restrict protects structural minima, including constraints in composed schemas. */
fun canEmptyCompositeDraft(
    value: JsonElement?,
    schema: JsonSchema,
    rootSchema: JsonSchema,
    restrict: Boolean,
    validator: ValidateFunction? = null,
    rootData: JsonElement? = null,
    path: String? = null,
): Boolean {
    if (value !is JsonObject && value !is JsonArray) return false
    if (!restrict) return true
    val resolved = resolveSchema(schema, "", rootSchema) ?: schema
    val directlyAllowed = when (value) {
        is JsonObject -> (resolved.minProperties ?: 0) <= 0 && resolved.required.isNullOrEmpty()
        is JsonArray -> (resolved.minItems ?: 0) <= 0
    }
    if (!directlyAllowed) return false
    val needsValidation = resolved["contains"] != null ||
        listOf("allOf", "anyOf", "oneOf", "not", "if", "then", "else")
            .any { resolved[it] != null }
    if (!needsValidation) return true
    if (validator == null || rootData == null || path == null) return true
    val empty = if (value is JsonObject) JsonObject(emptyMap()) else JsonArray(emptyList())
    val target = pathSegments(path)
    val structural = setOf("required", "minProperties", "minItems", "contains", "minContains",
        "allOf", "anyOf", "oneOf", "not")
    return validator.validate(JsonPath.set(rootData, path, empty, validator.schema)).none { error ->
        if (error.keyword !in structural) false
        else {
            val at = errorPathSegments(error)
            at == target || (error.keyword == "required" && at.size == target.size + 1 &&
                at.take(target.size) == target)
        }
    }
}
