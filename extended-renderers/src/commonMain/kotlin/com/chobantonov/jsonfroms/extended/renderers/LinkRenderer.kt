package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateScopedText
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.LinkElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.util.isAllowedUrl
import com.chobantonov.jsonfroms.core.util.resolveUrlPolicy
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi

val linkTester: RankedTester = rankWith(2, uiTypeIs("Link"))

internal fun linkRel(target: String?, authored: String?): String? {
    val tokens = authored.orEmpty().trim().split(Regex("\\s+")).filter(String::isNotEmpty).toMutableList()
    if (target == "_blank") {
        if (tokens.none { it.equals("noopener", ignoreCase = true) }) tokens += "noopener"
        if (tokens.none { it.equals("noreferrer", ignoreCase = true) }) tokens += "noreferrer"
    }
    return tokens.takeIf(List<String>::isNotEmpty)?.joinToString(" ")
}

internal expect fun openPortableLink(url: String, target: String, rel: String?, uriHandler: UriHandler)

internal fun linkRenderer(cupertino: Boolean): ComposeJsonFormsRenderer =
    ComposeJsonFormsRenderer { props -> Link(props, cupertino) }

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun Link(props: ComposeJsonFormsRendererProps, cupertino: Boolean) {
    val element = props.uischema as? LinkElement ?: return
    if (!Runtime.isVisible(element, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val label = translateScopedText(
        props.state.jsonforms.i18n.translate, element.i18n, "label", element.label ?: element.href,
    )
    val policy = resolveUrlPolicy(props.config)
    val validTarget = element.target == null || element.target in setOf("_self", "_blank", "_parent", "_top")
    val navigable = element.href.isNotEmpty() && validTarget && isAllowedUrl(element.href, policy)
    val diagnosticCode = when {
        !validTarget -> "link.invalidTarget"
        element.href.isNotEmpty() && !isAllowedUrl(element.href, policy) -> "link.urlRefused"
        else -> null
    }
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val diagnosticMessage = diagnosticCode?.let { code -> translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale, code,
        if (code == "link.urlRefused") "Link destination refused by URL policy" else "Link target is unsupported",
        presentationLocaleBundles,
    ) }
    LaunchedEffect(reporter, diagnosticCode, props.path) {
        if (reporter != null && diagnosticCode != null) reporter.report(UiDiagnostic(
            code = diagnosticCode,
            severity = UiDiagnosticSeverity.Warning,
            message = diagnosticMessage.orEmpty(),
            details = mapOf("path" to props.path),
        ))
    }
    if (!navigable) {
        if (cupertino) CupertinoText(label) else Text(label)
        return
    }
    val uriHandler = LocalUriHandler.current
    val onClick = {
        openPortableLink(element.href, element.target ?: "_self", linkRel(element.target, element.rel), uriHandler)
    }
    if (cupertino) {
        CupertinoButton(
            onClick = onClick,
            colors = CupertinoButtonDefaults.plainButtonColors(),
        ) { CupertinoText(label) }
    } else {
        TextButton(onClick = onClick) { Text(label) }
    }
}
