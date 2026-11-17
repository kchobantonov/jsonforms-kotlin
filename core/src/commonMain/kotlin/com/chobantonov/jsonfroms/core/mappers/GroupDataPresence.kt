package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.Categorization
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.toDataPathSegments
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class BoundScopes(
    val controlScopes: List<List<String>>,
    val validationScopes: List<List<String>>,
)

/** One authored-schema traversal serves data presence and validation aggregation. */
fun groupBoundScopes(group: GroupElement): BoundScopes = collectBoundScopes(group.elements)

/** Direct triggers belong to this layout; any authored descendant can represent their missing target. */
fun directLayoutBoundScopes(elements: List<UiSchemaElement>): BoundScopes {
    val controls = elements.filterIsInstance<ControlElement>().map { toDataPathSegments(it.scope) }
    return BoundScopes(controls, collectBoundScopes(elements).validationScopes)
}

/** Inactive Category content is still traversed for header validation. */
fun categoryBoundScopes(category: CategoryElement): BoundScopes = collectBoundScopes(category.elements)

/** Relative validation paths exposed by one authored detail form. */
fun detailValidationScopes(
    detail: UiSchemaElement,
    include: (UiSchemaElement) -> Boolean = { true },
): List<List<String>> = collectBoundScopes(listOf(detail), include).validationScopes

private fun collectBoundScopes(
    elements: List<UiSchemaElement>,
    include: (UiSchemaElement) -> Boolean = { true },
): BoundScopes {
    val controls = mutableListOf<List<String>>()
    val validation = mutableListOf<List<String>>()
    fun visit(element: UiSchemaElement) {
        if (!include(element)) return
        when (element) {
            is ControlElement -> {
                val scope = toDataPathSegments(element.scope)
                controls += scope
                validation += scope
            }
            is ListWithDetailElement -> validation += toDataPathSegments(element.scope)
            is Layout -> element.elements.forEach(::visit)
            is Categorization -> element.elements.forEach(::visit)
            is UnknownUiSchemaElement -> element.elements.forEach(::visit)
            else -> Unit
        }
    }
    elements.forEach(::visit)
    return BoundScopes(controls, validation.distinct())
}

fun groupControlScopes(group: GroupElement): List<List<String>> = groupBoundScopes(group).controlScopes

/** The Group is structural: bindings resolve from the current form or array-item context. */
fun groupContainsData(
    group: GroupElement,
    rootData: JsonElement,
    currentPath: String = "",
): Boolean = groupContainsData(groupControlScopes(group), rootData, currentPath)

fun groupContainsData(
    scopes: List<List<String>>,
    rootData: JsonElement,
    currentPath: String = "",
): Boolean {
    val context = JsonPath.get(rootData, currentPath) ?: return false
    return scopes.any { scope ->
        var value: JsonElement? = context
        scope.forEach { segment ->
            value = when (val parent = value) {
                is JsonObject -> parent[segment]
                is JsonArray -> segment.toIntOrNull()?.takeIf { it >= 0 }?.let(parent::getOrNull)
                else -> null
            }
        }
        containsMeaningfulData(value)
    }
}

fun containsMeaningfulData(value: JsonElement?): Boolean = when (value) {
    null, JsonNull -> false
    is JsonObject -> value.values.any(::containsMeaningfulData)
    is JsonArray -> value.any(::containsMeaningfulData)
    is JsonPrimitive -> !value.isString || value.content.isNotBlank()
}
