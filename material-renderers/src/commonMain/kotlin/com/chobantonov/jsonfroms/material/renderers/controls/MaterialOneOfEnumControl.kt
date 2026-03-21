package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfEnum
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToOneOfEnumControlProps
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isOneOfEnumControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.util.MaterialInputVariant
import com.chobantonov.jsonfroms.material.renderers.util.useInputVariant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val MaterialOneOfEnumControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialOneOfEnumControl(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialOneOfEnumControl(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? com.chobantonov.jsonfroms.core.models.ControlElement
        ?: error("MaterialOneOfEnumControl can only render Control elements")
    val stateProps = mapStateToOneOfEnumControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            id = props.id,
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
        enumProps = OwnPropsOfEnum(),
    )
    val dispatchProps = mapDispatchToControlProps(
        Dispatch { action ->
            props.dispatch(action)
            action
        },
    )
    if (!stateProps.visible) {
        return
    }

    val appliedOptions = stateProps.config + stateProps.uischema.options
    val autocomplete = appliedOptions["autocomplete"].toBooleanStrictOrNullCompat() ?: true
    val trim = (stateProps.uischema.options["trim"] as? JsonPrimitive)?.contentOrNull == "true"
    val modifier = if (trim) Modifier else Modifier.fillMaxWidth()
    val variant = useInputVariant(stateProps.config, stateProps.uischema)
    val selectedOption = stateProps.options.firstOrNull { it.value == stateProps.data }
    var expanded by remember(stateProps.path) { mutableStateOf(false) }
    var query by remember(stateProps.path, selectedOption?.label) { mutableStateOf(selectedOption?.label.orEmpty()) }

    val filteredOptions = if (autocomplete) {
        stateProps.options.filter { option ->
            query.isBlank() || option.label.contains(query, ignoreCase = true)
        }
    } else {
        stateProps.options
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
        onExpandedChange = { shouldExpand ->
            expanded = shouldExpand && filteredOptions.isNotEmpty()
        },
    ) {
        when (variant) {
            MaterialInputVariant.Outlined -> {
                OutlinedTextField(
                    modifier = modifier.menuAnchor(
                        type = if (autocomplete) ExposedDropdownMenuAnchorType.PrimaryEditable else ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = stateProps.enabled && !stateProps.readonly,
                    ),
                    value = query,
                    onValueChange = { value ->
                        if (autocomplete) {
                            query = value
                            expanded = filteredOptions.isNotEmpty()
                        }
                    },
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = stateProps.errors.isNotBlank(),
                    label = if (stateProps.label.isNotBlank()) {
                        { Text(stateProps.label) }
                    } else {
                        null
                    },
                    supportingText = if (stateProps.errors.isNotBlank()) {
                        { Text(stateProps.errors) }
                    } else if (!stateProps.description.isNullOrBlank()) {
                        { Text(stateProps.description!!) }
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
                        type = if (autocomplete) ExposedDropdownMenuAnchorType.PrimaryEditable else ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        enabled = stateProps.enabled && !stateProps.readonly,
                    ),
                    value = query,
                    onValueChange = { value ->
                        if (autocomplete) {
                            query = value
                            expanded = filteredOptions.isNotEmpty()
                        }
                    },
                    enabled = stateProps.enabled && !stateProps.readonly,
                    readOnly = stateProps.readonly || !autocomplete,
                    isError = stateProps.errors.isNotBlank(),
                    label = if (stateProps.label.isNotBlank()) {
                        { Text(stateProps.label) }
                    } else {
                        null
                    },
                    supportingText = if (stateProps.errors.isNotBlank()) {
                        { Text(stateProps.errors) }
                    } else if (!stateProps.description.isNullOrBlank()) {
                        { Text(stateProps.description!!) }
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
            expanded = expanded && filteredOptions.isNotEmpty() && stateProps.enabled && !stateProps.readonly,
            onDismissRequest = {
                expanded = false
                query = selectedOption?.label.orEmpty()
            },
        ) {
            filteredOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        query = option.label
                        expanded = false
                        dispatchProps.handleChange(stateProps.path, option.value)
                    },
                )
            }
        }
    }
}

val materialOneOfEnumControlTester: RankedTester = rankWith(
    5,
    isOneOfEnumControl,
)
