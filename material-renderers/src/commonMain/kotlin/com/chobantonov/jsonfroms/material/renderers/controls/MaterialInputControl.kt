package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip

@Composable
internal fun MaterialInputControl(
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
    clearLabel: String = "Clear value",
    onChange: (String) -> Unit,
) {
    val description = props.description
    val variant = useInputVariant(props.config, props.uischema)
    var hovered by remember(props.path) { mutableStateOf(false) }
    val modifier = Modifier.fillMaxWidth().pointerInput(props.path) {
        awaitPointerEventScope {
            while (true) {
                when (awaitPointerEvent().type) {
                    PointerEventType.Enter -> hovered = true
                    PointerEventType.Exit -> hovered = false
                }
            }
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    val canClear = value.isNotEmpty() && props.enabled && !props.readonly
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
    val effectiveError = props.errors.takeIf { it.isNotBlank() } ?: localError.orEmpty()
    val appliedOptions = props.config + props.uischema.options
    val showUnfocusedDescription =
        appliedOptions["showUnfocusedDescription"].toBooleanStrictOrNullCompat() ?: false
    val descriptionVisible = !showDescriptionOnlyWhenFocused || !isDescriptionHidden(
        visible = true,
        description = description,
        isFocused = focused,
        showUnfocusedDescription = showUnfocusedDescription,
    )
    val supportingText: (@Composable () -> Unit)? = when {
        effectiveError.isNotBlank() -> { { Text(effectiveError.toMaterialSupportingText()) } }
        !description.isNullOrBlank() && descriptionVisible -> { { Text(description) } }
        else -> null
    }

    when (variant) {
        MaterialInputVariant.Outlined -> {
            OutlinedTextField(
                modifier = modifier,
                value = value,
                onValueChange = onChange,
                enabled = props.enabled && !props.readonly,
                readOnly = props.readonly,
                isError = effectiveError.isNotBlank(),
                keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
                interactionSource = interactionSource,
                leadingIcon = leadingIcon,
                trailingIcon = effectiveTrailingIcon,
                visualTransformation = visualTransformation,
                singleLine = singleLine,
                label = if (props.label.isNotBlank()) {
                    { Text(props.label) }
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
                enabled = props.enabled && !props.readonly,
                readOnly = props.readonly,
                isError = effectiveError.isNotBlank(),
                keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
                interactionSource = interactionSource,
                leadingIcon = leadingIcon,
                trailingIcon = effectiveTrailingIcon,
                visualTransformation = visualTransformation,
                singleLine = singleLine,
                label = if (props.label.isNotBlank()) {
                    { Text(props.label) }
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
