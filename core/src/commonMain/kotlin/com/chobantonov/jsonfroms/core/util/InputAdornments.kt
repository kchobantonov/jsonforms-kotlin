package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.store.Translator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Informational content for the logical edges of an input field. */
data class InputAdornment(
    val text: String,
    val tooltip: String? = null,
    val icon: String? = null,
    val decorative: Boolean = false,
)

data class InvalidInputAdornment(val edge: String, val index: Int? = null)

/** Malformed authored entries are diagnosed separately from valid decorative icons. */
fun invalidInputAdornments(options: JsonOptions): List<InvalidInputAdornment> =
    listOf("startAdornment", "endAdornment").flatMap { edge ->
        when (val value = options[edge]) {
            null -> emptyList()
            is JsonArray -> value.mapIndexedNotNull { index, item ->
                if (item is JsonObject && validAdornmentObject(item)) null
                else InvalidInputAdornment(edge, index)
            }
            is JsonPrimitive -> if (value.isString) emptyList()
                else listOf(InvalidInputAdornment(edge))
            is JsonObject -> if (validAdornmentObject(value)) emptyList()
                else listOf(InvalidInputAdornment(edge))
        }
    }

fun resolveInputAdornments(
    options: JsonOptions,
    edge: String,
    translator: Translator?,
): List<InputAdornment> {
    val value = options[edge] ?: return emptyList()
    val values = if (value is JsonArray) value.toList() else listOf(value)
    return values.mapNotNull { item ->
        if (value is JsonArray && item is JsonPrimitive) null
        else resolveInputAdornment(item, translator)
    }
}

private fun resolveInputAdornment(value: JsonElement, translator: Translator?): InputAdornment? {
    if (value is JsonPrimitive) {
        return value.content.takeIf { value.isString && it.isNotEmpty() }
            ?.let { InputAdornment(it) }
    }
    val objectValue = value as? JsonObject ?: return null
    if (!validAdornmentObject(objectValue)) return null
    if (objectValue.string("kind") == "icon" && objectValue["decorative"]?.toString() == "true")
        return InputAdornment("", icon = objectValue.string("icon"), decorative = true)
    val property = if (objectValue.string("kind") == "text") "text" else "label"
    val fallback = objectValue.string(property) ?: return null
    val prefix = objectValue.string("i18n")
    val text = prefix?.let { translator?.translate("$it.$property", fallback, emptyMap()) }
        ?: fallback
    val tooltipFallback = objectValue.string("tooltip")
    val tooltip = tooltipFallback?.let { authored ->
        prefix?.let { translator?.translate("$it.tooltip", authored, emptyMap()) } ?: authored
    }
    return InputAdornment(text, tooltip, objectValue.string("icon"))
}

private fun JsonObject.string(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content

private fun JsonObject.validString(name: String, required: Boolean): Boolean =
    if (name !in this) !required else !string(name).isNullOrEmpty()

private fun validAdornmentObject(value: JsonObject): Boolean {
    val kind = value.string("kind")
    val allowed = when (kind) {
        "text" -> setOf("kind", "text", "i18n", "tooltip")
        "icon" -> setOf("kind", "icon", "label", "i18n", "tooltip", "decorative")
        else -> return false
    }
    if (value.keys.any { it !in allowed } ||
        !value.validString("i18n", required = false) ||
        !value.validString("tooltip", required = false)) return false
    if (kind == "text") return value.validString("text", required = true)
    if (!value.validString("icon", required = true)) return false
    val decorative = when (val marker = value["decorative"]) {
        null -> false
        is JsonPrimitive -> when {
            marker.isString -> return false
            marker.content == "true" -> true
            marker.content == "false" -> false
            else -> return false
        }
        else -> return false
    }
    return if (decorative) listOf("label", "i18n", "tooltip").none { it in value }
        else value.validString("label", required = true)
}
