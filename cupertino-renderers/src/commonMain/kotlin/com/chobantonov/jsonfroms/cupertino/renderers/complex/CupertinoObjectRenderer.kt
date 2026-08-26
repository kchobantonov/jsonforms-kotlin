package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlWithDetailProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoGroupLayout
import com.chobantonov.jsonfroms.material.renderers.complex.resolveObjectDetailUiSchema

val CupertinoObjectRenderer = ComposeJsonFormsRenderer { CupertinoObjectRenderer(it) }

@Composable
fun CupertinoObjectRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoObjectRenderer can only render Control elements")
    val detailProps = mapStateToControlWithDetailProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
                uischemas = props.state.jsonforms.uischemas,
            ),
        ),
    )
    if (!detailProps.visible) return

    val detailUiSchema = resolveObjectDetailUiSchema(detailProps, control)
    val detailRendererProps = props.copy(
        schema = detailProps.schema,
        uischema = detailUiSchema,
        path = detailProps.path,
        enabled = detailProps.enabled,
        renderers = props.renderers,
        cells = props.cells,
    )
    if (detailUiSchema is GroupElement) {
        CupertinoGroupLayout(detailRendererProps) {
            CupertinoAdditionalProperties(props, detailProps)
        }
    } else {
        JsonFormsDispatchRenderer(detailRendererProps)
        CupertinoAdditionalProperties(props, detailProps)
    }
}

val cupertinoObjectControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isObjectControl))
