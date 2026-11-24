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
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_DISPLAY_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalBoundKind
import com.chobantonov.jsonfroms.material.renderers.controls.DateViewMode
import com.chobantonov.jsonfroms.material.renderers.controls.booleanOption
import com.chobantonov.jsonfroms.material.renderers.controls.currentTemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import com.chobantonov.jsonfroms.material.renderers.controls.temporalViolationMessage
import com.chobantonov.jsonfroms.material.renderers.controls.isAllowedTemporalEdit
import com.chobantonov.jsonfroms.material.renderers.controls.parseDateValue
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.chobantonov.jsonfroms.material.renderers.controls.resolveDateViewMode
import com.chobantonov.jsonfroms.material.renderers.controls.stringOption
import com.chobantonov.jsonfroms.material.renderers.controls.temporalRangeViolation
import com.chobantonov.jsonfroms.material.renderers.controls.temporalSaveFormatForControl
import com.chobantonov.jsonfroms.material.renderers.controls.toPickerMillis
import kotlinx.serialization.json.JsonPrimitive

val CupertinoDateControl = ComposeJsonFormsRenderer { CupertinoDateControl(it) }

@Composable
internal fun CupertinoDateCell(props: ComposeJsonFormsCellProps) =
    CupertinoDateControl(props.asRendererProps(), compact = true)

@Composable
fun CupertinoDateControl(props: ComposeJsonFormsRendererProps, compact: Boolean = false) {
    val bindings = cupertinoControlBindings(props, "CupertinoDateControl")
    val controller = useJsonForms().controller
    val control = bindings.controlProps
    if (!control.visible) return
    val options = control.config + control.uischema.options
    val displayFormat = options.stringOption("dateFormat") ?: DEFAULT_DATE_DISPLAY_FORMAT
    val saveFormat = temporalSaveFormatForControl(control.schema.format,
        options.stringOption("dateSaveFormat"), DEFAULT_DATE_SAVE_FORMAT,
        bindings.stateProps.path, props.config, props.state.jsonforms.i18n,
        bindings.stateProps.i18nKeyPrefix, displayFormat)
    val dateMode = resolveDateViewMode(options["views"], displayFormat)
    val prefix = bindings.stateProps.i18nKeyPrefix
    val values = mapOf<String, Any?>("label" to bindings.stateProps.label, "format" to displayFormat)
    fun text(key: String, fallback: String) = translateTemporalControlText(props.state.jsonforms.i18n,
        prefix, key, fallback, displayFormat, values)
    val pickerLabel = text("chooseDateTooltip", "Pick")
    val pickerAriaLabel = text("chooseDateAriaLabel", "Choose date")
    val invalidDateText = text("invalidDate", "Invalid date. Expected $displayFormat.")
    val pickerRangeText = text("date.outOfRange", "Select a date within the allowed range.")
    val titleText = text("selectDate", "Select date")
    val cancelText = options.stringOption("cancelLabel") ?: text("cancel", "Cancel")
    val okText = options.stringOption("okLabel") ?: text("ok", "OK")
    val committed = bindings.stateProps.data.toCupertinoInputString()
    val invalidStatus = invalidStringStoredValue(bindings.stateProps.data, bindings.stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(props.state.jsonforms.i18n)}: $it" }
    val parsed = parseDateValue(committed, listOf(saveFormat, DEFAULT_DATE_SAVE_FORMAT))
    val restrict = resolveRestrict(control.uischema.options["restrict"], control.config)
    fun rangeError(value: TemporalValue): ErrorObject? = temporalRangeViolation(value,
        control.schema, TemporalBoundKind.Date, controller.state.jsonforms.core.data,
        bindings.stateProps.path)?.let { violation -> ErrorObject(
        keyword = violation.keyword,
        message = temporalViolationMessage(props.state.jsonforms.i18n,
            prefix, displayFormat, TemporalBoundKind.Date, violation),
        params = mapOf("bound" to violation.bound),
    ) }
    val displayed = parsed?.let { formatTemporal(it, displayFormat) } ?: committed
    var draft by remember(bindings.stateProps.path, props.schema, props.uischema, displayFormat, saveFormat) { mutableStateOf(displayed) }
    var observed by remember(bindings.stateProps.path, props.schema, props.uischema, displayFormat, saveFormat) { mutableStateOf(committed) }
    var showPicker by remember(bindings.stateProps.path, props.uischema, displayFormat, saveFormat, dateMode) { mutableStateOf(false) }
    val parsedDraft = draft.takeIf(String::isNotEmpty)?.let { parseDateValue(it, listOf(displayFormat)) }
    val localError = when {
        draft.isEmpty() -> null
        parsedDraft == null -> invalidDateText
        else -> rangeError(parsedDraft)?.message
    }
    val preserveEmptyValue = LocalPreserveEmptyValue.current
    val setPending = rememberPendingEditOwner("temporal:date:${bindings.stateProps.path}:${props.id}")
    LaunchedEffect(props.uischema, displayFormat, saveFormat) { setPending(false) }

    LaunchedEffect(committed, displayed, props.state.jsonforms.core.data) {
        if (committed != observed) {
            draft = displayed
            observed = committed
            setPending(false)
        } else if (draft != displayed && parseDateValue(draft, listOf(displayFormat)) == null) {
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
            parseDateValue(value, listOf(displayFormat))?.let(::commit) ?: run {
                setPending(true)
            }
        }
    }

    if (showPicker) {
        val initial = parsed ?: currentTemporalValue()
        if (dateMode != DateViewMode.Day) CupertinoDateViewsDialog(
            initial = initial,
            mode = dateMode,
            titleLabel = titleText,
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
                if (restrict && rangeError(selected) != null) return@CupertinoDateViewsDialog
                commit(selected)
                showPicker = false
            },
        ) else CupertinoDatePickerDialog(
            initialDateMillis = initial.date.toPickerMillis(),
            titleLabel = titleText,
            cancelLabel = cancelText,
            okLabel = okText,
            invalidSelectionMessage = pickerRangeText,
            showActions = options.booleanOption("showActions") == true,
            onSelectionChange = { millis ->
                val date = pickerMillisToDate(millis)
                val selected = TemporalValue(date.year, date.month.ordinal + 1, date.day)
                if (!restrict || rangeError(selected) == null) commit(selected)
            },
            onDismiss = { showPicker = false },
            onConfirm = { millis ->
                val date = pickerMillisToDate(millis)
                val selected = TemporalValue(date.year, date.month.ordinal + 1, date.day)
                if (restrict && rangeError(selected) != null) return@CupertinoDatePickerDialog
                commit(selected)
                showPicker = false
            },
            isSelectableDate = { millis ->
                !restrict || pickerMillisToDate(millis).let { date ->
                    rangeError(TemporalValue(date.year, date.month.ordinal + 1, date.day)) == null
                }
            },
        )
    }
}

val cupertinoDateControlTester: RankedTester = withIncreasedRank(100, rankWith(4, isDateControl))
