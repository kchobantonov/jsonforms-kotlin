package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.util.convertDateToString
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val arraysSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("comments") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("date") {
                            put("type", "string")
                            put("format", "date")
                        }
                        putJsonObject("message") {
                            put("type", "string")
                            put("maxLength", 5)
                        }
                        putJsonObject("enum") {
                            put("type", "string")
                            put("const", "foo")
                        }
                        putJsonObject("oneOfEnum") {
                            put("type", "string")
                            putJsonArray("oneOf") {
                                add(buildJsonObject { put("const", "foo") })
                                add(buildJsonObject { put("const", "bar") })
                            }
                        }
                    }
                }
            }
            putJsonObject("foo") {
                put("type", "string")
            }
        }
    },
)

val arraysUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/foo"),
        ControlElement(
            scope = "#/properties/comments",
            options = mapOf("showSortButtons" to JsonPrimitive(false)),
        ),
    ),
)

val arraysData = buildJsonObject {
    putJsonArray("comments") {
        add(
            buildJsonObject {
                put("date", "2001-09-11")
                put("message", "This is an example message")
            },
        )
        add(
            buildJsonObject {
                put("date", convertDateToString("date"))
                put("message", "Get ready for booohay")
                put("oneOfEnum", "test")
            },
        )
    }
}

val arraysExample = ExampleDescription(
    name = "array",
    label = "Array",
    data = arraysData,
    schema = arraysSchema,
    uischema = arraysUiSchema,
    config = mapOf("withSort" to arraysUiSchema),
)
