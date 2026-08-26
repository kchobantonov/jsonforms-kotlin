package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
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
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip

@Composable
internal fun CupertinoInputControl(
    props: ControlProps,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    localError: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    clearLabel: String = "Clear value",
    onChange: (String) -> Unit,
) {
    if (!props.visible) return
    val colors = LocalCupertinoColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var hovered by remember(props.path) { mutableStateOf(false) }
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    val canClear = value.isNotEmpty() && props.enabled && !props.readonly
    val showClear = canClear && (focused || hovered || touch)
    val error = props.errors.takeIf(String::isNotBlank) ?: localError.orEmpty()
    val borderColor = when {
        error.isNotBlank() -> colors.destructive
        focused -> colors.accent
        else -> colors.controlBorder
    }
    val hideAsterisk = (props.config + props.uischema.options)["hideRequiredAsterisk"] == true
    val label = computeLabel(props.label, props.required, hideAsterisk)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryLabel)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .pointerInput(props.path) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> hovered = true
                                PointerEventType.Exit -> hovered = false
                            }
                        }
                    }
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            enabled = props.enabled && !props.readonly,
            readOnly = props.readonly,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (props.enabled) colors.label else colors.secondaryLabel,
            ),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && !props.description.isNullOrBlank() && !focused) {
                            Text(props.description!!, color = colors.secondaryLabel)
                        }
                        inner()
                    }
                    if (canClear) {
                        ActionTooltip(clearLabel) {
                            IconButton(
                                onClick = { onChange("") },
                                modifier = Modifier.size(30.dp).graphicsLayer { alpha = if (showClear) 1f else 0f },
                            ) {
                                Icon(
                                    JsonFormsActionIcons.Clear,
                                    clearLabel,
                                    tint = colors.secondaryLabel,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                    trailingContent?.invoke()
                }
            },
        )
        when {
            error.isNotBlank() -> Text(error, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
            focused && !props.description.isNullOrBlank() -> Text(
                props.description!!,
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
        }
    }
}
