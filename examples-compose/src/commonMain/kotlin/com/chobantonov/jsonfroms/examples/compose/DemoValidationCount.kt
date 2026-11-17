package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.actions.InitActionOptions
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.reducers.coreReducer
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.examples.StateProps

internal enum class DemoErrorSource { Schema, Host }

internal data class DemoValidationIssue(
    val source: DemoErrorSource,
    val path: String,
    val message: String,
)

/** Counts the active form's document and host errors even when the Demo pane is not mounted. */
internal fun demoValidationErrorCount(state: StateProps, mode: ValidationMode): Int =
    demoValidationIssues(state, mode).size

/** Keeps the summary's count and details tied to the same validation pass. */
internal fun demoValidationIssues(state: StateProps, mode: ValidationMode): List<DemoValidationIssue> {
    val schema = state.schema ?: Generate.jsonSchema(state.data)
    val core = (state.middleware ?: defaultMiddleware)(
        JsonFormsCoreState(),
        Actions.init(
            data = state.data,
            dataProvided = state.dataProvided,
            schema = schema,
            uischema = state.uischema,
            options = InitActionOptions(validationMode = mode, additionalErrors = state.additionalErrors),
        ),
        coreReducer,
    )
    return core.errors.map { DemoValidationIssue(DemoErrorSource.Schema, it.instancePath, it.message) } +
        core.additionalErrors.map { DemoValidationIssue(DemoErrorSource.Host, it.instancePath, it.message) }
}
