package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateRadioUnknownValue

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.util.jsonSchemaChoiceEquals
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.accessibleChoiceLabel
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToOneOfEnumControlProps
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isOneOfEnumControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.InputAdornment
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val MaterialOneOfEnumControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialOneOfEnumControl(props)
}

@Composable
internal fun MaterialPlainOneOfEnumCell(props: ComposeJsonFormsCellProps) =
    MaterialOneOfEnumControl(props.asRendererProps(), compact = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialOneOfEnumControl(
    props: ComposeJsonFormsRendererProps,
    compact: Boolean = false,
) {
    val control = props.uischema as? com.chobantonov.jsonfroms.core.models.ControlElement
        ?: error("MaterialOneOfEnumControl can only render Control elements")
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
    val dispatchProps = mapDispatchToControlProps(
        Dispatch { action ->
            props.dispatch(action)
            action
        },
    )
    if (!stateProps.visible) {
        return
    }

    val autocomplete = com.chobantonov.jsonfroms.core.util.resolveAutocomplete(
        stateProps.uischema.options["autocomplete"], stateProps.config)
    val trim = (stateProps.uischema.options["trim"] as? JsonPrimitive)?.contentOrNull == "true"
    val modifier = if (trim) Modifier else Modifier.fillMaxWidth()
    val variant = useInputVariant(stateProps.config, stateProps.uischema)
    val translator = props.state.jsonforms.i18n.translate
    val startAdornments = resolveInputAdornments(stateProps.uischema.options, "startAdornment", translator)
    val endAdornments = resolveInputAdornments(stateProps.uischema.options, "endAdornment", translator)
    val selectedOption = stateProps.options.firstOrNull { jsonSchemaChoiceEquals(it.value, stateProps.data) }
    val unknownStatus = stateProps.data.takeIf { stateProps.dataProvided && selectedOption == null }
        ?.let { "${translateRadioUnknownValue(props.state.jsonforms.i18n)}: $it" }
    val focusRequester = rememberInitialControlFocusRequester(stateProps.path,
        stateProps.config + stateProps.uischema.options, stateProps.enabled && !stateProps.readonly)
    val canClear = JsonPath.get(props.state.jsonforms.core.data, stateProps.path) != null &&
        stateProps.enabled && !stateProps.readonly &&
        (stateProps.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    val requiredState = translateControlRequired(props.state.jsonforms.i18n)
        .takeIf { stateProps.required }
    val hideAsterisk = strictControlBooleanOption(stateProps.config + stateProps.uischema.options,
        "hideRequiredAsterisk") ?: false
    val label = if (compact) "" else computeLabel(stateProps.label, stateProps.required, hideAsterisk)
    val committedText = selectedOption?.label ?:
        if (unknownStatus != null && stateProps.data == JsonNull) "null"
        else stateProps.data.toMaterialInputString()
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(stateProps.path) { mutableStateOf(false) }
    var touched by remember(stateProps.path) { mutableStateOf(false) }
    var query by remember(stateProps.path) { mutableStateOf(committedText) }
    var observedCommittedText by remember(stateProps.path) { mutableStateOf(committedText) }
    var observedCommittedValue by remember(stateProps.path) { mutableStateOf(stateProps.data) }
    val setPending = rememberPendingEditOwner("one-of-enum:${stateProps.path}:${props.id}")
    var selectorFocused by remember(stateProps.path) { mutableStateOf(false) }
    var selectorHovered by remember(stateProps.path) { mutableStateOf(false) }
    var clearFocused by remember(stateProps.path) { mutableStateOf(false) }
    var clearHovered by remember(stateProps.path) { mutableStateOf(false) }
    val clearFocusRequester = remember(stateProps.path) { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    fun clearVisible() = selectorFocused || selectorHovered || clearFocused || clearHovered ||
        inputModeManager.inputMode == InputMode.Touch
    val clearModifier = Modifier.size(32.dp)
        .onFocusChanged { clearFocused = it.isFocused }
        .pointerInput(stateProps.path) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> clearHovered = true
                        PointerEventType.Exit -> clearHovered = false
                    }
                }
            }
        }
        .graphicsLayer {
            alpha = if (clearVisible()) 1f else 0f
        }
        .focusRequester(clearFocusRequester)
        .focusProperties { canFocus = clearVisible() }
        .clearAndSetSemantics {
            if (clearVisible()) {
                contentDescription = clearLabel
                focused = clearFocused
                onClick {
                    query = ""; observedCommittedText = ""; expanded = false
                    setPending(false)
                    dispatchProps.handleChange(stateProps.path, null)
                    true
                }
                requestFocus { clearFocusRequester.requestFocus(); true }
            }
        }

    LaunchedEffect(stateProps.data, committedText) {
        if (committedText != observedCommittedText || stateProps.data != observedCommittedValue) {
            query = committedText
            expanded = false
            observedCommittedText = committedText
            observedCommittedValue = stateProps.data
            setPending(false)
        }
    }
    LaunchedEffect(stateProps.enabled, stateProps.readonly) {
        if (!stateProps.enabled || stateProps.readonly) {
            query = committedText
            setPending(false)
        }
    }

    val localError = unmatchedEnumQueryError(query, selectedOption?.label,
        translateControlLocalError(props.state.jsonforms.i18n, "enum.invalidOption", "Select a valid option."))
    val visibleError = if (touched) stateProps.errors else stateProps.errorsBeforeTouch
    val effectiveError = visibleError.takeIf(String::isNotBlank) ?: localError.orEmpty()
    val showUnfocusedDescription = strictControlBooleanOption(
        stateProps.config + stateProps.uischema.options, "showUnfocusedDescription") == true
    val visibleDescription = stateProps.description?.takeIf {
        !compact &&
        it.isNotBlank() && (selectorFocused || showUnfocusedDescription)
    }
    val accessibleState = listOfNotNull(requiredState, unknownStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(unknownStatus, effectiveError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val supportingText: (@Composable () -> Unit)? = if (unknownStatus != null ||
        !compact && (effectiveError.isNotBlank() || visibleDescription != null)) {
        { Column {
            if (unknownStatus != null) Text(unknownStatus)
            if (!compact && effectiveError.isNotBlank()) Text(effectiveError)
            visibleDescription?.let { Text(it) }
        } }
    } else null

    val filteredOptions = if (autocomplete) {
        stateProps.options.filter { option ->
            query.isBlank() || option.label.contains(query, ignoreCase = true)
        }
    } else {
        stateProps.options
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
        onExpandedChange = { shouldExpand ->
            if (!shouldExpand && expanded) touched = true
            expanded = shouldExpand && filteredOptions.isNotEmpty()
        },
    ) {
        val fieldModifier = modifier.then(if (compact) Modifier.testTag("material-enum-cell") else Modifier)
            .focusRequester(focusRequester).semantics {
            if (accessibleState.isNotBlank()) stateDescription = accessibleState
            if (accessibleError.isNotBlank()) error(accessibleError)
        }.onFocusChanged { focusState ->
            selectorFocused = focusState.isFocused
            if (focusState.isFocused) hasReceivedFocus = true
            else if (hasReceivedFocus) touched = true
        }.pointerInput(stateProps.path) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> selectorHovered = true
                        PointerEventType.Exit -> selectorHovered = false
                    }
                }
            }
        }.menuAnchor(
            type = if (autocomplete) ExposedDropdownMenuAnchorType.PrimaryEditable else ExposedDropdownMenuAnchorType.PrimaryNotEditable,
            enabled = stateProps.enabled && !stateProps.readonly,
        )
        when (variant) {
            MaterialInputVariant.Outlined -> {
                OutlinedTextField(
                    modifier = fieldModifier,
                    value = query,
                    onValueChange = { value ->
                        if (autocomplete) {
                            query = value
                            setPending(value != committedText)
                            expanded = stateProps.options.any { option ->
                                value.isBlank() || option.label.contains(value, ignoreCase = true)
                            }
                        }
                    },
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canClear) ActionTooltip(clearLabel) {
                            IconButton(onClick = {
                                if (clearVisible()) {
                                    query = ""; observedCommittedText = ""; expanded = false
                                    setPending(false)
                                    dispatchProps.handleChange(stateProps.path, null)
                                }
                            }, modifier = clearModifier) {
                                Icon(JsonFormsActionIcons.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredOptions.isNotEmpty())
                    } },
                )
            }

            MaterialInputVariant.Filled,
            MaterialInputVariant.Standard,
            -> {
                TextField(
                    modifier = fieldModifier,
                    value = query,
                    onValueChange = { value ->
                        if (autocomplete) {
                            query = value
                            setPending(value != committedText)
                            expanded = stateProps.options.any { option ->
                                value.isBlank() || option.label.contains(value, ignoreCase = true)
                            }
                        }
                    },
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    trailingIcon = { Row(verticalAlignment = Alignment.CenterVertically) {
                        if (canClear) ActionTooltip(clearLabel) {
                            IconButton(onClick = {
                                if (clearVisible()) {
                                    query = ""; observedCommittedText = ""; expanded = false
                                    setPending(false)
                                    dispatchProps.handleChange(stateProps.path, null)
                                }
                            }, modifier = clearModifier) {
                                Icon(JsonFormsActionIcons.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredOptions.isNotEmpty())
                    } },
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded && filteredOptions.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
            onDismissRequest = {
                expanded = false
                touched = true
                query = committedText
                setPending(false)
            },
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    modifier = Modifier.semantics {
                        contentDescription = accessibleChoiceLabel(option, stateProps.options)
                    },
                    onClick = {
                        query = option.label
                        observedCommittedText = option.label
                        expanded = false
                        setPending(false)
                        dispatchProps.handleChange(stateProps.path, option.value)
                    },
                )
            }
        }
    }
}

val materialOneOfEnumControlTester: RankedTester = rankWith(
    5,
    isOneOfEnumControl,
)
