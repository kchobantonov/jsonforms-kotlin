package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val stringSchema = JsonSchema(
    buildJsonObject {
        put("type", "string")
        put("title", "String")
        put("description", "The form output will be a string")
    },
)

val stringUiSchema = ControlElement(scope = "#/")

val stringData = JsonPrimitive("This is a test string")

val stringExample = ExampleDescription(
    name = "string",
    label = "String",
    data = stringData,
    schema = stringSchema,
    uischema = stringUiSchema,
)
