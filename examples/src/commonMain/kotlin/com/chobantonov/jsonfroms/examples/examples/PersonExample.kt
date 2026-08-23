package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val personCoreSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") {
                put("type", "string")
                put("minLength", 3)
                put("description", "Please enter your name")
            }
            putJsonObject("vegetarian") {
                put("type", "boolean")
            }
            putJsonObject("birthDate") {
                put("type", "string")
                put("format", "date")
                put("description", "Please enter your birth date.")
            }
            putJsonObject("nationality") {
                put("type", "string")
                putJsonArray("enum") {
                    add(JsonPrimitive("DE"))
                    add(JsonPrimitive("IT"))
                    add(JsonPrimitive("JP"))
                    add(JsonPrimitive("US"))
                    add(JsonPrimitive("RU"))
                    add(JsonPrimitive("Other"))
                }
            }
        }
    },
)

val personSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            personCoreSchema.properties.orEmpty().forEach { (name, schema) ->
                put(name, schema.toJsonObject())
            }
            putJsonObject("personalData") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("age") {
                        put("type", "integer")
                        put("description", "Please enter your age.")
                    }
                    putJsonObject("height") {
                        put("type", "number")
                    }
                    putJsonObject("drivingSkill") {
                        put("type", "number")
                        put("maximum", 10)
                        put("minimum", 1)
                        put("default", 7)
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("age"))
                    add(JsonPrimitive("height"))
                }
            }
            putJsonObject("vegetarian") {
                put("type", "boolean")
            }
            putJsonObject("birthDate") {
                put("type", "string")
                put("format", "date")
            }
            putJsonObject("occupation") {
                put("type", "string")
            }
            putJsonObject("postalCode") {
                put("type", "string")
                put("maxLength", 5)
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("occupation"))
            add(JsonPrimitive("nationality"))
        }
    },
)

val personUiSchema = VerticalLayout(
    elements = listOf(
        HorizontalLayout(
            options = mapOf("stackOnSmallScreen" to JsonPrimitive(true)),
            elements = listOf(
                ControlElement(scope = "#/properties/name"),
                ControlElement(scope = "#/properties/personalData/properties/age"),
                ControlElement(scope = "#/properties/birthDate"),
            ),
        ),
        LabelElement(text = "Additional Information"),
        HorizontalLayout(
            options = mapOf("stackOnSmallScreen" to JsonPrimitive(true)),
            elements = listOf(
                ControlElement(scope = "#/properties/personalData/properties/height"),
                ControlElement(scope = "#/properties/nationality"),
                ControlElement(
                    scope = "#/properties/occupation",
                    options = mapOf(
                        "suggestion" to buildJsonArray {
                            add(JsonPrimitive("Accountant"))
                            add(JsonPrimitive("Engineer"))
                            add(JsonPrimitive("Freelancer"))
                            add(JsonPrimitive("Journalism"))
                            add(JsonPrimitive("Physician"))
                            add(JsonPrimitive("Student"))
                            add(JsonPrimitive("Teacher"))
                            add(JsonPrimitive("Other"))
                        },
                    ),
                ),
            ),
        ),
    ),
)

val personData = buildJsonObject {
    put("name", "John Doe")
    put("vegetarian", false)
    put("birthDate", "1985-06-02")
    putJsonObject("personalData") {
        put("age", 34)
    }
    put("postalCode", "12345")
}

val personExample = ExampleDescription(
    name = "person",
    label = "Person",
    data = personData,
    schema = personSchema,
    uischema = personUiSchema,
)
