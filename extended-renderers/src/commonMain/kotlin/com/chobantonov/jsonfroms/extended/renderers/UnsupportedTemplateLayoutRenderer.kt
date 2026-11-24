package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Runtime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal data class TemplateLayoutUnavailable(val language: String, val code: String, val fallback: String)

internal fun templateLayoutUnavailable(
    element: UnknownUiSchemaElement,
    config: Map<String, Any?>,
): TemplateLayoutUnavailable {
    val extended = config["jsonformsExtended"].member("defaultTemplateLang")
    val requested = if ("lang" in element.properties) element.properties["lang"]
        else extended ?: "ractive"
    val language = when (requested) {
        is JsonPrimitive -> requested.content
        else -> requested.toString()
    }
    if (language.lowercase() !in setOf("ractive", "jsx")) return TemplateLayoutUnavailable(
        language, "template.unsupportedLanguage", "Template language is unsupported: $language",
    )
    val security = config["jsonformsExtended"].member("security")
    val enabled = when (val value = security.member("allowScriptEvaluation")) {
        true -> true
        is JsonPrimitive -> !value.isString && value.content == "true"
        else -> false
    }
    return if (enabled) TemplateLayoutUnavailable(
        language, "template.engineUnavailable", "Template engine is unavailable on this host: $language",
    ) else TemplateLayoutUnavailable(
        language, "template.evaluationDisabled", "Template script evaluation is disabled",
    )
}

private fun Any?.member(name: String): Any? = when (this) {
    is Map<*, *> -> this[name]
    is JsonObject -> this[name]
    else -> null
}

val unsupportedTemplateLayoutTester: RankedTester = rankWith(2, uiTypeIs("TemplateLayout"))

val unsupportedTemplateLayoutRenderer = ComposeJsonFormsRenderer { props ->
    UnsupportedTemplateLayout(props)
}

@Composable
private fun UnsupportedTemplateLayout(props: ComposeJsonFormsRendererProps) {
    val element = props.uischema as? UnknownUiSchemaElement ?: return
    if (!Runtime.isVisible(element, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val unavailable = templateLayoutUnavailable(element, props.config)
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val message = translateRendererText(props.state.jsonforms.i18n.translate,
        props.state.jsonforms.i18n.locale, unavailable.code, unavailable.fallback,
        templateLayoutLocaleBundles(unavailable.language))
    LaunchedEffect(reporter, element, unavailable, props.path, message) {
        reporter?.report(UiDiagnostic(
            code = unavailable.code,
            severity = UiDiagnosticSeverity.Error,
            message = message,
            details = mapOf("path" to props.path, "language" to unavailable.language),
        ))
    }
    BasicText(
        text = message,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
