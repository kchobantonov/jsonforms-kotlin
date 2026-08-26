package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.InitAction
import com.chobantonov.jsonfroms.core.actions.InitActionOptions
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.SetSchemaAction
import com.chobantonov.jsonfroms.core.actions.SetUISchemaAction
import com.chobantonov.jsonfroms.core.actions.SetValidationModeAction
import com.chobantonov.jsonfroms.core.actions.SetValidatorAction
import com.chobantonov.jsonfroms.core.actions.UpdateCoreAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.actions.UpdateErrorsAction
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.Reducer
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlinx.serialization.json.JsonNull

val initState = JsonFormsCoreState()

val coreReducer: Reducer<JsonFormsCoreState, JsonFormsAction> = Reducer { state, action ->
    val currentState = state ?: initState
    when (action) {
        is InitAction -> initialize(
            currentState, action.data, action.dataProvided, action.schema, action.uiSchema, action.options,
        )
        is UpdateCoreAction -> initialize(
            currentState, action.data, action.dataProvided, action.schema, action.uiSchema, action.options,
        )
        is SetValidatorAction -> {
            val compiled = compileIfEnabled(action.validator, currentState.schema, currentState.validationMode)
            currentState.copy(
                validatorEngine = action.validator,
                validator = compiled,
                errors = validate(compiled, currentState.data),
            )
        }

        is SetSchemaAction -> {
            val compiled = compileIfEnabled(currentState.validatorEngine, action.schema, currentState.validationMode)
            currentState.copy(
                schema = action.schema,
                validator = compiled,
                errors = validate(compiled, currentState.data),
            )
        }

        is SetUISchemaAction -> currentState.copy(uischema = action.uiSchema)
        is UpdateAction -> {
            val updated = if (action.path.isEmpty()) {
                action.updater(currentState.data) ?: JsonNull
            } else {
                val existing = JsonPath.get(currentState.data, action.path)
                JsonPath.set(currentState.data, action.path, action.updater(existing), currentState.schema)
            }
            currentState.copy(
                data = updated,
                dataProvided = if (action.path.isEmpty()) action.dataProvided ?: true else currentState.dataProvided,
                errors = validate(currentState.validator, updated),
            )
        }

        is UpdateErrorsAction -> currentState.copy(errors = action.errors)
        is SetValidationModeAction -> updateValidationMode(currentState, action.validationMode)
        else -> currentState
    }
}

private fun initialize(
    state: JsonFormsCoreState,
    data: JsonData,
    dataProvided: Boolean,
    schema: JsonSchema,
    uiSchema: UiSchemaElement?,
    options: InitActionOptions?,
): JsonFormsCoreState {
    val validatorEngine = options?.validator ?: state.validatorEngine
    val validationMode = options?.validationMode ?: state.validationMode
    val compiled = compileIfEnabled(validatorEngine, schema, validationMode)
    return state.copy(
        data = data,
        dataProvided = dataProvided,
        schema = schema,
        uischema = uiSchema,
        validatorEngine = validatorEngine,
        validator = compiled,
        errors = validate(compiled, data),
        validationMode = validationMode,
        additionalErrors = options?.additionalErrors ?: state.additionalErrors,
    )
}

private fun updateValidationMode(
    state: JsonFormsCoreState,
    validationMode: ValidationMode,
): JsonFormsCoreState {
    if (state.validationMode == validationMode) {
        return state
    }

    return when (validationMode) {
        ValidationMode.NoValidation -> state.copy(
            validationMode = validationMode,
            errors = emptyList(),
            validator = null,
        )

        else -> {
            val compiled = state.validatorEngine.compile(state.schema)
            state.copy(
                validationMode = validationMode,
                validator = compiled,
                errors = validate(compiled, state.data),
            )
        }
    }
}

private fun compileIfEnabled(
    validatorEngine: JsonSchemaValidator,
    schema: JsonSchema,
    validationMode: ValidationMode,
): ValidateFunction? =
    if (validationMode == ValidationMode.NoValidation) {
        null
    } else {
        validatorEngine.compile(schema)
    }

private fun validate(
    validator: ValidateFunction?,
    data: kotlinx.serialization.json.JsonElement,
): List<ErrorObject> = validator?.validate(data).orEmpty()
