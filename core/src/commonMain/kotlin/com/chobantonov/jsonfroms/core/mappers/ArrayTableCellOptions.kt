package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.util.JsonPointer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class ArrayTableCellOptions(
    val summary: UiSchemaElement?,
    val detail: UiSchemaElement?,
    val dialog: JsonObject?,
)

/** Cell options override collection options, then the form-wide default. */
fun resolveArrayTableCellTypeIndicator(
    cellOptions: JsonObject?,
    collectionOptions: JsonOptions,
    config: Map<String, Any?>,
): Boolean {
    fun boolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) null else value.booleanOrNull
        else -> null
    }
    return boolean(cellOptions?.get("showTypeIndicator"))
        ?: boolean(collectionOptions["showTypeIndicator"])
        ?: boolean(config["showTypeIndicator"])
        ?: false
}

fun resolveArrayTableCellOptions(options: JsonOptions, field: String): ArrayTableCellOptions? {
    val cell = ((options["cells"] as? JsonObject)?.get(field) as? JsonObject) ?: return null
    fun element(name: String) = (cell[name] as? JsonObject)
        ?.let { runCatching { it.toUiSchemaElement() }.getOrNull() }
    return ArrayTableCellOptions(
        summary = element("summary"),
        detail = element("detail"),
        dialog = cell["dialog"] as? JsonObject,
    )
}

/** A compact, read-only description for a complex cell. */
data class ArrayTableCellPreview(val text: String, val generated: Boolean)

fun arrayTableCellPreview(
    value: JsonElement?,
    summary: ControlElement? = null,
    translator: Translator? = null,
    schema: JsonSchema? = null,
): String = arrayTableCellPreviewResult(value, summary, translator, schema).text

fun arrayTableCellPreviewResult(
    value: JsonElement?,
    summary: ControlElement? = null,
    translator: Translator? = null,
    schema: JsonSchema? = null,
): ArrayTableCellPreview {
    fun translated(key: String, fallback: String, params: Map<String, Any> = emptyMap()): String {
        val message = translator?.translate(key, fallback, params) ?: fallback
        return params.entries.fold(message) { text, (name, value) ->
            text.replace("{$name}", value.toString())
        }
    }
    if (value == null || value == JsonNull) return ArrayTableCellPreview(
        translated("composite.summary.unset", "Not set"), true)
    if (value is JsonArray) {
        val count = value.size
        val previews = if (summary == null) emptyList() else value.mapNotNull { item ->
            summaryValue(item, summary.scope)
        }.take(2)
        if (previews.isNotEmpty()) {
            val remaining = count - previews.size
            val suffix = if (remaining > 0) " " + translated("composite.summary.more", "(+$remaining more)",
                mapOf("count" to remaining))
                else ""
            return ArrayTableCellPreview(previews.joinToString(", ") + suffix, false)
        }
        return ArrayTableCellPreview(translated(
            if (count == 1) "composite.summary.item" else "composite.summary.items",
            if (count == 1) "1 item" else "$count items", mapOf("count" to count)), true)
    }
    if (value is JsonObject) {
        summary?.let { summaryValue(value, it.scope) }?.let { return ArrayTableCellPreview(it, false) }
        if (value.isEmpty()) return ArrayTableCellPreview(
            translated("composite.summary.emptyObject", "Empty object"), true)
        val label = summary?.let { summaryFieldLabel(it, schema, translator) }
        return if (label != null) ArrayTableCellPreview(
            translated("composite.summary.unspecified", "$label not specified", mapOf("label" to label)), true)
        else ArrayTableCellPreview(translated("composite.summary.details", "Details"), true)
    }
    return scalarPreview(value)?.let { ArrayTableCellPreview(it, false) }
        ?: ArrayTableCellPreview(translated("composite.summary.unset", "Not set"), true)
}

private fun summaryFieldLabel(summary: ControlElement, schema: JsonSchema?, translator: Translator?): String? {
    if ((summary.label as? BooleanControlLabel)?.value == false ||
        (summary.label as? DescriptionControlLabel)?.value?.show == false) return null
    val segments = summary.scope.removePrefix("#/").split('/')
        .filter { it.isNotBlank() && it != "properties" && it != "items" }
    if (segments.isEmpty() || summary.scope == "#") return null
    val authored = when (val label = summary.label) {
        is StringControlLabel -> label.value
        is DescriptionControlLabel -> label.value.text
        else -> null
    }?.takeIf { it.isNotBlank() }
    val fieldSchema = segments.fold(schema) { current, segment -> current?.properties?.get(segment) }
    val fallback = authored ?: fieldSchema?.title ?: segments.last().replaceFirstChar { it.uppercase() }
    return translator?.translate(summary.i18n?.let { "$it.label" } ?: fallback, fallback, emptyMap()) ?: fallback
}

private fun summaryValue(root: JsonElement, scope: String): String? {
    val pointer = if (scope == "#" || scope.isBlank()) "#" else {
        val segments = scope.removePrefix("#/").split('/').filter { it.isNotBlank() && it != "properties" && it != "items" }
        "#/" + segments.joinToString("/")
    }
    return scalarPreview(JsonPointer.resolve(root, pointer))
}

private fun scalarPreview(value: JsonElement?): String? = (value as? JsonPrimitive)
    ?.takeUnless { it == JsonNull }
    ?.content?.trim()?.takeIf(String::isNotBlank)
