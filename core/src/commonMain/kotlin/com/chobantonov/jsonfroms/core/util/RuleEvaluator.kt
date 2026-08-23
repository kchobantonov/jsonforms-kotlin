package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.AndCondition
import com.chobantonov.jsonfroms.core.models.Condition
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.OrCondition
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.ValidateFunctionContext
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

object RuleEvaluator {
    fun isVisible(
        uiSchemaElement: UiSchemaElement,
        data: JsonData,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = evaluateRuleEffect(uiSchemaElement, data, validator, path, config, visibleByDefault = true)

    fun isEnabled(
        uiSchemaElement: UiSchemaElement,
        data: JsonData,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = evaluateRuleEffect(uiSchemaElement, data, validator, path, config, enabledByDefault = true)

    fun isReadonly(
        uiSchemaElement: UiSchemaElement,
        data: JsonData,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = evaluateRuleEffect(uiSchemaElement, data, validator, path, config, readonlyByDefault = false)

    fun evaluateCondition(
        condition: Condition,
        data: JsonData,
        validator: JsonSchemaValidator,
        uiSchemaElement: UiSchemaElement = UnknownUiSchemaElement(type = "Unknown"),
        path: String? = null,
        config: Any? = null,
    ): Boolean =
        when (condition) {
            is AndCondition -> condition.conditions.all { evaluateCondition(it, data, validator, uiSchemaElement, path, config) }
            is OrCondition -> condition.conditions.any { evaluateCondition(it, data, validator, uiSchemaElement, path, config) }
            is LeafCondition -> evaluateLeafCondition(condition, data)
            is ValidateFunctionCondition -> condition.validate(
                ValidateFunctionContext(
                    data = resolveScopedData(data, condition.scope),
                    fullData = data,
                    path = path,
                    uischemaElement = uiSchemaElement,
                    config = config,
                ),
            )
            is SchemaBasedCondition -> evaluateSchemaBasedCondition(condition, data, validator)
        }

    private fun evaluateRuleEffect(
        uiSchemaElement: UiSchemaElement,
        data: JsonData,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
        visibleByDefault: Boolean = false,
        enabledByDefault: Boolean = false,
        readonlyByDefault: Boolean = false,
    ): Boolean {
        val rule = uiSchemaElement.rule ?: return when {
            visibleByDefault -> true
            enabledByDefault -> true
            else -> readonlyByDefault
        }

        val fulfilled = evaluateCondition(rule.condition, data, validator, uiSchemaElement, path, config)
        return when (rule.effect) {
            RuleEffect.SHOW -> fulfilled
            RuleEffect.HIDE -> !fulfilled
            RuleEffect.ENABLE -> fulfilled
            RuleEffect.DISABLE -> !fulfilled
            RuleEffect.READONLY -> fulfilled
            RuleEffect.WRITABLE -> !fulfilled
        }
    }

    private fun evaluateLeafCondition(
        condition: LeafCondition,
        data: JsonData,
    ): Boolean {
        val candidate = resolveScopedData(data, condition.scope)
        val expectedValue = condition.expectedValue ?: JsonNull
        return candidate == expectedValue
    }

    private fun evaluateSchemaBasedCondition(
        condition: SchemaBasedCondition,
        data: JsonData,
        validator: JsonSchemaValidator,
    ): Boolean {
        val scopedData = resolveScopedData(data, condition.scope)
        if (scopedData == null) {
            return !condition.failWhenUndefined
        }

        return validator.validate(condition.schema, scopedData).isEmpty()
    }

    private fun resolveScopedData(
        data: JsonElement,
        scope: String?,
    ): JsonElement? {
        if (scope.isNullOrBlank() || scope == "#") return data

        val segments = scope
            .removePrefix("#")
            .split('/')
            .filter(String::isNotEmpty)
        val isSchemaPath = segments.any { segment ->
            segment in setOf("properties", "anyOf", "allOf", "oneOf", "then", "else")
        }

        return if (isSchemaPath) {
            Resolve.data(data, toDataPath(scope))
        } else {
            // Retain support for direct JSON Pointer condition scopes such as "#/role".
            JsonPointer.resolve(data, scope)
        }
    }
}
