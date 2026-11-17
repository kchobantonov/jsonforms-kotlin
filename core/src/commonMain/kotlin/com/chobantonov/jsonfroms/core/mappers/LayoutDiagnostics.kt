package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Diagnose authored layout hints separately from form validation. */
fun collectLayoutDiagnostics(
    options: JsonOptions,
    config: Map<String, Any?>,
    elements: List<UiSchemaElement>,
    horizontal: Boolean,
): List<UiDiagnostic> {
    val result = mutableListOf<UiDiagnostic>()
    fun report(code: String, message: String, details: Map<String, Any?>) {
        result += UiDiagnostic(code, UiDiagnosticSeverity.Warning, message, details)
    }
    fun invalid(source: String, key: String, reason: String, details: Map<String, Any?>) =
        report("layout.invalidOption", "$source.$key must be $reason; ignored.", details + ("key" to key))
    fun unsupported(source: String, key: String, reason: String, details: Map<String, Any?>) =
        report("layout.unsupportedOption", "$source.$key $reason; ignored.", details + ("key" to key))
    fun sourceValue(source: Any?, key: String): Any? = when (source) {
        is Map<*, *> -> source[key]
        is JsonObject -> source[key]
        else -> null
    }

    val sources = listOf("options" to options, "config.jsonformsExtended.layoutDefaults" to layoutDefaults(config))
    for ((name, source) in sources) {
        val details = mapOf("source" to name)
        fun value(key: String): Any? = sourceValue(source, key)
        for (key in listOf("gap", "minItemWidth")) {
            if (name != "options" && key == "minItemWidth") continue
            val raw = value(key) ?: continue
            if (logicalDimension(raw) == null) {
                val code = if (isUnsupportedUnit(raw)) "layout.unsupportedOption" else "layout.invalidOption"
                report(code, "$name.$key cannot be used as a non-negative logical dimension; ignored.", details + ("key" to key))
            }
        }
        value("wrap")?.let { raw ->
            if (booleanValue(raw) == null) invalid(name, "wrap", "a boolean", details)
            else if (booleanValue(raw) == true && !horizontal && name == "options")
                unsupported(name, "wrap", "has no effect in a vertical layout", details)
        }
        value("gridColumns")?.let { raw ->
            if (positiveInt(raw) == null) invalid(name, "gridColumns", "a positive integer", details)
            else if (!horizontal && name == "options") unsupported(name, "gridColumns", "has no effect in a vertical layout", details)
        }
        if (name == "options") {
            value("align")?.let { raw ->
                if (stringValue(raw) !in setOf("start", "center", "end", "stretch"))
                    invalid(name, "align", "a supported alignment", details)
            }
            value("justify")?.let { raw ->
                if (stringValue(raw) !in setOf("start", "center", "end", "space-between", "space-around", "space-evenly"))
                    invalid(name, "justify", "a supported justification", details)
            }
            value("resizable")?.let { unsupported(name, "resizable", "has no effect outside a split layout", details) }
        }
    }

    val grid = resolveLayoutSizing(options, config).gridColumns
    elements.forEachIndexed { index, element ->
        val details = mapOf("childIndex" to index, "elementType" to element.type)
        if (horizontal && stringValue(options["align"]) == "stretch" &&
            element.type in setOf("HorizontalLayout", "VerticalLayout", "Group", "Categorization")) {
            report("layout.stretchFallback",
                "options.align stretch cannot size nested ${element.type} content intrinsically; its natural-height content is retained.",
                details + ("key" to "align"))
        }
        // The legacy fixed-grid child option is excluded by the portable sizing model.
        // Diagnose it even when the child has no portable options.layout block.
        if (element.options.containsKey("columns")) {
            report(
                "layout.excludedOption",
                "options.columns uses a fixed child grid; use options.layout.span with gridColumns instead; ignored.",
                details + ("key" to "columns"),
            )
        }
        if (element is SpacerElement) {
            spacerSizeDiagnostic(element)?.let { result += it.copy(details = it.details + details) }
        }
        val layoutValue = element.options["layout"] ?: return@forEachIndexed
        val layout = layoutValue as? JsonObject
        if (layout == null) {
            invalid("options", "layout", "an object", details)
            return@forEachIndexed
        }
        val mainKey = if (horizontal) "width" else "height"
        for (key in listOf("width", "height", "minWidth", "maxWidth", "minHeight", "maxHeight")) {
            val raw = layout[key] ?: continue
            if (logicalDimension(raw) == null) {
                val code = if (isUnsupportedUnit(raw)) "layout.unsupportedOption" else "layout.invalidOption"
                report(code, "options.layout.$key cannot be used as a non-negative logical dimension; ignored.", details + ("key" to key))
            }
        }
        val spanRaw = layout["span"]
        val span = positiveInt(spanRaw)
        if (spanRaw != null && span == null) {
            invalid("options.layout", "span", "a positive integer", details)
        } else if (span != null && !horizontal) {
            report("layout.spanUnsupported", "options.layout.span has no effect in a vertical layout; ignored.", details)
        } else if (span != null && span > grid) {
            report("layout.spanClamped", "options.layout.span $span exceeds gridColumns $grid; clamped.", details)
        }
        val weightRaw = layout["weight"]
        val weight = positiveFloat(weightRaw)
        if (weightRaw != null && weight == null) invalid("options.layout", "weight", "finite and greater than zero", details)

        val modes = listOfNotNull(
            mainKey.takeIf { logicalDimension(layout[mainKey]) != null },
            "span".takeIf { span != null && horizontal },
            "weight".takeIf { weight != null },
        )
        if (modes.size > 1) {
            report("layout.multipleSizingModes", "Multiple sizing modes (${modes.joinToString()}); using ${modes.first()}.", details)
        }
    }
    return result
}

fun spacerSizeDiagnostic(element: SpacerElement): UiDiagnostic? {
    val raw = element.size ?: return null
    if (logicalDimension(raw) != null) return null
    val code = if (isUnsupportedUnit(raw)) "layout.unsupportedOption" else "layout.invalidOption"
    return UiDiagnostic(
        code = code,
        severity = UiDiagnosticSeverity.Warning,
        message = "Spacer.size cannot be used as a non-negative logical dimension; defaulting to 32.",
        details = mapOf("key" to "size", "elementType" to "Spacer"),
    )
}

private fun stringValue(value: Any?): String? = when (value) {
    is JsonPrimitive -> if (value.isString) value.content else null
    is String -> value
    else -> null
}

private fun isUnsupportedUnit(value: Any?): Boolean =
    stringValue(value)?.trim()?.let { it.isNotEmpty() && it.toFloatOrNull() == null &&
        !it.endsWith("dp") && !it.endsWith("px") } == true
