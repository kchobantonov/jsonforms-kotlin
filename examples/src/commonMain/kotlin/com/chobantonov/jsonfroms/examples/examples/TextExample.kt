package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val textSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("zipCode") {
                put("type", "string")
                put("maxLength", 5)
            }
            putJsonObject("zipCodeWithoutTrim") {
                put("type", "string")
                put("maxLength", 5)
            }
            putJsonObject("zipCodeWithoutRestrict") {
                put("type", "string")
                put("maxLength", 5)
            }
        }
    },
)

val textUiSchema = VerticalLayout(
    elements = listOf(
        HorizontalLayout(
            elements = listOf(
                ControlElement(
                    scope = "#/properties/zipCode",
                    label = StringControlLabel("ZIP Code (with trim and restrict options)"),
                    options = mapOf(
                        "trim" to JsonPrimitive(true),
                        "restrict" to JsonPrimitive(true),
                    ),
                ),
                ControlElement(
                    scope = "#/properties/zipCodeWithoutTrim",
                    label = StringControlLabel("ZIP Code (without trimming)"),
                    options = mapOf(
                        "trim" to JsonPrimitive(false),
                        "restrict" to JsonPrimitive(true),
                    ),
                ),
                ControlElement(
                    scope = "#/properties/zipCodeWithoutRestrict",
                    label = StringControlLabel("ZIP Code (without restricting)"),
                    options = mapOf(
                        "trim" to JsonPrimitive(true),
                        "restrict" to JsonPrimitive(false),
                    ),
                ),
            ),
        ),
    ),
)

val textData = buildJsonObject {
    put("zipCode", "12345")
    put("zipCodeWithoutTrim", "12345678")
    put("zipCodeWithoutRestrict", "12345678")
}

val textExample = ExampleDescription(
    name = "text",
    label = "Text Control Options",
    data = textData,
    schema = textSchema,
    uischema = textUiSchema,
)
