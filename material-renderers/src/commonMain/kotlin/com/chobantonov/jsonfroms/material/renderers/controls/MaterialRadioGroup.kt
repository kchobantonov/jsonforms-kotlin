package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.core.mappers.EnumOption
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
@Composable
internal fun MaterialRadioGroup(
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
    if (!visible) {
        return
    }

    val appliedOptions = config + optionsMap
    val hideRequiredAsterisk = appliedOptions["hideRequiredAsterisk"].toBooleanStrictOrNullCompat() ?: false
    val showUnfocusedDescription = appliedOptions["showUnfocusedDescription"].toBooleanStrictOrNullCompat() ?: false
    val showDescription = !isDescriptionHidden(
        visible = visible,
        description = description,
        isFocused = false,
        showUnfocusedDescription = showUnfocusedDescription,
    )
    val helperText = if (errors.isNotBlank()) errors else if (showDescription) description else null

    Column {
        Text(computeLabel(label, required, hideRequiredAsterisk))
        options.forEach { option ->
            Row {
                RadioButton(
                    selected = selectedValue == option.value,
                    enabled = enabled && !readonly,
                    onClick = { onSelect(option) },
                )
                Text(option.label)
            }
        }
        if (!helperText.isNullOrBlank()) {
            Text(helperText)
        }
    }
}
