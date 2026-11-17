package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class GroupExpansionOptions(
    val collapsible: Boolean,
    val collapsed: Boolean,
    val showDataIndicator: Boolean,
    val showValidationIndicator: Boolean,
    val showValidationIndicatorCount: Boolean,
    val diagnostic: UiDiagnostic? = null,
)

/** Data-presence visibility has the same local-over-namespaced precedence in Groups and Categories. */
fun resolveDataIndicatorOption(options: JsonOptions, config: Map<String, Any?>): Boolean {
    val defaults = config["jsonformsExtended"]
    val value = if (options.containsKey("showDataIndicator")) options["showDataIndicator"] else when (defaults) {
        is Map<*, *> -> defaults["showDataIndicator"]
        is JsonObject -> defaults["showDataIndicator"]
        else -> null
    }
    return when (value) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) false else value.content.toBooleanStrictOrNull() ?: false
        else -> false
    }
}

/** Mixed outer frames share Group settings but default to a collapsible, open frame. */
fun resolveMixedExpansionOptions(options: JsonOptions, config: Map<String, Any?>): GroupExpansionOptions =
    resolveGroupExpansionOptions(options, config, defaultCollapsible = true, elementType = "Mixed")

/** Local options win over namespaced config, including explicit false. */
fun resolveGroupExpansionOptions(
    options: JsonOptions,
    config: Map<String, Any?>,
    defaultCollapsible: Boolean = false,
    elementType: String = "Group",
): GroupExpansionOptions {
    val defaults = config["jsonformsExtended"]
    fun raw(key: String): Any? = if (options.containsKey(key)) options[key] else when (defaults) {
        is Map<*, *> -> defaults[key]
        is JsonObject -> defaults[key]
        else -> null
    }
    fun boolean(value: Any?): Boolean? = when (value) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) null else value.content.toBooleanStrictOrNull()
        else -> null
    }
    val validation = resolveContainerValidationOptions(options, config)
    val collapsible = boolean(raw("collapsible")) ?: defaultCollapsible
    val collapsedRaw = raw("collapsed")
    val collapsed = boolean(collapsedRaw)
    val diagnostic = if (collapsedRaw != null && collapsed == null) UiDiagnostic(
        code = "section.invalidCollapsed",
        severity = UiDiagnosticSeverity.Warning,
        message = "collapsed must be a boolean; using false.",
        details = mapOf("key" to "collapsed", "elementType" to elementType),
    ) else null
    return GroupExpansionOptions(
        collapsible = collapsible,
        collapsed = collapsed ?: false,
        showDataIndicator = resolveDataIndicatorOption(options, config),
        showValidationIndicator = validation.showIndicator,
        showValidationIndicatorCount = validation.showCount,
        diagnostic = diagnostic,
    )
}
