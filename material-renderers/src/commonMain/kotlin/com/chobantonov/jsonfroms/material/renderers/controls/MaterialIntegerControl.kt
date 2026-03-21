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
import com.chobantonov.jsonfroms.core.testers.isIntegerControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonNull
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
        onChange = { value ->
            draftValue = value
            when {
                value.isBlank() -> {
                    lastCommittedValue = ""
                    bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonNull)
                }

                value.toIntOrNull() != null -> {
                    lastCommittedValue = value
                    bindings.dispatchProps.handleChange(
                        bindings.stateProps.path,
                        JsonPrimitive(value.toInt()),
                    )
                }
            }
        },
    )
}

val materialIntegerControlTester: RankedTester = rankWith(
    2,
    isIntegerControl,
)
