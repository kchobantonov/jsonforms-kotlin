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
import com.chobantonov.jsonfroms.material.renderers.controls.materialOneOfEnumControlTester

val CupertinoOneOfEnumControl = ComposeJsonFormsRenderer { CupertinoOneOfEnumControl(it) }

@Composable
fun CupertinoOneOfEnumControl(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoOneOfEnumControl can only render Control elements")
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
    if (!stateProps.visible) return
    val dispatch = mapDispatchToControlProps(Dispatch { action ->
        props.dispatch(action)
        action
    })
    val clearLabel = props.state.jsonforms.i18n.translate?.translate("clear", "Clear value", emptyMap()) ?: "Clear value"
    CupertinoSelectControl(stateProps, dispatch, clearLabel)
}

val cupertinoOneOfEnumControlTester: RankedTester =
    withIncreasedRank(100, materialOneOfEnumControlTester)
