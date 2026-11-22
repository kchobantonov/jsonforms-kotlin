package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs

const val ExtendedSlotsConfigKey = "extendedSlots"
const val ExtendedTemplateDiagnosticsConfigKey = "extendedTemplateDiagnostics"

enum class TemplateDiagnosticCode { MissingName, MissingTemplate, DuplicateName, RecursiveTemplate, RecursiveSlot }

data class TemplateDiagnostic(val code: TemplateDiagnosticCode, val name: String?, val path: String)

fun interface ExtendedTemplateDiagnosticReporter {
    fun report(diagnostic: TemplateDiagnostic)
}
val slotTester: RankedTester = rankWith(2, uiTypeIs("Slot"))
val templateTester: RankedTester = rankWith(2, uiTypeIs("Template"))

fun interface ExtendedNamedRenderer {
    @Composable
    fun Render(props: ComposeJsonFormsRendererProps)
}

private data class TemplateContext(
    val slots: Map<String, UiSchemaElement> = emptyMap(),
    val activeNames: Set<String> = emptySet(),
    val activeSlots: Set<String> = emptySet(),
)

private val LocalTemplateContext = compositionLocalOf { TemplateContext() }

internal data class NamedTemplateResolution(
    val template: UiSchemaElement?,
    val diagnostic: TemplateDiagnosticCode? = null,
)

internal fun resolveNamedTemplate(
    name: String?,
    registry: List<JsonFormsUISchemaRegistryEntry>,
    activeNames: Set<String> = emptySet(),
): NamedTemplateResolution {
    if (name.isNullOrEmpty()) return NamedTemplateResolution(null, TemplateDiagnosticCode.MissingName)
    if (name in activeNames) return NamedTemplateResolution(null, TemplateDiagnosticCode.RecursiveTemplate)
    val matches = registry.filter { it.uischema.name == name }
    return when {
        matches.isEmpty() -> NamedTemplateResolution(null, TemplateDiagnosticCode.MissingTemplate)
        matches.size > 1 -> NamedTemplateResolution(null, TemplateDiagnosticCode.DuplicateName)
        else -> NamedTemplateResolution(matches.first().uischema)
    }
}

internal fun mergeNamedSlots(
    inherited: Map<String, UiSchemaElement>,
    supplied: List<UiSchemaElement>,
): Map<String, UiSchemaElement> {
    val named = supplied.mapNotNull { child -> child.name?.let { it to child } }
    return inherited + named.toMap()
}

@Composable
internal fun TemplateContent(props: ComposeJsonFormsRendererProps) {
    val caller = props.uischema as? UnknownUiSchemaElement ?: return
    val name = caller.name
    val context = LocalTemplateContext.current
    val resolution = resolveNamedTemplate(name, props.state.jsonforms.uischemas, context.activeNames)
    val reporter = props.config[ExtendedTemplateDiagnosticsConfigKey] as? ExtendedTemplateDiagnosticReporter
    val diagnostic = resolution.diagnostic?.let { TemplateDiagnostic(it, name, props.path) }
    LaunchedEffect(reporter, diagnostic) {
        if (reporter != null && diagnostic != null) reporter.report(diagnostic)
    }
    val template = resolution.template ?: return
    val resolvedName = name ?: return
    val merged = mergeNamedSlots(context.slots, caller.elements)
    val next = TemplateContext(
        slots = merged,
        activeNames = context.activeNames + resolvedName,
        activeSlots = context.activeSlots,
    )
    CompositionLocalProvider(LocalTemplateContext provides next) {
        DispatchChild(props, template)
    }
}

@Composable
internal fun NamedContent(props: ComposeJsonFormsRendererProps, configKey: String, fallback: String) {
    val element = props.uischema as? UnknownUiSchemaElement ?: return
    val name = element.name ?: fallback
    val context = LocalTemplateContext.current
    val reporter = props.config[ExtendedTemplateDiagnosticsConfigKey] as? ExtendedTemplateDiagnosticReporter
    if (name in context.activeSlots) {
        LaunchedEffect(reporter, name, props.path) {
            reporter?.report(TemplateDiagnostic(TemplateDiagnosticCode.RecursiveSlot, name, props.path))
        }
        return
    }
    val supplied = context.slots[name]
    if (supplied != null) {
        CompositionLocalProvider(LocalTemplateContext provides context.copy(activeSlots = context.activeSlots + name)) {
            DispatchChild(props, supplied)
        }
        return
    }
    @Suppress("UNCHECKED_CAST")
    val renderers = props.config[configKey] as? Map<String, ExtendedNamedRenderer>
    val renderer = renderers?.get(name)
    when {
        renderer != null -> CompositionLocalProvider(
            LocalTemplateContext provides context.copy(activeSlots = context.activeSlots + name),
        ) {
            renderer.Render(props)
        }
        element.elements.isNotEmpty() -> CompositionLocalProvider(
            LocalTemplateContext provides context.copy(activeSlots = context.activeSlots + name),
        ) {
            DispatchChild(props, element.elements.first())
        }
    }
}
