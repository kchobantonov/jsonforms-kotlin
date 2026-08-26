package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import kotlinx.serialization.json.JsonPrimitive

@Composable
internal fun CupertinoNumericControl(props: ComposeJsonFormsRendererProps, integer: Boolean) {
    val bindings = cupertinoControlBindings(props, if (integer) "CupertinoIntegerControl" else "CupertinoNumberControl")
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val committed = bindings.stateProps.data.toCupertinoInputString()
    var draft by remember(bindings.stateProps.path) { mutableStateOf(committed) }
    var lastCommitted by remember(bindings.stateProps.path) { mutableStateOf(committed) }
    LaunchedEffect(committed) {
        if (committed != lastCommitted) {
            draft = committed
            lastCommitted = committed
        }
    }
    val error = when {
        draft.isEmpty() -> null
        integer && draft.toLongOrNull() == null -> "Invalid integer."
        !integer && draft.toDoubleOrNull()?.isFinite() != true -> "Invalid number."
        else -> null
    }
    CupertinoInputControl(
        props = bindings.controlProps,
        value = draft,
        keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
        localError = error,
        clearLabel = bindings.clearLabel,
    ) { value ->
        draft = value
        lastCommitted = value
        val data = when {
            value.isEmpty() && preserveEmptyValue -> if (integer) JsonPrimitive(0) else JsonPrimitive(0.0)
            value.isEmpty() -> null
            integer -> value.toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(value)
            else -> value.toDoubleOrNull()?.takeIf(Double::isFinite)?.let(::JsonPrimitive) ?: JsonPrimitive(value)
        }
        bindings.dispatchProps.handleChange(bindings.stateProps.path, data)
    }
}
