package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.isTimeControl
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.controls.stringOption
import kotlinx.serialization.json.JsonPrimitive

val CupertinoNativeControl = ComposeJsonFormsRenderer { CupertinoNativeControl(it) }
val cupertinoNativeControlTester = rankWith(2, or(isDateControl, isTimeControl))

@Composable
private fun CupertinoNativeControl(props: ComposeJsonFormsRendererProps) {
    val bindings = cupertinoControlBindings(props, "CupertinoNativeControl")
    val control = bindings.controlProps
    val format = (control.config + control.uischema.options).stringOption("format") ?: control.schema.format
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data,
        bindings.stateProps.dataProvided)?.let {
        "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it"
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    CupertinoInputControl(
        props = control,
        value = bindings.stateProps.data.toCupertinoInputString(),
        keyboardType = KeyboardType.Text,
        placeholder = control.uischema.options.stringOption("placeholder") ?: format,
        clearLabel = bindings.clearLabel,
        storedValueStatus = invalidStatus,
        onChange = { value ->
            bindings.dispatchProps.handleChange(control.path,
                if (value.isNotEmpty() || preserveEmptyValue) JsonPrimitive(value) else null)
        },
    )
}
