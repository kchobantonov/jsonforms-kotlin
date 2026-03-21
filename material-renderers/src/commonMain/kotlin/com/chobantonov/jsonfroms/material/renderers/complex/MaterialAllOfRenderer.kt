package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isAllOfControl
import com.chobantonov.jsonfroms.core.testers.rankWith

val MaterialAllOfRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialAllOfRenderer(props)
}

@Composable
fun MaterialAllOfRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialAllOfRenderer can only render Control elements")
    val controlProps = mapStateToControlProps(
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

    if (!controlProps.visible) {
        return
    }

    val delegateUiSchema = findMatchingUISchema(props.state.jsonforms.uischemas)(
        controlProps.schema,
        control.scope,
        controlProps.path,
    )
    if (delegateUiSchema != null) {
        JsonFormsDispatchRenderer(
            props = props.copy(
                schema = controlProps.schema,
                rootSchema = controlProps.rootSchema,
                uischema = delegateUiSchema,
                path = controlProps.path,
                enabled = controlProps.enabled,
            ),
        )
        return
    }

    val renderInfos = createCombinatorRenderInfos(
        combinatorSubSchemas = controlProps.schema.allOf.orEmpty(),
        rootSchema = controlProps.rootSchema,
        keyword = CombinatorKeyword.allOf,
        control = control,
        path = controlProps.path,
        uischemas = props.state.jsonforms.uischemas,
    )

    renderInfos.forEachIndexed { index, renderInfo ->
        JsonFormsDispatchRenderer(
            props = props.copy(
                schema = renderInfo.schema,
                rootSchema = controlProps.rootSchema,
                uischema = renderInfo.uischema,
                path = controlProps.path,
                enabled = controlProps.enabled,
                id = "${controlProps.id}-allOf-$index",
            ),
        )
    }
}

val materialAllOfControlTester: RankedTester = rankWith(
    3,
    isAllOfControl,
)
