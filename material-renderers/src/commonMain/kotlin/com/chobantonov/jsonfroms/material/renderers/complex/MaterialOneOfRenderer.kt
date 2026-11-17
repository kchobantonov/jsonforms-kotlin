package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.mappers.CombinatorKeyword
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createCombinatorRenderInfos
import com.chobantonov.jsonfroms.core.mappers.findFittingSchemaIndex
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isOneOfControl
import com.chobantonov.jsonfroms.core.testers.isSingleScalarEditorCombinator
import com.chobantonov.jsonfroms.core.testers.not
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.JsonPath
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val MaterialOneOfRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialOneOfRenderer(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialOneOfRenderer(
    props: ComposeJsonFormsRendererProps,
    confirmationDialog: @Composable (String, String, String, String, () -> Unit, () -> Unit) -> Unit =
        { title, message, accept, decline, confirm, dismiss ->
            MaterialBranchConfirmationDialog(title, message, accept, decline, confirm, dismiss)
        },
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
        rootSchema = controlProps.rootSchema,
    )
    val scopedValue = JsonPath.get(props.state.jsonforms.core.data, controlProps.path)
    var selectedIndex by remember(controlProps.schema, controlProps.path) {
        mutableIntStateOf(if (scopedValue == null) -1 else fittingSchemaIndex ?: 0)
    }
    var clearedValue by remember(controlProps.schema, controlProps.path) { mutableStateOf<JsonElement?>(null) }
    var explicitlyCleared by remember(controlProps.schema, controlProps.path) { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var touched by remember(controlProps.path) { mutableStateOf(false) }
    LaunchedEffect(controlProps.schema, controlProps.path, scopedValue, fittingSchemaIndex) {
        if (scopedValue == null || (explicitlyCleared && scopedValue == clearedValue)) {
            selectedIndex = -1
        } else {
            explicitlyCleared = false
            if (fittingSchemaIndex != null) selectedIndex = fittingSchemaIndex
            else if (selectedIndex !in renderInfos.indices) selectedIndex = 0
        }
    }
    val safeIndex = selectedIndex.coerceIn(0, renderInfos.lastIndex)
    val branchChange = rememberBranchChangeController(
        props, controlProps, control, "oneOf", renderInfos, selectedIndex, { index ->
            explicitlyCleared = index == -1
            clearedValue = if (index == -1) clearOneOfBranch(scopedValue, controlProps.schema) else null
            selectedIndex = index
        },
    )
    val clearable = (control.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val hasSelection = selectedIndex >= 0 && scopedValue != null
    val branchFeedback = if (renderInfos.size == 1 || hasSelection)
        activeBranchFeedback(props, control, controlProps, renderInfos[safeIndex].schema)
    else ActiveBranchFeedback(props, controlProps.errors, controlProps.errorsBeforeTouch)
    val canClear = clearable && branchChange.interactive && hasSelection
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    var selectorFocused by remember(controlProps.path) { mutableStateOf(false) }
    var selectorHovered by remember(controlProps.path) { mutableStateOf(false) }
    var clearFocused by remember(controlProps.path) { mutableStateOf(false) }
    var clearHovered by remember(controlProps.path) { mutableStateOf(false) }
    val inputModeManager = LocalInputModeManager.current

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

    if (renderInfos.size == 1) {
        Text(controlProps.label)
        if (renderInfos[0].label != controlProps.label) Text(renderInfos[0].label)
        controlProps.description?.takeIf(String::isNotBlank)?.let { Text(it) }
    }

    if (renderInfos.size > 1) Row(verticalAlignment = Alignment.CenterVertically) {
    ExposedDropdownMenuBox(
        modifier = Modifier.weight(1f),
        expanded = expanded,
        onExpandedChange = { shouldExpand ->
            if (expanded && !shouldExpand) touched = true
            expanded = shouldExpand && branchChange.interactive
        },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, branchChange.interactive)
                .onFocusChanged {
                    if (selectorFocused && !it.isFocused) touched = true
                    selectorFocused = it.isFocused
                }
                .pointerInput(controlProps.path) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> selectorHovered = true
                                PointerEventType.Exit -> selectorHovered = false
                            }
                        }
                    }
                }
                .fillMaxWidth(),
            value = if (hasSelection) renderInfos[safeIndex].label else "",
            onValueChange = {},
            readOnly = true,
            enabled = branchChange.interactive,
            label = { Text(controlProps.label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(
            expanded = expanded && branchChange.interactive,
            onDismissRequest = { expanded = false; touched = true },
        ) {
            renderInfos.forEachIndexed { index, renderInfo ->
                DropdownMenuItem(
                    text = { Text(renderInfo.label) },
                    enabled = branchChange.interactive,
                    onClick = {
                        expanded = false
                        touched = true
                        branchChange.request(index)
                    },
                )
            }
        }
    }
    if (canClear) {
        IconButton(
            onClick = branchChange.clear,
            modifier = Modifier.size(32.dp).testTag("oneof-selector-clear")
                .onFocusChanged { clearFocused = it.isFocused }
                .pointerInput(controlProps.path) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> clearHovered = true
                                PointerEventType.Exit -> clearHovered = false
                            }
                        }
                    }
                }
                // Keep focus/hover changes in the layer so the dropdown anchor stays mounted mid-click.
                .graphicsLayer {
                    alpha = if (selectorFocused || selectorHovered || clearFocused || clearHovered ||
                        inputModeManager.inputMode == InputMode.Touch) 1f else 0f
                },
        ) {
            Icon(JsonFormsActionIcons.Clear, clearLabel, modifier = Modifier.size(16.dp))
        }
    }
    }
    CombinatorOwnErrors(if (touched) branchFeedback.errors else branchFeedback.errorsBeforeTouch)
    if (renderInfos.size == 1 || hasSelection) JsonFormsDispatchRenderer(
        props = branchFeedback.props.copy(
            schema = renderInfos[safeIndex].schema,
            rootSchema = controlProps.rootSchema,
            uischema = renderInfos[safeIndex].uischema,
            path = controlProps.path,
            enabled = controlProps.enabled,
            id = "${controlProps.id}-oneOf-$safeIndex",
        ),
    )
    }
    BranchChangeDialog(branchChange, props.state.jsonforms.i18n, confirmationDialog)
}

val materialOneOfControlTester: RankedTester = rankWith(
    3,
    and(isOneOfControl, not(isSingleScalarEditorCombinator)),
)
