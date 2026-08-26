package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
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
fun CupertinoOneOfRadioGroupControl(props: ComposeJsonFormsRendererProps) {
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

    CupertinoRadioGroup(
        label = stateProps.label,
        required = stateProps.required,
        description = stateProps.description,
        errors = stateProps.errors,
        visible = stateProps.visible,
        enabled = stateProps.enabled,
        readonly = stateProps.readonly,
        config = stateProps.config,
        optionsMap = stateProps.uischema.options,
        options = stateProps.options,
        selectedValue = stateProps.data,
        onSelect = { option -> dispatchProps.handleChange(stateProps.path, option.value) },
    )
}

val cupertinoOneOfRadioGroupControlTester: RankedTester =
    withIncreasedRank(100, materialOneOfRadioGroupControlTester)
