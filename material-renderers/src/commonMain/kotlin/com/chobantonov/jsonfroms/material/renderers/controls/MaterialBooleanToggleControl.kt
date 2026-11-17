package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val MaterialBooleanToggleControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialBooleanToggleControl(props)
}

@Composable
fun MaterialBooleanToggleControl(
    props: ComposeJsonFormsRendererProps,
    compact: Boolean = false,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialBooleanToggleControl",
    )
    val controlProps = bindings.controlProps
    if (!controlProps.visible) {
        return
    }

    val appliedOptions = controlProps.config + controlProps.uischema.options
    val showUnfocusedDescription = strictControlBooleanOption(appliedOptions, "showUnfocusedDescription") == true
    val hideRequiredAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") ?: false

    var touched by remember(controlProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(controlProps.path) { mutableStateOf(false) }
    var focused by remember(controlProps.path) { mutableStateOf(false) }
    val focusModifier = Modifier.onFocusChanged { state ->
        focused = state.isFocused
        if (state.isFocused) hasReceivedFocus = true
        else if (hasReceivedFocus) touched = true
    }
    val visibleError = if (touched) controlProps.errors else controlProps.errorsBeforeTouch
    val description = controlProps.description
    val showDescription = !isDescriptionHidden(
        visible = controlProps.visible,
        description = description,
        isFocused = focused,
        showUnfocusedDescription = showUnfocusedDescription,
    )
    val visibleDescription = description.takeIf { !compact && showDescription && !it.isNullOrBlank() }
    val booleanState = booleanControlState(props.state.jsonforms.core.data, controlProps.path)
    val enabled = controlProps.enabled && !controlProps.readonly
    val focusRequester = rememberInitialControlFocusRequester(controlProps.path, appliedOptions, enabled)
    val canClear = booleanState.hasValue && enabled &&
        (appliedOptions["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    val notSetLabel = translateBooleanNotSet(props.state.jsonforms.i18n)
    val statusLabel = booleanState.invalidValue?.let {
        "${translateBooleanInvalidValue(props.state.jsonforms.i18n)}: $it"
    } ?: notSetLabel.takeIf { booleanState.value == null }
    val requiredState = if (controlProps.required) translateControlRequired(props.state.jsonforms.i18n) else null
    val accessibleState = listOfNotNull(requiredState,
        statusLabel, visibleDescription).joinToString(". ")
    val clearAffordance = rememberClearAffordanceState(controlProps.path)
    val label = if (compact) "" else computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk)

    Column(modifier = if (compact) Modifier.testTag("material-boolean-toggle-cell") else Modifier) {
        ToggleWithLabel(
            value = booleanState.value,
            label = label,
            enabled = enabled,
            modifier = Modifier.focusRequester(focusRequester).then(focusModifier)
                .then(clearAffordance.fieldModifier),
            statusLabel = statusLabel,
            accessibleState = accessibleState,
            visibleError = visibleError,
            onCheckedChange = { value ->
                bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
            },
            clear = if (canClear) {
                {
                    IconButton(
                        onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path, booleanState.clearValue) },
                        modifier = clearAffordance.clearModifier(32.dp),
                    ) {
                        Icon(JsonFormsActionIcons.Clear, clearLabel, modifier = Modifier.size(16.dp))
                    }
                }
            } else null,
        )

        if (visibleDescription != null) {
            Text(visibleDescription)
        }
        if (!compact && visibleError.isNotBlank()) {
            Text(visibleError)
        }
    }
}

@Composable
private fun ToggleWithLabel(
    value: Boolean?,
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    statusLabel: String?,
    accessibleState: String,
    visibleError: String,
    onCheckedChange: (Boolean) -> Unit,
    clear: (@Composable () -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(
            modifier = modifier.semantics {
                if (accessibleState.isNotEmpty()) stateDescription = accessibleState
                if (visibleError.isNotBlank()) error(visibleError)
            },
            checked = value == true,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
        if (label.isNotEmpty()) Text(label)
        if (statusLabel != null) Text(statusLabel)
        clear?.invoke()
    }
}

val materialBooleanToggleControlTester: RankedTester = rankWith(
    3,
    and(isBooleanControl, optionIs("toggle", true)),
)
