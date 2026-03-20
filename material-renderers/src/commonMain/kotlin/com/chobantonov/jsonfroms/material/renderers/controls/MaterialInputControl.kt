package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
internal fun MaterialInputControl(
    props: ControlProps,
    value: String,
    onChange: (String) -> Unit,
) {
    val description = props.description
    val variant = useInputVariant(props.config, props.uischema)
    val trim = (props.uischema.options["trim"] as? JsonPrimitive)?.contentOrNull == "true"
    val modifier = if (trim) Modifier else Modifier.fillMaxWidth()

    when (variant) {
        MaterialInputVariant.Outlined -> {
            OutlinedTextField(
                modifier = modifier,
                value = value,
                onValueChange = onChange,
                enabled = props.enabled && !props.readonly,
                readOnly = props.readonly,
                isError = props.errors.isNotBlank(),
                label = if (props.label.isNotBlank()) {
                    { Text(props.label) }
                } else {
                    null
                },
                supportingText = if (props.errors.isNotBlank()) {
                    { Text(props.errors) }
                } else if (!description.isNullOrBlank()) {
                    { Text(description) }
                } else {
                    null
                },
            )
        }

        MaterialInputVariant.Filled,
        MaterialInputVariant.Standard,
        -> {
            TextField(
                modifier = modifier,
                value = value,
                onValueChange = onChange,
                enabled = props.enabled && !props.readonly,
                readOnly = props.readonly,
                isError = props.errors.isNotBlank(),
                label = if (props.label.isNotBlank()) {
                    { Text(props.label) }
                } else {
                    null
                },
                supportingText = if (props.errors.isNotBlank()) {
                    { Text(props.errors) }
                } else if (!description.isNullOrBlank()) {
                    { Text(description) }
                } else {
                    null
                },
            )
        }
    }
}
