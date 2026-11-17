package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.compose.currentJsonFormsItem
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.util.resolveUrlPolicy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import com.slapps.cupertino.CupertinoText

val markupLabelTester: RankedTester = { ui, _, _ ->
    val label = ui as? LabelElement
    val markup = label?.options?.get("markup")
    val interpolate = (label?.options?.get("interpolate") as? JsonPrimitive)?.booleanOrNull == true
    if (interpolate || (markup != null && (markup as? JsonPrimitive)?.content != "plain")) 2 else -1
}

internal fun markupLabelRenderer(cupertino: Boolean) = ComposeJsonFormsRenderer { props ->
    MarkupLabel(props, cupertino)
}

private fun member(value: Any?, key: String): Any? = when (value) {
    is Map<*, *> -> value[key]
    is JsonObject -> value[key]
    else -> null
}

private fun booleanValue(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is JsonPrimitive -> value.booleanOrNull
    else -> null
}

private fun markupConfig(config: Map<String, Any?>): Any? = member(member(config, "jsonformsExtended"), "markup")

internal fun markdownEnabled(config: Map<String, Any?>): Boolean =
    booleanValue(member(member(markupConfig(config), "markdown"), "enabled")) != false

internal fun markdownProfile(config: Map<String, Any?>): MarkdownProfile =
    if ((member(member(markupConfig(config), "markdown"), "profile") as? JsonPrimitive)?.content == "extended" ||
        member(member(markupConfig(config), "markdown"), "profile") == "extended") MarkdownProfile.Extended
    else MarkdownProfile.Basic

internal fun interpolationDataEnabled(config: Map<String, Any?>): Boolean =
    booleanValue(member(member(config["jsonformsExtended"], "dynamicValues"), "enabled")) == true

internal fun markupTypographyEnabled(label: LabelElement, config: Map<String, Any?>): Boolean =
    booleanValue(label.options["typography"]) ?: booleanValue(member(markupConfig(config), "typography")) ?: true

@Composable
private fun MarkupLabel(props: ComposeJsonFormsRendererProps, cupertino: Boolean) {
    val label = props.uischema as? LabelElement ?: return
    if (!Runtime.isVisible(label, getData(props.state), getValidator(props.state), props.path, props.config)) return
    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    val content = deriveLabelForUISchemaElement(label, translator).orEmpty()
    val requested = (label.options["markup"] as? JsonPrimitive)?.takeIf { it.isString }?.content
    val interpolates = (label.options["interpolate"] as? JsonPrimitive)?.booleanOrNull == true
    val diagnostic = when {
        label.options.containsKey("markup") && requested != "markdown" && requested != "plain" -> "markup.unsupported"
        requested == "markdown" && !markdownEnabled(props.config) -> "markup.markdownDisabled"
        else -> null
    }
    val markdown = requested == "markdown" && diagnostic == null
    val locale = props.state.jsonforms.i18n.locale ?: "en"
    val data = getData(props.state)
    val hostContext = useJsonForms().hostContext
    val item = currentJsonFormsItem()
    val interpolated = if (interpolates) remember(content, label.options, data, locale, props.config, hostContext, item, translator, markdown) {
        interpolateLabel(content, label.options["textParams"] as? JsonObject, data, locale,
            interpolationDataEnabled(props.config), markdown,
            translate = { key, fallback -> translator.translate(key, fallback, emptyMap()) ?: fallback },
            config = jsonSafeConfig(props.config), context = hostContext, item = item)
    } else null
    val resolvedContent = interpolated?.text ?: content
    val document = if (markdown) remember(resolvedContent, props.config) {
        parseSafeMarkdown(resolvedContent, resolveUrlPolicy(props.config), markdownProfile(props.config))
    } else null
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    LaunchedEffect(reporter, diagnostic, document?.refusedLinks, interpolated?.failures, interpolated?.warnings, props.path) {
        if (diagnostic != null) reporter?.report(UiDiagnostic(
            code = diagnostic,
            severity = UiDiagnosticSeverity.Warning,
            message = if (diagnostic == "markup.unsupported") "Text markup format is unsupported"
                else "Markdown is disabled by the form configuration",
            details = mapOf("path" to props.path),
        ))
        val refusedLinks = document?.refusedLinks ?: 0
        if (refusedLinks > 0) reporter?.report(UiDiagnostic(
            code = "markup.urlRefused",
            severity = UiDiagnosticSeverity.Error,
            message = "Markdown link destination refused by URL policy",
            details = mapOf("path" to props.path, "count" to refusedLinks),
        ))
        val interpolationFailures = interpolated?.failures.orEmpty()
        if (interpolationFailures.isNotEmpty()) reporter?.report(UiDiagnostic(
            code = "markup.interpolationFailed",
            severity = UiDiagnosticSeverity.Error,
            message = "Label interpolation failed",
            details = mapOf("path" to props.path, "failures" to interpolationFailures),
        ))
        val interpolationWarnings = interpolated?.warnings.orEmpty()
        if (interpolationWarnings.isNotEmpty()) reporter?.report(UiDiagnostic(
            code = "markup.escapedExpression",
            severity = UiDiagnosticSeverity.Warning,
            message = "Double-braced dynamic paths render as literal text",
            details = mapOf("path" to props.path, "warnings" to interpolationWarnings),
        ))
    }
    if (resolvedContent.isBlank() && diagnostic == null && interpolated?.failures.isNullOrEmpty()) return
    val typography = markupTypographyEnabled(label, props.config)
    Column {
        if (resolvedContent.isNotBlank()) {
            if (document == null) MarkdownText(AnnotatedString(resolvedContent), cupertino, typography)
            else MarkdownContent(document, cupertino, typography)
        }
        if (diagnostic != null) {
            val fallback = if (diagnostic == "markup.unsupported") "Markup format unsupported"
                else "Markdown disabled"
            val message = translateRendererText(props.state.jsonforms.i18n.translate, locale,
                diagnostic, fallback, markupLabelLocaleBundles)
            if (cupertino) CupertinoText(message) else Text(message)
        }
        if ((document?.refusedLinks ?: 0) > 0) {
            val message = translateRendererText(props.state.jsonforms.i18n.translate, locale,
                "markup.urlRefused", "Link blocked by URL policy", markupLabelLocaleBundles)
            if (cupertino) CupertinoText(message) else Text(message)
        }
        if (!interpolated?.failures.isNullOrEmpty()) {
            val message = translateRendererText(props.state.jsonforms.i18n.translate, locale,
                "markup.interpolationFailed", "Some label values could not be resolved", markupLabelLocaleBundles)
            if (cupertino) CupertinoText(message) else Text(message)
        }
    }
}

@Composable
private fun MarkdownText(text: AnnotatedString, cupertino: Boolean, typography: Boolean) {
    if (typography && cupertino) CupertinoText(text)
    else if (typography) Text(text, style = MaterialTheme.typography.titleLarge)
    else BasicText(text)
}

@Composable
private fun MarkdownContent(document: SafeMarkdownDocument, cupertino: Boolean, typography: Boolean) {
    if (document.blocks.none { it !is SafeMarkdownBlock.Text }) {
        MarkdownText(document.text, cupertino, typography)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        document.blocks.forEach { block -> MarkdownBlock(block, cupertino, typography) }
    }
}

@Composable
private fun MarkdownBlock(block: SafeMarkdownBlock, cupertino: Boolean, typography: Boolean) {
    when (block) {
        is SafeMarkdownBlock.Text -> MarkdownText(block.content, cupertino, typography)
        is SafeMarkdownBlock.Heading -> {
            val size = when (block.level) { 1 -> 28.sp; 2 -> 24.sp; 3 -> 20.sp; else -> 18.sp }
            val style = TextStyle(fontSize = size, fontWeight = FontWeight.Bold)
            val headingModifier = Modifier.semantics { heading() }
            if (cupertino) CupertinoText(block.content, modifier = headingModifier, style = style)
            else Text(block.content, modifier = headingModifier, style = style)
        }
        is SafeMarkdownBlock.Quote -> Row {
            MarkdownText(AnnotatedString("│"), cupertino, typography)
            Column(Modifier.padding(start = 12.dp)) {
                block.blocks.forEach { MarkdownBlock(it, cupertino, typography) }
            }
        }
        is SafeMarkdownBlock.Code -> BasicText(block.content, style = TextStyle(fontFamily = FontFamily.Monospace))
        is SafeMarkdownBlock.Table -> Column(Modifier.semantics {
            collectionInfo = CollectionInfo(rowCount = block.rows.size,
                columnCount = block.rows.maxOfOrNull { it.size } ?: 0)
        }) {
            block.rows.forEachIndexed { rowIndex, cells ->
                Row {
                    cells.forEachIndexed { columnIndex, cell ->
                        Column(Modifier.weight(1f).semantics {
                            collectionItemInfo = CollectionItemInfo(rowIndex, 1, columnIndex, 1)
                            if (rowIndex == 0) heading()
                        }.padding(4.dp)) {
                            MarkdownText(cell, cupertino, typography)
                        }
                    }
                }
            }
        }
        is SafeMarkdownBlock.ListBlock -> Column(
            modifier = Modifier.semantics {
                collectionInfo = CollectionInfo(rowCount = block.items.size, columnCount = 1)
            },
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            block.items.forEachIndexed { index, item ->
                Row(Modifier.semantics {
                    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                }) {
                    MarkdownText(AnnotatedString(item.marker), cupertino, typography)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.blocks.forEach { child -> MarkdownBlock(child, cupertino, typography) }
                    }
                }
            }
        }
    }
}
