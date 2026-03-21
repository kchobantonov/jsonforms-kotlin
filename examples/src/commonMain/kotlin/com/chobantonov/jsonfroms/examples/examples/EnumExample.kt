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

val enumSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("plainEnum") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("foo"))
                    add(JsonPrimitive("bar"))
                }
            }
            putJsonObject("plainEnumSet") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("foo"))
                    add(JsonPrimitive("bar"))
                }
            }
            putJsonObject("enumWithError") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("foo"))
                    add(JsonPrimitive("bar"))
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
            putJsonObject("oneOfEnumSet") {
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
            putJsonObject("oneOfEnumWithError") {
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
            putJsonObject("constEnum") {
                put("const", "Const Value")
            }
        }
    },
)

val enumUiSchema = VerticalLayout(
    elements = listOf(
        GroupElement(
            label = "Enums",
            elements = listOf(
                ControlElement(scope = "#/properties/constEnum"),
                ControlElement(scope = "#/properties/plainEnum"),
                ControlElement(scope = "#/properties/plainEnumSet"),
                ControlElement(
                    scope = "#/properties/plainEnum",
                    options = mapOf("autocomplete" to JsonPrimitive(false)),
                ),
                ControlElement(
                    scope = "#/properties/plainEnumSet",
                    options = mapOf("autocomplete" to JsonPrimitive(false)),
                ),
                ControlElement(scope = "#/properties/enumWithError"),
            ),
        ),
        GroupElement(
            label = "One of Enums",
            elements = listOf(
                ControlElement(scope = "#/properties/oneOfEnum"),
                ControlElement(scope = "#/properties/oneOfEnumSet"),
                ControlElement(
                    scope = "#/properties/oneOfEnum",
                    options = mapOf("autocomplete" to JsonPrimitive(false)),
                ),
                ControlElement(
                    scope = "#/properties/oneOfEnumSet",
                    options = mapOf("autocomplete" to JsonPrimitive(false)),
                ),
                ControlElement(scope = "#/properties/oneOfEnumWithError"),
            ),
        ),
    ),
)

val enumData = buildJsonObject {
    put("plainEnumSet", "foo")
    put("enumWithError", "bogus")
    put("oneOfEnumSet", "bar")
    put("oneOfEnumWithError", "bogus")
}

val enumExample = ExampleDescription(
    name = "enum",
    label = "Enums",
    data = enumData,
    schema = enumSchema,
    uischema = enumUiSchema,
)
