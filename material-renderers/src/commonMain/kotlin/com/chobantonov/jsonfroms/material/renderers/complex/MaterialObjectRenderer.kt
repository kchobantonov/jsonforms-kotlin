package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlWithDetailProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectControl
import com.chobantonov.jsonfroms.core.testers.rankWith

val MaterialObjectRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialObjectRenderer(props)
}

@Composable
fun MaterialObjectRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialObjectRenderer can only render Control elements")
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

    if (!detailProps.visible) {
        return
    }

    val detailUiSchema = resolveDetailUiSchema(
        props = detailProps,
        control = control,
    )

    JsonFormsDispatchRenderer(
        props = props.copy(
            schema = detailProps.schema,
            uischema = detailUiSchema,
            path = detailProps.path,
            enabled = detailProps.enabled,
            renderers = props.renderers,
            cells = props.cells,
        ),
    )
}

private fun resolveDetailUiSchema(
    props: com.chobantonov.jsonfroms.core.mappers.StatePropsOfControlWithDetail,
    control: ControlElement,
): UiSchemaElement {
    val matched = findMatchingUISchema(props.uischemas)(props.schema, control.scope, props.path)
    if (matched != null) {
        return matched
    }

    return if (props.path.isBlank()) {
        Generate.uiSchema(
            jsonSchema = props.schema,
            layoutType = "VerticalLayout",
            rootSchema = props.rootSchema,
        )
    } else {
        val generated = Generate.uiSchema(
            jsonSchema = props.schema,
            layoutType = "Group",
            rootSchema = props.rootSchema,
        )
        if (generated is GroupElement) {
            generated.copy(label = props.label)
        } else {
            GroupElement(
                label = props.label,
                elements = listOf(generated),
            )
        }
    }
}

val materialObjectControlTester: RankedTester = rankWith(
    2,
    isObjectControl,
)
