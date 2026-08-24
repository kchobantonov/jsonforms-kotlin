package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.TesterContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MaterialTextControlTest {
    private val schema = JsonSchema(buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("value", buildJsonObject { put("type", "string") })
        })
    })

    @Test
    fun multilineRendererOutranksRegularStringRendererWhenRequested() {
        val control = ControlElement(
            scope = "#/properties/value",
            options = mapOf("multi" to JsonPrimitive(true)),
        )
        val context = TesterContext(rootSchema = schema)

        assertEquals(1, materialTextControlTester(control, schema, context))
        assertEquals(2, materialMultiTextControlTester(control, schema, context))
    }

    @Test
    fun multilineRendererDoesNotMatchOrdinaryStringControl() {
        val control = ControlElement(scope = "#/properties/value")

        assertEquals(
            NOT_APPLICABLE,
            materialMultiTextControlTester(control, schema, TesterContext(rootSchema = schema)),
        )
    }
}
