package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.VisualTransformation
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.StringMaskVisualTransformation
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isMultiLineControl
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedStringEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.StringMask
import com.chobantonov.jsonfroms.core.util.matchingStringSuggestions
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull

val MaterialTextControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialTextControl(props)
}

val MaterialMultiTextControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialMultiTextControl(props)
}

@Composable
fun MaterialTextControl(
    props: ComposeJsonFormsRendererProps,
) = MaterialTextControl(props, singleLine = true)

@Composable
fun MaterialMultiTextControl(
    props: ComposeJsonFormsRendererProps,
) = MaterialTextControl(props, singleLine = false)

@Composable
internal fun MaterialPlainTextCell(props: ComposeJsonFormsCellProps) = MaterialTextControl(
    props.asRendererProps(),
    singleLine = (props.uischema.options["multi"] as? JsonPrimitive)?.booleanOrNull != true,
    compact = true,
)

@Composable
private fun MaterialTextControl(
    props: ComposeJsonFormsRendererProps,
    singleLine: Boolean,
    compact: Boolean = false,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialTextControl",
    )
    val textValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val mask = StringMask.fromOptions(bindings.controlProps.uischema.options)
    val placeholder = (bindings.controlProps.uischema.options["placeholder"] as? JsonPrimitive)
        ?.takeIf { it.isString }?.contentOrNull
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    var showSuggestions by remember(bindings.stateProps.path) { mutableStateOf(false) }

    fun commit(value: String, fromSuggestion: Boolean = false) {
        val storedValue = mask?.edit(value, textValue) ?: value
        if (!acceptsRestrictedStringEdit(
                textValue, storedValue, bindings.stateProps.schema.maxLength,
                resolveRestrict(bindings.controlProps.uischema.options["restrict"], bindings.controlProps.config),
            )) return
        showSuggestions = !fromSuggestion && value.isNotBlank()
        bindings.dispatchProps.handleChange(
            bindings.stateProps.path,
            if (storedValue.isNotEmpty() || preserveEmptyValue) JsonPrimitive(storedValue) else null,
        )
    }

    Column(modifier = if (compact) Modifier.testTag("material-text-cell") else Modifier) {
        MaterialInputControl(
            props = bindings.controlProps,
            value = mask?.raw(textValue) ?: textValue,
            visualTransformation = mask?.let(::StringMaskVisualTransformation) ?: VisualTransformation.None,
            singleLine = singleLine,
            placeholder = placeholder,
            clearLabel = bindings.clearLabel,
            storedValueStatus = invalidStatus,
            compact = compact,
            onChange = { commit(it) },
        )
        if (showSuggestions && bindings.controlProps.enabled && !bindings.controlProps.readonly) {
            matchingStringSuggestions(bindings.controlProps.uischema.options, textValue).forEach { suggestion ->
                TextButton(onClick = { commit(suggestion, fromSuggestion = true) }) {
                    Text(suggestion)
                }
            }
        }
    }
}

val materialTextControlTester: RankedTester = rankWith(
    1,
    isStringControl,
)

val materialMultiTextControlTester: RankedTester = rankWith(
    2,
    and(isStringControl, isMultiLineControl),
)
