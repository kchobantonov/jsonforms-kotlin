@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip

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
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    key(initialDateMillis) {
        val state = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = { state.selectedDateMillis?.let(onConfirm) },
                ) { Text(okLabel) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
internal fun MaterialTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    cancelLabel: String,
    okLabel: String,
    titleLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    key(initialHour, initialMinute, is24Hour) {
        val state = rememberTimePickerState(initialHour, initialMinute, is24Hour)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(titleLabel) },
            text = {
                Column(Modifier.padding(top = 8.dp)) {
                    TimePicker(state = state)
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(state.hour, state.minute) }) { Text(okLabel) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        )
    }
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
