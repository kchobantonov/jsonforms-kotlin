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
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import kotlinx.serialization.json.JsonPrimitive

val MaterialTimeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialTimeControl(props) }

@Composable
fun MaterialTimeControl(props: ComposeJsonFormsRendererProps) {
    val bindings = materialControlBindings(props, "MaterialTimeControl")
    val controlProps = bindings.controlProps
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("timeFormat") ?: DEFAULT_TIME_DISPLAY_FORMAT
    val saveFormat = options.stringOption("timeSaveFormat") ?: DEFAULT_TIME_SAVE_FORMAT
    val translator = props.state.jsonforms.i18n.translate
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    val pickerText = translatedActionText(
        translator,
        i18nKeyPrefix,
        "chooseTimeTooltip",
        "chooseTimeAriaLabel",
        "Choose time",
        values = translationValues,
    )
    val invalidTimeText = translatedText(
        translator,
        i18nKeyPrefix,
        "invalidTime",
        "Invalid time. Expected $displayFormat.",
        translationValues,
    )
    val cancelText = options.stringOption("cancelLabel")
        ?: translatedText(translator, i18nKeyPrefix, "cancel", "Cancel", translationValues)
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
    val committedTemporal = parseTimeValue(committedValue, listOf(saveFormat, DEFAULT_TIME_SAVE_FORMAT, "HH:mm:ssZ"))
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    var draftValue by remember(bindings.stateProps.path) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path) { mutableStateOf(committedValue) }
    var showPicker by remember { mutableStateOf(false) }
    val maskInput = options.booleanOption("mask") != false
    val localError = draftValue
        .takeIf { it.isNotEmpty() && parseTimeValue(it, listOf(displayFormat)) == null }
        ?.let { invalidTimeText }

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
                type = TemporalPickerType.Time,
                enabled = controlProps.enabled && !controlProps.readonly,
                tooltipLabel = pickerText.tooltip,
                ariaLabel = pickerText.ariaLabel,
                onClick = { showPicker = true },
            )
        },
        showDescriptionOnlyWhenFocused = true,
        localError = localError,
        onChange = { value ->
            if (!isAllowedTemporalEdit(draftValue, value, displayFormat, maskInput)) return@MaterialInputControl
            draftValue = value
            when {
                value.isEmpty() -> {
                    observedCommittedValue = ""
                    bindings.dispatchProps.handleChange(bindings.stateProps.path, null)
                }
                else -> {
                    val temporal = parseTimeValue(value, listOf(displayFormat))
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
        val initial = committedTemporal ?: currentTemporalValue()
        MaterialTimePickerDialog(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = options.booleanOption("ampm") == false,
            cancelLabel = cancelText,
            okLabel = okText,
            titleLabel = selectTimeText,
            onDismiss = { showPicker = false },
            onConfirm = { hour, minute ->
                commit(initial.copy(hour = hour, minute = minute))
                showPicker = false
            },
        )
    }
}

val materialTimeControlTester: RankedTester = rankWith(4, isTimeControl)
