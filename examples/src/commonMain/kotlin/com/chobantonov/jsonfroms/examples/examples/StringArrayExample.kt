package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val stringArraySchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("comments") {
                put("description", "Description for array of String Type")
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                    put("maxLength", 5)
                }
            }
        }
    },
)

val stringArrayUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/comments"),
    ),
)

val stringArrayData = buildJsonObject {
    put("comments", buildJsonArray {
        add(JsonPrimitive("one string"))
        add(JsonPrimitive("two strings"))
    })
}

val stringArrayExample = ExampleDescription(
    name = "stringArray",
    label = "Array of Strings",
    data = stringArrayData,
    schema = stringArraySchema,
    uischema = stringArrayUiSchema,
)
