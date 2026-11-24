package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.util.JsonPointer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Resolves static parameters and gated row/root parameters for a table Label summary. */
fun resolveTableLabelSummary(
    template: String,
    parameters: JsonObject?,
    root: JsonElement,
    item: JsonElement,
    config: Map<String, Any?>,
): String? {
    val extended = config["jsonformsExtended"].summaryField("dynamicValues")
    val enabled = extended.summaryField("enabled").summaryBoolean() ?: false
    if (parameters == null) return null
    val token = Regex("\\{([A-Za-z][A-Za-z0-9_]*)\\}")
    val names = token.findAll(template).map { it.groupValues[1] }.toSet()
    if (names.isEmpty()) return template.takeIf { it.isNotBlank() && '{' !in it && '}' !in it }
    val resolved = names.associateWith { name ->
        val expression = (parameters[name] as? JsonPrimitive)?.contentOrNull ?: return null
        if ('{' !in expression && '}' !in expression) return@associateWith expression
        if (!enabled) return null
        val match = Regex("^\\{(item|data)(?:\\.([A-Za-z0-9_.-]+))?\\}$")
            .matchEntire(expression) ?: return null
        val source = if (match.groupValues[1] == "item") item else root
        val path = match.groupValues[2]
        val pointer = if (path.isBlank()) "#" else "#/" + path.split('.')
            .joinToString("/") { JsonPointer.encode(it) }
        (JsonPointer.resolve(source, pointer) as? JsonPrimitive)
            ?.takeUnless { it == JsonNull }
            ?.takeUnless { it.isString && it.content.isBlank() }
            ?.content.orEmpty()
    }
    val text = token.replace(template) { resolved[it.groupValues[1]].orEmpty() }.trim()
    return text.takeIf { it.isNotBlank() && !it.contains(Regex("\\{[^}]*\\}")) }
}

private fun Any?.summaryField(name: String): Any? = when (this) {
    is Map<*, *> -> this[name]
    is JsonObject -> this[name]
    else -> null
}

private fun Any?.summaryBoolean(): Boolean? = when (this) {
    is Boolean -> this
    is JsonPrimitive -> takeUnless { isString }?.booleanOrNull
    else -> null
}
