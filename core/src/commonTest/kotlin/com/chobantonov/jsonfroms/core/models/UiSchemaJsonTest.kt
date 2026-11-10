package com.chobantonov.jsonfroms.core.models

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UiSchemaJsonTest {
    @Test
    fun parsesCategoriesLabelsAndRules() {
        val schema = Json.parseToJsonElement("""
            {
              "type": "Categorization",
              "elements": [{
                "type": "Category", "label": "Contact", "elements": [{
                  "type": "Control", "scope": "#/properties/email",
                  "label": {"text": "Email", "show": true},
                  "rule": {"effect": "SHOW", "condition": {
                    "scope": "#/properties/newsletter", "expectedValue": true
                  }}
                }]
              }]
            }
        """) as JsonObject

        val category = assertIs<CategoryElement>(assertIs<CategorizationElement>(schema.toUiSchemaElement()).elements.single())
        val control = assertIs<ControlElement>(category.elements.single())
        assertEquals("#/properties/email", control.scope)
        assertEquals("Email", assertIs<DescriptionControlLabel>(control.label).value.text)
        assertEquals(RuleEffect.SHOW, control.rule?.effect)
        assertIs<LeafCondition>(control.rule?.condition)
    }

    @Test
    fun rejectsMalformedNestedElements() {
        val schema = Json.parseToJsonElement("""
            {"type":"VerticalLayout","elements":[{"type":"Control"}]}
        """) as JsonObject
        assertFailsWith<IllegalArgumentException> { schema.toUiSchemaElement() }
    }
}
