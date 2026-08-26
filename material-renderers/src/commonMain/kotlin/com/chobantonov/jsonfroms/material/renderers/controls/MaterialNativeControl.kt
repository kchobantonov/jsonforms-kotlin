package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.isTimeControl
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive

val MaterialNativeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialNativeControl(props)
}

@Composable
fun MaterialNativeControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialNativeControl",
    )
    val controlProps = bindings.controlProps
    val appliedOptions = controlProps.config + controlProps.uischema.options
    val fieldType = (appliedOptions["format"] as? String) ?: controlProps.schema.format
    val placeholder = fieldType ?: controlProps.schema.format
    val preserveEmptyValue = LocalPreserveEmptyValue.current

    MaterialInputControl(
        props = controlProps,
        value = bindings.stateProps.data.toMaterialInputString(),
        keyboardType = KeyboardType.Text,
        placeholder = placeholder,
        clearLabel = bindings.clearLabel,
        onChange = { value ->
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null,
            )
        },
    )
}

val materialNativeControlTester: RankedTester = rankWith(
    2,
    or(isDateControl, isTimeControl),
)
