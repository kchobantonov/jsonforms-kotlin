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
import com.chobantonov.jsonfroms.core.testers.isDateTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.ActionText
import kotlinx.serialization.json.JsonPrimitive

val MaterialDateTimeControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialDateTimeControl(props) }

@Composable
internal fun MaterialDateTimeCell(props: ComposeJsonFormsCellProps) =
    MaterialDateTimeControl(props.asRendererProps(), compact = true)

@Composable
fun MaterialDateTimeControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = materialControlBindings(props, "MaterialDateTimeControl")
    val controller = useJsonForms().controller
    val controlProps = bindings.controlProps
    if (!controlProps.visible) return
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("dateTimeFormat") ?: DEFAULT_DATE_TIME_DISPLAY_FORMAT
    val saveFormat = temporalSaveFormatForControl(controlProps.schema.format,
        options.stringOption("dateTimeSaveFormat"), DEFAULT_DATE_TIME_SAVE_FORMAT,
        bindings.stateProps.path, props.config, props.state.jsonforms.i18n,
        bindings.stateProps.i18nKeyPrefix, displayFormat)
    val timeViews = resolveTimeViews(options["views"], displayFormat)
    val dateMode = resolveDateViewMode(options["views"], displayFormat)
    val showActions = options.booleanOption("showActions") == true
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    fun text(key: String, fallback: String) = translateTemporalControlText(props.state.jsonforms.i18n,
        i18nKeyPrefix, key, fallback, displayFormat, translationValues)
    val pickerText = ActionText(text("chooseDateTimeTooltip", "Choose date and time"),
        text("chooseDateTimeAriaLabel", "Choose date and time"))
    val invalidDateTimeText = text("invalidDateTime", "Invalid date and time. Expected $displayFormat.")
    val pickerRangeText = text("dateTime.outOfRange", "Select a date and time within the allowed range.")
    val cancelText = options.stringOption("cancelLabel")
        ?: text("cancel", "Cancel")
    val nextText = text("next", "Next")
    val okText = options.stringOption("okLabel")
        ?: text("ok", "OK")
    val selectTimeText = text("selectTime", "Select time")
    val selectDateText = text("selectDate", "Select date")
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val committedTemporal = parseDateTimeValue(
        committedValue,
        listOf(saveFormat, DEFAULT_DATE_TIME_SAVE_FORMAT, "YYYY-MM-DDTHH:mm:ss.SSSZ", "YYYY-MM-DDTHH:mm:ss"),
    )
    val restrict = resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config)
    fun rangeError(value: TemporalValue): ErrorObject? = temporalRangeViolation(value,
        controlProps.schema, TemporalBoundKind.DateTime, controller.state.jsonforms.core.data,
        bindings.stateProps.path)?.let { violation -> ErrorObject(
        keyword = violation.keyword,
        message = temporalViolationMessage(props.state.jsonforms.i18n,
            i18nKeyPrefix, displayFormat, TemporalBoundKind.DateTime, violation),
        params = mapOf("bound" to violation.bound),
    ) }
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    val editOptions = props.uischema.options - "placeholder"
    var draftValue by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(committedValue) }
    var pickerStep by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat) { mutableStateOf<PickerStep?>(null) }
    var pendingValue by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat) { mutableStateOf<TemporalValue?>(null) }
    val maskInput = options.booleanOption("mask") != false
    val parsedDraft = draftValue.takeIf(String::isNotEmpty)?.let { parseDateTimeValue(it, listOf(displayFormat)) }
    val localError = when {
        draftValue.isEmpty() -> null
        parsedDraft == null -> invalidDateTimeText
        else -> rangeError(parsedDraft)?.message
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("temporal:date-time:${bindings.stateProps.path}:${props.id}")
    LaunchedEffect(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { setPending(false) }
    LaunchedEffect(committedValue, displayValue, props.state.jsonforms.core.data) {
        if (committedValue != observedCommittedValue) {
            draftValue = displayValue
            observedCommittedValue = committedValue
            pickerStep = null
            pendingValue = null
            setPending(false)
        } else if (draftValue != displayValue && parseDateTimeValue(draftValue, listOf(displayFormat)) == null) {
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
                    val temporal = parseDateTimeValue(value, listOf(displayFormat))
                    if (temporal != null) {
                        commit(temporal)
                    } else {
                        setPending(true)
                    }
                }
            }
        },
    )

    when (pickerStep) {
        PickerStep.Date -> {
            val initial = pendingValue ?: currentTemporalValue()
            if (dateMode != DateViewMode.Day) MaterialDateViewsDialog(
                initial = initial,
                mode = dateMode,
                titleLabel = selectDateText,
                yearLabel = text("year", "Year"),
                monthLabel = text("month", "Month"),
                previousLabel = if (dateMode == DateViewMode.Year) text("previousYears", "Previous 12 years")
                    else text("previousYear", "Previous"),
                nextLabel = if (dateMode == DateViewMode.Year) text("nextYears", "Next 12 years")
                    else text("nextYear", "Next"),
                cancelLabel = cancelText,
                okLabel = nextText,
                invalidSelectionMessage = pickerRangeText,
                canConfirm = { true },
                showDismissAction = showActions,
                commitOnSelection = !showActions,
                onSelectionChange = { selected ->
                    if (!showActions) {
                        pendingValue = selected
                        if (!restrict || rangeError(selected) == null) commit(selected)
                    }
                },
                onDismiss = { pickerStep = null },
                onConfirm = { selected ->
                    pendingValue = selected
                    pickerStep = PickerStep.Time
                },
            ) else MaterialDatePickerDialog(
                language = props.state.jsonforms.i18n.locale ?: "en",
                initialDateMillis = initial.date.toPickerMillis(),
                pickerText = MaterialDatePickerText(
                    selectDate = selectDateText,
                    enterDate = text("enterDate", "Enter date"),
                    selectedDate = text("selectedDate", "Selected date"),
                    noDateSelected = text("noDateSelected", "No date selected"),
                    enteredDate = text("enteredDate", "Entered date"),
                    noDateEntered = text("noDateEntered", "No date entered"),
                    switchToInputMode = ActionText(
                        text("switchToInputModeTooltip", "Switch to text input mode"),
                        text("switchToInputModeAriaLabel", "Switch to text input mode"),
                    ),
                    switchToCalendarMode = ActionText(
                        text("switchToCalendarModeTooltip", "Switch to calendar mode"),
                        text("switchToCalendarModeAriaLabel", "Switch to calendar mode"),
                    ),
                ),
                cancelLabel = cancelText,
                okLabel = nextText,
                invalidSelectionMessage = pickerRangeText,
                isSelectableDate = { millis ->
                    !restrict || timeViews != null || pickerMillisToDate(millis).let { date ->
                        temporalDateHasSelectableMinute(TemporalValue(date.year, date.month.ordinal + 1, date.day),
                            controlProps.schema, controller.state.jsonforms.core.data,
                            bindings.stateProps.path, initial.second)
                    }
                },
                showDismissAction = showActions,
                commitOnSelection = !showActions,
                onSelectionChange = { millis ->
                    if (!showActions) {
                        val date = pickerMillisToDate(millis)
                        val selected = initial.copy(year = date.year, month = date.month.ordinal + 1,
                            day = date.day)
                        pendingValue = selected
                        if (!restrict || rangeError(selected) == null) commit(selected)
                    }
                },
                onDismiss = { pickerStep = null },
                onConfirm = { millis ->
                    val date = pickerMillisToDate(millis)
                    if (restrict && timeViews == null && !temporalDateHasSelectableMinute(
                            TemporalValue(date.year, date.month.ordinal + 1, date.day), controlProps.schema,
                        controller.state.jsonforms.core.data, bindings.stateProps.path, initial.second))
                        return@MaterialDatePickerDialog
                    pendingValue = initial.copy(year = date.year, month = date.month.ordinal + 1, day = date.day)
                    pickerStep = PickerStep.Time
                },
            )
        }
        PickerStep.Time -> {
            val initial = pendingValue ?: currentTemporalValue()
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
                showActions = showActions,
                onSelectionChange = { selected ->
                    if (!showActions && (!restrict || rangeError(selected) == null)) {
                        pendingValue = selected
                        commit(selected)
                    }
                },
                onDismiss = { pickerStep = null },
                onConfirm = { selected ->
                    if (restrict && rangeError(selected) != null) return@MaterialTimeViewsDialog
                    commit(selected)
                    pickerStep = null
                },
            ) else MaterialTimePickerDialog(
                initialHour = initial.hour,
                initialMinute = initial.minute,
                is24Hour = options.booleanOption("ampm") != true,
                cancelLabel = cancelText,
                okLabel = okText,
                titleLabel = selectTimeText,
                invalidSelectionMessage = pickerRangeText,
                showActions = showActions,
                canConfirm = { hour, minute ->
                    !restrict || rangeError(initial.copy(hour = hour, minute = minute)) == null
                },
                onSelectionChange = { hour, minute ->
                    val selected = initial.copy(hour = hour, minute = minute)
                    if (!restrict || rangeError(selected) == null) {
                        pendingValue = selected
                        commit(selected)
                    }
                },
                onDismiss = { pickerStep = null },
                onConfirm = { hour, minute ->
                    val selected = initial.copy(hour = hour, minute = minute)
                    if (restrict && rangeError(selected) != null) return@MaterialTimePickerDialog
                    commit(selected)
                    pickerStep = null
                },
            )
        }
        null -> Unit
    }
}

private enum class PickerStep { Date, Time }

val materialDateTimeControlTester: RankedTester = rankWith(2, isDateTimeControl)
