package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateSliderDiagnostic
import com.chobantonov.jsonfroms.core.i18n.translateSliderStatus
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isRangeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.core.util.buildSliderPositions
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.slapps.cupertino.CupertinoSlider
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.roundToInt

val CupertinoSliderControl = ComposeJsonFormsRenderer { CupertinoSliderControl(it) }

@Composable
internal fun CupertinoSliderCell(props: ComposeJsonFormsCellProps) =
    CupertinoSliderControl(props.asRendererProps(), compact = true)

@Composable
@OptIn(ExperimentalCupertinoApi::class)
fun CupertinoSliderControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = cupertinoControlBindings(props, "CupertinoSliderControl")
    val control = bindings.controlProps
    if (!control.visible) return
    com.chobantonov.jsonfroms.compose.ReportUnsupportedInputAdornments(props, "slider")
    val colors = LocalCupertinoColors.current
    val options = control.config + control.uischema.options
    val showUnfocusedDescription = strictControlBooleanOption(options, "showUnfocusedDescription") == true
    val minimum = control.schema.minimum ?: 0.0
    val maximum = control.schema.maximum ?: 100.0
    val positions = remember(control.schema) { buildSliderPositions(control.schema) }
    val default = (control.schema.default as? JsonPrimitive)?.doubleOrNull
    val stored = JsonPath.get(props.state.jsonforms.core.data, bindings.stateProps.path)
    val numeric = (stored as? JsonPrimitive)?.takeUnless { it.isString }
        ?.takeIf { it.doubleOrNull != null }
    val currentIndex = positions.indexOf(numeric)
    val status = translateSliderStatus(props.state.jsonforms.i18n,
        stored?.toString(),
        invalid = stored != null && currentIndex < 0)
    val invalidStatus = status.takeIf { stored != null && currentIndex < 0 }
    val thumbIndex = if (currentIndex >= 0) currentIndex else
        positions.nearestIndex(numeric?.doubleOrNull ?: default)
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val clearable = (control.uischema.options["clearable"] as? JsonPrimitive)?.booleanOrNull != false
    val canClear = clearable && stored != null && control.enabled && !control.readonly
    val focusRequester = rememberInitialControlFocusRequester(control.path, options,
        control.enabled && !control.readonly &&
            (positions.values.size > 1 || (positions.values.size == 1 && currentIndex < 0)))
    val hideAsterisk = strictControlBooleanOption(options, "hideRequiredAsterisk") ?: false
    var touched by remember(control.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(control.path) { mutableStateOf(false) }
    var focused by remember(control.path) { mutableStateOf(false) }
    val visibleError = if (touched) control.errors else control.errorsBeforeTouch
    val accessibleError = listOfNotNull(invalidStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val visibleDescription = control.description?.takeIf {
        !compact &&
        it.isNotBlank() && (focused || showUnfocusedDescription)
    }
    val accessibleStatus = listOfNotNull(
        translateControlRequired(props.state.jsonforms.i18n).takeIf { control.required },
        status,
        visibleDescription,
    ).joinToString(". ")

    Column(Modifier.fillMaxWidth()) {
        if (!compact) Text(computeLabel(control.label, control.required, hideAsterisk), color = colors.label)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatEndpoint(minimum), color = colors.secondaryLabel)
            when {
                positions.values.size > 1 -> CupertinoSlider(
                    value = thumbIndex.toFloat(),
                    onValueChange = { value ->
                        val next = positions.values[value.roundToInt().coerceIn(positions.values.indices)]
                        if (next != stored) bindings.dispatchProps.handleChange(bindings.stateProps.path, next)
                    },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester)
                        .testTag(if (compact) "cupertino-slider-cell" else "numeric-slider-${control.path}")
                        .semantics {
                            stateDescription = accessibleStatus
                            if (accessibleError.isNotBlank()) error(accessibleError)
                        }.onFocusChanged { state ->
                            focused = state.isFocused
                            if (state.isFocused) hasReceivedFocus = true
                            else if (hasReceivedFocus) touched = true
                        },
                    enabled = control.enabled && !control.readonly,
                    valueRange = 0f..positions.values.lastIndex.toFloat(),
                    steps = (positions.values.size - 2).coerceAtLeast(0),
                )
                positions.values.size == 1 -> CupertinoButton(
                    onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path, positions.values.single()) },
                    enabled = control.enabled && !control.readonly && currentIndex < 0,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester)
                        .testTag(if (compact) "cupertino-slider-cell" else "numeric-slider-${control.path}")
                        .semantics {
                            stateDescription = accessibleStatus
                            if (accessibleError.isNotBlank()) error(accessibleError)
                        }.onFocusChanged { state ->
                            focused = state.isFocused
                            if (state.isFocused) hasReceivedFocus = true
                            else if (hasReceivedFocus) touched = true
                        },
                    colors = CupertinoButtonDefaults.filledButtonColors(),
                ) { CupertinoText(translateControlLocalError(props.state.jsonforms.i18n,
                    "slider.selectValue", "Select value")) }
            }
            Text(formatEndpoint(maximum), color = colors.secondaryLabel)
            if (canClear) ActionTooltip(bindings.clearLabel) {
                IconButton(onClick = { bindings.dispatchProps.handleChange(bindings.stateProps.path,
                    if (preserveEmptyValue) JsonPrimitive(0) else null) }) {
                    Icon(JsonFormsActionIcons.Clear, bindings.clearLabel)
                }
            }
        }
        if (!compact || invalidStatus != null) Text(status,
            color = if (invalidStatus != null) colors.destructive else colors.secondaryLabel)
        positions.error?.let { Text(translateSliderDiagnostic(props.state.jsonforms.i18n, it),
            color = colors.destructive) }
        if (visibleDescription != null) Text(visibleDescription,
            style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
        if (!compact && visibleError.isNotBlank()) Text(visibleError,
            style = MaterialTheme.typography.bodySmall, color = colors.destructive)
    }
}

private fun formatEndpoint(value: Double): String =
    value.toLong().takeIf { it.toDouble() == value }?.toString() ?: value.toString()

val cupertinoSliderControlTester: RankedTester = withIncreasedRank(100, rankWith(4, isRangeControl))
