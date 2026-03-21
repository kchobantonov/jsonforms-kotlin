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

val allOfSchema = JsonSchema(
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
        }
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("billing_address") {
                put("\$ref", "#/definitions/address")
            }
            putJsonObject("shipping_address") {
                putJsonArray("allOf") {
                    add(
                        buildJsonObject {
                            put("\$ref", "#/definitions/address")
                        },
                    )
                    add(
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("residential"))
                                        add(JsonPrimitive("business"))
                                    }
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("type"))
                            }
                        },
                    )
                }
            }
        }
    },
)

val allOfUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(
            scope = "#/properties/billing_address",
            label = StringControlLabel("Billing address"),
        ),
        ControlElement(scope = "#/properties/shipping_address"),
    ),
)

val allOfData = buildJsonObject {
    putJsonObject("billing_address") {
        put("street_address", "1600 Pennsylvania Avenue NW")
        put("city", "Washington")
        put("state", "DC")
    }
}

val allOfExample = ExampleDescription(
    name = "allOf",
    label = "allOf",
    data = allOfData,
    schema = allOfSchema,
    uischema = allOfUiSchema,
)
