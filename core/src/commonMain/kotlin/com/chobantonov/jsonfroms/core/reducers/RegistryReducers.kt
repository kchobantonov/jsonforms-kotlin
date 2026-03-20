package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.AddCellRendererAction
import com.chobantonov.jsonfroms.core.actions.AddRendererAction
import com.chobantonov.jsonfroms.core.actions.AddUISchemaAction
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.RemoveCellRendererAction
import com.chobantonov.jsonfroms.core.actions.RemoveRendererAction
import com.chobantonov.jsonfroms.core.actions.RemoveUISchemaAction
import com.chobantonov.jsonfroms.core.actions.SetConfigAction
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.Reducer
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE

typealias ValidRendererReducerActions = JsonFormsAction
typealias ValidCellReducerActions = JsonFormsAction
typealias UISchemaReducerActions = JsonFormsAction

val rendererReducer: Reducer<List<JsonFormsRendererRegistryEntry<Any?>>, ValidRendererReducerActions> = Reducer { state, action ->
    val currentState = state ?: emptyList()
    when (action) {
        is AddRendererAction<*> -> currentState + JsonFormsRendererRegistryEntry(action.tester, action.renderer)
        is RemoveRendererAction -> currentState.filterNot { it.tester == action.tester }
        else -> currentState
    }
}

val cellReducer: Reducer<List<JsonFormsCellRendererRegistryEntry<Any?>>, ValidCellReducerActions> = Reducer { state, action ->
    val currentState = state ?: emptyList()
    when (action) {
        is AddCellRendererAction<*> -> currentState + JsonFormsCellRendererRegistryEntry(action.tester, action.cell)
        is RemoveCellRendererAction -> currentState.filterNot { it.tester == action.tester }
        else -> currentState
    }
}

val configReducer: Reducer<Map<String, Any?>, JsonFormsAction> = Reducer { state, action ->
    val currentState = state ?: defaultConfig()
    when (action) {
        is SetConfigAction -> defaultConfig() + action.config
        else -> currentState
    }
}

val uischemaRegistryReducer: Reducer<List<JsonFormsUISchemaRegistryEntry>, UISchemaReducerActions> = Reducer { state, action ->
    val currentState = state ?: emptyList()
    when (action) {
        is AddUISchemaAction -> currentState + JsonFormsUISchemaRegistryEntry(action.tester, action.uiSchema)
        is RemoveUISchemaAction -> currentState.filterNot { it.tester == action.tester }
        else -> currentState
    }
}

fun findMatchingUISchema(
    state: List<JsonFormsUISchemaRegistryEntry>,
): (JsonSchema?, String, String) -> UiSchemaElement? = { schema, schemaPath, path ->
    state
        .map { entry -> entry to entry.tester(schema, schemaPath, path) }
        .filter { (_, rank) -> rank > NOT_APPLICABLE }
        .maxByOrNull { (_, rank) -> rank }
        ?.first
        ?.uischema
}

fun findUISchema(
    state: List<JsonFormsUISchemaRegistryEntry>,
    schema: JsonSchema,
    schemaPath: String,
    path: String,
): UiSchemaElement =
    findMatchingUISchema(state)(schema, schemaPath, path) ?: Generate.uiSchema(schema)

fun defaultConfig(): Map<String, Any?> = mapOf(
    "restrict" to false,
    "trim" to false,
    "showUnfocusedDescription" to false,
    "hideRequiredAsterisk" to false,
    "separateReadonlyFromDisabled" to false,
)
