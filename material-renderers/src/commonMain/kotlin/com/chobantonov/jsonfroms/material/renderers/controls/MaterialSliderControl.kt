package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateSliderDiagnostic
import com.chobantonov.jsonfroms.core.i18n.translateSliderStatus
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isRangeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.buildSliderPositions
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

val MaterialSliderControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialSliderControl(props)
}

@Composable
internal fun MaterialSliderCell(props: ComposeJsonFormsCellProps) =
    MaterialSliderControl(props.asRendererProps(), compact = true)

@Composable
fun MaterialSliderControl(
    props: ComposeJsonFormsRendererProps,
    compact: Boolean = false,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialSliderControl",
    )
    val controlProps = bindings.controlProps
    if (!controlProps.visible) {
        return
    }

    val appliedOptions = controlProps.config + controlProps.uischema.options
    val hideRequiredAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") ?: false
    val showUnfocusedDescription = strictControlBooleanOption(appliedOptions, "showUnfocusedDescription") == true
    val minimum = controlProps.schema.minimum ?: 0.0
    val maximum = controlProps.schema.maximum ?: 100.0
    val positions = remember(controlProps.schema) { buildSliderPositions(controlProps.schema) }
    val defaultValue = (controlProps.schema.default as? JsonPrimitive)?.doubleOrNull
    val stored = JsonPath.get(props.state.jsonforms.core.data, bindings.stateProps.path)
    val numeric = (stored as? JsonPrimitive)?.takeUnless { it.isString }
        ?.takeIf { it.doubleOrNull != null }
    val currentIndex = positions.indexOf(numeric)
    val status = translateSliderStatus(props.state.jsonforms.i18n,
        stored?.toString(),
        invalid = stored != null && currentIndex < 0)
    val invalidStatus = status.takeIf { stored != null && currentIndex < 0 }
    val thumbIndex = if (currentIndex >= 0) currentIndex else
        positions.nearestIndex(numeric?.doubleOrNull ?: defaultValue)
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val clearable = (controlProps.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val canClear = clearable && stored != null && controlProps.enabled && !controlProps.readonly
    val focusRequester = rememberInitialControlFocusRequester(controlProps.path, appliedOptions,
        controlProps.enabled && !controlProps.readonly &&
            (positions.values.size > 1 || (positions.values.size == 1 && currentIndex < 0)))
    var touched by remember(controlProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(controlProps.path) { mutableStateOf(false) }
    var focused by remember(controlProps.path) { mutableStateOf(false) }
    val visibleError = if (touched) controlProps.errors else controlProps.errorsBeforeTouch
    val accessibleError = listOfNotNull(invalidStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val visibleDescription = controlProps.description?.takeIf {
        !compact &&
        !isDescriptionHidden(controlProps.visible, it, focused, showUnfocusedDescription)
    }
    val accessibleStatus = listOfNotNull(
        translateControlRequired(props.state.jsonforms.i18n).takeIf { controlProps.required },
        status,
        visibleDescription,
    ).joinToString(". ")

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!compact) Text(computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatSliderEndpoint(minimum))
            when {
                positions.values.size > 1 -> Slider(
                    modifier = Modifier.weight(1f).focusRequester(focusRequester)
                        .testTag(if (compact) "material-slider-cell" else "numeric-slider-${controlProps.path}")
                        .semantics {
                            stateDescription = accessibleStatus
                            if (accessibleError.isNotBlank()) error(accessibleError)
                        }.onFocusChanged { state ->
                            focused = state.isFocused
                            if (state.isFocused) hasReceivedFocus = true
                            else if (hasReceivedFocus) touched = true
                        },
                    value = thumbIndex.toFloat(),
                    onValueChange = { value ->
                        val next = positions.values[value.roundToInt().coerceIn(positions.values.indices)]
                        if (next != stored) bindings.dispatchProps.handleChange(bindings.stateProps.path, next)
                    },
                    valueRange = 0f..positions.values.lastIndex.toFloat(),
                    steps = (positions.values.size - 2).coerceAtLeast(0),
                    enabled = controlProps.enabled && !controlProps.readonly,
                )
                positions.values.size == 1 -> Button(
                    onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path, positions.values.single()) },
                    enabled = controlProps.enabled && !controlProps.readonly && currentIndex < 0,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester)
                        .testTag(if (compact) "material-slider-cell" else "numeric-slider-${controlProps.path}")
                        .semantics {
                            stateDescription = accessibleStatus
                            if (accessibleError.isNotBlank()) error(accessibleError)
                        }.onFocusChanged { state ->
                            focused = state.isFocused
                            if (state.isFocused) hasReceivedFocus = true
                            else if (hasReceivedFocus) touched = true
                        },
                ) { Text(translateControlLocalError(props.state.jsonforms.i18n,
                    "slider.selectValue", "Select value")) }
            }
            Text(formatSliderEndpoint(maximum))
            if (canClear) ActionTooltip(bindings.clearLabel) {
                IconButton(onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path,
                    if (preserveEmptyValue) JsonPrimitive(0) else null) }) {
                    Icon(JsonFormsActionIcons.Clear, bindings.clearLabel)
                }
            }
        }
        if (!compact || invalidStatus != null) Text(status,
            color = if (invalidStatus != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface)
        positions.error?.let { Text(translateSliderDiagnostic(props.state.jsonforms.i18n, it)) }
        if (visibleDescription != null) Text(visibleDescription)
        if (!compact && visibleError.isNotBlank()) Text(visibleError)
    }
}

internal fun formatSliderEndpoint(value: Double): String {
    val integralValue = value.toLong()
    return if (integralValue.toDouble() == value) integralValue.toString() else value.toString()
}

val materialSliderControlTester: RankedTester = rankWith(
    4,
    isRangeControl,
)
