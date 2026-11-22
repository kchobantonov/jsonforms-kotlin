package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.error as accessibilityError
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.chobantonov.jsonfroms.core.util.InputAdornment
import com.chobantonov.jsonfroms.compose.inputAdornmentGlyph
import com.chobantonov.jsonfroms.compose.LocalInputAdornmentIconResolver
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoPlainTooltip
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@Composable
fun CupertinoInputControl(
    props: ControlProps,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    localError: String? = null,
    storedValueStatus: String? = null,
    compact: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null,
    clearLabel: String = "Clear value",
    placeholder: String? = null,
    inputModifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    if (!props.visible) return
    val colors = LocalCupertinoColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var hovered by remember(props.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(props.path) { mutableStateOf(false) }
    var touched by remember(props.path) { mutableStateOf(false) }
    val touch = LocalInputModeManager.current.inputMode == InputMode.Touch
    val appliedOptions = props.config + props.uischema.options
    val translator = useJsonForms().state.jsonforms.i18n.translate
    val startAdornments = resolveInputAdornments(props.uischema.options, "startAdornment", translator)
    val endAdornments = resolveInputAdornments(props.uischema.options, "endAdornment", translator)
    fun booleanOption(key: String): Boolean = when (val option = appliedOptions[key]) {
        is Boolean -> option
        is JsonPrimitive -> !option.isString && option.booleanOrNull == true
        else -> false
    }
    val focusRequester = remember(props.path) { FocusRequester() }
    val initialFocus = booleanOption("focus")
    LaunchedEffect(focusRequester) {
        if (initialFocus && props.enabled) focusRequester.requestFocus()
    }
    val clearable = (props.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val canClear = clearable && value.isNotEmpty() && props.enabled && !props.readonly
    val showClear = canClear && (focused || hovered || touch)
    val visibleError = if (touched) props.errors else props.errorsBeforeTouch
    val error = localError?.takeIf(String::isNotBlank)
        ?: visibleError.takeIf(String::isNotBlank).orEmpty()
    val accessibleError = listOfNotNull(storedValueStatus, error.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val borderColor = when {
        error.isNotBlank() || storedValueStatus != null -> colors.destructive
        focused -> colors.accent
        else -> colors.controlBorder
    }
    val hideAsterisk = booleanOption("hideRequiredAsterisk")
    val descriptionVisible = !compact && !props.description.isNullOrBlank() &&
        (focused || booleanOption("showUnfocusedDescription"))
    val label = if (compact) "" else computeLabel(props.label, props.required, hideAsterisk)
    val requiredState = if (props.required) translateControlRequired(useJsonForms().state.jsonforms.i18n) else null
    val accessibleState = listOfNotNull(requiredState,
        storedValueStatus,
        props.description?.takeIf { it.isNotBlank() && descriptionVisible },
        placeholder?.takeIf { value.isEmpty() && it.isNotBlank() }).joinToString(". ")

    @Composable fun inputField() {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryLabel)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = inputModifier.fillMaxWidth().focusRequester(focusRequester).semantics {
                val accessibleName = listOfNotNull(
                    props.label.takeIf(String::isNotBlank),
                    accessibleError.takeIf { compact && it.isNotBlank() }).joinToString(". ")
                if (accessibleName.isNotBlank()) contentDescription = accessibleName
                if (accessibleError.isNotBlank()) accessibilityError(accessibleError)
                if (accessibleState.isNotBlank()) stateDescription = accessibleState
            }.onFocusChanged { focusState ->
                if (focusState.isFocused) hasReceivedFocus = true
                else if (hasReceivedFocus) touched = true
            }.clip(RoundedCornerShape(10.dp))
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
                .padding(horizontal = if (compact) 8.dp else 12.dp,
                    vertical = if (compact) 6.dp else 11.dp),
            enabled = props.enabled,
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
                    CupertinoAdornments(startAdornments)
                    androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && !placeholder.isNullOrBlank()) Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.secondaryLabel,
                        )
                        inner()
                    }
                    if (showClear) {
                        ActionTooltip(clearLabel) {
                            IconButton(
                                onClick = { onChange(""); focusRequester.requestFocus() },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    JsonFormsActionIcons.Clear,
                                    clearLabel,
                                    tint = colors.secondaryLabel,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    } else if (canClear) Spacer(Modifier.size(30.dp))
                    trailingContent?.invoke()
                    CupertinoAdornments(endAdornments)
                }
            },
        )
        if (!compact && error.isNotBlank()) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        if (!compact && storedValueStatus != null) Text(storedValueStatus,
            style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        if (descriptionVisible) Text(
                props.description!!,
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryLabel,
            )
        }
    }
    if (compact && accessibleError.isNotBlank()) ActionTooltip(accessibleError) { inputField() }
    else inputField()
}

@Composable
internal fun CupertinoAdornments(items: List<InputAdornment>) {
    items.forEach { item ->
        CupertinoAdornment(item)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CupertinoAdornment(item: InputAdornment) {
    val painter = item.icon?.let { LocalInputAdornmentIconResolver.current?.invoke(it) }
    val display = inputAdornmentGlyph(item.icon) ?: item.text
    if (item.decorative) {
        if (painter != null) Icon(painter, null, Modifier.size(18.dp), tint = LocalCupertinoColors.current.label)
        else if (display.isNotEmpty()) Text(display, Modifier.clearAndSetSemantics { })
        return
    }
    val explanation = item.tooltip?.takeIf(String::isNotBlank)
    if (explanation == null) {
        if (painter != null) Icon(painter, item.text, Modifier.size(18.dp), tint = LocalCupertinoColors.current.label)
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
        tooltip = { CupertinoPlainTooltip(explanation) },
        state = tooltip,
    ) {
        val modifier = Modifier.onFocusChanged { focused = it.isFocused }
            .semantics {
                if (item.icon != null) contentDescription = item.text
                stateDescription = explanation
            }.focusable()
        if (painter != null) Icon(painter, item.text, modifier.size(18.dp), tint = LocalCupertinoColors.current.label)
        else Text(display, modifier)
    }
}
