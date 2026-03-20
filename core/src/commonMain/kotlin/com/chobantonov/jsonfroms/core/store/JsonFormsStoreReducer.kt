package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.reducers.cellReducer
import com.chobantonov.jsonfroms.core.reducers.configReducer
import com.chobantonov.jsonfroms.core.reducers.coreReducer
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.reducers.Middleware
import com.chobantonov.jsonfroms.core.reducers.rendererReducer

object JsonFormsStoreReducer {
    fun reduceCore(
        state: JsonFormsCoreState = initState(),
        action: JsonFormsAction,
        middleware: Middleware = defaultMiddleware,
    ): JsonFormsCoreState = middleware(state, action, coreReducer)

    @Suppress("UNCHECKED_CAST")
    fun <TRenderer, TCell> reduce(
        state: JsonFormsState<TRenderer, TCell> = JsonFormsState(),
        action: JsonFormsAction,
        middleware: Middleware = defaultMiddleware,
    ): JsonFormsState<TRenderer, TCell> = state.copy(
        jsonforms = state.jsonforms.copy(
            core = middleware(state.jsonforms.core as JsonFormsCoreState, action, coreReducer),
            config = configReducer(
                state.jsonforms.config as? Map<String, Any?> ?: emptyMap(),
                action,
            ),
            renderers = rendererReducer(state.jsonforms.renderers as List<JsonFormsRendererRegistryEntry<Any?>>, action) as List<JsonFormsRendererRegistryEntry<TRenderer>>,
            cells = cellReducer(state.jsonforms.cells as List<JsonFormsCellRendererRegistryEntry<Any?>>, action) as List<JsonFormsCellRendererRegistryEntry<TCell>>,
        ),
    )

    private fun initState(): JsonFormsCoreState = JsonFormsCoreState()
}
