package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.translateScopedText
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.ImageViewElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.ImageSourceDiagnostic
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.util.resolveImageSource
import com.chobantonov.jsonfroms.core.util.resolveUrlPolicy
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

val imageViewTester: RankedTester = rankWith(2, uiTypeIs("ImageView"))

internal fun imageViewRenderer(cupertino: Boolean) = ComposeJsonFormsRenderer { props -> ImageView(props, cupertino) }

internal fun imageDimensionPixels(value: JsonElement?): Float? {
    val primitive = value as? JsonPrimitive ?: return null
    val number = primitive.content.removeSuffix("px").toFloatOrNull() ?: return null
    return number.takeIf { it.isFinite() && it > 0f }
}

@Composable
private fun ImageView(props: ComposeJsonFormsRendererProps, cupertino: Boolean) {
    val element = props.uischema as? ImageViewElement ?: return
    val data = getData(props.state)
    if (!Runtime.isVisible(element, data, getValidator(props.state), props.path, props.config)) return
    val resolution = resolveImageSource(element, props.schema, props.rootSchema, data, props.path,
        resolveUrlPolicy(props.config))
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val diagnosticDefault = when (resolution.diagnostic) {
        ImageSourceDiagnostic.NoSource -> "ImageView needs src or scope"
        ImageSourceDiagnostic.ScopeNotString -> "Image scope does not permit a string source"
        ImageSourceDiagnostic.NonStringSource -> "Image source data is not a string"
        ImageSourceDiagnostic.UrlRefused -> "Image source refused by URL policy"
        null -> null
    }
    val diagnosticMessage = diagnosticDefault?.let { translateRendererText(
        props.state.jsonforms.i18n.translate, props.state.jsonforms.i18n.locale,
        requireNotNull(resolution.diagnostic).code, it, presentationLocaleBundles,
    ) }
    LaunchedEffect(reporter, resolution.diagnostic, props.path) {
        resolution.diagnostic?.let { diagnostic ->
            reporter?.report(UiDiagnostic(
                code = diagnostic.code,
                severity = UiDiagnosticSeverity.Error,
                message = diagnosticMessage.orEmpty(),
                details = mapOf("path" to props.path),
            ))
        }
    }
    val missingAltMessage = translateRendererText(props.state.jsonforms.i18n.translate,
        props.state.jsonforms.i18n.locale, "image.missingAlt", "Image alternative text is missing",
        presentationLocaleBundles)
    LaunchedEffect(reporter, element.alt, props.path) {
        if (element.alt == null) reporter?.report(UiDiagnostic(
            code = "image.missingAlt",
            severity = UiDiagnosticSeverity.Warning,
            message = missingAltMessage,
            details = mapOf("path" to props.path),
        ))
    }
    val url = resolution.url
    if (url == null) {
        if (resolution.diagnostic != null) ImageFailureText(props, cupertino,
            blocked = resolution.diagnostic == ImageSourceDiagnostic.UrlRefused)
        return
    }
    val malformedDataUrl = malformedBase64ImageDataUrl(url)
    var loadFailed by remember(url) { mutableStateOf(malformedDataUrl) }
    var retryAttempt by remember(url) { mutableIntStateOf(0) }
    val loadFailedMessage = translateRendererText(props.state.jsonforms.i18n.translate,
        props.state.jsonforms.i18n.locale, "image.loadFailed", "Image source could not be loaded",
        presentationLocaleBundles)
    LaunchedEffect(reporter, loadFailed, url, props.path, retryAttempt) {
        if (loadFailed) reporter?.report(UiDiagnostic(
            code = "image.loadFailed",
            severity = UiDiagnosticSeverity.Error,
            message = loadFailedMessage,
            details = mapOf("path" to props.path),
        ))
    }
    if (loadFailed) {
        ImageFailureText(props, cupertino, blocked = false, onRetry = {
            if (malformedDataUrl) retryAttempt++ else loadFailed = false
        })
        return
    }
    val alt = translateScopedText(props.state.jsonforms.i18n.translate, element.i18n, "alt", element.alt.orEmpty())
    val density = LocalDensity.current
    val authoredWidth = imageDimensionPixels(element.options["width"])?.let { with(density) { it.toDp() } }
    val authoredHeight = imageDimensionPixels(element.options["height"])?.let { with(density) { it.toDp() } }
    AsyncImage(
        model = url,
        contentDescription = alt.takeIf(String::isNotEmpty),
        modifier = Modifier
            .then(if (authoredWidth != null) Modifier.width(authoredWidth) else Modifier.widthIn(max = 512.dp))
            .then(if (authoredHeight != null) Modifier.height(authoredHeight) else Modifier.heightIn(max = 256.dp)),
        contentScale = ContentScale.Fit,
        onError = { loadFailed = true },
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun ImageFailureText(
    props: ComposeJsonFormsRendererProps,
    cupertino: Boolean,
    blocked: Boolean,
    onRetry: (() -> Unit)? = null,
) {
    val message = translateRendererText(props.state.jsonforms.i18n.translate,
        props.state.jsonforms.i18n.locale,
        if (blocked) "image.blocked" else "image.unavailable",
        if (blocked) "Image blocked" else "Image unavailable", presentationLocaleBundles)
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        if (cupertino) CupertinoText(message) else Text(message)
        if (onRetry != null) {
            val retry = translateRendererText(props.state.jsonforms.i18n.translate,
                props.state.jsonforms.i18n.locale, "image.retry", "Retry image", presentationLocaleBundles)
            if (cupertino) CupertinoButton(onClick = onRetry,
                colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText(retry) }
            else TextButton(onClick = onRetry) { Text(retry) }
        }
    }
}
