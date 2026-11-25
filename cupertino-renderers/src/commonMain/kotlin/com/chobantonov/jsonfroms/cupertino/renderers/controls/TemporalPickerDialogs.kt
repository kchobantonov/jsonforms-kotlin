package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.TimeView
import com.chobantonov.jsonfroms.material.renderers.controls.DateViewMode
import com.chobantonov.jsonfroms.material.renderers.controls.rememberDateViewsDraft
import com.chobantonov.jsonfroms.material.renderers.controls.yearPageStart
import com.chobantonov.jsonfroms.material.renderers.controls.yearsOnPage
import com.chobantonov.jsonfroms.material.renderers.controls.rememberTimeViewsDraft
import com.slapps.cupertino.CupertinoDatePicker
import com.slapps.cupertino.CupertinoTimePicker
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.rememberCupertinoDatePickerState
import com.slapps.cupertino.rememberCupertinoTimePickerState

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoDatePickerDialog(
    initialDateMillis: Long,
    titleLabel: String,
    cancelLabel: String,
    okLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    isSelectableDate: (Long) -> Boolean = { true },
    invalidSelectionMessage: String? = null,
    showActions: Boolean = true,
    showDismissAction: Boolean = showActions,
    commitOnSelection: Boolean = !showActions,
    onSelectionChange: (Long) -> Unit = {},
) {
    val initialYear = pickerMillisToDate(initialDateMillis).year
    val yearRange = (initialYear - 100).coerceAtLeast(1)..(initialYear + 100).coerceAtMost(9999)
    val state = rememberCupertinoDatePickerState(initialDateMillis, yearRange)
    val canConfirm = isSelectableDate(state.selectedDateMillis)
    var observed by remember(initialDateMillis) { mutableStateOf(initialDateMillis) }
    LaunchedEffect(state.selectedDateMillis, commitOnSelection) {
        val selected = state.selectedDateMillis
        if (commitOnSelection && selected != observed && isSelectableDate(selected)) onSelectionChange(selected)
        observed = selected
    }
    CupertinoPickerDialog(titleLabel, cancelLabel, okLabel, onDismiss,
        { if (canConfirm) onConfirm(state.selectedDateMillis) }, canConfirm = canConfirm,
        invalidSelectionMessage = invalidSelectionMessage, showActions = showActions,
        showDismissAction = showDismissAction) {
        CupertinoDatePicker(state, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun CupertinoDateViewsDialog(
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
    val colors = LocalCupertinoColors.current
    fun changed(fromText: Boolean = false) {
        if (fromText && (draft.year.length != 4 ||
                (mode == DateViewMode.Month && draft.month.length != 2))) return
        if (commitOnSelection) draft.selected(mode)?.takeIf { it != initial && canConfirm(it) }
            ?.let(onSelectionChange)
    }
    CupertinoPickerDialog(titleLabel, cancelLabel, okLabel, onDismiss,
        { selectedValue?.takeIf(canConfirm)?.let(onConfirm) }, allowed, invalidSelectionMessage,
        showActions = showActions, showDismissAction = showDismissAction) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(104.dp)) {
                    Text(yearLabel, color = colors.secondaryLabel)
                    BasicTextField(
                        value = draft.year,
                        onValueChange = {
                            draft.changeYear(it)
                            it.toIntOrNull()?.takeIf { year -> year in 1..9999 }
                                ?.let { year -> yearPage = yearPageStart(year) }
                            changed(fromText = true)
                        },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = yearLabel }
                            .border(1.dp, colors.controlBorder, RoundedCornerShape(8.dp)).padding(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
                    )
                }
                if (mode == DateViewMode.Month) Column(Modifier.width(76.dp)) {
                    Text(monthLabel, color = colors.secondaryLabel)
                    BasicTextField(
                        value = draft.month,
                        onValueChange = { draft.changeMonth(it); changed(fromText = true) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = monthLabel }
                            .border(1.dp, colors.controlBorder, RoundedCornerShape(8.dp)).padding(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("‹", Modifier.clickable(enabled = if (mode == DateViewMode.Year) yearPage > 1
                    else (draft.year.toIntOrNull() ?: 0) > 1) {
                    if (mode == DateViewMode.Year) yearPage -= 12 else {
                        draft.shiftYear(-1)
                        changed()
                    }
                }.semantics { contentDescription = if (mode == DateViewMode.Year) previousLabel
                    else "$previousLabel $yearLabel" }.padding(8.dp),
                    color = colors.accent)
                Text(if (mode == DateViewMode.Year)
                    "${yearPage}–${yearsOnPage(yearPage).last()}" else draft.year,
                    color = colors.label)
                Text("›", Modifier.clickable(enabled = if (mode == DateViewMode.Year)
                    yearPage < yearPageStart(9999) else (draft.year.toIntOrNull() ?: 10000) < 9999) {
                    if (mode == DateViewMode.Year) yearPage += 12 else {
                        draft.shiftYear(1)
                        changed()
                    }
                }.semantics { contentDescription = if (mode == DateViewMode.Year) nextLabel
                    else "$nextLabel $yearLabel" }.padding(8.dp),
                    color = colors.accent)
            }
            val choices = if (mode == DateViewMode.Year) yearsOnPage(yearPage) else (1..12).toList()
            repeat(4) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    choices.drop(row * 3).take(3).forEach { number ->
                        val chosen = if (mode == DateViewMode.Year) draft.year.toIntOrNull() == number
                            else draft.month.toIntOrNull() == number
                        val label = if (mode == DateViewMode.Year) "$yearLabel $number"
                            else "$monthLabel ${number.toString().padStart(2, '0')}"
                        Text(number.toString().padStart(if (mode == DateViewMode.Year) 4 else 2, '0'),
                            Modifier.width(74.dp)
                                .background(if (chosen) colors.accent.copy(alpha = 0.16f)
                                    else colors.secondaryBackground, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (mode == DateViewMode.Year) draft.changeYear(number.toString())
                                    else draft.changeMonth(number.toString().padStart(2, '0'))
                                    changed()
                                }
                                .semantics {
                                    contentDescription = label
                                    selected = chosen
                                }
                                .padding(8.dp),
                            color = colors.label)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    cancelLabel: String,
    okLabel: String,
    titleLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    canConfirm: (Int, Int) -> Boolean = { _, _ -> true },
    invalidSelectionMessage: String? = null,
    showActions: Boolean = true,
    onSelectionChange: (Int, Int) -> Unit = { _, _ -> },
) {
    val state = rememberCupertinoTimePickerState(initialHour, initialMinute, is24Hour)
    val valid = canConfirm(state.hour, state.minute)
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
    CupertinoPickerDialog(titleLabel, cancelLabel, okLabel, onDismiss,
        { if (valid) onConfirm(state.hour, state.minute) }, canConfirm = valid,
        invalidSelectionMessage = invalidSelectionMessage, showActions = showActions) {
        CupertinoTimePicker(state, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
internal fun CupertinoTimeViewsDialog(
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
    val colors = LocalCupertinoColors.current
    fun changed(fromText: Boolean = false) {
        if (fromText && views.any { draft.value(it).length != 2 }) return
        if (!showActions) draft.selected(views)?.takeIf { it != initial && canConfirm(it) }
            ?.let(onSelectionChange)
    }
    CupertinoPickerDialog(titleLabel, cancelLabel, okLabel, onDismiss,
        { selected?.takeIf(canConfirm)?.let(onConfirm) }, allowed, invalidSelectionMessage,
        showActions = showActions) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.forEach { view ->
                    val label = viewLabel(view)
                    Column(Modifier.width(76.dp)) {
                        Text(label, color = colors.secondaryLabel)
                        BasicTextField(
                            value = draft.value(view),
                            onValueChange = { draft.change(view, it); changed(fromText = true) },
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
                                .border(1.dp, colors.controlBorder, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
                        )
                    }
                }
            }
            if (!is24Hour && TimeView.Hours in views) {
                Text(if (draft.isPm) "PM" else "AM",
                    Modifier.clickable { draft.isPm = !draft.isPm; changed() }.padding(8.dp),
                    color = colors.accent)
            }
        }
    }
}

@Composable
private fun CupertinoPickerDialog(
    title: String,
    cancelLabel: String,
    okLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    canConfirm: Boolean = true,
    invalidSelectionMessage: String? = null,
    showActions: Boolean = true,
    showDismissAction: Boolean = showActions,
    content: @Composable () -> Unit,
) {
    val colors = LocalCupertinoColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Column(
            Modifier.background(colors.secondaryBackground, RoundedCornerShape(14.dp)).padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleMedium,
                color = colors.label,
            )
            content()
            if (!canConfirm && invalidSelectionMessage != null) {
                Text(invalidSelectionMessage, Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    color = colors.label)
            }
            if (showActions) Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (showDismissAction) Text(cancelLabel,
                    Modifier.clickable(onClick = onDismiss).padding(10.dp), color = colors.accent)
                Text(okLabel, Modifier
                    .then(if (!canConfirm && invalidSelectionMessage != null)
                        Modifier.semantics { error(invalidSelectionMessage) } else Modifier)
                    .clickable(enabled = canConfirm, onClick = onConfirm).padding(10.dp),
                    color = colors.accent.copy(alpha = if (canConfirm) 1f else 0.4f))
            }
        }
    }
}
