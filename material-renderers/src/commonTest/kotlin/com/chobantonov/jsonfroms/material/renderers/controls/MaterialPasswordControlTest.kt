package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.TesterContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MaterialPasswordControlTest {
    @Test
    fun matchesPasswordFormattedStringControl() {
        val schema = objectSchemaWithProperty(format = "password")

        assertEquals(
            2,
            materialPasswordControlTester(
                ControlElement(scope = "#/properties/secret"),
                schema,
                TesterContext(rootSchema = schema),
            ),
        )
    }

    @Test
    fun doesNotMatchOrdinaryStringControl() {
        val schema = objectSchemaWithProperty(format = null)

        assertEquals(
            NOT_APPLICABLE,
            materialPasswordControlTester(
                ControlElement(scope = "#/properties/secret"),
                schema,
                TesterContext(rootSchema = schema),
            ),
        )
    }

    private fun objectSchemaWithProperty(format: String?): JsonSchema = JsonSchema(buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("secret", buildJsonObject {
                put("type", "string")
                if (format != null) {
                    put("format", format)
                }
            })
        })
    })
}
