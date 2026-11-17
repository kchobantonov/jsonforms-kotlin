package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chobantonov.jsonfroms.core.mappers.BoundScopes
import com.chobantonov.jsonfroms.core.i18n.translateCategoryValidationIndicator
import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText
import com.chobantonov.jsonfroms.core.mappers.categoryBoundScopes
import com.chobantonov.jsonfroms.core.mappers.groupContainsData
import com.chobantonov.jsonfroms.core.mappers.resolveDataIndicatorOption
import com.chobantonov.jsonfroms.core.mappers.resolveContainerValidationOptions
import com.chobantonov.jsonfroms.core.models.CategoryElement
import kotlinx.serialization.json.JsonElement

data class CategoryValidationStatus(
    val count: Int?,
    val accessibleName: String,
)

/** One UI-schema traversal supplies both header indicators for a Category. */
@Composable
fun rememberCategoryBoundScopes(category: CategoryElement): BoundScopes =
    remember(category) { categoryBoundScopes(category) }

/** Structural Category scopes include unmounted and rule-hidden descendants. */
@Composable
fun categoryDataPresenceStatus(
    category: CategoryElement,
    config: Map<String, Any?>,
    path: String,
    data: JsonElement,
    boundScopes: BoundScopes,
): String? {
    if (!resolveDataIndicatorOption(category.options, config)) return null
    if (!groupContainsData(boundScopes.controlScopes, data, path)) return null
    return translateGroupRendererText(useJsonForms().state.jsonforms.i18n,
        "group.dataIndicator", "Contains data")
}

/** The same displayable-error index serves active and inactive category headers. */
@Composable
fun categoryValidationStatus(
    category: CategoryElement,
    config: Map<String, Any?>,
    path: String,
    boundScopes: BoundScopes,
): CategoryValidationStatus? {
    val options = resolveContainerValidationOptions(category.options, config)
    if (!options.showIndicator) return null
    val index = useJsonForms().containerErrorIndex
    if (!index.containsAny(boundScopes.validationScopes, path)) return null
    val count = if (options.showCount) index.countAny(boundScopes.validationScopes, path) else null
    val label = translateCategoryValidationIndicator(useJsonForms().state.jsonforms.i18n, count)
    return CategoryValidationStatus(count, label)
}
