package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isBooleanControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val MaterialBooleanControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialBooleanControl(props)
}

@Composable
fun MaterialBooleanControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialBooleanControl",
    )
    val controlProps = bindings.controlProps
    if (!controlProps.visible) {
        return
    }

    val appliedOptions = controlProps.config + controlProps.uischema.options
    val showUnfocusedDescription = appliedOptions["showUnfocusedDescription"]?.toBooleanStrictOrNullCompat() ?: false
    val hideRequiredAsterisk = appliedOptions["hideRequiredAsterisk"]?.toBooleanStrictOrNullCompat() ?: false

    val description = controlProps.description
    val isValid = controlProps.errors.isBlank()
    val showDescription = !isDescriptionHidden(
        visible = controlProps.visible,
        description = description,
        isFocused = false,
        showUnfocusedDescription = showUnfocusedDescription,
    )
    val firstFormHelperText = when {
        showDescription -> description
        !isValid -> controlProps.errors
        else -> null
    }
    val secondFormHelperText = if (showDescription && !isValid) controlProps.errors else null
    val checked = (controlProps.data as? JsonPrimitive)?.booleanOrNull ?: false
    val label = computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk)

    Column {
        CheckboxWithLabel(
            checked = checked,
            label = label,
            enabled = controlProps.enabled && !controlProps.readonly,
            onCheckedChange = { value ->
                bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
            },
        )

        if (!firstFormHelperText.isNullOrBlank()) {
            Text(firstFormHelperText)
        }
        if (!secondFormHelperText.isNullOrBlank()) {
            Text(secondFormHelperText)
        }
    }
}

@Composable
private fun CheckboxWithLabel(
    checked: Boolean,
    label: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
        Text(label)
    }
}

val materialBooleanControlTester: RankedTester = rankWith(
    2,
    isBooleanControl,
)
