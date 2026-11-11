package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.util.errorsAt
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlinx.serialization.json.JsonElement

fun getData(state: JsonFormsState<*, *>): JsonElement = state.jsonforms.core.data

fun getSchema(state: JsonFormsState<*, *>): JsonSchema = state.jsonforms.core.schema

fun getUiSchema(state: JsonFormsState<*, *>): UiSchemaElement? = state.jsonforms.core.uischema

fun getValidator(state: JsonFormsState<*, *>): JsonSchemaValidator = state.jsonforms.core.validatorEngine

fun getRenderers(state: JsonFormsState<*, *>): List<JsonFormsRendererRegistryEntry<*>> =
    state.jsonforms.renderers

fun getCells(state: JsonFormsState<*, *>): List<JsonFormsCellRendererRegistryEntry<*>> =
    state.jsonforms.cells

fun getUISchemas(state: JsonFormsState<*, *>): List<JsonFormsUISchemaRegistryEntry> =
    state.jsonforms.uischemas

fun getConfig(state: JsonFormsState<*, *>): Map<String, Any?> =
    (state.jsonforms.config as? Map<*, *>)
        ?.entries
        ?.mapNotNull { (key, value) -> (key as? String)?.let { it to value } }
        ?.toMap()
        ?: emptyMap()

fun getErrorAt(
    instancePath: String,
    schema: JsonSchema,
): (JsonFormsState<*, *>) -> List<ErrorObject> = { state ->
    val errors = if (state.jsonforms.core.validationMode == ValidationMode.ValidateAndHide) {
        state.jsonforms.core.additionalErrors
    } else {
        state.jsonforms.core.errors + state.jsonforms.core.additionalErrors
    }
    errorsAt(instancePath, schema) { path -> path == instancePath }(errors)
}

fun getSubErrorsAt(
    instancePath: String,
    schema: JsonSchema,
): (JsonFormsState<*, *>) -> List<ErrorObject> = { state ->
    val errors = if (state.jsonforms.core.validationMode == ValidationMode.ValidateAndHide) {
        state.jsonforms.core.additionalErrors
    } else {
        state.jsonforms.core.errors + state.jsonforms.core.additionalErrors
    }
    errorsAt(instancePath, schema) { path -> path.startsWith("$instancePath.") }(errors)
}
