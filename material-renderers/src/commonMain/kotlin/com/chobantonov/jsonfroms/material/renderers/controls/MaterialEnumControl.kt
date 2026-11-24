package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateRadioUnknownValue

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
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
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.resolvedControlBooleanOption
import com.chobantonov.jsonfroms.compose.rememberClearAffordanceState
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.util.jsonSchemaChoiceEquals
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.accessibleChoiceLabel
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToEnumControlProps
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.NOT_APPLICABLE
import com.chobantonov.jsonfroms.core.testers.isEnumControl
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.InputAdornment
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val MaterialEnumControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialEnumControl(props)
}

@Composable
internal fun MaterialPlainEnumCell(props: ComposeJsonFormsCellProps) =
    MaterialEnumControl(props.asRendererProps(), compact = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialEnumControl(
    props: ComposeJsonFormsRendererProps,
    compact: Boolean = false,
) {
    val control = props.uischema as? com.chobantonov.jsonfroms.core.models.ControlElement
        ?: error("MaterialEnumControl can only render Control elements")
    val stateProps = mapStateToEnumControlProps(
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
    val keepLabelMinimized = startAdornments.isNotEmpty() || endAdornments.isNotEmpty()
    val selectedOption = stateProps.options.firstOrNull { jsonSchemaChoiceEquals(it.value, stateProps.data) }
    val unknownStatus = stateProps.data.takeIf { stateProps.dataProvided && selectedOption == null }
        ?.let { "${translateRadioUnknownValue(props.state.jsonforms.i18n)}: $it" }
    val focusRequester = rememberInitialControlFocusRequester(stateProps.path,
        stateProps.config + stateProps.uischema.options, stateProps.enabled && !stateProps.readonly)
    val canClear = JsonPath.get(props.state.jsonforms.core.data, stateProps.path) != null &&
        stateProps.enabled && !stateProps.readonly &&
        resolvedControlBooleanOption(stateProps.uischema.options, stateProps.config, "clearable") != false
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    val requiredState = translateControlRequired(props.state.jsonforms.i18n)
        .takeIf { stateProps.required }
    val hideAsterisk = strictControlBooleanOption(stateProps.config + stateProps.uischema.options,
        "hideRequiredAsterisk") ?: false
    val label = if (compact) "" else computeLabel(stateProps.label, stateProps.required, hideAsterisk)
    val placeholder = stateProps.uischema.options.stringOption("placeholder")
        ?.takeIf { !stateProps.dataProvided }
    val committedText = selectedOption?.label ?:
        if (unknownStatus != null && stateProps.data == JsonNull) "null"
        else stateProps.data.toMaterialInputString()
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(stateProps.path) { mutableStateOf(false) }
    var touched by remember(stateProps.path) { mutableStateOf(false) }
    var query by remember(stateProps.path) { mutableStateOf(committedText) }
    val choiceState = remember(stateProps.path) { TextFieldState(query) }
    // Observe edits here so host replacements and rejected queries reconcile after recomposition.
    if (keepLabelMinimized) {
        choiceState.text.toString()
        SideEffect {
            if (choiceState.text.toString() != query) choiceState.setTextAndPlaceCursorAtEnd(query)
        }
    }
    var observedCommittedText by remember(stateProps.path) { mutableStateOf(committedText) }
    var observedCommittedValue by remember(stateProps.path) { mutableStateOf(stateProps.data) }
    val setPending = rememberPendingEditOwner("enum:${stateProps.path}:${props.id}")
    var selectorFocused by remember(stateProps.path) { mutableStateOf(false) }
    val clearAffordance = rememberClearAffordanceState(stateProps.path)
    var selectorHovered by remember(stateProps.path) { mutableStateOf(false) }
    var clearFocused by remember(stateProps.path) { mutableStateOf(false) }
    var clearHovered by remember(stateProps.path) { mutableStateOf(false) }
    val clearFocusRequester = remember(stateProps.path) { FocusRequester() }
    fun clearVisible() = selectorFocused || selectorHovered || clearFocused || clearHovered ||
        clearAffordance.isVisible()
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
    val accessibleState = listOfNotNull(requiredState, unknownStatus, visibleDescription,
        placeholder?.takeIf { query.isEmpty() && it.isNotBlank() }).joinToString(". ")
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
    fun updateQuery(value: String) {
        if (autocomplete) {
            query = value
            setPending(value != committedText)
            expanded = stateProps.options.any { option ->
                value.isBlank() || option.label.contains(value, ignoreCase = true)
            }
        }
    }
    val inputTransformation = InputTransformation {
        if (autocomplete) updateQuery(toString()) else revertAllChanges()
    }
    val prefixContent: (@Composable () -> Unit)? =
        startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
            { MaterialAdornments(items) }
        }
    val suffixContent: (@Composable () -> Unit)? =
        endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
            { MaterialAdornments(items) }
        }
    fun clearValue() {
        query = ""; observedCommittedText = ""; expanded = false
        setPending(false)
        dispatchProps.handleChange(stateProps.path, null)
    }
    val trailingContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canClear && keepLabelMinimized) Spacer(Modifier.width(4.dp))
            if (canClear) ActionTooltip(clearLabel) {
                IconButton(onClick = {
                    if (clearVisible()) clearValue()
                },
                    modifier = clearModifier.graphicsLayer {
                        alpha = if (clearVisible()) 1f else 0f
                    }.focusRequester(clearFocusRequester)
                        .focusProperties { canFocus = clearVisible() }
                        .clearAndSetSemantics {
                            if (clearVisible()) {
                                contentDescription = clearLabel
                                focused = clearFocused
                                onClick { clearValue(); true }
                                requestFocus { clearFocusRequester.requestFocus(); true }
                            }
                        }) {
                    Icon(JsonFormsActionIcons.Clear,
                        null,
                        modifier = Modifier.size(16.dp))
                }
            }
            ExposedDropdownMenuDefaults.TrailingIcon(
                expanded = expanded && filteredOptions.isNotEmpty(),
                modifier = Modifier.testTag("choice-dropdown-arrow"))
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
        onExpandedChange = { shouldExpand ->
            clearAffordance.selectField()
            if (!shouldExpand && expanded) touched = true
            expanded = shouldExpand && filteredOptions.isNotEmpty()
        },
    ) {
        val fieldModifier = modifier.then(if (compact) Modifier.testTag("material-enum-cell") else Modifier)
            .focusRequester(focusRequester).semantics {
            if (accessibleState.isNotBlank()) stateDescription = accessibleState
            if (accessibleError.isNotBlank()) error(accessibleError)
            requestFocus { clearAffordance.selectField(); focusRequester.requestFocus(); true }
        }.onFocusChanged { focusState ->
            if (focusState.isFocused) clearAffordance.selectField()
            selectorFocused = focusState.isFocused
            if (focusState.isFocused) hasReceivedFocus = true
            else if (hasReceivedFocus) touched = true
        }.pointerInput(stateProps.path) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Press -> clearAffordance.selectField()
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
                if (keepLabelMinimized) OutlinedTextField(
                    modifier = fieldModifier,
                    state = choiceState,
                    inputTransformation = inputTransformation,
                    labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    placeholder = placeholder?.takeIf(String::isNotBlank)?.let { hint -> { Text(hint) } },
                    prefix = prefixContent,
                    suffix = suffixContent,
                    trailingIcon = trailingContent,
                ) else OutlinedTextField(
                    modifier = fieldModifier,
                    value = query,
                    onValueChange = ::updateQuery,
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {{ Text(label) }} else null,
                    supportingText = supportingText,
                    placeholder = placeholder?.takeIf(String::isNotBlank)?.let { hint -> { Text(hint) } },
                    trailingIcon = trailingContent,
                )
            }

            MaterialInputVariant.Filled,
            MaterialInputVariant.Standard,
            -> {
                if (keepLabelMinimized) TextField(
                    modifier = fieldModifier,
                    state = choiceState,
                    inputTransformation = inputTransformation,
                    labelPosition = TextFieldLabelPosition.Inside(isAlwaysMinimized = true),
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    placeholder = placeholder?.takeIf(String::isNotBlank)?.let { hint -> { Text(hint) } },
                    prefix = prefixContent,
                    suffix = suffixContent,
                    trailingIcon = trailingContent,
                ) else TextField(
                    modifier = fieldModifier,
                    value = query,
                    onValueChange = ::updateQuery,
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = accessibleError.isNotBlank(),
                    label = if (label.isNotBlank()) {{ Text(label) }} else null,
                    supportingText = supportingText,
                    placeholder = placeholder?.takeIf(String::isNotBlank)?.let { hint -> { Text(hint) } },
                    trailingIcon = trailingContent,
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

private val hasExplicitEnum = schemaMatches { schema -> schema.containsKey("enum") }

val materialEnumControlTester: RankedTester = { uiSchema, schema, context ->
    when {
        !isEnumControl(uiSchema, schema, context) -> NOT_APPLICABLE
        hasExplicitEnum(uiSchema, schema, context) -> 3
        else -> 2
    }
}
