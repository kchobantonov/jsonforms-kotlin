package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.mappers.categorySelectionKey
import com.chobantonov.jsonfroms.core.mappers.resolveInitialCategorySelection
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import kotlinx.serialization.json.JsonPrimitive

data class AccordionSelectionState(
    val openIndex: Int,
    val toggle: (Int) -> Unit,
)

@Composable
fun rememberAccordionSelectionState(
    categorization: CategorizationElement,
    visible: List<CategoryElement>,
    collapsed: Boolean,
    config: Map<String, Any?>,
    path: String,
): AccordionSelectionState {
    val version = rememberCategorySetVersion(categorization, path)
    val initial = remember(path, version) { resolveInitialCategorySelection(categorization, visible) }
    val explicitInitial = (categorization.options["initial"] as? JsonPrimitive)
        ?.takeIf { it.isString && it.content.isNotEmpty() } != null
    val first = categorization.elements.getOrNull(initial.originalIndex) as? CategoryElement
    var openKey by remember(path, version) {
        mutableStateOf<Any?>(if (collapsed && !explicitInitial) null else first?.let { categorySelectionKey(categorization, it) })
    }
    var userClosed by remember(path, version) { mutableStateOf(collapsed && !explicitInitial) }
    var previousCollapsed by remember(path, version) { mutableStateOf(collapsed) }
    val matchingIndex = visible.indexOfFirst { categorySelectionKey(categorization, it) == openKey }
    val openIndex = when {
        userClosed || visible.isEmpty() -> -1
        matchingIndex >= 0 -> matchingIndex
        else -> 0
    }
    SideEffect {
        if (!userClosed) openKey = visible.getOrNull(openIndex)?.let { categorySelectionKey(categorization, it) }
    }
    LaunchedEffect(collapsed) {
        if (previousCollapsed != collapsed) {
            previousCollapsed = collapsed
            userClosed = collapsed
            openKey = if (collapsed) null else visible.firstOrNull()?.let { categorySelectionKey(categorization, it) }
        }
    }
    val reporter = config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val currentReporter by rememberUpdatedState(reporter)
    LaunchedEffect(initial.diagnostic, reporter != null, path, version) {
        initial.diagnostic?.let {
            currentReporter?.report(it.copy(details = it.details + ("path" to path)))
        }
    }
    return AccordionSelectionState(openIndex) { index ->
        visible.getOrNull(index)?.let { category ->
            if (index == openIndex) {
                userClosed = true
                openKey = null
            } else {
                userClosed = false
                openKey = categorySelectionKey(categorization, category)
            }
        }
    }
}
