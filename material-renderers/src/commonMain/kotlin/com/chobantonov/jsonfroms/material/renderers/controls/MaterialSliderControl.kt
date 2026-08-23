package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isRangeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

val MaterialSliderControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialSliderControl(props)
}

@Composable
fun MaterialSliderControl(
    props: ComposeJsonFormsRendererProps,
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
    val hideRequiredAsterisk = appliedOptions["hideRequiredAsterisk"].toBooleanStrictOrNullCompat() ?: false
    val showUnfocusedDescription = appliedOptions["showUnfocusedDescription"].toBooleanStrictOrNullCompat() ?: false
    val showDescription = !isDescriptionHidden(
        visible = controlProps.visible,
        description = controlProps.description,
        isFocused = false,
        showUnfocusedDescription = showUnfocusedDescription,
    )
    val minimum = controlProps.schema.minimum ?: 0.0
    val maximum = controlProps.schema.maximum ?: 100.0
    val step = controlProps.schema.multipleOf ?: 1.0
    val defaultValue = (controlProps.schema.default as? JsonPrimitive)?.doubleOrNull
    val currentValue = (bindings.stateProps.data as? JsonPrimitive)?.doubleOrNull ?: defaultValue ?: minimum

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(formatSliderEndpoint(minimum))
            Slider(
                modifier = Modifier.weight(1f),
                value = currentValue.toFloat(),
                onValueChange = { value ->
                    bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value.toDouble()))
                },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = computeSliderSteps(minimum, maximum, step),
                enabled = controlProps.enabled && !controlProps.readonly,
            )
            Text(formatSliderEndpoint(maximum))
        }
        if (controlProps.errors.isNotBlank()) {
            Text(controlProps.errors)
        } else if (showDescription && !controlProps.description.isNullOrBlank()) {
            Text(controlProps.description!!)
        }
    }
}

internal fun formatSliderEndpoint(value: Double): String {
    val integralValue = value.toLong()
    return if (integralValue.toDouble() == value) integralValue.toString() else value.toString()
}

private fun computeSliderSteps(
    minimum: Double,
    maximum: Double,
    step: Double,
): Int {
    if (step <= 0.0) {
        return 0
    }
    val intervals = ((maximum - minimum) / step).toInt()
    return (intervals - 1).coerceAtLeast(0)
}

val materialSliderControlTester: RankedTester = rankWith(
    4,
    isRangeControl,
)
