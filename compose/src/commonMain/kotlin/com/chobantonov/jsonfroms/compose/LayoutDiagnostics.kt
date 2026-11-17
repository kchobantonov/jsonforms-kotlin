package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.mappers.collectLayoutDiagnostics
import com.chobantonov.jsonfroms.core.mappers.effectiveLayoutChildren
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.util.Runtime

/** Emit author diagnostics once per distinct mounted layout contract. */
@Composable
fun ReportLayoutDiagnostics(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    horizontal: Boolean,
) {
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter ?: return
    val diagnostics = collectLayoutDiagnostics(
        options = props.uischema.options,
        config = props.config,
        elements = elements,
        horizontal = horizontal,
    ).map { it.copy(details = it.details + ("path" to props.path)) } +
        omittedStructuralLayoutDiagnostics(props, elements)
    LaunchedEffect(reporter, diagnostics) {
        diagnostics.forEach(reporter::report)
    }
}

private fun omittedStructuralLayoutDiagnostics(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    indices: List<Int> = emptyList(),
): List<UiDiagnostic> {
    val data = getData(props.state)
    val validator = getValidator(props.state)
    return elements.flatMapIndexed { index, child ->
        val nested = when (child) {
            is HorizontalLayout -> child.elements
            is VerticalLayout -> child.elements
            else -> return@flatMapIndexed emptyList()
        }
        if (!Runtime.isVisible(child, data, validator, props.path, props.config) ||
            effectiveLayoutChildren(nested, data, validator, props.path, props.config).isNotEmpty()) {
            return@flatMapIndexed emptyList()
        }
        val structuralPath = indices + index
        collectLayoutDiagnostics(child.options, props.config, nested, child is HorizontalLayout)
            .map { it.copy(details = it.details + mapOf(
                "path" to props.path,
                "structuralPath" to structuralPath,
            )) } + omittedStructuralLayoutDiagnostics(props, nested, structuralPath)
    }
}
