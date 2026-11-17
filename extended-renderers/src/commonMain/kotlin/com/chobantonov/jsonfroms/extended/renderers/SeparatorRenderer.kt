package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateScopedText
import com.chobantonov.jsonfroms.core.mappers.isVerticalSeparator
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Runtime
import com.slapps.cupertino.CupertinoHorizontalDivider
import com.slapps.cupertino.CupertinoVerticalDivider
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val separatorTester: RankedTester = rankWith(2, uiTypeIs("Separator"))

internal fun separatorAccessibleName(locale: String?, vertical: Boolean): String {
    val language = locale?.substringBefore('-')?.substringBefore('_')?.lowercase()
    return when (language) {
        "bg" -> if (vertical) "Вертикален разделител" else "Хоризонтален разделител"
        else -> if (vertical) "Vertical separator" else "Horizontal separator"
    }
}

internal fun separatorRenderer(cupertino: Boolean): ComposeJsonFormsRenderer =
    ComposeJsonFormsRenderer { props -> Separator(props, cupertino) }

@Composable
private fun Separator(props: ComposeJsonFormsRendererProps, cupertino: Boolean) {
    val element = props.uischema as? SeparatorElement ?: return
    if (!Runtime.isVisible(element, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val vertical = isVerticalSeparator(element)
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val invalidVertical = element.options.containsKey("vertical") &&
        (element.options["vertical"] as? JsonPrimitive)?.booleanOrNull == null
    LaunchedEffect(reporter, invalidVertical, props.path) {
        if (reporter != null && invalidVertical) reporter.report(UiDiagnostic(
            code = "separator.invalidVertical",
            severity = UiDiagnosticSeverity.Warning,
            message = "Separator options.vertical must be a boolean",
            details = mapOf("path" to props.path),
        ))
    }
    val locale = props.state.jsonforms.i18n.locale
    val fallback = separatorAccessibleName(locale, vertical)
    val name = translateScopedText(
        props.state.jsonforms.i18n.translate, null,
        if (vertical) "separator.vertical" else "separator.horizontal", fallback,
    )
    val modifier = (if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
        .semantics { contentDescription = name }
    if (cupertino) {
        if (vertical) CupertinoVerticalDivider(modifier) else CupertinoHorizontalDivider(modifier)
    } else {
        if (vertical) VerticalDivider(modifier) else HorizontalDivider(modifier)
    }
}
