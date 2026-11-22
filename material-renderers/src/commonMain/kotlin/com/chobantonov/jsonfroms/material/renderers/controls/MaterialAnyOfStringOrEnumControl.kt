package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.mappers.isDescriptionHidden
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedStringEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.InputAdornment
import com.chobantonov.jsonfroms.core.util.resolveInputAdornments
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
    val hideRequiredAsterisk = strictControlBooleanOption(appliedOptions, "hideRequiredAsterisk") == true
    val showUnfocusedDescription = strictControlBooleanOption(appliedOptions, "showUnfocusedDescription") == true
    val enabled = controlProps.enabled && !controlProps.readonly
    val focusRequester = rememberInitialControlFocusRequester(controlProps.path, appliedOptions, enabled)
    val maxLength = stringSchema?.maxLength
    val restrict = resolveRestrict(
        controlProps.uischema.options["restrict"], controlProps.config)
    val trim = appliedOptions["trim"].toBooleanStrictOrNullCompat() ?: false
    val variant = useInputVariant(controlProps.config, controlProps.uischema)
    val translator = props.state.jsonforms.i18n.translate
    val startAdornments = resolveInputAdornments(controlProps.uischema.options, "startAdornment", translator)
    val endAdornments = resolveInputAdornments(controlProps.uischema.options, "endAdornment", translator)
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data,
        bindings.stateProps.dataProvided)?.let {
        "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it"
    }
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var lastCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var expanded by remember(bindings.stateProps.path) { mutableStateOf(false) }
    var hasReceivedFocus by remember(bindings.stateProps.path) { mutableStateOf(false) }
    var touched by remember(bindings.stateProps.path) { mutableStateOf(false) }
    var focused by remember(bindings.stateProps.path) { mutableStateOf(false) }
    val visibleError = if (touched) controlProps.errors else controlProps.errorsBeforeTouch
    val visibleDescription = controlProps.description?.takeIf {
        !isDescriptionHidden(controlProps.visible, it, focused, showUnfocusedDescription)
    }
    val requiredState = translateControlRequired(props.state.jsonforms.i18n).takeIf { controlProps.required }
    val accessibleState = listOfNotNull(requiredState, invalidStatus, visibleDescription).joinToString(". ")
    val accessibleError = listOfNotNull(invalidStatus, visibleError.takeIf(String::isNotBlank))
        .distinct().joinToString(". ")
    val label = computeLabel(controlProps.label, controlProps.required, hideRequiredAsterisk)
    val supportingText: (@Composable () -> Unit)? =
        if (accessibleError.isNotBlank() || visibleDescription != null) {{
            Column {
                if (invalidStatus != null) Text(invalidStatus)
                if (visibleError.isNotBlank()) Text(visibleError)
                if (visibleDescription != null) Text(visibleDescription)
            }
        }} else null

    LaunchedEffect(committedValue) {
        if (committedValue != lastCommittedValue) {
            draftValue = committedValue
            lastCommittedValue = committedValue
        }
    }

    val filteredOptions = enumOptions.filter { option ->
        option.contains(draftValue, ignoreCase = true) && option != draftValue
    }
    fun updateDraft(value: String) {
        if (!acceptsRestrictedStringEdit(
                draftValue, value, maxLength, restrict)) return
        draftValue = value
        lastCommittedValue = value
        expanded = enumOptions.any { it.contains(value, ignoreCase = true) && it != value }
        bindings.dispatchProps.handleChange(
            bindings.stateProps.path,
            value.takeIf(String::isNotEmpty)?.let(::JsonPrimitive),
        )
    }
    val modifier = (if (!trim || maxLength == null) Modifier.fillMaxWidth() else Modifier)
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            focused = focusState.isFocused
            if (focusState.isFocused) hasReceivedFocus = true
            else if (hasReceivedFocus) touched = true
        }
        .semantics {
            if (accessibleState.isNotBlank()) stateDescription = accessibleState
            if (accessibleError.isNotBlank()) error(accessibleError)
        }

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
                    onValueChange = ::updateDraft,
                    enabled = controlProps.enabled && !controlProps.readonly,
                    readOnly = controlProps.readonly,
                    isError = accessibleError.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
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
                    onValueChange = ::updateDraft,
                    enabled = controlProps.enabled && !controlProps.readonly,
                    readOnly = controlProps.readonly,
                    isError = accessibleError.isNotBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = if (label.isNotBlank()) {
                        { Text(label) }
                    } else {
                        null
                    },
                    supportingText = supportingText,
                    prefix = startAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
                    },
                    suffix = endAdornments.takeIf(List<InputAdornment>::isNotEmpty)?.let { items ->
                        { MaterialAdornments(items) }
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
