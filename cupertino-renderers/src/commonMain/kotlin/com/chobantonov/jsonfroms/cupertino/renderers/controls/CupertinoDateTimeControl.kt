package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.asRendererProps
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
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_DISPLAY_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalBoundKind
import com.chobantonov.jsonfroms.material.renderers.controls.TimeView
import com.chobantonov.jsonfroms.material.renderers.controls.DateViewMode
import com.chobantonov.jsonfroms.material.renderers.controls.booleanOption
import com.chobantonov.jsonfroms.material.renderers.controls.currentTemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import com.chobantonov.jsonfroms.material.renderers.controls.temporalViolationMessage
import com.chobantonov.jsonfroms.material.renderers.controls.isAllowedTemporalEdit
import com.chobantonov.jsonfroms.material.renderers.controls.parseDateTimeValue
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.chobantonov.jsonfroms.material.renderers.controls.resolveTimeViews
import com.chobantonov.jsonfroms.material.renderers.controls.resolveDateViewMode
import com.chobantonov.jsonfroms.material.renderers.controls.stringOption
import com.chobantonov.jsonfroms.material.renderers.controls.temporalRangeViolation
import com.chobantonov.jsonfroms.material.renderers.controls.temporalDateHasSelectableMinute
import com.chobantonov.jsonfroms.material.renderers.controls.temporalSaveFormatForControl
import com.chobantonov.jsonfroms.material.renderers.controls.toPickerMillis
import kotlinx.serialization.json.JsonPrimitive

val CupertinoDateTimeControl = ComposeJsonFormsRenderer { CupertinoDateTimeControl(it) }

@Composable
internal fun CupertinoDateTimeCell(props: ComposeJsonFormsCellProps) =
    CupertinoDateTimeControl(props.asRendererProps(), compact = true)

@Composable
fun CupertinoDateTimeControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = cupertinoControlBindings(props, "CupertinoDateTimeControl")
    val controller = useJsonForms().controller
    val control = bindings.controlProps
    if (!control.visible) return
    val options = control.config + control.uischema.options
    val displayFormat = options.stringOption("dateTimeFormat") ?: DEFAULT_DATE_TIME_DISPLAY_FORMAT
    val saveFormat = temporalSaveFormatForControl(control.schema.format,
        options.stringOption("dateTimeSaveFormat"), DEFAULT_DATE_TIME_SAVE_FORMAT,
        bindings.stateProps.path, props.config, props.state.jsonforms.i18n,
        bindings.stateProps.i18nKeyPrefix, displayFormat)
    val timeViews = resolveTimeViews(options["views"], displayFormat)
    val dateMode = resolveDateViewMode(options["views"], displayFormat)
    val showActions = options.booleanOption("showActions") == true
    val prefix = bindings.stateProps.i18nKeyPrefix
    val values = mapOf<String, Any?>("label" to bindings.stateProps.label, "format" to displayFormat)
    fun text(key: String, fallback: String) = translateTemporalControlText(props.state.jsonforms.i18n,
        prefix, key, fallback, displayFormat, values)
    val pickerLabel = text("chooseDateTimeTooltip", "Pick")
    val pickerAriaLabel = text("chooseDateTimeAriaLabel", "Choose date and time")
    val invalidDateTimeText = text("invalidDateTime", "Invalid date and time. Expected $displayFormat.")
    val pickerRangeText = text("dateTime.outOfRange", "Select a date and time within the allowed range.")
    val selectTimeText = text("selectTime", "Select time")
    val selectDateText = text("selectDate", "Select date")
    val nextText = text("next", "Next")
    val cancelText = options.stringOption("cancelLabel") ?: text("cancel", "Cancel")
    val okText = options.stringOption("okLabel") ?: text("ok", "OK")
    val committed = bindings.stateProps.data.toCupertinoInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val parsed = parseDateTimeValue(
        committed,
        listOf(saveFormat, DEFAULT_DATE_TIME_SAVE_FORMAT, "YYYY-MM-DDTHH:mm:ss.SSSZ", "YYYY-MM-DDTHH:mm:ss"),
    )
    val restrict = resolveRestrict(control.uischema.options["restrict"], control.config)
    fun rangeError(value: TemporalValue): ErrorObject? = temporalRangeViolation(value,
        control.schema, TemporalBoundKind.DateTime, controller.state.jsonforms.core.data,
        bindings.stateProps.path)?.let { violation -> ErrorObject(
        keyword = violation.keyword,
        message = temporalViolationMessage(props.state.jsonforms.i18n,
            prefix, displayFormat, TemporalBoundKind.DateTime, violation),
        params = mapOf("bound" to violation.bound),
    ) }
    val displayed = parsed?.let { formatTemporal(it, displayFormat) } ?: committed
    val editOptions = props.uischema.options - "placeholder"
    var draft by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(displayed) }
    var observed by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(committed) }
    var showPicker by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat) { mutableStateOf(false) }
    var showTimeStep by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat) { mutableStateOf(false) }
    var pendingValue by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat) { mutableStateOf<TemporalValue?>(null) }
    val parsedDraft = draft.takeIf(String::isNotEmpty)?.let { parseDateTimeValue(it, listOf(displayFormat)) }
    val localError = when {
        draft.isEmpty() -> null
        parsedDraft == null -> invalidDateTimeText
        else -> rangeError(parsedDraft)?.message
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("temporal:date-time:${bindings.stateProps.path}:${props.id}")
    LaunchedEffect(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { setPending(false) }

    LaunchedEffect(committed, displayed, props.state.jsonforms.core.data) {
        if (committed != observed) {
            draft = displayed
            observed = committed
            showPicker = false
            showTimeStep = false
            pendingValue = null
            setPending(false)
        } else if (draft != displayed && parseDateTimeValue(draft, listOf(displayFormat)) == null) {
            setPending(true)
        }
    }

    fun commit(value: TemporalValue) {
        val saved = formatTemporal(value, saveFormat)
        observed = saved
        draft = formatTemporal(value, displayFormat)
        setPending(false)
        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(saved))
    }

    val colors = LocalCupertinoColors.current
    CupertinoInputControl(
        props = control,
        value = draft,
        localError = localError,
        storedValueStatus = invalidStatus,
        compact = compact,
        inputModifier = if (compact) Modifier.testTag("cupertino-temporal-cell") else Modifier,
        clearLabel = bindings.clearLabel,
        placeholder = options.stringOption("placeholder") ?: displayFormat,
        trailingContent = {
            Text(
                pickerLabel,
                Modifier.semantics { contentDescription = pickerAriaLabel }
                    .clickable(control.enabled && !control.readonly) { showPicker = true }
                    .padding(start = 10.dp),
                color = colors.accent,
            )
        },
    ) { value ->
        if (!isAllowedTemporalEdit(draft, value, displayFormat, options.booleanOption("mask") != false)) return@CupertinoInputControl
        draft = value
        if (value.isEmpty()) {
            setPending(false)
            observed = ""
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                JsonPrimitive("").takeIf { preserveEmptyValue },
            )
        } else {
            parseDateTimeValue(value, listOf(displayFormat))?.let(::commit) ?: run {
                setPending(true)
            }
        }
    }

    if (showPicker) {
        val initial = parsed ?: currentTemporalValue()
        if (dateMode != DateViewMode.Day) CupertinoDateViewsDialog(
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
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                pendingValue = selected
                showPicker = false
                showTimeStep = true
            },
        ) else CupertinoDatePickerDialog(
            initialDateMillis = initial.date.toPickerMillis(),
            titleLabel = selectDateText,
            cancelLabel = cancelText,
            okLabel = nextText,
            invalidSelectionMessage = pickerRangeText,
            isSelectableDate = { millis ->
                !restrict || timeViews != null || pickerMillisToDate(millis).let { date ->
                    temporalDateHasSelectableMinute(
                        TemporalValue(date.year, date.month.ordinal + 1, date.day),
                        control.schema, controller.state.jsonforms.core.data,
                        bindings.stateProps.path, initial.second,
                    )
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
            onDismiss = { showPicker = false },
            onConfirm = { millis ->
                val date = pickerMillisToDate(millis)
                if (restrict && timeViews == null && !temporalDateHasSelectableMinute(
                        TemporalValue(date.year, date.month.ordinal + 1, date.day),
                        control.schema, controller.state.jsonforms.core.data,
                        bindings.stateProps.path, initial.second,
                    )) return@CupertinoDatePickerDialog
                pendingValue = initial.copy(year = date.year, month = date.month.ordinal + 1, day = date.day)
                showPicker = false
                showTimeStep = true
            },
        )
    }
    if (showTimeStep) {
        val initial = pendingValue ?: parsed ?: currentTemporalValue()
        if (timeViews != null) CupertinoTimeViewsDialog(
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
            onDismiss = { showTimeStep = false },
            onConfirm = { selected ->
                if (restrict && rangeError(selected) != null) return@CupertinoTimeViewsDialog
                commit(selected)
                showTimeStep = false
            },
        ) else CupertinoTimePickerDialog(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = options.booleanOption("ampm") != true,
            cancelLabel = cancelText,
            okLabel = okText,
            titleLabel = selectTimeText,
            invalidSelectionMessage = pickerRangeText,
            canConfirm = { hour, minute ->
                !restrict || rangeError(initial.copy(hour = hour, minute = minute)) == null
            },
            showActions = showActions,
            onSelectionChange = { hour, minute ->
                if (!showActions) {
                    val selected = initial.copy(hour = hour, minute = minute)
                    if (!restrict || rangeError(selected) == null) {
                        pendingValue = selected
                        commit(selected)
                    }
                }
            },
            onDismiss = { showTimeStep = false },
            onConfirm = { hour, minute ->
                val selected = initial.copy(hour = hour, minute = minute)
                if (restrict && rangeError(selected) != null) return@CupertinoTimePickerDialog
                commit(selected)
                showTimeStep = false
            },
        )
    }
}

val cupertinoDateTimeControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isDateTimeControl))
