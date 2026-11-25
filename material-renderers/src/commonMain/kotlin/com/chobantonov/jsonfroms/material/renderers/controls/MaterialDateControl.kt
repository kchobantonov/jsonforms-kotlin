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
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.ActionText
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonPrimitive

val MaterialDateControl: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props -> MaterialDateControl(props) }

@Composable
internal fun MaterialDateCell(props: ComposeJsonFormsCellProps) =
    MaterialDateControl(props.asRendererProps(), compact = true)

@Composable
fun MaterialDateControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = materialControlBindings(props, "MaterialDateControl")
    val controller = useJsonForms().controller
    val controlProps = bindings.controlProps
    if (!controlProps.visible) return
    val options = controlProps.config + controlProps.uischema.options
    val displayFormat = options.stringOption("dateFormat") ?: DEFAULT_DATE_DISPLAY_FORMAT
    val saveFormat = temporalSaveFormatForControl(controlProps.schema.format,
        options.stringOption("dateSaveFormat"), DEFAULT_DATE_SAVE_FORMAT,
        bindings.stateProps.path, props.config, props.state.jsonforms.i18n,
        bindings.stateProps.i18nKeyPrefix, displayFormat)
    val dateMode = resolveDateViewMode(options["views"], displayFormat)
    val i18nKeyPrefix = bindings.stateProps.i18nKeyPrefix
    val translationValues = mapOf("label" to bindings.stateProps.label, "format" to displayFormat)
    fun text(key: String, fallback: String) = translateTemporalControlText(props.state.jsonforms.i18n,
        i18nKeyPrefix, key, fallback, displayFormat, translationValues)
    val pickerText = ActionText(text("chooseDateTooltip", "Choose date"),
        text("chooseDateAriaLabel", "Choose date"))
    val invalidDateText = text("invalidDate", "Invalid date. Expected $displayFormat.")
    val pickerRangeText = text("date.outOfRange", "Select a date within the allowed range.")
    val cancelText = options.stringOption("cancelLabel")
        ?: text("cancel", "Cancel")
    val okText = options.stringOption("okLabel")
        ?: text("ok", "OK")
    val selectDateText = text("selectDate", "Select date")
    val committedValue = bindings.stateProps.data.toMaterialInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val committedTemporal = parseDateValue(committedValue, listOf(saveFormat, DEFAULT_DATE_SAVE_FORMAT))
    val restrict = resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config)
    fun rangeError(value: TemporalValue): ErrorObject? = temporalRangeViolation(value,
        controlProps.schema, TemporalBoundKind.Date, controller.state.jsonforms.core.data,
        bindings.stateProps.path)?.let { violation -> ErrorObject(
        keyword = violation.keyword,
        message = temporalViolationMessage(props.state.jsonforms.i18n,
            i18nKeyPrefix, displayFormat, TemporalBoundKind.Date, violation),
        params = mapOf("bound" to violation.bound),
    ) }
    val displayValue = committedTemporal?.let { formatTemporal(it, displayFormat) } ?: committedValue
    val editOptions = props.uischema.options - "placeholder"
    var draftValue by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(displayValue) }
    var observedCommittedValue by remember(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { mutableStateOf(committedValue) }
    var showPicker by remember(bindings.stateProps.path, editOptions, displayFormat, saveFormat, dateMode) { mutableStateOf(false) }
    val maskInput = options.booleanOption("mask") != false
    val parsedDraft = draftValue.takeIf(String::isNotEmpty)
        ?.let { parseDateValue(it, listOf(displayFormat)) }
    val localError = when {
        draftValue.isEmpty() -> null
        parsedDraft == null -> invalidDateText
        else -> rangeError(parsedDraft)?.message
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("temporal:date:${bindings.stateProps.path}:${props.id}")
    LaunchedEffect(bindings.stateProps.path, props.schema, editOptions, displayFormat, saveFormat) { setPending(false) }
    LaunchedEffect(committedValue, displayValue, props.state.jsonforms.core.data) {
        if (committedValue != observedCommittedValue) {
            draftValue = displayValue
            observedCommittedValue = committedValue
            setPending(false)
        } else if (draftValue != displayValue && parseDateValue(draftValue, listOf(displayFormat)) == null) {
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
                type = TemporalPickerType.Date,
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
                    val temporal = parseDateValue(value, listOf(displayFormat))
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
            okLabel = okText,
            invalidSelectionMessage = pickerRangeText,
            canConfirm = { !restrict || rangeError(it) == null },
            showActions = options.booleanOption("showActions") == true,
            onSelectionChange = { selected ->
                if (!restrict || rangeError(selected) == null) commit(selected)
            },
            onDismiss = { showPicker = false },
            onConfirm = { selected ->
                if (restrict && rangeError(selected) != null) return@MaterialDateViewsDialog
                commit(selected)
                showPicker = false
            },
        ) else MaterialDatePickerDialog(
            initialDateMillis = initial.date.toPickerMillis(),
            language = props.state.jsonforms.i18n.locale ?: "en",
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
            okLabel = okText,
            invalidSelectionMessage = pickerRangeText,
            showActions = options.booleanOption("showActions") == true,
            onSelectionChange = { millis ->
                val date = pickerMillisToDate(millis)
                val selected = TemporalValue(date.year, date.month.ordinal + 1, date.day)
                if (!restrict || rangeError(selected) == null) commit(selected)
            },
            isSelectableDate = { millis ->
                !restrict || pickerMillisToDate(millis).let { date ->
                    temporalRangeViolation(TemporalValue(date.year, date.month.ordinal + 1, date.day),
                        controlProps.schema, TemporalBoundKind.Date,
                        controller.state.jsonforms.core.data, bindings.stateProps.path) == null
                }
            },
            onDismiss = { showPicker = false },
            onConfirm = { millis ->
                val date = pickerMillisToDate(millis)
                val selected = TemporalValue(date.year, date.month.ordinal + 1, date.day)
                if (restrict && temporalRangeViolation(selected, controlProps.schema,
                        TemporalBoundKind.Date, controller.state.jsonforms.core.data,
                        bindings.stateProps.path) != null) return@MaterialDatePickerDialog
                commit(selected)
                showPicker = false
            },
        )
    }
}

val materialDateControlTester: RankedTester = rankWith(4, isDateControl)
