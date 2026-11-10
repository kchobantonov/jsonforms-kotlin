package com.chobantonov.jsonfroms.examples.upstream.behavior

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.ExampleAction
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.StateProps
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

val ruleInheritanceSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("toggleTopLayout") {
                put("type", "boolean")
            }
            putJsonObject("topString") {
                put("type", "string")
            }
            putJsonObject("middleNumber") {
                put("type", "number")
            }
            putJsonObject("toggleBottomLayout") {
                put("type", "boolean")
            }
            putJsonObject("bottomBoolean") {
                put("type", "boolean")
            }
        }
    },
)

private val constTrueSchema = JsonSchema(
    buildJsonObject {
        put("const", true)
    },
)

val ruleInheritanceUiSchema = VerticalLayout(
    rule = Rule(
        effect = RuleEffect.ENABLE,
        condition = SchemaBasedCondition(
            scope = "#/properties/toggleTopLayout",
            schema = constTrueSchema,
        ),
    ),
    elements = listOf(
        ControlElement(scope = "#/properties/topString"),
        HorizontalLayout(
            elements = listOf(
                ControlElement(scope = "#/properties/middleNumber"),
                GroupElement(
                    label = "group",
                    rule = Rule(
                        effect = RuleEffect.SHOW,
                        condition = SchemaBasedCondition(
                            scope = "#/properties/toggleBottomLayout",
                            schema = constTrueSchema,
                        ),
                    ),
                    elements = listOf(
                        ControlElement(scope = "#/properties/bottomBoolean"),
                    ),
                ),
            ),
        ),
    ),
)

val ruleInheritanceData = buildJsonObject {
    put("toggleTopLayout", true)
    put("toggleBottomLayout", true)
    put("toggleControl", true)
}

private fun toggleBooleanField(
    state: StateProps,
    field: String,
): StateProps {
    val data = state.data as? JsonObject ?: return state
    val current = data[field]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: false
    val nextData = buildJsonObject {
        data.forEach { (key, value) ->
            if (key != field) {
                put(key, value)
            }
        }
        put(field, !current)
    }
    return state.copy(data = nextData)
}

val ruleInheritanceActions = listOf(
    ExampleAction(
        label = "Enable/Disable top layout",
        apply = { props -> toggleBooleanField(props, "toggleTopLayout") },
    ),
    ExampleAction(
        label = "Show/Hide bottom layout",
        apply = { props -> toggleBooleanField(props, "toggleBottomLayout") },
    ),
)

val ruleInheritanceExample = ExampleDescription(
    name = "rule-enable",
    label = "Rule Inheritance",
    data = ruleInheritanceData,
    schema = ruleInheritanceSchema,
    uischema = ruleInheritanceUiSchema,
    actions = ruleInheritanceActions,
)
