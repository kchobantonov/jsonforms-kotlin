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
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.core.i18n.translateTemporalControlText
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.ActionText
import kotlinx.serialization.json.JsonPrimitive

val MaterialTimeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialTimeControl(props) }

@Composable
internal fun MaterialTimeCell(props: ComposeJsonFormsCellProps) =
    MaterialTimeControl(props.asRendererProps(), compact = true)

@Composable
fun MaterialTimeControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = materialControlBindings(props, "MaterialTimeControl")
    val controller = useJsonForms().controller
    val controlProps = bindings.controlProps
    if (!controlProps.visible) return
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("timeFormat") ?: DEFAULT_TIME_DISPLAY_FORMAT
    val saveFormat = temporalSaveFormatForControl(controlProps.schema.format,
        options.stringOption("timeSaveFormat"), DEFAULT_TIME_SAVE_FORMAT,
        bindings.stateProps.path, props.config)
    val timeViews = resolveTimeViews(options["views"], displayFormat)
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    fun text(key: String, fallback: String) = translateTemporalControlText(props.state.jsonforms.i18n,
        i18nKeyPrefix, key, fallback, displayFormat, translationValues)
    val pickerText = ActionText(text("chooseTimeTooltip", "Choose time"),
        text("chooseTimeAriaLabel", "Choose time"))
    val invalidTimeText = text("invalidTime", "Invalid time. Expected $displayFormat.")
    val pickerRangeText = text("time.outOfRange", "Select a time within the allowed range.")
    val cancelText = options.stringOption("cancelLabel")
        ?: text("cancel", "Cancel")
    val okText = options.stringOption("okLabel")
        ?: text("ok", "OK")
    val selectTimeText = text("selectTime", "Select time")
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val committedTemporal = parseTimeValue(committedValue, listOf(saveFormat, DEFAULT_TIME_SAVE_FORMAT, "HH:mm:ss"))
    val restrict = resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config)
    fun rangeError(value: TemporalValue): ErrorObject? = temporalRangeViolation(value,
        controlProps.schema, TemporalBoundKind.Time, controller.state.jsonforms.core.data,
        bindings.stateProps.path)?.let { violation -> ErrorObject(
        keyword = violation.keyword,
        message = temporalViolationMessage(props.state.jsonforms.i18n,
            i18nKeyPrefix, displayFormat, TemporalBoundKind.Time, violation),
        params = mapOf("bound" to violation.bound),
    ) }
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    var draftValue by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path, props.schema, props.uischema) { mutableStateOf(committedValue) }
    var showPicker by remember(bindings.stateProps.path, props.uischema) { mutableStateOf(false) }
    val maskInput = options.booleanOption("mask") != false
    val parsedDraft = draftValue.takeIf(String::isNotEmpty)?.let { parseTimeValue(it, listOf(displayFormat)) }
    val localError = when {
        draftValue.isEmpty() -> null
        parsedDraft == null -> invalidTimeText
        else -> rangeError(parsedDraft)?.message
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("temporal:time:${bindings.stateProps.path}:${props.id}")
    LaunchedEffect(props.uischema) { setPending(false) }
    LaunchedEffect(committedValue, displayValue, props.state.jsonforms.core.data) {
        if (committedValue != observedCommittedValue) {
            draftValue = displayValue
            observedCommittedValue = committedValue
            setPending(false)
        } else if (draftValue != displayValue && parseTimeValue(draftValue, listOf(displayFormat)) == null) {
            setPending(true)
        }
    }

    fun commit(temporal: TemporalValue) {
        val saved = formatTemporal(temporal, saveFormat)
        observedCommittedValue = saved
        draftValue = formatTemporal(temporal, displayFormat)
        setPending(false)
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
        storedValueStatus = invalidStatus,
        compact = compact,
        inputModifier = if (compact) Modifier.testTag("material-temporal-cell") else Modifier,
        clearLabel = bindings.clearLabel,
        onChange = { value ->
            if (!isAllowedTemporalEdit(draftValue, value, displayFormat, maskInput)) return@MaterialInputControl
            draftValue = value
            when {
                value.isEmpty() -> {
                    setPending(false)
                    observedCommittedValue = ""
                    bindings.dispatchProps.handleChange(
                        bindings.stateProps.path,
                        JsonPrimitive("").takeIf { preserveEmptyValue },
                    )
                }
                else -> {
                    val temporal = parseTimeValue(value, listOf(displayFormat))
                    if (temporal != null) {
                        commit(temporal)
                    } else {
                        setPending(true)
                    }
                }
            }
        },
    )

    if (showPicker) {
        val initial = committedTemporal ?: currentTemporalValue()
        if (timeViews != null) MaterialTimeViewsDialog(
            initial = initial,
            views = timeViews,
            is24Hour = options.booleanOption("ampm") != true,
            cancelLabel = cancelText,
            okLabel = okText,
            titleLabel = selectTimeText,
            viewLabel = { view -> when (view) {
                TimeView.Hours -> text("hours", "Hours")
                TimeView.Minutes -> text("minutes", "Minutes")
                TimeView.Seconds -> text("seconds", "Seconds")
            } },
            invalidSelectionMessage = pickerRangeText,
            canConfirm = { !restrict || rangeError(it) == null },
            showActions = options.booleanOption("showActions") == true,
            onSelectionChange = { selected ->
                if (!restrict || rangeError(selected) == null) commit(selected)
            },
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                if (restrict && rangeError(selected) != null) return@MaterialTimeViewsDialog
                commit(selected)
                showPicker = false
            },
        ) else MaterialTimePickerDialog(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = options.booleanOption("ampm") != true,
            cancelLabel = cancelText,
            okLabel = okText,
            titleLabel = selectTimeText,
            invalidSelectionMessage = pickerRangeText,
            showActions = options.booleanOption("showActions") == true,
            onSelectionChange = { hour, minute ->
                val selected = initial.copy(hour = hour, minute = minute, second = initial.second)
                if (!restrict || rangeError(selected) == null) commit(selected)
            },
            canConfirm = { hour, minute ->
                !restrict || rangeError(initial.copy(hour = hour, minute = minute, second = initial.second)) == null
            },
            onDismiss = { showPicker = false },
            onConfirm = { hour, minute ->
                val selected = initial.copy(hour = hour, minute = minute, second = initial.second)
                if (restrict && rangeError(selected) != null) return@MaterialTimePickerDialog
                commit(selected)
                showPicker = false
            },
        )
    }
}

val materialTimeControlTester: RankedTester = rankWith(4, isTimeControl)
