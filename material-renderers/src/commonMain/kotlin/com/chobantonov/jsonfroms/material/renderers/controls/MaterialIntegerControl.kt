package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.core.i18n.translateControlLocalError
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidNumericStoredValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isIntegerControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedNumericEdit
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveNumericStep
import com.chobantonov.jsonfroms.core.util.stepNumericDraft
import kotlinx.serialization.json.JsonPrimitive

val MaterialIntegerControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialIntegerControl(props)
}

@Composable
fun MaterialIntegerControl(
    props: ComposeJsonFormsRendererProps,
    compact: Boolean = false,
) {
    val bindings = materialControlBindings(
        props = props,
        rendererName = "MaterialIntegerControl",
    )
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidNumericStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    var draftValue by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(committedValue) }
    var lastCommittedValue by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(committedValue) }
    val parsedDraft = parseIntegerInput(draftValue, translateControlLocalError(
        props.state.jsonforms.i18n, "input.invalidInteger", "Invalid integer."))
    val restrict = resolveRestrict(bindings.controlProps.uischema.options["restrict"], bindings.controlProps.config)
    val boundsError = (parsedDraft.data as? JsonPrimitive)?.takeUnless {
        acceptsRestrictedNumericEdit(bindings.stateProps.data, it, bindings.stateProps.schema, restrict,
            bindings.stateProps.rootSchema)
    }?.let { translateControlLocalError(props.state.jsonforms.i18n,
        "input.outOfRange", "Outside allowed range.") }
    val step = resolveNumericStep(bindings.controlProps.uischema.options["step"],
        bindings.stateProps.schema.multipleOf, integer = true)
    val stepError = if (step.invalidExplicitStep) translateControlLocalError(
        props.state.jsonforms.i18n, "input.invalidStep", "Invalid numeric step.") else null
    fun stepCandidate(direction: Int): JsonPrimitive? = step.increment?.takeIf {
        parsedDraft.error == null && boundsError == null
    }?.let { increment ->
        stepNumericDraft(bindings.stateProps.data as? JsonPrimitive, increment, true, direction)
            ?.takeIf { acceptsRestrictedNumericEdit(bindings.stateProps.data, it,
                bindings.stateProps.schema, restrict, bindings.stateProps.rootSchema) }
    }
    val decrease = stepCandidate(-1)
    val increase = stepCandidate(1)
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("numeric:integer:${bindings.stateProps.path}:${props.id}")

    LaunchedEffect(props.schema, props.uischema) { setPending(false) }
    LaunchedEffect(bindings.controlProps.enabled, bindings.controlProps.readonly) {
        if (!bindings.controlProps.enabled || bindings.controlProps.readonly) {
            draftValue = committedValue
            lastCommittedValue = committedValue
            setPending(false)
        }
    }

    LaunchedEffect(committedValue) {
        if (committedValue != lastCommittedValue) {
            draftValue = committedValue
            lastCommittedValue = committedValue
            setPending(false)
        }
    }

    MaterialInputControl(
        props = bindings.controlProps,
        value = draftValue,
        keyboardType = KeyboardType.Number,
        localError = parsedDraft.error ?: boundsError ?: stepError,
        storedValueStatus = invalidStatus,
        compact = compact,
        inputModifier = if (compact) Modifier.testTag("material-numeric-cell") else Modifier,
        clearLabel = bindings.clearLabel,
        trailingIcon = { MaterialNumericStepButtons(decrease, increase,
            bindings.controlProps.enabled && !bindings.controlProps.readonly,
            props.state.jsonforms.i18n) { next ->
            draftValue = next.content
            lastCommittedValue = next.content
            setPending(false)
            bindings.dispatchProps.handleChange(bindings.stateProps.path, next)
        } },
        onChange = { value ->
            draftValue = value
            when {
                value.isEmpty() -> {
                    lastCommittedValue = value
                    setPending(false)
                    bindings.dispatchProps.handleChange(bindings.stateProps.path,
                        JsonPrimitive(0).takeIf { preserveEmptyValue })
                }
                else -> {
                    val parsed = parseIntegerInput(value)
                    if (parsed.error == null && parsed.data is JsonPrimitive &&
                        acceptsRestrictedNumericEdit(bindings.stateProps.data, parsed.data,
                            bindings.stateProps.schema, restrict, bindings.stateProps.rootSchema)) {
                        lastCommittedValue = value
                        setPending(false)
                        bindings.dispatchProps.handleChange(bindings.stateProps.path, parsed.data)
                    } else {
                        setPending(true)
                    }
                }
            }
        },
    )
}

val materialIntegerControlTester: RankedTester = rankWith(
    2,
    isIntegerControl,
)
