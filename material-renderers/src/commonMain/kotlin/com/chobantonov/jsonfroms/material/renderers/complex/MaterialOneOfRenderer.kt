package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.findFittingSchemaIndex
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isOneOfControl
import com.chobantonov.jsonfroms.core.testers.rankWith

val MaterialOneOfRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialOneOfRenderer(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialOneOfRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialOneOfRenderer can only render Control elements")
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
        combinatorSubSchemas = controlProps.schema.oneOf.orEmpty(),
        rootSchema = controlProps.rootSchema,
        keyword = CombinatorKeyword.oneOf,
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
    )
    var selectedIndex by remember(controlProps.schema, controlProps.path) {
        mutableIntStateOf(fittingSchemaIndex ?: 0)
    }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(controlProps.schema, controlProps.path, fittingSchemaIndex) {
        if (fittingSchemaIndex != null) {
            selectedIndex = fittingSchemaIndex
        } else if (selectedIndex !in renderInfos.indices) {
            selectedIndex = 0
        }
    }
    val safeIndex = selectedIndex.coerceIn(0, renderInfos.lastIndex)

    CombinatorProperties(
        props = props.copy(
            schema = controlProps.schema,
            rootSchema = controlProps.rootSchema,
            path = controlProps.path,
            enabled = controlProps.enabled,
        ),
        schema = controlProps.schema,
        combinatorKeyword = "oneOf",
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            value = renderInfos[safeIndex].label,
            onValueChange = {},
            readOnly = true,
            enabled = controlProps.enabled,
            label = { Text(controlProps.label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            renderInfos.forEachIndexed { index, renderInfo ->
                DropdownMenuItem(
                    text = { Text(renderInfo.label) },
                    onClick = {
                        expanded = false
                        if (index != safeIndex) {
                            props.dispatch(
                                UpdateAction(
                                    path = controlProps.path,
                                    updater = {
                                        createDefaultValue(renderInfos[index].schema, controlProps.rootSchema)
                                    },
                                ),
                            )
                            selectedIndex = index
                        }
                    },
                )
            }
        }
    }

    JsonFormsDispatchRenderer(
        props = props.copy(
            schema = renderInfos[safeIndex].schema,
            rootSchema = controlProps.rootSchema,
            uischema = renderInfos[safeIndex].uischema,
            path = controlProps.path,
            enabled = controlProps.enabled,
            id = "${controlProps.id}-oneOf-$safeIndex",
        ),
    )
}

val materialOneOfControlTester: RankedTester = rankWith(
    3,
    isOneOfControl,
)
