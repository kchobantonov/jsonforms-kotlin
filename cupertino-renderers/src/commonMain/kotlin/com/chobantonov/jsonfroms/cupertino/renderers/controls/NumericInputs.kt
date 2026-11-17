package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidNumericStoredValue
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.util.parseIntegerDraft
import com.chobantonov.jsonfroms.core.util.parseNumberDraft
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedNumericEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveNumericStep
import com.chobantonov.jsonfroms.core.util.stepNumericDraft
import kotlinx.serialization.json.JsonPrimitive

@Composable
internal fun CupertinoPlainNumericCell(props: ComposeJsonFormsCellProps) = CupertinoNumericControl(
    props.asRendererProps(),
    integer = props.schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Integer),
    compact = true,
)

@Composable
internal fun CupertinoNumericControl(
    props: ComposeJsonFormsRendererProps,
    integer: Boolean,
    compact: Boolean = false,
) {
    val bindings = cupertinoControlBindings(props, if (integer) "CupertinoIntegerControl" else "CupertinoNumberControl")
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val committed = bindings.stateProps.data.toCupertinoInputString()
    val invalidStatus = invalidNumericStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    var draft by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(committed) }
    var lastCommitted by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(committed) }
    val setPending = rememberPendingEditOwner(
        "numeric:${if (integer) "integer" else "number"}:${bindings.stateProps.path}:${props.id}")
    val restrict = resolveRestrict(bindings.controlProps.uischema.options["restrict"], bindings.controlProps.config)
    LaunchedEffect(props.schema, props.uischema) { setPending(false) }
    LaunchedEffect(bindings.controlProps.enabled, bindings.controlProps.readonly) {
        if (!bindings.controlProps.enabled || bindings.controlProps.readonly) {
            draft = committed
            lastCommitted = committed
            setPending(false)
        }
    }
    LaunchedEffect(committed) {
        if (committed != lastCommitted) {
            draft = committed
            lastCommitted = committed
            setPending(false)
        }
    }
    val error = when {
        draft.isEmpty() -> null
        integer && parseIntegerDraft(draft) == null -> translateControlLocalError(
            props.state.jsonforms.i18n, "input.invalidInteger", "Invalid integer.")
        !integer && parseNumberDraft(draft) == null -> translateControlLocalError(
            props.state.jsonforms.i18n, "input.invalidNumber", "Invalid number.")
        !acceptsRestrictedNumericEdit(bindings.stateProps.data,
            if (integer) parseIntegerDraft(draft)!! else parseNumberDraft(draft)!!,
            bindings.stateProps.schema, restrict, bindings.stateProps.rootSchema) -> translateControlLocalError(
            props.state.jsonforms.i18n, "input.outOfRange", "Outside allowed range.")
        else -> null
    }
    val step = resolveNumericStep(bindings.controlProps.uischema.options["step"],
        bindings.stateProps.schema.multipleOf, integer)
    val stepError = if (step.invalidExplicitStep) translateControlLocalError(
        props.state.jsonforms.i18n, "input.invalidStep", "Invalid numeric step.") else null
    fun stepCandidate(direction: Int): JsonPrimitive? = step.increment?.takeIf {
        error == null
    }?.let { increment ->
        stepNumericDraft(bindings.stateProps.data as? JsonPrimitive, increment, integer, direction)
            ?.takeIf { acceptsRestrictedNumericEdit(bindings.stateProps.data, it,
                bindings.stateProps.schema, restrict, bindings.stateProps.rootSchema) }
    }
    val decrease = stepCandidate(-1)
    val increase = stepCandidate(1)
    CupertinoInputControl(
        props = bindings.controlProps,
        value = draft,
        keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal,
        localError = error ?: stepError,
        storedValueStatus = invalidStatus,
        compact = compact,
        inputModifier = if (compact) Modifier.testTag("cupertino-numeric-cell") else Modifier,
        clearLabel = bindings.clearLabel,
        trailingContent = { CupertinoNumericStepButtons(decrease, increase,
            bindings.controlProps.enabled && !bindings.controlProps.readonly,
            props.state.jsonforms.i18n) { next ->
            draft = next.content
            lastCommitted = next.content
            setPending(false)
            bindings.dispatchProps.handleChange(bindings.stateProps.path, next)
        } },
    ) { value ->
        draft = value
        val data = when {
            value.isEmpty() -> if (preserveEmptyValue) {
                if (integer) JsonPrimitive(0) else JsonPrimitive(0.0)
            } else null
            integer -> parseIntegerDraft(value)
            else -> parseNumberDraft(value)
        }
        if (value.isEmpty() || data != null &&
            acceptsRestrictedNumericEdit(bindings.stateProps.data, data,
                bindings.stateProps.schema, restrict, bindings.stateProps.rootSchema)) {
            lastCommitted = value
            setPending(false)
            bindings.dispatchProps.handleChange(bindings.stateProps.path, data)
        } else {
            setPending(true)
        }
    }
}
