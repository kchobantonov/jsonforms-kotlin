package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant

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
    localError: String? = null,
    onChange: (String) -> Unit,
) {
    val description = props.description
    val variant = useInputVariant(props.config, props.uischema)
    val modifier = Modifier.fillMaxWidth()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
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
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
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
                trailingIcon = trailingIcon,
                visualTransformation = visualTransformation,
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
