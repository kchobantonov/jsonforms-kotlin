package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedStringEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.material.renderers.controls.materialAnyOfStringOrEnumControlTester
import com.slapps.cupertino.CupertinoDropdownMenu
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.MenuPickerAction
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val CupertinoAnyOfStringOrEnumControl = ComposeJsonFormsRenderer {
    CupertinoAnyOfStringOrEnumControl(it)
}
val cupertinoAnyOfStringOrEnumControlTester =
    withIncreasedRank(100, materialAnyOfStringOrEnumControlTester)

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoAnyOfStringOrEnumControl(props: ComposeJsonFormsRendererProps) {
    val bindings = cupertinoControlBindings(props, "CupertinoAnyOfStringOrEnumControl")
    val control = bindings.controlProps
    if (!control.visible) return

    val alternatives = control.schema.anyOf.orEmpty()
    val textSchema = alternatives.firstOrNull {
        it.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) && it.enumValues == null
    }
    val suggestions = alternatives.firstOrNull {
        it.enumValues != null && (it.typeValue == null ||
            it.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String))
    }?.enumValues.orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    val restrict = resolveRestrict(control.uischema.options["restrict"], control.config)
    val committedValue = bindings.stateProps.data.toCupertinoInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data,
        bindings.stateProps.dataProvided)?.let {
        "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it"
    }
    var draft by remember(control.path) { mutableStateOf(committedValue) }
    var observedCommitted by remember(control.path) { mutableStateOf(committedValue) }
    var expanded by remember(control.path) { mutableStateOf(false) }

    LaunchedEffect(committedValue) {
        if (committedValue != observedCommitted) {
            draft = committedValue
            observedCommitted = committedValue
            expanded = false
        }
    }
    LaunchedEffect(control.enabled, control.readonly) {
        if (!control.enabled || control.readonly) expanded = false
    }
    fun commit(value: String) {
        if (!acceptsRestrictedStringEdit(draft, value, textSchema?.maxLength, restrict)) return
        draft = value
        observedCommitted = value
        expanded = suggestions.any { it != value && it.contains(value, ignoreCase = true) }
        bindings.dispatchProps.handleChange(control.path,
            value.takeIf(String::isNotEmpty)?.let(::JsonPrimitive))
    }
    val matches = suggestions.filter { it != draft && it.contains(draft, ignoreCase = true) }
    Box {
        CupertinoInputControl(
            props = control,
            value = draft,
            clearLabel = bindings.clearLabel,
            storedValueStatus = invalidStatus,
            onChange = ::commit,
        )
        CupertinoDropdownMenu(
            expanded = expanded && matches.isNotEmpty() && control.enabled && !control.readonly,
            onDismissRequest = { expanded = false },
        ) {
            matches.forEach { suggestion ->
                MenuPickerAction(
                    isSelected = false,
                    onClick = {
                        draft = suggestion
                        observedCommitted = suggestion
                        expanded = false
                        bindings.dispatchProps.handleChange(control.path, JsonPrimitive(suggestion))
                    },
                    title = { CupertinoText(suggestion) },
                )
            }
        }
    }
}
