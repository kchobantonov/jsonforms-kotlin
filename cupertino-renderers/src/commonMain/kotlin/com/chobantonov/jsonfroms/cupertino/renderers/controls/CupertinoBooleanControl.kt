package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.rememberClearAffordanceState
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.core.i18n.translateBooleanNotSet
import com.chobantonov.jsonfroms.core.i18n.translateBooleanInvalidValue
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.mappers.booleanControlState
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoCheckbox
import com.chobantonov.jsonfroms.cupertino.renderers.CupertinoSwitch
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val CupertinoBooleanControl = ComposeJsonFormsRenderer { CupertinoBooleanControl(it, toggle = false) }

@Composable
internal fun CupertinoBooleanControl(
    props: ComposeJsonFormsRendererProps,
    toggle: Boolean,
    compact: Boolean = false,
) {
    val bindings = cupertinoControlBindings(props, "CupertinoBooleanControl")
    val control = bindings.controlProps
    if (!control.visible) return
    val colors = LocalCupertinoColors.current
    val booleanState = booleanControlState(props.state.jsonforms.core.data, control.path)
    val appliedOptions = control.config + control.uischema.options
    val showUnfocusedDescription = strictControlBooleanOption(appliedOptions, "showUnfocusedDescription") == true
    val hideAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") ?: false
    val enabled = control.enabled && !control.readonly
    val focusRequester = rememberInitialControlFocusRequester(control.path, appliedOptions, enabled)
    val canClear = booleanState.hasValue && enabled &&
        (appliedOptions["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    val notSetLabel = translateBooleanNotSet(props.state.jsonforms.i18n)
    val statusLabel = booleanState.invalidValue?.let {
        "${translateBooleanInvalidValue(props.state.jsonforms.i18n)}: $it"
    } ?: notSetLabel.takeIf { booleanState.value == null }
    val requiredState = if (control.required) translateControlRequired(props.state.jsonforms.i18n) else null
    val clearAffordance = rememberClearAffordanceState(control.path)
    var touched by remember(control.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(control.path) { mutableStateOf(false) }
    var focused by remember(control.path) { mutableStateOf(false) }
    val focusModifier = Modifier.onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) hasReceivedFocus = true
        else if (hasReceivedFocus) touched = true
    }
    val visibleError = if (touched) control.errors else control.errorsBeforeTouch
    val visibleDescription = control.description?.takeIf {
        !compact && it.isNotBlank() && (focused || showUnfocusedDescription)
    }
    val accessibleState = listOfNotNull(requiredState,
        statusLabel, visibleDescription).joinToString(". ")
    val change: (Boolean) -> Unit = { value ->
        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
    }
    Column(modifier = if (compact) Modifier.testTag(
        if (toggle) "cupertino-boolean-toggle-cell" else "cupertino-boolean-cell") else Modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.then(if (compact) Modifier else Modifier.fillMaxWidth())
                .padding(vertical = if (compact) 0.dp else 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!compact) Text(
                computeLabel(control.label, control.required, hideAsterisk),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.label else colors.secondaryLabel,
            )
            if (statusLabel != null) {
                Text(statusLabel, style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
            }
            val fieldModifier = Modifier.focusRequester(focusRequester).then(focusModifier)
                .then(clearAffordance.fieldModifier).semantics {
                    if (accessibleState.isNotEmpty()) stateDescription = accessibleState
                    if (visibleError.isNotBlank()) error(visibleError)
                }
            if (toggle) CupertinoSwitch(booleanState.value == true, change,
                modifier = fieldModifier, enabled = enabled)
            else CupertinoCheckbox(booleanState.value == true, change,
                modifier = fieldModifier, enabled = enabled)
            if (canClear) {
                IconButton(
                    onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path, booleanState.clearValue) },
                    modifier = clearAffordance.clearModifier(28.dp),
                ) {
                    Icon(JsonFormsActionIcons.Clear, clearLabel,
                        tint = colors.secondaryLabel, modifier = Modifier.size(15.dp))
                }
            }
        }
        if (visibleDescription != null) {
            Text(visibleDescription, style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
        }
        if (!compact && visibleError.isNotBlank()) {
            Text(visibleError, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
        }
    }
}

val cupertinoBooleanControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isBooleanControl))
