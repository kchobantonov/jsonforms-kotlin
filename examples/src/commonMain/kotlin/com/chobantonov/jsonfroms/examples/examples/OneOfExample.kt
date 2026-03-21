package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val oneOfSchema = JsonSchema(
    buildJsonObject {
        put("\$schema", "http://json-schema.org/draft-07/schema#")
        putJsonObject("definitions") {
            putJsonObject("address") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("street_address") { put("type", "string") }
                    putJsonObject("city") { put("type", "string") }
                    putJsonObject("state") { put("type", "string") }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("street_address"))
                    add(JsonPrimitive("city"))
                    add(JsonPrimitive("state"))
                }
                put("additionalProperties", false)
            }
            putJsonObject("user") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("name") { put("type", "string") }
                    putJsonObject("mail") { put("type", "string") }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("name"))
                    add(JsonPrimitive("mail"))
                }
                put("additionalProperties", false)
            }
        }
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") {
                put("type", "string")
            }
            putJsonObject("addressOrUser") {
                putJsonArray("oneOf") {
                    add(buildJsonObject { put("\$ref", "#/definitions/address") })
                    add(buildJsonObject { put("\$ref", "#/definitions/user") })
                }
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("name"))
        }
    },
)

val oneOfUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/name"),
        ControlElement(scope = "#/properties/addressOrUser"),
    ),
)

val oneOfData = buildJsonObject {
    put("name", "test")
    putJsonObject("addressOrUser") {
        put("name", "User")
        put("mail", "mail@example.com")
    }
}

val oneOfExample = ExampleDescription(
    name = "oneOf",
    label = "oneOf",
    data = oneOfData,
    schema = oneOfSchema,
    uischema = oneOfUiSchema,
)
