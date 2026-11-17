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
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.mappers.categorySelectionKey
import com.chobantonov.jsonfroms.core.mappers.duplicateCategoryNames
import com.chobantonov.jsonfroms.core.mappers.resolveInitialCategorySelection
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement

data class CategorySelectionState(
    val selectedIndex: Int,
    val select: (Int) -> Unit,
)

@Composable
fun rememberCategorySelectionState(
    categorization: CategorizationElement,
    visible: List<CategoryElement>,
    config: Map<String, Any?>,
    path: String,
): CategorySelectionState {
    val initial = resolveInitialCategorySelection(categorization, visible)
    var selectedKey by remember(path) {
        val category = categorization.elements.getOrNull(initial.originalIndex) as? CategoryElement
        mutableStateOf<Any?>(category?.let { categorySelectionKey(categorization, it) })
    }
    val matchingIndex = visible.indexOfFirst {
        categorySelectionKey(categorization, it) == selectedKey
    }
    val selectedIndex = when {
        visible.isEmpty() -> -1
        matchingIndex >= 0 -> matchingIndex
        else -> 0
    }
    val effectiveKey = visible.getOrNull(selectedIndex)?.let {
        categorySelectionKey(categorization, it)
    }
    SideEffect {
        if (selectedKey != effectiveKey) selectedKey = effectiveKey
    }

    val reporter = config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val currentReporter by rememberUpdatedState(reporter)
    val initialDiagnostic = initial.diagnostic
    val diagnostics = listOfNotNull(initialDiagnostic) + duplicateCategoryNames(categorization)
        .filterNot { name -> initialDiagnostic?.code == "categorization.duplicateName" &&
            initialDiagnostic.details["name"] == name }
        .sorted().map { name -> UiDiagnostic(
            code = "categorization.duplicateName",
            severity = UiDiagnosticSeverity.Warning,
            message = "The Category name is ambiguous within this Categorization.",
            details = mapOf("name" to name),
        ) }
    LaunchedEffect(diagnostics, reporter != null, path) {
        diagnostics.forEach {
            currentReporter?.report(it.copy(details = it.details + ("path" to path)))
        }
    }
    return CategorySelectionState(selectedIndex) { visibleIndex ->
        visible.getOrNull(visibleIndex)?.let {
            selectedKey = categorySelectionKey(categorization, it)
        }
    }
}
