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
