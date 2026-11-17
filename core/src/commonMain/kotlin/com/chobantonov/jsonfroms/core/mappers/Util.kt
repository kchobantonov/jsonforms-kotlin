package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsState
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.util.hasEnableRule
import com.chobantonov.jsonfroms.core.util.hasReadonlyRule
import kotlinx.serialization.json.JsonPrimitive

fun isInherentlyEnabled(
    state: JsonFormsState<*, *>,
    ownProps: Map<String, Any?> = emptyMap(),
    uiSchema: UiSchemaElement,
    schema: JsonSchema?,
    rootData: kotlinx.serialization.json.JsonElement,
    config: Map<String, Any?> = emptyMap(),
    path: String? = null,
): Boolean {
    val inheritedEnabled = ownProps["enabled"] as? Boolean ?: true
    if (!inheritedEnabled) {
        return false
    }
    if (config["separateReadonlyFromDisabled"] != true) {
        if (isInherentlyReadonly(state, ownProps, uiSchema, schema, rootData, config, path)) return false
    }
    return !hasEnableRule(uiSchema) || Runtime.isEnabled(
        uiSchema, rootData, state.jsonforms.core.validatorEngine, path, config)
}

fun isInherentlyReadonly(
    state: JsonFormsState<*, *>,
    ownProps: Map<String, Any?> = emptyMap(),
    uiSchema: UiSchemaElement,
    schema: JsonSchema?,
    rootData: kotlinx.serialization.json.JsonElement,
    config: Map<String, Any?> = emptyMap(),
    path: String? = null,
): Boolean {
    if (state.jsonforms.readonly) {
        return true
    }
    if (hasReadonlyRule(uiSchema)) {
        return Runtime.isReadonly(uiSchema, rootData, state.jsonforms.core.validatorEngine, path, config)
    }
    readBooleanOption(uiSchema, "readonly")?.let { return it }
    readBooleanOption(uiSchema, "readOnly")?.let { return it }
    (config["readonly"] as? Boolean)?.let { return it }
    (config["readOnly"] as? Boolean)?.let { return it }
    if (readBooleanSchemaProperty(schema, "readOnly") == true) {
        return true
    }
    return ownProps["readonly"] as? Boolean ?: false
}

private fun readBooleanOption(
    uiSchema: UiSchemaElement,
    name: String,
): Boolean? = (uiSchema.options[name] as? JsonPrimitive)
    ?.takeUnless { it.isString }?.content?.toBooleanStrictOrNull()

private fun readBooleanSchemaProperty(
    schema: JsonSchema?,
    name: String,
): Boolean? = (schema?.get(name) as? JsonPrimitive)
    ?.takeUnless { it.isString }?.content?.toBooleanStrictOrNull()
