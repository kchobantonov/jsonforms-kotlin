package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val loginSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("username") {
                put("type", "string")
                put("description", "Login Name")
            }
            putJsonObject("password") {
                put("type", "string")
                put("format", "password")
                put("description", "Login password")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("username"))
            add(JsonPrimitive("password"))
        }
    },
)

val loginUiSchema = VerticalLayout(
    elements = listOf(
        LabelElement(text = "Login Information"),
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/username"),
                ControlElement(scope = "#/properties/password"),
            ),
        ),
    ),
)

val loginData = buildJsonObject {
    put("username", "john.doe@email.com")
}

val loginExample = ExampleDescription(
    name = "login",
    label = "Login Form",
    data = loginData,
    schema = loginSchema,
    uischema = loginUiSchema,
)
