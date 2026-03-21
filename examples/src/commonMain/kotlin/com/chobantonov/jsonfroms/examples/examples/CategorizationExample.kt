package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val categorizationSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("firstName") {
                put("type", "string")
                put("minLength", 3)
                put("description", "Please enter your first name")
            }
            putJsonObject("secondName") {
                put("type", "string")
                put("minLength", 3)
                put("description", "Please enter your second name")
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
            putJsonObject("provideAddress") {
                put("type", "boolean")
            }
            putJsonObject("address") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("street") {
                        put("type", "string")
                    }
                    putJsonObject("streetNumber") {
                        put("type", "string")
                    }
                    putJsonObject("city") {
                        put("type", "string")
                    }
                    putJsonObject("postalCode") {
                        put("type", "string")
                        put("maxLength", 5)
                    }
                }
            }
            putJsonObject("vegetarianOptions") {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("vegan") {
                        put("type", "boolean")
                    }
                    putJsonObject("favoriteVegetable") {
                        put("type", "string")
                        putJsonArray("enum") {
                            add(JsonPrimitive("Tomato"))
                            add(JsonPrimitive("Potato"))
                            add(JsonPrimitive("Salad"))
                            add(JsonPrimitive("Aubergine"))
                            add(JsonPrimitive("Cucumber"))
                            add(JsonPrimitive("Other"))
                        }
                    }
                    putJsonObject("otherFavoriteVegetable") {
                        put("type", "string")
                    }
                }
            }
        }
    },
)

private val constTrueSchema = JsonSchema(
    buildJsonObject {
        put("const", true)
    },
)

private val constOtherSchema = JsonSchema(
    buildJsonObject {
        put("const", "Other")
    },
)

val categorizationUiSchema = CategorizationElement(
    elements = listOf(
        CategoryElement(
            label = "categoryLabelKey",
            elements = listOf(
                HorizontalLayout(
                    elements = listOf(
                        ControlElement(scope = "#/properties/firstName"),
                        ControlElement(scope = "#/properties/secondName"),
                    ),
                ),
                HorizontalLayout(
                    elements = listOf(
                        ControlElement(scope = "#/properties/birthDate"),
                        ControlElement(scope = "#/properties/nationality"),
                    ),
                ),
                ControlElement(scope = "#/properties/provideAddress"),
                ControlElement(scope = "#/properties/vegetarian"),
            ),
        ),
        CategoryElement(
            label = "",
            i18n = "address",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/provideAddress",
                    schema = constTrueSchema,
                ),
            ),
            elements = listOf(
                HorizontalLayout(
                    elements = listOf(
                        ControlElement(scope = "#/properties/address/properties/street"),
                        ControlElement(scope = "#/properties/address/properties/streetNumber"),
                    ),
                ),
                HorizontalLayout(
                    elements = listOf(
                        ControlElement(scope = "#/properties/address/properties/city"),
                        ControlElement(scope = "#/properties/address/properties/postalCode"),
                    ),
                ),
            ),
        ),
        CategoryElement(
            label = "Additional",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/vegetarian",
                    schema = constTrueSchema,
                ),
            ),
            elements = listOf(
                ControlElement(scope = "#/properties/vegetarianOptions/properties/vegan"),
                ControlElement(scope = "#/properties/vegetarianOptions/properties/favoriteVegetable"),
                ControlElement(
                    scope = "#/properties/vegetarianOptions/properties/otherFavoriteVegetable",
                    rule = Rule(
                        effect = RuleEffect.SHOW,
                        condition = SchemaBasedCondition(
                            scope = "#/properties/vegetarianOptions/properties/favoriteVegetable",
                            schema = constOtherSchema,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

val categorizationData = buildJsonObject {
    put("provideAddress", true)
    put("vegetarian", false)
}

private val categorizationTranslator = Translator { id, defaultMessage, _ ->
    when (id) {
        "categoryLabelKey" -> "Basic"
        "address.label" -> "Address"
        else -> defaultMessage
    }
}

val categorizationExample = ExampleDescription(
    name = "categorization",
    label = "Categorization",
    data = categorizationData,
    schema = categorizationSchema,
    uischema = categorizationUiSchema,
    i18n = JsonFormsI18nState(
        locale = "en",
        translate = categorizationTranslator,
    ),
)
