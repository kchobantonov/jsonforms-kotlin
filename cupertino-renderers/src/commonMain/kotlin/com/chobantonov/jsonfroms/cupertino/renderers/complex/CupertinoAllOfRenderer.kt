package com.chobantonov.jsonfroms.cupertino.renderers.complex

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
import com.chobantonov.jsonfroms.core.util.getDetailUiSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.material.renderers.complex.CombinatorProperties
import com.chobantonov.jsonfroms.material.renderers.complex.materialAllOfControlTester

val CupertinoAllOfRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    CupertinoAllOfRenderer(props)
}

@Composable
fun CupertinoAllOfRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoAllOfRenderer can only render Control elements")
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

        CupertinoCombinatorOwnErrors(if (touched) controlProps.errors else controlProps.errorsBeforeTouch)
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

val cupertinoAllOfControlTester = withIncreasedRank(100, materialAllOfControlTester)
