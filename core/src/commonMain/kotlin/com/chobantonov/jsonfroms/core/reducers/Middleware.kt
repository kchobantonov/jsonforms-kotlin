package com.chobantonov.jsonfroms.core.reducers

import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.Reducer

fun interface Middleware {
    operator fun invoke(
        state: JsonFormsCoreState,
        action: JsonFormsAction,
        defaultReducer: Reducer<JsonFormsCoreState, JsonFormsAction>,
    ): JsonFormsCoreState
}

val defaultMiddleware: Middleware = Middleware { state, action, defaultReducer ->
    defaultReducer(state, action)
}
