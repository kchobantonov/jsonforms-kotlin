package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.core.mappers.EnumOption
import com.chobantonov.jsonfroms.core.util.jsonSchemaChoiceEquals
import kotlinx.serialization.json.JsonElement
import com.chobantonov.jsonfroms.core.mappers.accessibleChoiceLabel
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.radioChoiceVertical
import com.chobantonov.jsonfroms.core.i18n.translateChoiceGroupOrientation
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.initialControlFocusRequested
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.resolvedControlBooleanOption
import com.chobantonov.jsonfroms.compose.rememberClearAffordanceState
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors

@Composable
internal fun CupertinoRadioGroup(
    focusKey: String,
    label: String,
    required: Boolean,
    description: String?,
    errors: String,
    visible: Boolean,
    enabled: Boolean,
    readonly: Boolean,
    config: Map<String, Any?>,
    optionsMap: Map<String, Any?>,
    options: List<EnumOption>,
    selectedValue: JsonElement,
    dataProvided: Boolean,
    unknownValueLabel: String,
    clearLabel: String,
    i18n: JsonFormsI18nState,
    onGroupFocusChanged: (Boolean) -> Unit,
    onSelect: (EnumOption) -> Unit,
    onClear: () -> Unit,
    compact: Boolean = false,
) {
    if (!visible) return
    val appliedOptions = config + optionsMap
    val hideAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") ?: false
    val requiredState = translateControlRequired(i18n).takeIf { required }
    val initialFocus = initialControlFocusRequested(appliedOptions)
    val focusOnMount = remember(focusKey) { initialFocus && enabled && !readonly && options.isNotEmpty() }
    val colors = LocalCupertinoColors.current
    val interactive = enabled && !readonly
    val vertical = radioChoiceVertical(config, optionsMap)
    val canClear = dataProvided && interactive &&
        resolvedControlBooleanOption(optionsMap, config, "clearable") != false
    var groupFocused by remember { mutableStateOf(false) }
    val showUnfocusedDescription = strictControlBooleanOption(appliedOptions,
        "showUnfocusedDescription") == true
    val visibleDescription = description?.takeIf {
        !compact &&
        it.isNotBlank() && (groupFocused || showUnfocusedDescription)
    }
    val accessibleState = listOfNotNull(requiredState, visibleDescription).joinToString(". ")
    var groupHovered by remember { mutableStateOf(false) }
    var clearFocused by remember { mutableStateOf(false) }
    val clearAffordance = rememberClearAffordanceState(focusKey)
    val clearVisible = groupFocused || groupHovered || clearFocused || clearAffordance.isVisible()
    val clearModifier = Modifier.size(28.dp).testTag("radio-clear-slot")
        .onFocusChanged { clearFocused = it.isFocused }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val focusRequesters = remember(focusKey, options.map { it.value }) {
        List(options.size) { FocusRequester() }
    }
    LaunchedEffect(focusKey) {
        if (focusOnMount) {
            val selected = options.indexOfFirst { dataProvided && jsonSchemaChoiceEquals(it.value, selectedValue) }
            focusRequesters[selected.coerceAtLeast(0)].requestFocus()
        }
    }
    Column(Modifier.then(if (compact) Modifier.testTag("cupertino-radio-cell") else Modifier).onFocusChanged {
            groupFocused = it.hasFocus
            if (it.hasFocus) clearAffordance.selectField()
            onGroupFocusChanged(it.hasFocus)
        }.pointerInput(canClear) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent().type) {
                        PointerEventType.Enter -> groupHovered = true
                        PointerEventType.Exit -> groupHovered = false
                        PointerEventType.Press -> clearAffordance.selectField()
                    }
                }
            }
        }.focusGroup().selectableGroup()
        .semantics { stateDescription = listOfNotNull(requiredState,
            translateChoiceGroupOrientation(i18n, vertical)).joinToString(". ") }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!compact) Text(computeLabel(label, required, hideAsterisk), modifier = Modifier.weight(1f),
                color = colors.label)
            if (canClear) ActionTooltip(clearLabel) {
                if (clearVisible) IconButton(onClick = onClear, modifier = clearModifier) {
                    Icon(JsonFormsActionIcons.Clear, clearLabel,
                        tint = colors.secondaryLabel, modifier = Modifier.size(15.dp))
                } else Spacer(clearModifier)
            }
        }
        if (vertical) Column {
            options.forEachIndexed { index, option ->
                CupertinoRadioChoice(index, option, options, focusRequesters, selectedValue,
                    dataProvided, interactive, accessibleState, errors, vertical = true, rtl = rtl,
                    onSelect = { clearAffordance.selectField(); onSelect(it) })
            }
        } else FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { index, option ->
                CupertinoRadioChoice(index, option, options, focusRequesters, selectedValue,
                    dataProvided, interactive, accessibleState, errors, vertical = false, rtl = rtl,
                    onSelect = { clearAffordance.selectField(); onSelect(it) })
            }
        }
        if (dataProvided && options.none { jsonSchemaChoiceEquals(it.value, selectedValue) }) {
            Text("$unknownValueLabel: $selectedValue", color = colors.destructive)
        }
        if (!compact && errors.isNotBlank()) Text(errors, color = colors.destructive)
        visibleDescription?.let { Text(it, color = colors.secondaryLabel) }
    }
}

@Composable
private fun CupertinoRadioChoice(
    index: Int,
    option: EnumOption,
    options: List<EnumOption>,
    focusRequesters: List<FocusRequester>,
    selectedValue: JsonElement,
    dataProvided: Boolean,
    interactive: Boolean,
    accessibleState: String,
    errors: String,
    vertical: Boolean,
    rtl: Boolean,
    onSelect: (EnumOption) -> Unit,
) {
    val colors = LocalCupertinoColors.current
    val selected = dataProvided && jsonSchemaChoiceEquals(selectedValue, option.value)
    Row(
        modifier = (if (vertical) Modifier.fillMaxWidth() else Modifier)
            .focusRequester(focusRequesters[index])
            .onPreviewKeyEvent { event ->
                val step = when {
                    vertical && event.key == Key.DirectionDown -> 1
                    vertical && event.key == Key.DirectionUp -> -1
                    !vertical && event.key == (if (rtl) Key.DirectionLeft else Key.DirectionRight) -> 1
                    !vertical && event.key == (if (rtl) Key.DirectionRight else Key.DirectionLeft) -> -1
                    else -> 0
                }
                if (event.type == KeyEventType.KeyDown && step != 0 && interactive && options.isNotEmpty()) {
                    val target = (index + step + options.size) % options.size
                    focusRequesters[target].requestFocus()
                    onSelect(options[target])
                    true
                } else false
            }
            .selectable(
                selected = selected,
                enabled = interactive,
                role = Role.RadioButton,
                onClick = { onSelect(option) },
            )
            .semantics {
                contentDescription = accessibleChoiceLabel(option, options)
                if (accessibleState.isNotBlank()) stateDescription = accessibleState
                if (errors.isNotBlank()) error(errors)
            }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val indicatorColor = if (interactive) colors.accent else colors.secondaryLabel
            drawCircle(
                color = indicatorColor,
                radius = size.minDimension / 2 - 1.dp.toPx(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
            )
            if (selected) {
                drawCircle(color = indicatorColor, radius = size.minDimension * .27f)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(option.label, color = if (interactive) colors.label else colors.secondaryLabel)
    }
}
