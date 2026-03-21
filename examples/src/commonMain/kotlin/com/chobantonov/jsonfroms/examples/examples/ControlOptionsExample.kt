package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val controlOptionsBaseSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("string") {
                put("type", "string")
            }
            putJsonObject("boolean") {
                put("type", "boolean")
                put("description", "Boolean description as a tooltip")
            }
            putJsonObject("number") {
                put("type", "number")
            }
            putJsonObject("integer") {
                put("type", "integer")
            }
            putJsonObject("date") {
                put("type", "string")
                put("format", "date")
            }
            putJsonObject("time") {
                put("type", "string")
                put("format", "time")
            }
            putJsonObject("dateTime") {
                put("type", "string")
                put("format", "date-time")
            }
            putJsonObject("enum") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("One"))
                    add(JsonPrimitive("Two"))
                    add(JsonPrimitive("Three"))
                }
            }
        }
    },
)

val controlOptionsBaseUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/string"),
        ControlElement(scope = "#/properties/boolean"),
        ControlElement(scope = "#/properties/number"),
        ControlElement(scope = "#/properties/integer"),
        ControlElement(scope = "#/properties/date"),
        ControlElement(scope = "#/properties/time"),
        ControlElement(scope = "#/properties/dateTime"),
        ControlElement(scope = "#/properties/enum"),
    ),
)

val controlOptionsBaseData = buildJsonObject {
    put("string", "This is a string")
    put("boolean", true)
    put("number", 50.5)
    put("integer", 50)
    put("date", "2020-06-25")
    put("time", "23:08:00")
    put("dateTime", "2020-06-25T23:08:42+02:00")
    put("enum", "Two")
}

val controlOptionsExtendedSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("multilineString") {
                put("type", "string")
                put("description", "Multiline Example")
            }
            putJsonObject("slider") {
                put("type", "number")
                put("minimum", 1)
                put("maximum", 5)
                put("default", 2)
                put("description", "Slider Example")
            }
            putJsonObject("trimText") {
                put("type", "string")
                put("description", "Trim indicates whether the control shall grab the full width available")
            }
            putJsonObject("restrictText") {
                put("type", "string")
                put("maxLength", 5)
                put("description", "Restricts the input length to the set value (in this case: 5)")
            }
            putJsonObject("unfocusedDescription") {
                put("type", "string")
                put("description", "This description is shown even when the control is not focused")
            }
            putJsonObject("hideRequiredAsterisk") {
                put("type", "string")
                put("description", "Hides the \"*\" symbol, when the field is required")
            }
            putJsonObject("toggle") {
                put("type", "boolean")
                put("description", "The \"toggle\" option renders boolean values as a toggle.")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("hideRequiredAsterisk"))
            add(JsonPrimitive("restrictText"))
        }
    },
)

val controlOptionsExtendedUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(
            scope = "#/properties/multilineString",
            options = mapOf("multi" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/slider",
            options = mapOf("slider" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/trimText",
            options = mapOf("trim" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/restrictText",
            options = mapOf("restrict" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/unfocusedDescription",
            options = mapOf("showUnfocusedDescription" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/hideRequiredAsterisk",
            options = mapOf("hideRequiredAsterisk" to JsonPrimitive(true)),
        ),
        ControlElement(
            scope = "#/properties/toggle",
            label = StringControlLabel("Boolean as Toggle"),
            options = mapOf("toggle" to JsonPrimitive(true)),
        ),
    ),
)

val controlOptionsExtendedData = buildJsonObject {
    put("multilineString", "Multi-\nline\nexample")
    put("slider", 4)
    put("trimText", "abcdefg")
    put("restrictText", "abcde")
    put("toggle", false)
}

val controlOptionsSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            controlOptionsBaseSchema.properties.orEmpty().forEach { (name, schema) ->
                put(name, schema.toJsonObject())
            }
            controlOptionsExtendedSchema.properties.orEmpty().forEach { (name, schema) ->
                put(name, schema.toJsonObject())
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("hideRequiredAsterisk"))
            add(JsonPrimitive("restrictText"))
        }
    },
)

val controlOptionsUiSchema = CategorizationElement(
    elements = listOf(
        CategoryElement(
            label = "Normal controls",
            elements = listOf(controlOptionsBaseUiSchema),
        ),
        CategoryElement(
            label = "Configured controls",
            elements = listOf(controlOptionsExtendedUiSchema),
        ),
    ),
)

val controlOptionsData = buildJsonObject {
    controlOptionsBaseData.forEach { (key, value) -> put(key, value) }
    controlOptionsExtendedData.forEach { (key, value) -> put(key, value) }
}

val controlOptionsExample = ExampleDescription(
    name = "control-options",
    label = "Control Options",
    data = controlOptionsData,
    schema = controlOptionsSchema,
    uischema = controlOptionsUiSchema,
)
