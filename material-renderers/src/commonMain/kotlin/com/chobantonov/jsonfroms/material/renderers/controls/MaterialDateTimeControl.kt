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
import com.chobantonov.jsonfroms.core.testers.isDateTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import kotlinx.serialization.json.JsonPrimitive

val MaterialDateTimeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialDateTimeControl(props) }

@Composable
fun MaterialDateTimeControl(props: ComposeJsonFormsRendererProps) {
    val bindings = materialControlBindings(props, "MaterialDateTimeControl")
    val controlProps = bindings.controlProps
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("dateTimeFormat") ?: DEFAULT_DATE_TIME_DISPLAY_FORMAT
    val saveFormat = options.stringOption("dateTimeSaveFormat") ?: DEFAULT_DATE_TIME_SAVE_FORMAT
    val translator = props.state.jsonforms.i18n.translate
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    val pickerText = translatedActionText(
        translator,
        i18nKeyPrefix,
        "chooseDateTimeTooltip",
        "chooseDateTimeAriaLabel",
        "Choose date and time",
        values = translationValues,
    )
    val invalidDateTimeText = translatedText(
        translator,
        i18nKeyPrefix,
        "invalidDateTime",
        "Invalid date and time. Expected $displayFormat.",
        translationValues,
    )
    val cancelText = options.stringOption("cancelLabel")
        ?: translatedText(translator, i18nKeyPrefix, "cancel", "Cancel", translationValues)
    val nextText = translatedText(translator, i18nKeyPrefix, "next", "Next", translationValues)
    val okText = options.stringOption("okLabel")
        ?: translatedText(translator, i18nKeyPrefix, "ok", "OK", translationValues)
    val selectTimeText = translatedText(
        translator,
        i18nKeyPrefix,
        "selectTime",
        "Select time",
        translationValues,
    )
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val committedTemporal = parseDateTimeValue(
        committedValue,
        listOf(saveFormat, DEFAULT_DATE_TIME_SAVE_FORMAT, "YYYY-MM-DDTHH:mm:ss.SSSZ", "YYYY-MM-DDTHH:mm:ss"),
    )
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var pickerStep by remember { mutableStateOf<PickerStep?>(null) }
    var pendingValue by remember { mutableStateOf<TemporalValue?>(null) }
    val maskInput = options.booleanOption("mask") != false
    val localError = draftValue
        .takeIf { it.isNotEmpty() && parseDateTimeValue(it, listOf(displayFormat)) == null }
        ?.let { invalidDateTimeText }
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
                type = TemporalPickerType.DateTime,
                enabled = controlProps.enabled && !controlProps.readonly,
                tooltipLabel = pickerText.tooltip,
                ariaLabel = pickerText.ariaLabel,
                onClick = {
                    pendingValue = committedTemporal ?: currentTemporalValue()
                    pickerStep = PickerStep.Date
                },
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
                    val temporal = parseDateTimeValue(value, listOf(displayFormat))
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

    when (pickerStep) {
        PickerStep.Date -> {
            val initial = pendingValue ?: currentTemporalValue()
            MaterialDatePickerDialog(
                initialDateMillis = initial.date.toPickerMillis(),
                cancelLabel = cancelText,
                okLabel = nextText,
                onDismiss = { pickerStep = null },
                onConfirm = { millis ->
                    val date = pickerMillisToDate(millis)
                    pendingValue = initial.copy(year = date.year, month = date.month.ordinal + 1, day = date.day)
                    pickerStep = PickerStep.Time
                },
            )
        }
        PickerStep.Time -> {
            val initial = pendingValue ?: currentTemporalValue()
            MaterialTimePickerDialog(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = options.booleanOption("ampm") == false,
                cancelLabel = cancelText,
                okLabel = okText,
                titleLabel = selectTimeText,
                onDismiss = { pickerStep = null },
                onConfirm = { hour, minute ->
                    commit(initial.copy(hour = hour, minute = minute))
                    pickerStep = null
                },
            )
        }
        null -> Unit
    }
}

private enum class PickerStep { Date, Time }

val materialDateTimeControlTester: RankedTester = rankWith(2, isDateTimeControl)
