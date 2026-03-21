package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.DescriptionControlLabel
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelDescription
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val numbersSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("price") {
                put("type", "number")
                put("maximum", 100)
                put("minimum", 1)
                put("default", 50)
            }
            putJsonObject("age") {
                put("type", "integer")
            }
            putJsonObject("height") {
                put("type", "number")
            }
        }
    },
)

val numbersUiSchema = VerticalLayout(
    elements = listOf(
        HorizontalLayout(
            elements = listOf(
                ControlElement(
                    scope = "#/properties/price",
                    label = DescriptionControlLabel(LabelDescription(text = "Price")),
                ),
                ControlElement(
                    scope = "#/properties/age",
                ),
                ControlElement(
                    scope = "#/properties/height",
                    options = mapOf("step" to JsonPrimitive(1e-2)),
                ),
                ControlElement(
                    scope = "#/properties/price",
                    label = DescriptionControlLabel(LabelDescription(text = "Price with Slider")),
                    options = mapOf("slider" to JsonPrimitive(true)),
                ),
            ),
        ),
    ),
)

val numbersData = buildJsonObject {}

val numbersExample = ExampleDescription(
    name = "numbers",
    label = "Numbers",
    data = numbersData,
    schema = numbersSchema,
    uischema = numbersUiSchema,
)
