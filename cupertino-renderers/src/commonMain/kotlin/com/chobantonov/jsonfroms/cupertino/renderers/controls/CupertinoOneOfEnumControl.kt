package com.chobantonov.jsonfroms.cupertino.renderers.controls

import com.chobantonov.jsonfroms.core.i18n.translateControlClear

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
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
internal fun CupertinoPlainOneOfEnumCell(props: ComposeJsonFormsCellProps) =
    CupertinoOneOfEnumControl(props.asRendererProps(), compact = true)

@Composable
fun CupertinoOneOfEnumControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
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
                readonly = props.readonly,
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
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    CupertinoSelectControl(stateProps, dispatch, clearLabel,
        hasValue = com.chobantonov.jsonfroms.core.util.JsonPath.get(
            props.state.jsonforms.core.data, stateProps.path) != null,
        compact = compact)
}

val cupertinoOneOfEnumControlTester: RankedTester =
    withIncreasedRank(100, materialOneOfEnumControlTester)
