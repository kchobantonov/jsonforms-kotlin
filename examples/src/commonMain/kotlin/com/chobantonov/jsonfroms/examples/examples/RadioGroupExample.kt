package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val radioGroupSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("exampleRadioEnum") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("One"))
                    add(JsonPrimitive("Two"))
                    add(JsonPrimitive("Three"))
                }
            }
            putJsonObject("exampleRadioOneOfEnum") {
                put("type", "string")
                putJsonArray("oneOf") {
                    add(buildJsonObject {
                        put("const", "foo")
                        put("title", "Foo")
                    })
                    add(buildJsonObject {
                        put("const", "bar")
                        put("title", "Bar")
                    })
                    add(buildJsonObject {
                        put("const", "foobar")
                        put("title", "FooBar")
                    })
                }
            }
        }
    },
)

val radioGroupUiSchema = VerticalLayout(
    elements = listOf(
        GroupElement(
            label = "Simple enum",
            elements = listOf(
                ControlElement(
                    scope = "#/properties/exampleRadioEnum",
                    options = mapOf(
                        "format" to JsonPrimitive("radio"),
                        "orientation" to JsonPrimitive("vertical"),
                    ),
                ),
            ),
        ),
        GroupElement(
            label = "One of Enum",
            elements = listOf(
                ControlElement(
                    scope = "#/properties/exampleRadioOneOfEnum",
                    options = mapOf("format" to JsonPrimitive("radio")),
                ),
            ),
        ),
    ),
)

val radioGroupData = buildJsonObject {}

val radioGroupExample = ExampleDescription(
    name = "radio-group",
    label = "Radio Group",
    data = radioGroupData,
    schema = radioGroupSchema,
    uischema = radioGroupUiSchema,
)
