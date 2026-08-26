package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isMultiLineControl
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive

val MaterialTextControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialTextControl(props)
}

val MaterialMultiTextControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialMultiTextControl(props)
}

@Composable
fun MaterialTextControl(
    props: ComposeJsonFormsRendererProps,
) = MaterialTextControl(props, singleLine = true)

@Composable
fun MaterialMultiTextControl(
    props: ComposeJsonFormsRendererProps,
) = MaterialTextControl(props, singleLine = false)

@Composable
private fun MaterialTextControl(
    props: ComposeJsonFormsRendererProps,
    singleLine: Boolean,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialTextControl",
    )
    val textValue = bindings.stateProps.data.toMaterialInputString()
    val preserveEmptyValue = LocalPreserveEmptyValue.current

    MaterialInputControl(
        props = bindings.controlProps,
        value = textValue,
        singleLine = singleLine,
        clearLabel = bindings.clearLabel,
        onChange = { value ->
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null,
            )
        },
    )
}

val materialTextControlTester: RankedTester = rankWith(
    1,
    isStringControl,
)

val materialMultiTextControlTester: RankedTester = rankWith(
    2,
    and(isStringControl, isMultiLineControl),
)
