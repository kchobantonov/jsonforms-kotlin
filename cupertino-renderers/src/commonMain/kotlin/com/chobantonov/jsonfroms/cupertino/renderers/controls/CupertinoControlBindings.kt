package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.DispatchPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControl
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.Dispatch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal data class CupertinoControlBindings(
    val stateProps: StatePropsOfControl,
    val dispatchProps: DispatchPropsOfControl,
    val controlProps: ControlProps,
    val clearLabel: String,
)

internal fun cupertinoControlBindings(
    props: ComposeJsonFormsRendererProps,
    rendererName: String,
): CupertinoControlBindings {
    val control = props.uischema as? ControlElement
        ?: error("$rendererName can only render Control elements")
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
    val dispatchProps = mapDispatchToControlProps(Dispatch { action ->
        props.dispatch(action)
        action
    })
    return CupertinoControlBindings(
        stateProps = stateProps,
        dispatchProps = dispatchProps,
        controlProps = ControlProps(
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
        ),
        clearLabel = props.state.jsonforms.i18n.translate?.translate(
            "clear",
            "Clear value",
            mapOf("control" to stateProps.label),
        ) ?: "Clear value",
    )
}

internal fun JsonElement?.toCupertinoInputString(): String = when (this) {
    null, JsonNull -> ""
    is JsonPrimitive -> contentOrNull ?: toString()
    else -> toString()
}
