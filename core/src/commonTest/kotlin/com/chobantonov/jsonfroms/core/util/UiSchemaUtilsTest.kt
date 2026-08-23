package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class UiSchemaUtilsTest {
    @Test
    fun parsesNestedDetailLayoutFromControlOptions() {
        val control = ControlElement(
            scope = "#/properties/comments",
            options = mapOf(
                "detail" to buildJsonObject {
                    put("type", "VerticalLayout")
                    putJsonArray("elements") {
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/message")
                                put("options", buildJsonObject { put("trim", true) })
                            },
                        )
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/date")
                            },
                        )
                    }
                },
            ),
        )

        val detail = assertIs<VerticalLayout>(getDetailUiSchema(control))

        assertEquals(2, detail.elements.size)
        assertEquals("#/properties/message", assertIs<ControlElement>(detail.elements[0]).scope)
        assertEquals(JsonPrimitive(true), detail.elements[0].options["trim"])
        assertEquals("#/properties/date", assertIs<ControlElement>(detail.elements[1]).scope)
    }
}
