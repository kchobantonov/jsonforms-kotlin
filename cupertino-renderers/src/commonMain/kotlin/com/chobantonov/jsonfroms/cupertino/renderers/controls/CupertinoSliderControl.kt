package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isRangeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoSlider
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

val CupertinoSliderControl = ComposeJsonFormsRenderer { CupertinoSliderControl(it) }

@Composable
fun CupertinoSliderControl(props: ComposeJsonFormsRendererProps) {
    val bindings = cupertinoControlBindings(props, "CupertinoSliderControl")
    val control = bindings.controlProps
    if (!control.visible) return
    val colors = LocalCupertinoColors.current
    val options = control.config + control.uischema.options
    val minimum = control.schema.minimum ?: 0.0
    val maximum = control.schema.maximum ?: 100.0
    val step = control.schema.multipleOf ?: 1.0
    val default = (control.schema.default as? JsonPrimitive)?.doubleOrNull
    val value = (bindings.stateProps.data as? JsonPrimitive)?.doubleOrNull ?: default ?: minimum
    val hideAsterisk = options["hideRequiredAsterisk"] == true

    Column(Modifier.fillMaxWidth()) {
        Text(computeLabel(control.label, control.required, hideAsterisk), color = colors.label)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatEndpoint(minimum), color = colors.secondaryLabel)
            CupertinoSlider(
                value = value.toFloat(),
                onValueChange = {
                    bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(it.toDouble()))
                },
                modifier = Modifier.weight(1f),
                enabled = control.enabled && !control.readonly,
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = sliderSteps(minimum, maximum, step),
            )
            Text(formatEndpoint(maximum), color = colors.secondaryLabel)
        }
        when {
            control.errors.isNotBlank() -> Text(control.errors, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
            !control.description.isNullOrBlank() -> Text(control.description!!, style = MaterialTheme.typography.bodySmall, color = colors.secondaryLabel)
        }
    }
}

private fun sliderSteps(minimum: Double, maximum: Double, step: Double): Int =
    if (step <= 0.0) 0 else (((maximum - minimum) / step).toInt() - 1).coerceAtLeast(0)

private fun formatEndpoint(value: Double): String =
    value.toLong().takeIf { it.toDouble() == value }?.toString() ?: value.toString()

val cupertinoSliderControlTester: RankedTester = withIncreasedRank(100, rankWith(4, isRangeControl))
