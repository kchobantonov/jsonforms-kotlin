package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val ifThenElseSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("b") {
                put("type", "boolean")
            }
            putJsonObject("c") {
                put("type", "string")
                put("minLength", 1)
            }
        }
        putJsonObject("if") {
            putJsonObject("properties") {
                putJsonObject("b") {
                    putJsonArray("enum") {
                        add(JsonPrimitive(false))
                    }
                }
            }
        }
        putJsonObject("then") {
            putJsonArray("required") {
                add(JsonPrimitive("c"))
            }
        }
    },
)

val ifThenElseExample = ExampleDescription(
    name = "ifthenelse",
    label = "If Then Else",
    data = buildJsonObject {},
    schema = ifThenElseSchema,
    uischema = null,
)
