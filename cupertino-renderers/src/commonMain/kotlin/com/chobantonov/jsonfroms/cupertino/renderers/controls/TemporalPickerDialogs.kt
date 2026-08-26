package com.chobantonov.jsonfroms.cupertino.renderers.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.slapps.cupertino.CupertinoDatePicker
import com.slapps.cupertino.CupertinoDateTimePicker
import com.slapps.cupertino.CupertinoTimePicker
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.rememberCupertinoDatePickerState
import com.slapps.cupertino.rememberCupertinoDateTimePickerState
import com.slapps.cupertino.rememberCupertinoTimePickerState

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoDatePickerDialog(
    initialDateMillis: Long,
    cancelLabel: String,
    okLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initialYear = pickerMillisToDate(initialDateMillis).year
    val state = rememberCupertinoDatePickerState(initialDateMillis, (initialYear - 100)..(initialYear + 100))
    CupertinoPickerDialog("Select date", cancelLabel, okLabel, onDismiss, { onConfirm(state.selectedDateMillis) }) {
        CupertinoDatePicker(state, modifier = Modifier.fillMaxWidth())
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
) {
    val state = rememberCupertinoTimePickerState(initialHour, initialMinute, is24Hour)
    CupertinoPickerDialog(titleLabel, cancelLabel, okLabel, onDismiss, { onConfirm(state.hour, state.minute) }) {
        CupertinoTimePicker(state, modifier = Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CupertinoDateTimePickerDialog(
    initialDateMillis: Long,
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    cancelLabel: String,
    okLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, Int) -> Unit,
) {
    val initialYear = pickerMillisToDate(initialDateMillis).year
    val state = rememberCupertinoDateTimePickerState(
        initialSelectedDateMillis = initialDateMillis,
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
        yearRange = (initialYear - 100)..(initialYear + 100),
    )
    CupertinoPickerDialog("Select date and time", cancelLabel, okLabel, onDismiss, {
        onConfirm(state.selectedDateTimeMillis, state.selectedHour, state.selectedMinute)
    }) {
        CupertinoDateTimePicker(state, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun CupertinoPickerDialog(
    title: String,
    cancelLabel: String,
    okLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(cancelLabel, Modifier.clickable(onClick = onDismiss).padding(10.dp), color = colors.accent)
                Text(okLabel, Modifier.clickable(onClick = onConfirm).padding(10.dp), color = colors.accent)
            }
        }
    }
}
