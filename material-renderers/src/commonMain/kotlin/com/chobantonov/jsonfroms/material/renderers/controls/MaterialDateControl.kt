package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import kotlinx.serialization.json.JsonPrimitive

val MaterialDateControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialDateControl(props) }

@Composable
fun MaterialDateControl(props: ComposeJsonFormsRendererProps) {
    val bindings = materialControlBindings(props, "MaterialDateControl")
    val controlProps = bindings.controlProps
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("dateFormat") ?: DEFAULT_DATE_DISPLAY_FORMAT
    val saveFormat = options.stringOption("dateSaveFormat") ?: DEFAULT_DATE_SAVE_FORMAT
    val translator = props.state.jsonforms.i18n.translate
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    val pickerText = translatedActionText(
        translator,
        i18nKeyPrefix,
        "chooseDateTooltip",
        "chooseDateAriaLabel",
        "Choose date",
        values = translationValues,
    )
    val invalidDateText = translatedText(
        translator,
        i18nKeyPrefix,
        "invalidDate",
        "Invalid date. Expected $displayFormat.",
        translationValues,
    )
    val cancelText = options.stringOption("cancelLabel")
        ?: translatedText(translator, i18nKeyPrefix, "cancel", "Cancel", translationValues)
    val okText = options.stringOption("okLabel")
        ?: translatedText(translator, i18nKeyPrefix, "ok", "OK", translationValues)
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val committedTemporal = parseDateValue(committedValue, listOf(saveFormat, DEFAULT_DATE_SAVE_FORMAT))
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var showPicker by remember { mutableStateOf(false) }
    val maskInput = options.booleanOption("mask") != false
    val localError = draftValue
        .takeIf { it.isNotEmpty() && parseDateValue(it, listOf(displayFormat)) == null }
        ?.let { invalidDateText }
    val preserveEmptyValue = LocalPreserveEmptyValue.current

    LaunchedEffect(committedValue, displayValue) {
        if (committedValue != observedCommittedValue) {
            draftValue = displayValue
            observedCommittedValue = committedValue
        }
    }

    fun commit(temporal: TemporalValue) {
        val saved = formatTemporal(temporal, saveFormat)
        observedCommittedValue = saved
        draftValue = formatTemporal(temporal, displayFormat)
        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(saved))
    }

    MaterialInputControl(
        props = controlProps,
        value = draftValue,
        keyboardType = KeyboardType.Text,
        placeholder = options.stringOption("placeholder") ?: displayFormat,
        leadingIcon = {
            TemporalPickerIcon(
                type = TemporalPickerType.Date,
                enabled = controlProps.enabled && !controlProps.readonly,
                tooltipLabel = pickerText.tooltip,
                ariaLabel = pickerText.ariaLabel,
                onClick = { showPicker = true },
            )
        },
        showDescriptionOnlyWhenFocused = true,
        localError = localError,
        clearLabel = bindings.clearLabel,
        onChange = { value ->
            if (!isAllowedTemporalEdit(draftValue, value, displayFormat, maskInput)) return@MaterialInputControl
            draftValue = value
            when {
                value.isEmpty() -> {
                    observedCommittedValue = ""
                    bindings.dispatchProps.handleChange(
                        bindings.stateProps.path,
                        JsonPrimitive("").takeIf { preserveEmptyValue },
                    )
                }
                else -> {
                    val temporal = parseDateValue(value, listOf(displayFormat))
                    if (temporal != null) {
                        commit(temporal)
                    } else {
                        observedCommittedValue = value
                        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
                    }
                }
            }
        },
    )

    if (showPicker) {
        val initialDate = committedTemporal?.date ?: currentTemporalValue().date
        MaterialDatePickerDialog(
            initialDateMillis = initialDate.toPickerMillis(),
            cancelLabel = cancelText,
            okLabel = okText,
            onDismiss = { showPicker = false },
            onConfirm = { millis ->
                val date = pickerMillisToDate(millis)
                commit(TemporalValue(date.year, date.monthNumber, date.day))
                showPicker = false
            },
        )
    }
}

val materialDateControlTester: RankedTester = rankWith(4, isDateControl)
