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
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isIntegerControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive

val MaterialIntegerControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialIntegerControl(props)
}

@Composable
fun MaterialIntegerControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialIntegerControl",
    )
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var lastCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    val parsedDraft = parseIntegerInput(draftValue)
    val preserveEmptyValue = LocalPreserveEmptyValue.current

    LaunchedEffect(committedValue) {
        if (committedValue != lastCommittedValue) {
            draftValue = committedValue
            lastCommittedValue = committedValue
        }
    }

    MaterialInputControl(
        props = bindings.controlProps,
        value = draftValue,
        keyboardType = KeyboardType.Number,
        localError = parsedDraft.error,
        clearLabel = bindings.clearLabel,
        onChange = { value ->
            draftValue = value
            lastCommittedValue = value
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                if (value.isEmpty() && preserveEmptyValue) JsonPrimitive(0) else parseIntegerInput(value).data,
            )
        },
    )
}

val materialIntegerControlTester: RankedTester = rankWith(
    2,
    isIntegerControl,
)
