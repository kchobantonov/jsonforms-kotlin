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

val arrayWithDetailSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            personCoreSchema.properties.orEmpty().forEach { (name, schema) ->
                put(name, schema.toJsonObject())
            }
            putJsonObject("occupation") {
                put("type", "string")
            }
            putJsonObject("comments") {
                put("type", "array")
                put("description", "Description for array with details")
                put("minItems", 2)
                put("maxItems", 8)
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
                    }
                }
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("occupation"))
            add(JsonPrimitive("nationality"))
        }
    },
)

val arrayWithDetailUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(
            scope = "#/properties/comments",
            options = mapOf(
                "showSortButtons" to JsonPrimitive(true),
                "restrict" to JsonPrimitive(true),
                "detail" to buildJsonObject {
                    put("type", "VerticalLayout")
                    putJsonArray("elements") {
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/message")
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
        ),
    ),
)

val arrayWithDetailData = buildJsonObject {
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
            },
        )
    }
}

val arrayWithDetailExample = ExampleDescription(
    name = "array-with-detail",
    label = "Array with detail",
    data = arrayWithDetailData,
    schema = arrayWithDetailSchema,
    uischema = arrayWithDetailUiSchema,
)
