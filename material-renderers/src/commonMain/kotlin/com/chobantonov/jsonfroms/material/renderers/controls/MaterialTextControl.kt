package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.store.Dispatch
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
    val control = props.uischema as? com.chobantonov.jsonfroms.core.models.ControlElement
        ?: error("MaterialTextControl can only render Control elements")
    val stateProps = mapStateToControlProps(
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
    )
    val dispatchProps = mapDispatchToControlProps(
        Dispatch { action ->
            props.dispatch(action)
            action
        },
    )
    val controlProps = ControlProps(
        config = stateProps.config,
        uischema = stateProps.uischema,
        schema = stateProps.schema,
        data = stateProps.data,
        enabled = stateProps.enabled,
        readonly = stateProps.readonly,
        visible = stateProps.visible,
        path = stateProps.path,
        renderers = stateProps.renderers,
        cells = stateProps.cells,
        errors = stateProps.errors,
        rootSchema = stateProps.rootSchema,
        id = stateProps.id,
        label = stateProps.label,
        description = stateProps.description,
        required = stateProps.required,
        i18nKeyPrefix = stateProps.i18nKeyPrefix,
        handleChange = dispatchProps::handleChange,
    )
    val textValue = stateProps.data.toString().removeSurrounding("\"")

    MaterialInputControl(
        props = controlProps,
        value = textValue,
        onChange = { value ->
            dispatchProps.handleChange(stateProps.path, JsonPrimitive(value))
        },
    )
}

val materialTextControlTester: RankedTester = rankWith(
    1,
    isStringControl,
)
