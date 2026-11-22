package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.StringMaskVisualTransformation
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.util.InputAdornment
import com.chobantonov.jsonfroms.compose.inputAdornmentGlyph
import com.chobantonov.jsonfroms.compose.LocalInputAdornmentIconResolver
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialInputControl(
    props: ControlProps,
    value: String,
    keyboardType: KeyboardType? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    showDescriptionOnlyWhenFocused: Boolean = true,
    singleLine: Boolean = true,
    localError: String? = null,
    storedValueStatus: String? = null,
    compact: Boolean = false,
    inputModifier: Modifier = Modifier,
    clearLabel: String = "Clear value",
    onChange: (String) -> Unit,
) {
    if (!props.visible) return
    val description = props.description
    val variant = useInputVariant(props.config, props.uischema)
    var hovered by remember(props.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(props.path) { mutableStateOf(false) }
    var touched by remember(props.path) { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    val appliedOptions = props.config + props.uischema.options
    val translator = useJsonForms().state.jsonforms.i18n.translate
    val startAdornments = resolveInputAdornments(props.uischema.options, "startAdornment", translator)
    val endAdornments = resolveInputAdornments(props.uischema.options, "endAdornment", translator)
    // The value-based Material API expands an empty unfocused label over its prefix/suffix slots.
    // The state-based API keeps the label minimized while retaining output transformations.
    val alwaysMinimizeLabel = (startAdornments.isNotEmpty() || endAdornments.isNotEmpty()) &&
        (visualTransformation === VisualTransformation.None ||
            visualTransformation is PasswordVisualTransformation ||
            visualTransformation is StringMaskVisualTransformation)
    val adornedFieldState = remember(props.path) { TextFieldState(value) }
    if (alwaysMinimizeLabel) {
        // Read the edit in this composition so a rejected edit also schedules reconciliation.
        adornedFieldState.text.toString()
        SideEffect {
            if (adornedFieldState.text.toString() != value)
                adornedFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }
    fun booleanOption(key: String): Boolean = when (val option = appliedOptions[key]) {
        is Boolean -> option
        is JsonPrimitive -> !option.isString && option.booleanOrNull == true
        else -> false
    }
    val hideAsterisk = booleanOption("hideRequiredAsterisk")
    val labelText = if (compact) "" else computeLabel(props.label, props.required, hideAsterisk)
    val requiredState = if (props.required) translateControlRequired(useJsonForms().state.jsonforms.i18n) else null
    val focusRequester = remember(props.path) { FocusRequester() }
    val initialFocus = booleanOption("focus")
    LaunchedEffect(focusRequester) {
        if (initialFocus && props.enabled) focusRequester.requestFocus()
    }
    val clearable = (props.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val canClear = clearable && value.isNotEmpty() && props.enabled && !props.readonly
    val showClear = canClear && (focused || hovered || touch)
    val effectiveTrailingIcon: (@Composable () -> Unit)? = if (canClear || trailingIcon != null) {{
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showClear) {
                ActionTooltip(clearLabel) {
                    IconButton(
                        onClick = { onChange(""); focusRequester.requestFocus() },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(JsonFormsActionIcons.Clear, clearLabel, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (canClear) Spacer(Modifier.size(32.dp))
            trailingIcon?.invoke()
        }
    }} else null
    val visibleError = if (touched) props.errors else props.errorsBeforeTouch
    val effectiveError = localError?.takeIf(String::isNotBlank)
        ?: visibleError.takeIf(String::isNotBlank).orEmpty()
    val showUnfocusedDescription = booleanOption("showUnfocusedDescription")
    val descriptionVisible = !compact && (!showDescriptionOnlyWhenFocused || !isDescriptionHidden(
        visible = true,
        description = description,
        isFocused = focused,
        showUnfocusedDescription = showUnfocusedDescription,
    ))
    val accessibleState = listOfNotNull(requiredState,
        storedValueStatus,
        description?.takeIf { it.isNotBlank() && descriptionVisible },
        placeholder?.takeIf { value.isEmpty() && it.isNotBlank() }).joinToString(". ")
    val compactError = if (compact) listOfNotNull(
        effectiveError.takeIf(String::isNotBlank)?.toMaterialSupportingText(),
        storedValueStatus,
    ).distinct().joinToString(". ").takeIf(String::isNotBlank) else null
    val modifier = inputModifier.fillMaxWidth().focusRequester(focusRequester).semantics {
        val accessibleName = listOfNotNull(
            props.label.takeIf(String::isNotBlank), compactError).joinToString(". ")
        if (accessibleName.isNotBlank()) contentDescription = accessibleName
        if (storedValueStatus != null) error(storedValueStatus)
        else if (effectiveError.isNotBlank()) error(effectiveError.toMaterialSupportingText())
        if (accessibleState.isNotBlank()) stateDescription = accessibleState
    }.onFocusChanged { focusState ->
        if (focusState.isFocused) hasReceivedFocus = true
        else if (hasReceivedFocus) touched = true
    }.pointerInput(props.path) {
        awaitPointerEventScope {
            while (true) {
                when (awaitPointerEvent().type) {
                    PointerEventType.Enter -> hovered = true
                    PointerEventType.Exit -> hovered = false
                }
            }
        }
    }
    val supportingText: (@Composable () -> Unit)? =
        if (!compact && (effectiveError.isNotBlank() || storedValueStatus != null ||
            (!description.isNullOrBlank() && descriptionVisible))) {
            {
                Column {
                    if (effectiveError.isNotBlank()) Text(effectiveError.toMaterialSupportingText())
                    if (storedValueStatus != null) Text(storedValueStatus, color = MaterialTheme.colorScheme.error)
                    if (!description.isNullOrBlank() && descriptionVisible) Text(description)
                }
            }
        } else null

    @Composable fun inputField() {
        if (alwaysMinimizeLabel) {
            val inputTransformation = InputTransformation { onChange(toString()) }
            val outputTransformation = when (visualTransformation) {
                is PasswordVisualTransformation -> OutputTransformation {
                    for (index in 0 until length)
                        replace(index, index + 1, visualTransformation.mask.toString())
                }
                is StringMaskVisualTransformation -> OutputTransformation {
                    val raw = toString()
                    val display = visualTransformation.visualize(raw)
                    var rawIndex = 0
                    for (displayIndex in display.text.indices) {
                        if (rawIndex < raw.length && displayIndex == display.rawOffsets[rawIndex]) {
                            rawIndex++
                        } else {
                            replace(displayIndex, displayIndex, display.text[displayIndex].toString())
                        }
                    }
                }
                else -> null
            }
            val lineLimits = if (singleLine) TextFieldLineLimits.SingleLine
                else TextFieldLineLimits.MultiLine()
            when (variant) {
                MaterialInputVariant.Outlined -> OutlinedTextField(
                    modifier = modifier,
                    state = adornedFieldState,
                    enabled = props.enabled,
                    readOnly = props.readonly,
                    isError = effectiveError.isNotBlank() || storedValueStatus != null,
                    keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) }
                        ?: KeyboardOptions.Default,
                    interactionSource = interactionSource,
                    leadingIcon = leadingIcon,
                    trailingIcon = effectiveTrailingIcon,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    lineLimits = lineLimits,
                    inputTransformation = inputTransformation,
                    outputTransformation = outputTransformation,
                    labelPosition = TextFieldLabelPosition.Cutout(isAlwaysMinimized = true),
                    label = if (labelText.isNotBlank()) {{ Text(labelText) }} else null,
                    placeholder = if (!placeholder.isNullOrBlank()) {{ Text(placeholder) }} else null,
                    supportingText = supportingText,
                )
                MaterialInputVariant.Filled,
                MaterialInputVariant.Standard,
                -> TextField(
                    modifier = modifier,
                    state = adornedFieldState,
                    enabled = props.enabled,
                    readOnly = props.readonly,
                    isError = effectiveError.isNotBlank() || storedValueStatus != null,
                    keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) }
                        ?: KeyboardOptions.Default,
                    interactionSource = interactionSource,
                    leadingIcon = leadingIcon,
                    trailingIcon = effectiveTrailingIcon,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    lineLimits = lineLimits,
                    inputTransformation = inputTransformation,
                    outputTransformation = outputTransformation,
                    labelPosition = TextFieldLabelPosition.Inside(isAlwaysMinimized = true),
                    label = if (labelText.isNotBlank()) {{ Text(labelText) }} else null,
                    placeholder = if (!placeholder.isNullOrBlank()) {{ Text(placeholder) }} else null,
                    supportingText = supportingText,
                )
            }
            return
        }
        when (variant) {
        MaterialInputVariant.Outlined -> {
            OutlinedTextField(
                modifier = modifier,
                value = value,
                onValueChange = onChange,
                enabled = props.enabled,
                readOnly = props.readonly,
                isError = effectiveError.isNotBlank() || storedValueStatus != null,
                keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
                interactionSource = interactionSource,
                leadingIcon = leadingIcon,
                trailingIcon = effectiveTrailingIcon,
                prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                    { MaterialAdornments(items) }
                },
                suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                    { MaterialAdornments(items) }
                },
                visualTransformation = visualTransformation,
                singleLine = singleLine,
                label = if (labelText.isNotBlank()) {
                    { Text(labelText) }
                } else {
                    null
                },
                placeholder = if (!placeholder.isNullOrBlank()) {
                    { Text(placeholder) }
                } else {
                    null
                },
                supportingText = supportingText,
            )
        }

        MaterialInputVariant.Filled,
        MaterialInputVariant.Standard,
        -> {
            TextField(
                modifier = modifier,
                value = value,
                onValueChange = onChange,
                enabled = props.enabled,
                readOnly = props.readonly,
                isError = effectiveError.isNotBlank() || storedValueStatus != null,
                keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
                interactionSource = interactionSource,
                leadingIcon = leadingIcon,
                trailingIcon = effectiveTrailingIcon,
                prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                    { MaterialAdornments(items) }
                },
                suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                    { MaterialAdornments(items) }
                },
                visualTransformation = visualTransformation,
                singleLine = singleLine,
                label = if (labelText.isNotBlank()) {
                    { Text(labelText) }
                } else {
                    null
                },
                placeholder = if (!placeholder.isNullOrBlank()) {
                    { Text(placeholder) }
                } else {
                    null
                },
                supportingText = supportingText,
            )
        }
        }
    }
    if (compactError != null) ActionTooltip(compactError) { inputField() }
    else inputField()
}

@Composable
internal fun MaterialAdornments(items: List<InputAdornment>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            MaterialAdornment(item)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialAdornment(item: InputAdornment) {
    val painter = item.icon?.let { LocalInputAdornmentIconResolver.current?.invoke(it) }
    val display = inputAdornmentGlyph(item.icon) ?: item.text
    if (item.decorative) {
        if (painter != null) Icon(painter, null, Modifier.size(18.dp))
        else if (display.isNotEmpty()) Text(display, Modifier.clearAndSetSemantics { })
        return
    }
    val explanation = item.tooltip?.takeIf(String::isNotBlank)
    if (explanation == null) {
        if (painter != null) Icon(painter, item.text, Modifier.size(18.dp))
        else Text(display, if (item.icon == null) Modifier else Modifier.semantics {
            contentDescription = item.text
        })
        return
    }
    val tooltip = rememberTooltipState()
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused, tooltip) {
        if (focused) tooltip.show() else tooltip.dismiss()
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip(modifier = Modifier.testTag("material-adornment-tooltip")) { Text(explanation) } },
        state = tooltip,
    ) {
        val modifier = Modifier.onFocusChanged { focused = it.isFocused }
            .semantics {
                if (item.icon != null) contentDescription = item.text
                stateDescription = explanation
            }.focusable()
        if (painter != null) Icon(painter, item.text, modifier.size(18.dp))
        else Text(display, modifier)
    }
}
