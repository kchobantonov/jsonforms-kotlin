package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.util.invalidInputAdornments
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val adornmentDiagnosticLocales = mapOf("bg" to JsonObject(mapOf(
    "adornment.invalid" to JsonPrimitive("Невалидна украса на поле; проверете посочения край и индекс."),
    "adornment.unsupported" to JsonPrimitive("Украси на полета не се поддържат за избрания редактор."),
)))

/** Reports invalid authored shapes while leaving valid siblings available to the selected editor. */
@Composable
fun ReportInvalidInputAdornments(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement ?: return
    val issues = invalidInputAdornments(control.options)
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val i18n = props.state.jsonforms.i18n
    val message = translateRendererText(i18n.translate, i18n.locale, "adornment.invalid",
        "Invalid input adornment; check the authored edge and index.", adornmentDiagnosticLocales)
    LaunchedEffect(reporter, props.path, control.scope, issues, message) {
        if (reporter != null) issues.forEach { issue -> reporter.report(UiDiagnostic(
            code = "adornment.invalid",
            severity = UiDiagnosticSeverity.Warning,
            message = message,
            details = mapOf("path" to props.path, "scope" to control.scope,
                "edge" to issue.edge, "index" to issue.index),
        )) }
    }
}

/** Reports an authored adornment that the selected editor cannot place inside its field chrome. */
@Composable
fun ReportUnsupportedInputAdornments(props: ComposeJsonFormsRendererProps, variant: String) {
    val control = props.uischema as? ControlElement ?: return
    val edges = listOf("startAdornment", "endAdornment").filter { edge ->
        when (val value = control.options[edge]) {
            null -> false
            is JsonArray -> value.isNotEmpty()
            is JsonPrimitive -> !value.isString || value.content.isNotEmpty()
            else -> true
        }
    }
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val i18n = props.state.jsonforms.i18n
    val message = translateRendererText(i18n.translate, i18n.locale, "adornment.unsupported",
        "Input adornments are unsupported for the selected editor.", adornmentDiagnosticLocales)
    LaunchedEffect(reporter, props.path, control.scope, variant, edges, message) {
        if (reporter != null && edges.isNotEmpty()) reporter.report(UiDiagnostic(
            code = "adornment.unsupported",
            severity = UiDiagnosticSeverity.Warning,
            message = message,
            details = mapOf("path" to props.path, "scope" to control.scope,
                "variant" to variant, "edges" to edges),
        ))
    }
}
