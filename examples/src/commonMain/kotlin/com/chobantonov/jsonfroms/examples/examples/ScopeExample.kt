package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val scopeSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("orderId") {
                put("type", "string")
            }
            putJsonObject("purchaseDate") {
                put("type", "string")
                put("format", "date")
            }
            putJsonObject("price") {
                put("type", "number")
            }
            putJsonObject("shippingAddress") {
                put("\$ref", "#/definitions/shippingAddress")
            }
        }
        putJsonObject("definitions") {
            putJsonObject("shippingAddress") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("aptNo") {
                        put("type", "integer")
                    }
                    putJsonObject("streetNumber") {
                        put("type", "integer")
                    }
                }
            }
        }
    },
)

val scopeUiSchema = VerticalLayout(
    elements = listOf(
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/orderId"),
                ControlElement(scope = "#/properties/purchaseDate"),
                ControlElement(scope = "#/properties/price"),
            ),
        ),
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/shippingAddress/properties/aptNo"),
                ControlElement(scope = "#/properties/shippingAddress/properties/streetNumber"),
            ),
        ),
    ),
)

val scopeData = buildJsonObject {
    put("orderId", "123456")
    put("purchaseDate", "1985-06-02")
    put("price", 16)
    putJsonObject("shippingAddress") {
        put("aptNo", 3)
        put("streetNumber", 12)
    }
}

val scopeExample = ExampleDescription(
    name = "scope",
    label = "Scope",
    data = scopeData,
    schema = scopeSchema,
    uischema = scopeUiSchema,
)
