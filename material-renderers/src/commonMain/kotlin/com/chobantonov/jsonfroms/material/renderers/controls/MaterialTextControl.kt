package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive

val MaterialTextControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialTextControl(props)
}

@Composable
fun MaterialTextControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialTextControl",
    )
    val textValue = bindings.stateProps.data.toMaterialInputString()

    MaterialInputControl(
        props = bindings.controlProps,
        value = textValue,
        onChange = { value ->
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                value.takeIf { it.isNotEmpty() }?.let(::JsonPrimitive),
            )
        },
    )
}

val materialTextControlTester: RankedTester = rankWith(
    1,
    isStringControl,
)
