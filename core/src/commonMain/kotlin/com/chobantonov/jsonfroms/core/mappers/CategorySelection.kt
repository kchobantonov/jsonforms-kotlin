package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import kotlinx.serialization.json.JsonPrimitive

data class InitialCategorySelection(
    val originalIndex: Int,
    val diagnostic: UiDiagnostic? = null,
)

/** Only a JSON boolean opts into vertical category or step navigation. */
fun isVerticalCategorization(categorization: CategorizationElement): Boolean =
    (categorization.options["vertical"] as? JsonPrimitive)
        ?.takeUnless { it.isString }?.content == "true"

private data class CategoryPositionKey(val originalIndex: Int)

/** Unique names survive reordering; unnamed Categories follow their authored position. */
fun categorySelectionKey(
    categorization: CategorizationElement,
    category: CategoryElement,
): Any {
    val name = category.name?.takeIf(String::isNotBlank)
    if (name != null) {
        val sameName = categorization.elements.filterIsInstance<CategoryElement>()
            .filter { it.name == name }
        if (sameName.size == 1) return name
        if (sameName.count { it == category } == 1) return category
    }
    return CategoryPositionKey(categoryOriginalIndex(categorization, category))
}

/** An authored index survives visibility filtering, unlike a visible-list index. */
fun categoryOriginalIndex(
    categorization: CategorizationElement,
    category: CategoryElement,
): Int = categorization.elements.indexOfFirst { it === category }

/** Duplicate names are ambiguous only within their own Categorization. */
fun duplicateCategoryNames(categorization: CategorizationElement): Set<String> =
    categorization.elements.filterIsInstance<CategoryElement>()
        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
        .groupingBy { it }.eachCount().filterValues { it > 1 }.keys

fun resolveInitialCategorySelection(
    categorization: CategorizationElement,
    visible: List<CategoryElement>,
): InitialCategorySelection {
    val initial = (categorization.options["initial"] as? JsonPrimitive)
        ?.takeIf { it.isString }?.content?.takeIf(String::isNotEmpty)
    val ambiguous = initial != null && initial in duplicateCategoryNames(categorization)
    val target = initial?.takeUnless { ambiguous }?.let { name -> visible.firstOrNull { it.name == name } }
    val selected = target ?: visible.firstOrNull()
    return InitialCategorySelection(
        originalIndex = selected?.let { categoryOriginalIndex(categorization, it) } ?: -1,
        diagnostic = if (initial != null && target == null) UiDiagnostic(
            code = if (ambiguous) "categorization.duplicateName" else "categorization.initialNotFound",
            severity = UiDiagnosticSeverity.Warning,
            message = if (ambiguous) "The Category name is ambiguous; selecting the first visible Category."
                else "The initial Category is unavailable; selecting the first visible Category.",
            details = mapOf("initial" to initial, "name" to initial),
        ) else null,
    )
}

/** Return -1 when no Category is visible; otherwise fall back to the first visible one. */
fun resolveVisibleCategoryIndex(
    categorization: CategorizationElement,
    visible: List<CategoryElement>,
    selectedOriginalIndex: Int,
): Int {
    if (visible.isEmpty()) return -1
    val selected = visible.indexOfFirst {
        categoryOriginalIndex(categorization, it) == selectedOriginalIndex
    }
    return if (selected >= 0) selected else 0
}
