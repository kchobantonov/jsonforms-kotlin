package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isAnyOfControl
import com.chobantonov.jsonfroms.core.testers.rankWith

val MaterialAnyOfRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialAnyOfRenderer(props)
}

@Composable
fun MaterialAnyOfRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialAnyOfRenderer can only render Control elements")
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

    val renderInfos = createCombinatorRenderInfos(
        combinatorSubSchemas = controlProps.schema.anyOf.orEmpty(),
        rootSchema = controlProps.rootSchema,
        keyword = CombinatorKeyword.anyOf,
        control = control,
        path = controlProps.path,
        uischemas = props.state.jsonforms.uischemas,
    )
    if (renderInfos.isEmpty()) {
        return
    }

    var selectedAnyOf by remember(controlProps.schema, controlProps.path) { mutableIntStateOf(0) }
    LaunchedEffect(controlProps.schema, controlProps.path) {
        selectedAnyOf = 0
    }
    val safeIndex = selectedAnyOf.coerceIn(0, renderInfos.lastIndex)

    CombinatorProperties(
        props = props.copy(
            schema = controlProps.schema,
            rootSchema = controlProps.rootSchema,
            path = controlProps.path,
            enabled = controlProps.enabled,
        ),
        schema = controlProps.schema,
        combinatorKeyword = "anyOf",
    )

    PrimaryTabRow(
        selectedTabIndex = safeIndex,
    ) {
        renderInfos.forEachIndexed { index, renderInfo ->
            Tab(
                selected = index == safeIndex,
                onClick = {
                    if (index != safeIndex) {
                        props.dispatch(
                            UpdateAction(
                                path = controlProps.path,
                                updater = {
                                    createDefaultValue(renderInfos[index].schema, controlProps.rootSchema)
                                },
                            ),
                        )
                        selectedAnyOf = index
                    }
                },
                text = { androidx.compose.material3.Text(renderInfo.label) },
            )
        }
    }

    JsonFormsDispatchRenderer(
        props = props.copy(
            schema = renderInfos[safeIndex].schema,
            rootSchema = controlProps.rootSchema,
            uischema = renderInfos[safeIndex].uischema,
            path = controlProps.path,
            enabled = controlProps.enabled,
            id = "${controlProps.id}-anyOf-$safeIndex",
        ),
    )
}

val materialAnyOfControlTester: RankedTester = rankWith(
    3,
    isAnyOfControl,
)
