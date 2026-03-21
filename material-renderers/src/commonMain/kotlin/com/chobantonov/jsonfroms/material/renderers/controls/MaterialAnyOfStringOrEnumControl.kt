package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val MaterialAnyOfStringOrEnumControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialAnyOfStringOrEnumControl(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialAnyOfStringOrEnumControl(
    props: ComposeJsonFormsRendererProps,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialAnyOfStringOrEnumControl",
    )
    val controlProps = bindings.controlProps
    if (!controlProps.visible) {
        return
    }

    val anyOfSchemas = controlProps.schema.anyOf.orEmpty()
    val enumSchema = findEnumSchema(anyOfSchemas)
    val stringSchema = findTextSchema(anyOfSchemas)
    val enumOptions = enumSchema?.enumValues
        .orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    val appliedOptions = controlProps.config + controlProps.uischema.options
    val maxLength = stringSchema?.maxLength
    val restrict = appliedOptions["restrict"].toBooleanStrictOrNullCompat() ?: false
    val trim = appliedOptions["trim"].toBooleanStrictOrNullCompat() ?: false
    val variant = useInputVariant(controlProps.config, controlProps.uischema)
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var lastCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var expanded by remember(bindings.stateProps.path) { mutableStateOf(false) }

    LaunchedEffect(committedValue) {
        if (committedValue != lastCommittedValue) {
            draftValue = committedValue
            lastCommittedValue = committedValue
        }
    }

    val filteredOptions = enumOptions.filter { option ->
        option.contains(draftValue, ignoreCase = true) && option != draftValue
    }
    val modifier = if (!trim || maxLength == null) Modifier.fillMaxWidth() else Modifier

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty() && controlProps.enabled && !controlProps.readonly,
        onExpandedChange = { shouldExpand ->
            expanded = shouldExpand && filteredOptions.isNotEmpty()
        },
    ) {
        when (variant) {
            MaterialInputVariant.Outlined -> {
                OutlinedTextField(
                    modifier = modifier.menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = controlProps.enabled && !controlProps.readonly,
                    ),
                    value = draftValue,
                    onValueChange = { value ->
                        val nextValue = if (restrict && maxLength != null) value.take(maxLength) else value
                        draftValue = nextValue
                        lastCommittedValue = nextValue
                        expanded = filteredOptions.isNotEmpty()
                        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(nextValue))
                    },
                    enabled = controlProps.enabled && !controlProps.readonly,
                    readOnly = controlProps.readonly,
                    isError = controlProps.errors.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = if (controlProps.label.isNotBlank()) {
                        { Text(controlProps.label) }
                    } else {
                        null
                    },
                    supportingText = if (controlProps.errors.isNotBlank()) {
                        { Text(controlProps.errors) }
                    } else if (!controlProps.description.isNullOrBlank()) {
                        { Text(controlProps.description!!) }
                    } else {
                        null
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredOptions.isNotEmpty())
                    },
                )
            }

            MaterialInputVariant.Filled,
            MaterialInputVariant.Standard,
            -> {
                TextField(
                    modifier = modifier.menuAnchor(
                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                        enabled = controlProps.enabled && !controlProps.readonly,
                    ),
                    value = draftValue,
                    onValueChange = { value ->
                        val nextValue = if (restrict && maxLength != null) value.take(maxLength) else value
                        draftValue = nextValue
                        lastCommittedValue = nextValue
                        expanded = filteredOptions.isNotEmpty()
                        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(nextValue))
                    },
                    enabled = controlProps.enabled && !controlProps.readonly,
                    readOnly = controlProps.readonly,
                    isError = controlProps.errors.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = if (controlProps.label.isNotBlank()) {
                        { Text(controlProps.label) }
                    } else {
                        null
                    },
                    supportingText = if (controlProps.errors.isNotBlank()) {
                        { Text(controlProps.errors) }
                    } else if (!controlProps.description.isNullOrBlank()) {
                        { Text(controlProps.description!!) }
                    } else {
                        null
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && filteredOptions.isNotEmpty())
                    },
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded && filteredOptions.isNotEmpty() && controlProps.enabled && !controlProps.readonly,
            onDismissRequest = { expanded = false },
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        draftValue = option
                        lastCommittedValue = option
                        expanded = false
                        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(option))
                    },
                )
            }
        }
    }
}

private fun findEnumSchema(schemas: List<JsonSchema>): JsonSchema? =
    schemas.find { schema ->
        schema.enumValues != null && (schema.typeValue == null || schema.typeValue.isSingleString())
    }

private fun findTextSchema(schemas: List<JsonSchema>): JsonSchema? =
    schemas.find { schema ->
        schema.typeValue.isSingleString() && schema.enumValues == null
    }

private fun hasEnumAndText(schemas: List<JsonSchema>): Boolean {
    val enumSchema = findEnumSchema(schemas)
    val stringSchema = findTextSchema(schemas)
    val remainingSchemas = schemas.filter { it != enumSchema && it != stringSchema }
    val wrongType = remainingSchemas.any { schema ->
        val type = schema.typeValue
        type != null && !type.isSingleString()
    }
    return enumSchema != null && stringSchema != null && !wrongType
}

private fun JsonSchemaTypeValue?.isSingleString(): Boolean =
    this is JsonSchemaTypeValue.Single &&
        value == JsonSchemaType.String

private val simpleAnyOf = and(
    uiTypeIs("Control"),
    schemaMatches { schema -> schema.anyOf?.let(::hasEnumAndText) == true },
)

val materialAnyOfStringOrEnumControlTester: RankedTester = rankWith(
    5,
    simpleAnyOf,
)
