package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import com.chobantonov.jsonfroms.core.testers.isSingleScalarEditorCombinator
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.not
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.getDetailUiSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
    com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props, "allOf")

    val explicitGenerate = (control.options["detail"] as? JsonPrimitive)?.contentOrNull
        ?.equals("GENERATE", ignoreCase = true) == true
    val delegateUiSchema = if (explicitGenerate || getDetailUiSchema(control) != null) null else
        findMatchingUISchema(props.state.jsonforms.uischemas)(
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

    var touched by remember(controlProps.path) { mutableStateOf(false) }
    var hasFocus by remember(controlProps.path) { mutableStateOf(false) }
    var hadFocus by remember(controlProps.path) { mutableStateOf(false) }
    LaunchedEffect(hasFocus) {
        if (hadFocus && !hasFocus) {
            withFrameNanos { }
            if (!hasFocus) touched = true
        }
    }
    Column(Modifier.onFocusChanged { focusState ->
        hasFocus = focusState.hasFocus
        if (hasFocus) hadFocus = true
    }) {
        CombinatorProperties(
            props = props.copy(
                schema = controlProps.schema,
                rootSchema = controlProps.rootSchema,
                path = controlProps.path,
                enabled = controlProps.enabled,
            ),
            schema = controlProps.schema,
            combinatorKeyword = "allOf",
        )

        CombinatorOwnErrors(if (touched) controlProps.errors else controlProps.errorsBeforeTouch)
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
}

val materialAllOfControlTester: RankedTester = rankWith(
    3,
    and(isAllOfControl, not(isSingleScalarEditorCombinator)),
)
