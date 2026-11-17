package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.findFittingSchemaIndex
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isAnyOfControl
import com.chobantonov.jsonfroms.core.testers.isSingleScalarEditorCombinator
import com.chobantonov.jsonfroms.core.testers.not
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

    val fittingSchemaIndex = findFittingSchemaIndex(
        combinatorSubSchemas = renderInfos.map { it.schema },
        data = controlProps.data,
        validator = getValidator(props.state),
        rootSchema = controlProps.rootSchema,
    )
    var selectedAnyOf by remember(controlProps.schema, controlProps.path) {
        mutableIntStateOf(fittingSchemaIndex ?: 0)
    }
    var touched by remember(controlProps.path) { mutableStateOf(false) }
    LaunchedEffect(controlProps.schema, controlProps.path, fittingSchemaIndex) {
        if (fittingSchemaIndex != null) {
            selectedAnyOf = fittingSchemaIndex
        } else if (selectedAnyOf !in renderInfos.indices) {
            selectedAnyOf = 0
        }
    }
    val safeIndex = selectedAnyOf.coerceIn(0, renderInfos.lastIndex)
    val branchFeedback = activeBranchFeedback(props, control, controlProps, renderInfos[safeIndex].schema)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

    if (renderInfos.size == 1) {
        Text(controlProps.label)
        if (renderInfos[0].label != controlProps.label) Text(renderInfos[0].label)
        controlProps.description?.takeIf(String::isNotBlank)?.let { Text(it) }
    }

    if (renderInfos.size > 1) PrimaryTabRow(
        selectedTabIndex = safeIndex,
    ) {
        renderInfos.forEachIndexed { index, renderInfo ->
            Tab(
                selected = index == safeIndex,
                onClick = { selectedAnyOf = index; touched = true },
                text = { androidx.compose.material3.Text(renderInfo.label) },
            )
        }
    }

    CombinatorOwnErrors(if (touched) branchFeedback.errors else branchFeedback.errorsBeforeTouch)
    JsonFormsDispatchRenderer(
        props = branchFeedback.props.copy(
            schema = renderInfos[safeIndex].schema,
            rootSchema = controlProps.rootSchema,
            uischema = renderInfos[safeIndex].uischema,
            path = controlProps.path,
            enabled = controlProps.enabled,
            id = "${controlProps.id}-anyOf-$safeIndex",
        ),
    )
    }
}

val materialAnyOfControlTester: RankedTester = rankWith(
    3,
    and(isAnyOfControl, not(isSingleScalarEditorCombinator)),
)
