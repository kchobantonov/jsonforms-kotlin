package com.chobantonov.jsonfroms.core

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.util.RuleEvaluator
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RuleEvaluatorTest {
    @Test
    fun nestedRuleScopesUseTheCurrentRenderingDataPath() {
        val data = buildJsonObject {
            put("locked", false)
            put("person", buildJsonObject { put("locked", true) })
            put("other", buildJsonObject { put("locked", false) })
        }
        val validator = StubJsonSchemaValidator { _, candidate ->
            if (candidate == JsonPrimitive(true)) emptyList() else listOf(ErrorObject(message = "not true"))
        }
        val schemaRule = ControlElement("#/properties/name", rule = Rule(
            RuleEffect.DISABLE, SchemaBasedCondition("#/properties/locked",
                buildJsonSchema { put("const", true) }, failWhenUndefined = true),
        ))
        assertFalse(RuleEvaluator.isEnabled(schemaRule, data, validator, path = "person"))
        assertTrue(RuleEvaluator.isEnabled(schemaRule, data, validator, path = "other"))
        assertTrue(RuleEvaluator.isEnabled(schemaRule, data, validator))
        assertTrue(RuleEvaluator.isEnabled(schemaRule, data, validator, path = "missing"))

        val directPointerRule = schemaRule.copy(rule = Rule(RuleEffect.DISABLE,
            SchemaBasedCondition("#/locked", buildJsonSchema { put("const", true) },
                failWhenUndefined = true)))
        assertTrue(RuleEvaluator.isEnabled(directPointerRule, data, validator, path = "person"))

    }

    @Test
    fun enableRuleOnlyAffectsEnablement() {
        val validator = StubJsonSchemaValidator { _, data ->
            if (data == JsonPrimitive(true)) emptyList() else listOf(ErrorObject(message = "not true"))
        }
        val control = ruleControl(RuleEffect.ENABLE)

        assertTrue(RuleEvaluator.isVisible(control, ruleData(false), validator))
        assertFalse(RuleEvaluator.isEnabled(control, ruleData(false), validator))
        assertFalse(RuleEvaluator.isReadonly(control, ruleData(false), validator))

        assertTrue(RuleEvaluator.isVisible(control, ruleData(true), validator))
        assertTrue(RuleEvaluator.isEnabled(control, ruleData(true), validator))
        assertFalse(RuleEvaluator.isReadonly(control, ruleData(true), validator))
    }

    @Test
    fun showRuleOnlyAffectsVisibility() {
        val validator = StubJsonSchemaValidator { _, data ->
            if (data == JsonPrimitive(true)) emptyList() else listOf(ErrorObject(message = "not true"))
        }
        val control = ruleControl(RuleEffect.SHOW)

        assertFalse(RuleEvaluator.isVisible(control, ruleData(false), validator))
        assertTrue(RuleEvaluator.isEnabled(control, ruleData(false), validator))
        assertFalse(RuleEvaluator.isReadonly(control, ruleData(false), validator))

        assertTrue(RuleEvaluator.isVisible(control, ruleData(true), validator))
        assertTrue(RuleEvaluator.isEnabled(control, ruleData(true), validator))
        assertFalse(RuleEvaluator.isReadonly(control, ruleData(true), validator))
    }

    @Test
    fun showsControlWhenSchemaConditionMatches() {
        val validator = StubJsonSchemaValidator { _, data ->
            if (data == JsonPrimitive("admin")) emptyList() else listOf(ErrorObject(message = "not admin"))
        }
        val control = ControlElement(
            scope = "#/name",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/role",
                    schema = buildJsonSchema { put("const", "admin") },
                ),
            ),
        )

        val data = buildJsonObject {
            put("role", "admin")
        }

        assertTrue(RuleEvaluator.isVisible(control, data, validator))
    }

    @Test
    fun hidesControlWhenSchemaConditionDoesNotMatch() {
        val validator = StubJsonSchemaValidator { _, data ->
            if (data == JsonPrimitive("admin")) emptyList() else listOf(ErrorObject(message = "not admin"))
        }
        val control = ControlElement(
            scope = "#/name",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/role",
                    schema = buildJsonSchema { put("const", "admin") },
                ),
            ),
        )

        val data = buildJsonObject {
            put("role", "user")
        }

        assertFalse(RuleEvaluator.isVisible(control, data, validator))
    }

    @Test
    fun resolvesStandardUiSchemaConditionScopesAgainstData() {
        val validator = StubJsonSchemaValidator { _, data ->
            if (data == JsonPrimitive(true)) emptyList() else listOf(ErrorObject(message = "not enabled"))
        }
        val control = ControlElement(
            scope = "#/properties/name",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/vegetarian",
                    schema = buildJsonSchema { put("const", true) },
                ),
            ),
        )

        assertFalse(
            RuleEvaluator.isVisible(
                control,
                buildJsonObject { put("vegetarian", false) },
                validator,
            ),
        )
        assertTrue(
            RuleEvaluator.isVisible(
                control,
                buildJsonObject { put("vegetarian", true) },
                validator,
            ),
        )
    }

    @Test
    fun directPointerRuleScopeKeepsEmptyPropertySegment() {
        val validator = StubJsonSchemaValidator { _, value ->
            if (value == JsonPrimitive(true)) emptyList() else listOf(ErrorObject(message = "not true"))
        }
        val control = ControlElement(
            scope = "#/properties/name",
            rule = Rule(RuleEffect.SHOW, SchemaBasedCondition(
                scope = "#//name", schema = buildJsonSchema { put("const", true) },
            )),
        )
        val nestedTrue = buildJsonObject {
            put("", buildJsonObject { put("name", true) })
            put("name", false)
        }
        val nestedFalse = buildJsonObject {
            put("", buildJsonObject { put("name", false) })
            put("name", true)
        }

        assertTrue(RuleEvaluator.isVisible(control, nestedTrue, validator))
        assertFalse(RuleEvaluator.isVisible(control, nestedFalse, validator))
    }

    private fun ruleControl(effect: RuleEffect) = ControlElement(
        scope = "#/properties/value",
        rule = Rule(
            effect = effect,
            condition = SchemaBasedCondition(
                scope = "#/properties/trigger",
                schema = buildJsonSchema { put("const", true) },
            ),
        ),
    )

    private fun ruleData(trigger: Boolean) = buildJsonObject { put("trigger", trigger) }
}

private class StubJsonSchemaValidator(
    private val validateBlock: (JsonSchema, JsonElement) -> List<ErrorObject>,
) : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction =
        object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonElement): List<ErrorObject> = validateBlock(schema, data)
        }
}
