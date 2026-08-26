package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.mappers.isInherentlyEnabled
import com.chobantonov.jsonfroms.core.mappers.isInherentlyReadonly
import com.chobantonov.jsonfroms.core.models.AndCondition
import com.chobantonov.jsonfroms.core.models.Condition
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.OrCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.ValidateFunctionContext
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RuntimeTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    private fun state(readonly: Boolean = false): JsonFormsState<Any, Any> =
        JsonFormsState(
            jsonforms = JsonFormsSubStates(
                core = JsonFormsCoreState(
                    validatorEngine = validator,
                    validator = null,
                ),
                readonly = readonly,
            ),
        )

    @Test
    fun `evalVisibility show valid case`() {
        val uischema = controlWithRule(
            RuleEffect.SHOW,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val data = buildJsonObject {
            put("value", "foo")
            put("ruleValue", "bar")
        }

        assertTrue(Runtime.evalVisibility(uischema, data, validator = validator))
    }

    @Test
    fun `evalVisibility supports And and Or conditions`() {
        val andUiSchema = controlWithRule(
            RuleEffect.SHOW,
            AndCondition(
                listOf(
                    LeafCondition(scope = "#/ruleValue1", expectedValue = JsonPrimitive("bar")),
                    LeafCondition(scope = "#/ruleValue2", expectedValue = JsonPrimitive("foo")),
                ),
            ),
        )
        val orUiSchema = controlWithRule(
            RuleEffect.SHOW,
            OrCondition(
                listOf(
                    LeafCondition(scope = "#/ruleValue1", expectedValue = JsonPrimitive("bar")),
                    LeafCondition(scope = "#/ruleValue2", expectedValue = JsonPrimitive("foo")),
                ),
            ),
        )
        val data = buildJsonObject {
            put("value", "hello")
            put("ruleValue1", "bar1")
            put("ruleValue2", "foo")
        }

        assertFalse(Runtime.evalVisibility(andUiSchema, data, validator = validator))
        assertTrue(Runtime.evalVisibility(orUiSchema, data, validator = validator))
    }

    @Test
    fun `evalVisibility supports schema condition enum`() {
        val uischema = controlWithRule(
            RuleEffect.SHOW,
            SchemaBasedCondition(
                scope = "#/ruleValue",
                schema = buildJsonSchema {
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("bar"))
                        add(JsonPrimitive("baz"))
                    })
                },
            ),
        )

        assertTrue(Runtime.evalVisibility(uischema, jsonData("bar"), validator = validator))
        assertFalse(Runtime.evalVisibility(uischema, jsonData("foo"), validator = validator))
    }

    @Test
    fun `evalEnablement supports ValidateFunctionCondition and context`() {
        val uischema = controlWithRule(
            RuleEffect.ENABLE,
            ValidateFunctionCondition(
                scope = "#/ruleValue",
                validate = { context: ValidateFunctionContext ->
                    context.data == JsonPrimitive("bar") &&
                        (context.fullData as? kotlinx.serialization.json.JsonObject)?.get("value") == JsonPrimitive("foo") &&
                        context.path == "person.value" &&
                        context.uischemaElement is ControlElement &&
                        (context.config as? Map<*, *>)?.get("externalValidator") == true
                },
            ),
        )
        val data = buildJsonObject {
            put("value", "foo")
            put("ruleValue", "bar")
        }

        assertTrue(
            Runtime.evalEnablement(
                uischema,
                data,
                path = "person.value",
                validator = validator,
                config = mapOf("externalValidator" to true),
            ),
        )
    }

    @Test
    fun `evalEnablement failWhenUndefined matches current behavior`() {
        val uischema = controlWithRule(
            RuleEffect.DISABLE,
            SchemaBasedCondition(
                scope = "#/ruleValue",
                schema = buildJsonSchema {
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("bar"))
                        add(JsonPrimitive("baz"))
                    })
                },
                failWhenUndefined = true,
            ),
        )
        val data = buildJsonObject { put("value", "foo") }

        assertTrue(Runtime.evalEnablement(uischema, data, validator = validator))
    }

    @Test
    fun `evalReadonly and writable cases`() {
        val readonlyUiSchema = controlWithRule(
            RuleEffect.READONLY,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val writableUiSchema = controlWithRule(
            RuleEffect.WRITABLE,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val match = jsonData("bar")
        val mismatch = jsonData("baz")

        assertTrue(Runtime.evalReadonly(readonlyUiSchema, match, validator = validator))
        assertFalse(Runtime.evalReadonly(writableUiSchema, match, validator = validator))
        assertTrue(Runtime.evalReadonly(writableUiSchema, mismatch, validator = validator))
    }

    @Test
    fun `hasReadonlyRule and isReadonly`() {
        val readonlyUiSchema = controlWithRule(
            RuleEffect.READONLY,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val writableUiSchema = controlWithRule(
            RuleEffect.WRITABLE,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )

        assertTrue(hasReadonlyRule(readonlyUiSchema))
        assertTrue(hasReadonlyRule(writableUiSchema))
        assertFalse(hasReadonlyRule(control()))
        assertFalse(Runtime.isReadonly(control(), buildJsonObject { }, validator = validator))
        assertTrue(Runtime.isReadonly(readonlyUiSchema, jsonData("bar"), validator = validator))
    }

    @Test
    fun `isInherentlyEnabled covers readonly ownProps uiSchema schema rule and config`() {
        val defaultControl = control()
        val data = jsonData("bar")
        val readonlyOptionControl = control(options = mapOf("readonly" to JsonPrimitive(true)))
        val readwriteOptionControl = control(options = mapOf("readonly" to JsonPrimitive(false), "readOnly" to JsonPrimitive(true)))
        val ruleControl = controlWithRule(
            RuleEffect.DISABLE,
            SchemaBasedCondition(
                scope = "#/ruleValue",
                schema = buildJsonSchema {
                    put("type", "string")
                    put("pattern", "bar")
                },
            ),
        )
        val enabledRuleControl = controlWithRule(
            RuleEffect.ENABLE,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )

        assertFalse(isInherentlyEnabled(state(readonly = true), uiSchema = defaultControl, schema = null, rootData = JsonNull))
        assertTrue(
            isInherentlyEnabled(
                state(readonly = true),
                uiSchema = defaultControl,
                schema = null,
                rootData = JsonNull,
                config = mapOf("separateReadonlyFromDisabled" to true),
            ),
        )
        assertFalse(isInherentlyEnabled(state(), ownProps = mapOf("enabled" to false), uiSchema = defaultControl, schema = null, rootData = JsonNull))
        assertFalse(isInherentlyEnabled(state(), uiSchema = readonlyOptionControl, schema = null, rootData = JsonNull))
        assertTrue(isInherentlyEnabled(state(), uiSchema = readwriteOptionControl, schema = null, rootData = JsonNull))
        assertFalse(isInherentlyEnabled(state(), uiSchema = defaultControl, schema = buildJsonSchema { put("readOnly", true) }, rootData = JsonNull))
        assertFalse(
            isInherentlyEnabled(
                state(),
                ownProps = mapOf("enabled" to false),
                uiSchema = defaultControl,
                schema = null,
                rootData = JsonNull,
                config = mapOf("readonly" to false),
            ),
        )
        assertFalse(
            isInherentlyEnabled(
                state(),
                ownProps = mapOf("enabled" to false),
                uiSchema = enabledRuleControl,
                schema = null,
                rootData = data,
            ),
        )
        assertFalse(
            isInherentlyEnabled(
                state(),
                uiSchema = ruleControl,
                schema = null,
                rootData = data,
            ),
        )
    }

    @Test
    fun `isInherentlyReadonly covers global uiSchema schema ownProps and rules`() {
        val defaultControl = control()
        val readonlyOptionControl = control(options = mapOf("readonly" to JsonPrimitive(true)))
        val readwriteOptionControl = control(options = mapOf("readonly" to JsonPrimitive(false), "readOnly" to JsonPrimitive(true)))
        val readonlyRule = controlWithRule(
            RuleEffect.READONLY,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val writableRule = controlWithRule(
            RuleEffect.WRITABLE,
            LeafCondition(scope = "#/ruleValue", expectedValue = JsonPrimitive("bar")),
        )
        val data = jsonData("bar")

        assertTrue(isInherentlyReadonly(state(readonly = true), uiSchema = defaultControl, schema = null, rootData = JsonNull))
        assertTrue(isInherentlyReadonly(state(), ownProps = mapOf("readonly" to false), uiSchema = readonlyOptionControl, schema = null, rootData = JsonNull))
        assertFalse(isInherentlyReadonly(state(), uiSchema = readwriteOptionControl, schema = null, rootData = JsonNull))
        assertTrue(isInherentlyReadonly(state(), uiSchema = defaultControl, schema = buildJsonSchema { put("readOnly", true) }, rootData = JsonNull))
        assertTrue(isInherentlyReadonly(state(), ownProps = mapOf("readonly" to true), uiSchema = defaultControl, schema = null, rootData = JsonNull))
        assertTrue(isInherentlyReadonly(state(), uiSchema = readonlyRule, schema = null, rootData = data))
        assertFalse(isInherentlyReadonly(state(), uiSchema = writableRule, schema = null, rootData = data))
    }

    private fun control(
        options: Map<String, JsonElement> = emptyMap(),
    ): ControlElement = ControlElement(
        scope = "#/value",
        options = options,
    )

    private fun controlWithRule(
        effect: RuleEffect,
        condition: Condition,
    ): ControlElement = ControlElement(
        scope = "#/value",
        rule = Rule(effect = effect, condition = condition),
    )

    private fun jsonData(ruleValue: String) = buildJsonObject {
        put("value", "foo")
        put("ruleValue", ruleValue)
    }
}
