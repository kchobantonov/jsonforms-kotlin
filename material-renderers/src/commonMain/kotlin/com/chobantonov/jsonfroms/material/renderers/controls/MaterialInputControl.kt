package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

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
            if (canClear) {
                ActionTooltip(clearLabel) {
                    IconButton(
                        onClick = { onChange("") },
                        modifier = Modifier.size(32.dp).graphicsLayer { alpha = if (showClear) 1f else 0f },
                    ) {
                        Icon(JsonFormsActionIcons.Clear, clearLabel, modifier = Modifier.size(16.dp))
                    }
                }
            }
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
