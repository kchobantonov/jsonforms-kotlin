package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class ContainerValidationOptionsTest {
    @Test fun categoryDefaultsOffAndCountRequiresItsOwnOption() {
        val defaults = resolveContainerValidationOptions(emptyMap(), emptyMap())
        assertFalse(defaults.showIndicator)
        assertFalse(defaults.showCount)
        val marker = resolveContainerValidationOptions(
            mapOf("showValidationIndicator" to JsonPrimitive(true)), emptyMap(),
        )
        assertTrue(marker.showIndicator)
        assertFalse(marker.showCount)
    }

    @Test fun localFalseWinsOverNamespacedGlobalTrue() {
        val global = mapOf("jsonformsExtended" to mapOf(
            "showValidationIndicator" to true,
            "showValidationIndicatorCount" to true,
        ))
        val result = resolveContainerValidationOptions(mapOf(
            "showValidationIndicator" to JsonPrimitive(false),
            "showValidationIndicatorCount" to JsonPrimitive(false),
        ), global)
        assertFalse(result.showIndicator)
        assertFalse(result.showCount)
        assertTrue(resolveContainerValidationOptions(emptyMap(), global).showCount)
    }

    @Test fun categoryCollectsNestedControlAndListBindings() {
        val category = CategoryElement(label = "Details", elements = listOf(
            HorizontalLayout(elements = listOf(
                ControlElement(scope = "#/properties/a"),
                ListWithDetailElement(scope = "#/properties/items"),
            )),
        ))
        assertEquals(listOf(listOf("a"), listOf("items")),
            categoryBoundScopes(category).validationScopes)
    }
}
