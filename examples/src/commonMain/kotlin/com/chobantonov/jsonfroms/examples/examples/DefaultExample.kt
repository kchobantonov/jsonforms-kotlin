package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val defaultSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") {
                put("type", "string")
                put("default", "foo")
            }
            putJsonObject("name_noDefault") {
                put("type", "string")
            }
            putJsonObject("description") {
                put("type", "string")
                put("default", "bar")
            }
            putJsonObject("done") {
                put("type", "boolean")
                put("default", false)
            }
            putJsonObject("rating") {
                put("type", "integer")
                put("default", 5)
            }
            putJsonObject("cost") {
                put("type", "number")
                put("default", 5.5)
            }
            putJsonObject("dueDate") {
                put("type", "string")
                put("format", "date")
                put("default", "2019-04-01")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("name"))
            add(JsonPrimitive("name_noDefault"))
        }
    },
)

val defaultUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(scope = "#/properties/name"),
        ControlElement(scope = "#/properties/name_noDefault"),
        ControlElement(scope = "#/properties/done", label = BooleanControlLabel(false)),
        ControlElement(
            scope = "#/properties/description",
            options = buildJsonObject {
                put("multi", true)
            },
        ),
        ControlElement(scope = "#/properties/rating"),
        ControlElement(scope = "#/properties/cost"),
        ControlElement(scope = "#/properties/dueDate"),
    ),
)

val defaultData = buildJsonObject {
    put("name", "Send email to Adrian")
    put("name_noDefault", "Send email to Adrian")
    put("description", "Confirm if you have passed the subject\nHereby ...")
    put("done", true)
    put("rating", 1)
    put("cost", 3.14)
    put("dueDate", "2019-05-01")
}

val defaultExample = ExampleDescription(
    name = "default",
    label = "Default",
    data = defaultData,
    schema = defaultSchema,
    uischema = defaultUiSchema,
)
