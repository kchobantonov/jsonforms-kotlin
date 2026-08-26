package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.compositionLocalOf
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import kotlinx.serialization.json.JsonElement

/** Lets controls retain an empty value when their dynamic container owns removal. */
val LocalPreserveEmptyValue = compositionLocalOf { false }

/**
 * Protects a dynamic field from the core update convention where a `null` result removes a nested value.
 * Removing the field remains the responsibility of its explicit delete action.
 */
fun preserveDynamicValueOnClear(
    dispatch: (JsonFormsAction) -> Unit,
    path: String,
    emptyValue: JsonElement,
): (JsonFormsAction) -> Unit = { action ->
    val guardedAction = if (action is UpdateAction && action.path == path) {
        action.copy(updater = { current -> action.updater(current) ?: emptyValue })
    } else {
        action
    }
    dispatch(guardedAction)
}
