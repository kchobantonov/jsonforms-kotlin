package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chobantonov.jsonfroms.core.mappers.categorySelectionKey
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement

private class CategorySetSession(var keys: Set<Any>, var version: Int = 0)

/** Retain local selection through edits to the same category set, and restart for a replacement. */
@Composable
internal fun rememberCategorySetVersion(categorization: CategorizationElement, path: String): Int {
    val keys = categorization.elements.filterIsInstance<CategoryElement>().map { category ->
        categorySelectionKey(categorization, category)
    }.toSet()
    val session = remember(path) { CategorySetSession(keys) }
    if (session.keys != keys) {
        if (session.keys.intersect(keys).isEmpty()) session.version++
        session.keys = keys
    }
    return session.version
}
