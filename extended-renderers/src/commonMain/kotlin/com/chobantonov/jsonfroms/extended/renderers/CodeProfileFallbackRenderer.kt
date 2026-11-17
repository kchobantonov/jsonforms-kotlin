package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Column
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
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.util.Runtime
import kotlinx.serialization.json.JsonPrimitive

private const val CodeUnavailableCode = "editor.monacoUnavailable"
private const val CodeUnavailableText = "Monaco editor is unavailable on this host"

val codeProfileFallbackTester: RankedTester = { ui, _, _ ->
    if (ui is ControlElement && (ui.options["format"] as? JsonPrimitive)
            ?.takeIf { it.isString }?.content == "code") 30 else -1
}

fun codeProfileFallbackRenderer(
    nativeRenderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
) = ComposeJsonFormsRenderer { props -> CodeProfileFallback(props, nativeRenderers) }

@Composable
private fun CodeProfileFallback(
    props: ComposeJsonFormsRendererProps,
    nativeRenderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
) {
    val control = props.uischema as? ControlElement ?: return
    if (!Runtime.isVisible(control, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    LaunchedEffect(reporter, control, props.path) {
        reporter?.report(UiDiagnostic(
            code = CodeUnavailableCode,
            severity = UiDiagnosticSeverity.Warning,
            message = CodeUnavailableText,
            details = mapOf("path" to props.path, "format" to "code"),
        ))
    }
    val context = TesterContext(rootSchema = props.rootSchema, config = props.config)
    val native = nativeRenderers.maxByOrNull { it.tester(control, props.schema, context) }
        ?.takeIf { it.tester(control, props.schema, context) >= 0 }
    Column {
        BasicText(
            text = translateRendererText(props.state.jsonforms.i18n.translate,
                props.state.jsonforms.i18n.locale, CodeUnavailableCode, CodeUnavailableText,
                codeProfileLocaleBundles),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        native?.renderer?.Render(props)
    }
}
