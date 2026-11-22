package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.i18n.translateRadioUnknownValue
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToOneOfEnumControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.material.renderers.controls.materialOneOfRadioGroupControlTester

val CupertinoOneOfRadioGroupControl = ComposeJsonFormsRenderer { CupertinoOneOfRadioGroupControl(it) }

@Composable
fun CupertinoOneOfRadioGroupControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoOneOfRadioGroupControl can only render Control elements")
    val stateProps = mapStateToOneOfEnumControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            id = props.id,
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                readonly = props.readonly,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
        enumProps = OwnPropsOfEnum(),
    )
    val dispatchProps = mapDispatchToControlProps(Dispatch { action ->
        props.dispatch(action)
        action
    })
    if (stateProps.visible) com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props, "radio")

    var touched by remember(stateProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(stateProps.path) { mutableStateOf(false) }
    CupertinoRadioGroup(
        focusKey = stateProps.path,
        label = stateProps.label,
        required = stateProps.required,
        description = stateProps.description,
        errors = if (touched) stateProps.errors else stateProps.errorsBeforeTouch,
        visible = stateProps.visible,
        enabled = stateProps.enabled,
        readonly = stateProps.readonly,
        config = stateProps.config,
        optionsMap = stateProps.uischema.options,
        options = stateProps.options,
        selectedValue = stateProps.data,
        dataProvided = stateProps.dataProvided,
        unknownValueLabel = translateRadioUnknownValue(props.state.jsonforms.i18n),
        clearLabel = translateControlClear(props.state.jsonforms.i18n),
        i18n = props.state.jsonforms.i18n,
        onGroupFocusChanged = { hasFocus ->
            if (hasFocus) hasReceivedFocus = true
            else if (hasReceivedFocus) touched = true
        },
        onSelect = { option ->
            dispatchProps.handleChange(stateProps.path, option.value)
        },
        onClear = {
            touched = true
            dispatchProps.handleChange(stateProps.path, null)
        },
        compact = compact,
    )
}

val cupertinoOneOfRadioGroupControlTester: RankedTester =
    withIncreasedRank(100, materialOneOfRadioGroupControlTester)
