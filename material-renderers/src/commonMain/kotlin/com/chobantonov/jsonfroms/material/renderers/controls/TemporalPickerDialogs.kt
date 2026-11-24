@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.datetime.LocalDate

/** Compose's desktop month formatter uses the Julian calendar before 1582, while its grid is Gregorian. */
private class ProlepticDatePickerFormatter : DatePickerFormatter {
    private val delegate = DatePickerDefaults.dateFormatter()

    override fun formatMonthYear(monthMillis: Long?, locale: CalendarLocale): String? {
        if (monthMillis == null) return null
        val date = pickerMillisToDate(monthMillis)
        if (date.year > 1582) return delegate.formatMonthYear(monthMillis, locale)
        val reference = LocalDate(2000, date.month.ordinal + 1, 1).toPickerMillis()
        return delegate.formatMonthYear(reference, locale)?.replace("2000", date.year.toString())
    }

    override fun formatDate(dateMillis: Long?, locale: CalendarLocale, forContentDescription: Boolean): String? =
        delegate.formatDate(dateMillis, locale, forContentDescription)
}

@Composable
internal fun TemporalPickerIcon(
    type: TemporalPickerType,
    enabled: Boolean,
    tooltipLabel: String,
    ariaLabel: String = tooltipLabel,
    onClick: () -> Unit,
) {
    ActionTooltip(tooltipLabel) {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector = when (type) {
                    TemporalPickerType.Date -> CalendarIcon
                    TemporalPickerType.Time -> ClockIcon
                    TemporalPickerType.DateTime -> CalendarClockIcon
                },
                contentDescription = ariaLabel,
            )
        }
    }
}

internal enum class TemporalPickerType { Date, Time, DateTime }

@Composable
internal fun MaterialDatePickerDialog(
    initialDateMillis: Long?,
    cancelLabel: String,
    okLabel: String,
    isSelectableDate: (Long) -> Boolean = { true },
    invalidSelectionMessage: String? = null,
    showActions: Boolean = true,
    showDismissAction: Boolean = showActions,
    commitOnSelection: Boolean = !showActions,
    onSelectionChange: (Long) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    key(initialDateMillis) {
        val currentSelectable = rememberUpdatedState(isSelectableDate)
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis,
            initialDisplayedMonthMillis = initialDateMillis,
            yearRange = 1..9999,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    currentSelectable.value(utcTimeMillis)
            },
        )
        val selected = state.selectedDateMillis
        val canConfirm = selected?.let(isSelectableDate) == true
        var observed by remember(initialDateMillis) { mutableStateOf(initialDateMillis) }
        LaunchedEffect(selected, commitOnSelection) {
            if (commitOnSelection && selected != null && selected != observed && isSelectableDate(selected)) {
                onSelectionChange(selected)
            }
            observed = selected
        }
        val showRangeFeedback = !canConfirm && (selected ?: initialDateMillis)?.let {
            !isSelectableDate(it)
        } == true
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                if (showActions) TextButton(
                    modifier = if (!canConfirm && showRangeFeedback && invalidSelectionMessage != null)
                        Modifier.semantics { error(invalidSelectionMessage) } else Modifier,
                    enabled = canConfirm,
                    onClick = { state.selectedDateMillis?.takeIf(isSelectableDate)?.let(onConfirm) },
                ) { Text(okLabel) }
            },
            dismissButton = { if (showDismissAction) TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        ) {
            Column {
                DatePicker(state = state, dateFormatter = remember { ProlepticDatePickerFormatter() })
                if (showRangeFeedback && invalidSelectionMessage != null) {
                    Text(invalidSelectionMessage, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
internal fun MaterialDateViewsDialog(
    initial: TemporalValue,
    mode: DateViewMode,
    titleLabel: String,
    yearLabel: String,
    monthLabel: String,
    previousLabel: String,
    nextLabel: String,
    cancelLabel: String,
    okLabel: String,
    invalidSelectionMessage: String?,
    canConfirm: (TemporalValue) -> Boolean,
    showActions: Boolean = true,
    showDismissAction: Boolean = showActions,
    commitOnSelection: Boolean = !showActions,
    onSelectionChange: (TemporalValue) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (TemporalValue) -> Unit,
) {
    val draft = rememberDateViewsDraft(initial)
    var yearPage by remember(initial) { mutableIntStateOf(yearPageStart(initial.year)) }
    val selectedValue = draft.selected(mode)
    val allowed = selectedValue != null && canConfirm(selectedValue)
    fun changed(fromText: Boolean = false) {
        if (fromText && (draft.year.length != 4 ||
                (mode == DateViewMode.Month && draft.month.length != 2))) return
        if (commitOnSelection) draft.selected(mode)?.takeIf { it != initial && canConfirm(it) }
            ?.let(onSelectionChange)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.year,
                        onValueChange = {
                            draft.changeYear(it)
                            it.toIntOrNull()?.takeIf { year -> year in 1..9999 }
                                ?.let { year -> yearPage = yearPageStart(year) }
                            changed(fromText = true)
                        },
                        modifier = Modifier.width(104.dp).semantics { contentDescription = yearLabel },
                        label = { Text(yearLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    if (mode == DateViewMode.Month) OutlinedTextField(
                        value = draft.month,
                        onValueChange = { draft.changeMonth(it); changed(fromText = true) },
                        modifier = Modifier.width(90.dp).semantics { contentDescription = monthLabel },
                        label = { Text(monthLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = if (mode == DateViewMode.Year) yearPage > 1
                            else (draft.year.toIntOrNull() ?: 0) > 1,
                        modifier = Modifier.semantics { contentDescription =
                            if (mode == DateViewMode.Year) previousLabel else "$previousLabel $yearLabel" },
                        onClick = {
                            if (mode == DateViewMode.Year) yearPage -= 12 else {
                                draft.shiftYear(-1)
                                changed()
                            }
                        },
                    ) { Text("‹") }
                    Text(if (mode == DateViewMode.Year)
                        "${yearPage}–${yearsOnPage(yearPage).last()}" else draft.year)
                    TextButton(
                        enabled = if (mode == DateViewMode.Year) yearPage < yearPageStart(9999)
                            else (draft.year.toIntOrNull() ?: 10000) < 9999,
                        modifier = Modifier.semantics { contentDescription =
                            if (mode == DateViewMode.Year) nextLabel else "$nextLabel $yearLabel" },
                        onClick = {
                            if (mode == DateViewMode.Year) yearPage += 12 else {
                                draft.shiftYear(1)
                                changed()
                            }
                        },
                    ) { Text("›") }
                }
                val choices = if (mode == DateViewMode.Year) yearsOnPage(yearPage) else (1..12).toList()
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        choices.drop(row * 3).take(3).forEach { number ->
                            val chosen = if (mode == DateViewMode.Year) draft.year.toIntOrNull() == number
                                else draft.month.toIntOrNull() == number
                            val choose = {
                                if (mode == DateViewMode.Year) draft.changeYear(number.toString())
                                else draft.changeMonth(number.toString().padStart(2, '0'))
                                changed()
                            }
                            val label = if (mode == DateViewMode.Year) "$yearLabel $number"
                                else "$monthLabel ${number.toString().padStart(2, '0')}"
                            if (chosen) Button(onClick = choose,
                                modifier = Modifier.width(74.dp).semantics {
                                    contentDescription = label
                                    selected = true
                                }) {
                                Text(number.toString().padStart(if (mode == DateViewMode.Year) 4 else 2, '0'))
                            } else TextButton(onClick = choose,
                                modifier = Modifier.width(74.dp).semantics {
                                    contentDescription = label
                                    selected = false
                                }) {
                                Text(number.toString().padStart(if (mode == DateViewMode.Year) 4 else 2, '0'))
                            }
                        }
                    }
                }
                if (!allowed && invalidSelectionMessage != null) Text(invalidSelectionMessage)
            }
        },
        confirmButton = { if (showActions) Button(enabled = allowed,
            modifier = if (!allowed && invalidSelectionMessage != null)
                Modifier.semantics { error(invalidSelectionMessage) } else Modifier,
            onClick = { selectedValue?.takeIf(canConfirm)?.let(onConfirm) }) { Text(okLabel) } },
        dismissButton = { if (showDismissAction) TextButton(onClick = onDismiss) { Text(cancelLabel) } },
    )
}

@Composable
internal fun MaterialTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    cancelLabel: String,
    okLabel: String,
    titleLabel: String,
    canConfirm: (Int, Int) -> Boolean = { _, _ -> true },
    invalidSelectionMessage: String? = null,
    showActions: Boolean = true,
    onSelectionChange: (Int, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    key(initialHour, initialMinute, is24Hour) {
        val state = rememberTimePickerState(initialHour, initialMinute, is24Hour)
        var observed by remember(initialHour, initialMinute, is24Hour) {
            mutableStateOf(initialHour to initialMinute)
        }
        LaunchedEffect(state.hour, state.minute, showActions) {
            val selected = state.hour to state.minute
            if (!showActions && selected != observed && canConfirm(selected.first, selected.second)) {
                onSelectionChange(selected.first, selected.second)
            }
            observed = selected
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(titleLabel) },
            text = {
                Column(Modifier.padding(top = 8.dp)) {
                    TimePicker(state = state)
                    if (!canConfirm(state.hour, state.minute) && invalidSelectionMessage != null) {
                        Text(invalidSelectionMessage, Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                if (showActions) Button(enabled = canConfirm(state.hour, state.minute),
                    modifier = if (!canConfirm(state.hour, state.minute) && invalidSelectionMessage != null)
                        Modifier.semantics { error(invalidSelectionMessage) } else Modifier,
                    onClick = { if (canConfirm(state.hour, state.minute)) onConfirm(state.hour, state.minute) }) {
                    Text(okLabel)
                }
            },
            dismissButton = { if (showActions) TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        )
    }
}

@Composable
internal fun MaterialTimeViewsDialog(
    initial: TemporalValue,
    views: List<TimeView>,
    is24Hour: Boolean,
    cancelLabel: String,
    okLabel: String,
    titleLabel: String,
    viewLabel: (TimeView) -> String,
    invalidSelectionMessage: String?,
    canConfirm: (TemporalValue) -> Boolean,
    showActions: Boolean = true,
    onSelectionChange: (TemporalValue) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (TemporalValue) -> Unit,
) {
    val draft = rememberTimeViewsDraft(initial, is24Hour)
    val selected = draft.selected(views)
    val allowed = selected != null && canConfirm(selected)
    fun changed(fromText: Boolean = false) {
        if (fromText && views.any { draft.value(it).length != 2 }) return
        if (!showActions) draft.selected(views)?.takeIf { it != initial && canConfirm(it) }
            ?.let(onSelectionChange)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    views.forEach { view ->
                        val label = viewLabel(view)
                        OutlinedTextField(
                            value = draft.value(view),
                            onValueChange = { draft.change(view, it); changed(fromText = true) },
                            modifier = Modifier.width(86.dp).semantics { contentDescription = label },
                            label = { Text(label) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                if (!is24Hour && TimeView.Hours in views) {
                    TextButton(onClick = { draft.isPm = !draft.isPm; changed() }) {
                        Text(if (draft.isPm) "PM" else "AM")
                    }
                }
                if (!allowed && invalidSelectionMessage != null) Text(invalidSelectionMessage)
            }
        },
        confirmButton = { if (showActions) Button(enabled = allowed,
            modifier = if (!allowed && invalidSelectionMessage != null)
                Modifier.semantics { error(invalidSelectionMessage) } else Modifier,
            onClick = { selected?.takeIf(canConfirm)?.let(onConfirm) }) { Text(okLabel) } },
        dismissButton = { if (showActions) TextButton(onClick = onDismiss) { Text(cancelLabel) } },
    )
}

private val CalendarIcon = lineIcon("Calendar") {
    moveTo(6f, 2f); verticalLineTo(6f); moveTo(18f, 2f); verticalLineTo(6f)
    moveTo(3f, 9f); horizontalLineTo(21f)
    moveTo(5f, 4f); horizontalLineTo(19f); curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
    verticalLineTo(20f); curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
    horizontalLineTo(5f); curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f); verticalLineTo(6f)
    curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
}

private val ClockIcon = lineIcon("Clock") {
    moveTo(12f, 22f); curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
    curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f); curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
    curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
    moveTo(12f, 6f); verticalLineTo(12f); lineTo(16f, 14f)
}

private val CalendarClockIcon = lineIcon("Calendar and clock") {
    moveTo(6f, 2f); verticalLineTo(6f); moveTo(18f, 2f); verticalLineTo(6f)
    moveTo(3f, 9f); horizontalLineTo(15f)
    moveTo(5f, 4f); horizontalLineTo(19f); curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f); verticalLineTo(10f)
    moveTo(12f, 17f); curveTo(12f, 14.24f, 14.24f, 12f, 17f, 12f)
    curveTo(19.76f, 12f, 22f, 14.24f, 22f, 17f); curveTo(22f, 19.76f, 19.76f, 22f, 17f, 22f)
    curveTo(14.24f, 22f, 12f, 19.76f, 12f, 17f); moveTo(17f, 14.5f); verticalLineTo(17f); lineTo(19f, 18f)
    moveTo(12f, 22f); horizontalLineTo(5f); curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f); verticalLineTo(6f)
    curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
}

private fun lineIcon(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        )
    }.build()
