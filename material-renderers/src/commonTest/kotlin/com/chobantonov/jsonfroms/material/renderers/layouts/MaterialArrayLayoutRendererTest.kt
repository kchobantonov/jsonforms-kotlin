package com.chobantonov.jsonfroms.material.renderers.layouts

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MaterialArrayLayoutRendererTest {
    @Test
    fun usesConfiguredObjectPropertyAsItemLabel() {
        val item = buildJsonObject {
            put("date", "2001-09-11")
            put("message", "Example")
        }

        assertEquals("2001-09-11", computeChildLabel(item, 0, "date"))
    }

    @Test
    fun fallsBackToNumberedItemLabelWhenPropertyIsMissing() {
        assertEquals("Item 2", computeChildLabel(buildJsonObject {}, 1, "date"))
    }

    @Test
    fun usesStandardElementLabelPropBeforeLegacyChildLabelProp() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("message1", buildJsonObject { put("type", "string") })
                put("message2", buildJsonObject { put("type", "string") })
            })
        })
        val control = ControlElement(
            scope = "#",
            options = mapOf(
                "elementLabelProp" to JsonPrimitive("message2"),
                "childLabelProp" to JsonPrimitive("message1"),
            ),
        )

        assertEquals("message2", resolveChildLabelProp(control, schema))
    }

    @Test
    fun associatesOnlyErrorsBelowTheExactArrayItem() {
        val first = ErrorObject(instancePath = "/comments/1/message", message = "first")
        val tenth = ErrorObject(instancePath = "/comments/10/message", message = "tenth")

        assertEquals(
            listOf(first),
            errorsForArrayItem(listOf(first, tenth), "comments.1"),
        )
    }
}
