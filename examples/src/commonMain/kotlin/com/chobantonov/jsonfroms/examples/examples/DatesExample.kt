package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.util.convertDateToString
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val datesSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("schemaBased") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("date") {
                        put("type", "string")
                        put("format", "date")
                        put("description", "schema-based date picker")
                    }
                    putJsonObject("time") {
                        put("type", "string")
                        put("format", "time")
                        put("description", "schema-based time picker")
                    }
                    putJsonObject("datetime") {
                        put("type", "string")
                        put("format", "date-time")
                        put("description", "schema-based datetime picker")
                    }
                }
            }
            putJsonObject("uiSchemaBased") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("date") {
                        put("type", "string")
                        put("description", "does not allow to select days")
                    }
                    putJsonObject("time") {
                        put("type", "string")
                        put("description", "24 hour format")
                    }
                    putJsonObject("datetime") {
                        put("type", "string")
                        put("description", "uischema-based datetime picker")
                    }
                }
            }
        }
    },
)

val datesUiSchema = VerticalLayout(
    elements = listOf(
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/schemaBased/properties/date"),
                ControlElement(scope = "#/properties/schemaBased/properties/time"),
                ControlElement(scope = "#/properties/schemaBased/properties/datetime"),
            ),
        ),
        HorizontalLayout(
            elements = listOf(
                ControlElement(
                    scope = "#/properties/uiSchemaBased/properties/date",
                    label = StringControlLabel("Year Month Picker"),
                    options = mapOf(
                        "format" to JsonPrimitive("date"),
                        "clearLabel" to JsonPrimitive("Clear it!"),
                        "cancelLabel" to JsonPrimitive("Abort"),
                        "okLabel" to JsonPrimitive("Do it"),
                        "views" to buildJsonArray {
                            add(JsonPrimitive("year"))
                            add(JsonPrimitive("month"))
                        },
                        "dateFormat" to JsonPrimitive("YYYY.MM"),
                        "dateSaveFormat" to JsonPrimitive("YYYY-MM"),
                    ),
                ),
                ControlElement(
                    scope = "#/properties/uiSchemaBased/properties/time",
                    options = mapOf(
                        "format" to JsonPrimitive("time"),
                        "ampm" to JsonPrimitive(true),
                    ),
                ),
                ControlElement(
                    scope = "#/properties/uiSchemaBased/properties/datetime",
                    options = mapOf(
                        "format" to JsonPrimitive("date-time"),
                        "dateTimeFormat" to JsonPrimitive("DD-MM-YY hh:mm:a"),
                        "dateTimeSaveFormat" to JsonPrimitive("YYYY/MM/DD h:mm a"),
                        "ampm" to JsonPrimitive(true),
                    ),
                ),
            ),
        ),
    ),
)

val datesData = buildJsonObject {
    putJsonObject("schemaBased") {
        put("date", convertDateToString("date"))
        put("time", "13:37:00")
        put("datetime", convertDateToString("date-time"))
    }
    putJsonObject("uiSchemaBased") {
        put("date", "2024-01")
        put("time", "13:37:00")
        put("datetime", "1999/12/11 10:05 am")
    }
}

val datesExample = ExampleDescription(
    name = "dates",
    label = "Dates",
    data = datesData,
    schema = datesSchema,
    uischema = datesUiSchema,
)
