package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isDateTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.defaultDateTimeFormat
import kotlinx.serialization.json.JsonPrimitive

val MaterialDateTimeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialDateTimeControl(props)
}

@Composable
fun MaterialDateTimeControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialDateTimeControl",
    )
    val controlProps = bindings.controlProps
    val appliedOptions = controlProps.config + controlProps.uischema.options
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var lastCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }

    LaunchedEffect(committedValue) {
        if (committedValue != lastCommittedValue) {
            draftValue = committedValue
            lastCommittedValue = committedValue
        }
    }

    val placeholder = (appliedOptions["dateTimeFormat"] as? String)
        ?: (appliedOptions["dateTimeSaveFormat"] as? String)
        ?: defaultDateTimeFormat

    MaterialInputControl(
        props = controlProps,
        value = draftValue,
        keyboardType = KeyboardType.Text,
        placeholder = placeholder,
        onChange = { value ->
            draftValue = value
            lastCommittedValue = value
            bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
        },
    )
}

val materialDateTimeControlTester: RankedTester = rankWith(
    2,
    isDateTimeControl,
)
