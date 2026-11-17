package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals

class AdditionalPropertiesTextTest {
    @Test
    fun `scoped override wins and generic catalogue supplies portable strings`() {
        val messages = mapOf(
            "inventory.additionalProperties.title" to "Inventory extras",
            "additionalProperties.title" to "Допълнителни свойства",
            "additionalProperties.namePlaceholder" to "Име на свойство",
            "additionalProperties.addTo" to "Добавяне на свойство към Inventory",
        )
        val translator = Translator { key, _, _ -> messages[key] }
        assertEquals("Inventory extras", translateAdditionalPropertiesText(translator,
            "inventory.additionalProperties", "title", "Additional Properties"))
        assertEquals("Име на свойство", translateAdditionalPropertiesText(translator,
            "inventory.additionalProperties", "propertyNameLabel", "Property name"))
        assertEquals("Добавяне на свойство към Inventory", translateAdditionalPropertiesText(translator,
            "inventory.additionalProperties", "addAriaLabel", "Add button"))
        assertEquals("Cancel", translateAdditionalPropertiesText(translator,
            "inventory.additionalProperties", "cancelTooltip", "Cancel"))
        assertEquals("Property name", translateAdditionalPropertiesText(null,
            "inventory.additionalProperties", "propertyNameLabel", "Property name"))
    }
}
