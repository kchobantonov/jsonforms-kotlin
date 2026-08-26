package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.EnumOption
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors

@Composable
internal fun CupertinoRadioGroup(
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
    selectedValue: Any?,
    onSelect: (EnumOption) -> Unit,
) {
    if (!visible) return
    val hideAsterisk = (config + optionsMap)["hideRequiredAsterisk"] == true
    val colors = LocalCupertinoColors.current
    val interactive = enabled && !readonly
    Column {
        Text(computeLabel(label, required, hideAsterisk), color = colors.label)
        options.forEach { option ->
            val selected = selectedValue == option.value
            Row(
                modifier = Modifier.fillMaxWidth()
                    .selectable(
                        selected = selected,
                        enabled = interactive,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
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
        val helper = errors.takeIf(String::isNotBlank) ?: description
        if (!helper.isNullOrBlank()) {
            Text(helper, color = if (errors.isNotBlank()) colors.destructive else colors.secondaryLabel)
        }
    }
}
