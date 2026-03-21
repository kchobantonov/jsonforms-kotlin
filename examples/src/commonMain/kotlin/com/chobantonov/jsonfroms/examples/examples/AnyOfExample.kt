package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val anyOfSchema = JsonSchema(
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
            }
            putJsonObject("users") {
                put("type", "array")
                putJsonObject("items") {
                    put("\$ref", "#/definitions/user")
                }
            }
            putJsonObject("addresses") {
                put("type", "array")
                putJsonObject("items") {
                    put("\$ref", "#/definitions/address")
                }
            }
        }
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("addressOrUser") {
                putJsonArray("anyOf") {
                    add(buildJsonObject { put("\$ref", "#/definitions/address") })
                    add(buildJsonObject { put("\$ref", "#/definitions/user") })
                }
            }
            putJsonObject("addressesOrUsers") {
                putJsonArray("anyOf") {
                    add(buildJsonObject { put("\$ref", "#/definitions/addresses") })
                    add(buildJsonObject { put("\$ref", "#/definitions/users") })
                }
            }
            putJsonObject("addressesOrUsersAnyOfItems") {
                put("type", "array")
                putJsonObject("items") {
                    putJsonArray("anyOf") {
                        add(buildJsonObject { put("\$ref", "#/definitions/addresses") })
                        add(buildJsonObject { put("\$ref", "#/definitions/users") })
                    }
                }
            }
        }
    },
)

val anyOfUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/addressOrUser"),
        ControlElement(
            scope = "#/properties/addressesOrUsers",
            label = StringControlLabel("Addresses or Users (AnyOf Schema)"),
        ),
        ControlElement(
            scope = "#/properties/addressesOrUsersAnyOfItems",
            label = StringControlLabel("Addresses or Users (AnyOf Array Items)"),
        ),
    ),
)

val anyOfData = buildJsonObject {
    putJsonObject("addressOrUser") {
        put("street_address", "1600 Pennsylvania Avenue NW")
        put("city", "Washington")
        put("state", "DC")
    }
}

val anyOfExample = ExampleDescription(
    name = "anyOf",
    label = "anyOf",
    data = anyOfData,
    schema = anyOfSchema,
    uischema = anyOfUiSchema,
)
