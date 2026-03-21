package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val ruleSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") {
                put("type", "string")
            }
            putJsonObject("dead") {
                put("type", "boolean")
            }
            putJsonObject("kindOfDead") {
                put("type", "string")
                put("enum", buildJsonArray {
                    add(JsonPrimitive("Zombie"))
                    add(JsonPrimitive("Vampire"))
                    add(JsonPrimitive("Ghoul"))
                })
            }
            putJsonObject("vegetables") {
                put("type", "boolean")
            }
            putJsonObject("kindOfVegetables") {
                put("type", "string")
                put("enum", buildJsonArray {
                    add(JsonPrimitive("All"))
                    add(JsonPrimitive("Some"))
                    add(JsonPrimitive("Only potatoes"))
                })
            }
            putJsonObject("vitaminDeficiency") {
                put("type", "string")
                put("enum", buildJsonArray {
                    add(JsonPrimitive("None"))
                    add(JsonPrimitive("Vitamin A"))
                    add(JsonPrimitive("Vitamin B"))
                    add(JsonPrimitive("Vitamin C"))
                })
            }
        }
    },
)

val ruleUiSchema = VerticalLayout(
    elements = listOf(
        ControlElement(
            label = StringControlLabel("Name"),
            scope = "#/properties/name",
        ),
        GroupElement(
            elements = listOf(
                ControlElement(
                    label = StringControlLabel("Is Dead?"),
                    scope = "#/properties/dead",
                ),
                ControlElement(
                    label = StringControlLabel("Kind of dead"),
                    scope = "#/properties/kindOfDead",
                    rule = Rule(
                        effect = RuleEffect.ENABLE,
                        condition = SchemaBasedCondition(
                            scope = "#/properties/dead",
                            schema = JsonSchema(
                                buildJsonObject {
                                    put("const", true)
                                },
                            ),
                        ),
                    ),
                ),
            ),
        ),
        GroupElement(
            elements = listOf(
                ControlElement(
                    label = StringControlLabel("Eats vegetables?"),
                    scope = "#/properties/vegetables",
                ),
                ControlElement(
                    label = StringControlLabel("Kind of vegetables"),
                    scope = "#/properties/kindOfVegetables",
                    rule = Rule(
                        effect = RuleEffect.HIDE,
                        condition = SchemaBasedCondition(
                            scope = "#/properties/vegetables",
                            schema = JsonSchema(
                                buildJsonObject {
                                    put("const", false)
                                },
                            ),
                        ),
                    ),
                ),
                ControlElement(
                    label = StringControlLabel("Vitamin deficiency?"),
                    scope = "#/properties/vitaminDeficiency",
                    rule = Rule(
                        effect = RuleEffect.SHOW,
                        condition = ValidateFunctionCondition(
                            scope = "#",
                            validate = { context ->
                                val data = context.data?.jsonObject ?: return@ValidateFunctionCondition false
                                val dead = data["dead"]?.jsonPrimitive?.booleanOrNull ?: false
                                val kindOfVegetables = data["kindOfVegetables"]?.jsonPrimitive?.contentOrNull
                                !dead && kindOfVegetables != "All"
                            },
                        ),
                    ),
                ),
            ),
        ),
    ),
)

val ruleData = buildJsonObject {
    put("name", "John Doe")
    put("dead", false)
    put("vegetables", false)
}

val ruleExample = ExampleDescription(
    name = "rule",
    label = "Rule",
    data = ruleData,
    schema = ruleSchema,
    uischema = ruleUiSchema,
)
