package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val enumInArraySchema = JsonSchema(
    buildJsonObject {
        put("type", "array")
        putJsonObject("items") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("plainEnum") {
                    put("type", "string")
                    putJsonArray("enum") {
                        add(kotlinx.serialization.json.JsonPrimitive("foo"))
                        add(kotlinx.serialization.json.JsonPrimitive("bar"))
                    }
                }
                putJsonObject("oneOfEnum") {
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
        }
    },
)

val enumInArrayUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#"),
    ),
)

val enumInArrayData = buildJsonArray {}

val enumInArrayExample = ExampleDescription(
    name = "enumInArray",
    label = "Array containing enums",
    data = enumInArrayData,
    schema = enumInArraySchema,
    uischema = enumInArrayUiSchema,
)
