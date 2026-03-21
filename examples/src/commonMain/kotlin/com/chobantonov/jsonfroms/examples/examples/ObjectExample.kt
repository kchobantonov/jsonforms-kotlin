package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val objectSchema = JsonSchema(
    buildJsonObject {
        put("\$schema", "http://json-schema.org/draft-07/schema#")
        put("type", "object")
        putJsonObject("properties") {
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
        }
    },
)

val objectRootUiSchema = ControlElement(scope = "#")

val objectUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/address"),
        ControlElement(
            scope = "#/properties/user",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = LeafCondition(
                    scope = "#/properties/address/properties/state",
                    expectedValue = JsonPrimitive("DC"),
                ),
            ),
            options = mapOf(
                "detail" to buildJsonObject {
                    put("type", "Group")
                    put("label", "User Data")
                    putJsonArray("elements") {
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/name")
                            },
                        )
                        add(
                            buildJsonObject {
                                put("type", "Control")
                                put("scope", "#/properties/mail")
                            },
                        )
                    }
                },
            ),
        ),
    ),
)

val objectData = buildJsonObject {
    putJsonObject("address") {
        put("street_address", "1600 Pennsylvania Avenue NW")
        put("city", "Washington")
        put("state", "DC")
    }
}

val objectRootExample = ExampleDescription(
    name = "rootObject",
    label = "Object - Root Scope",
    data = objectData,
    schema = objectSchema,
    uischema = objectRootUiSchema,
)

val objectExample = ExampleDescription(
    name = "object",
    label = "Object",
    data = objectData,
    schema = objectSchema,
    uischema = objectUiSchema,
)
