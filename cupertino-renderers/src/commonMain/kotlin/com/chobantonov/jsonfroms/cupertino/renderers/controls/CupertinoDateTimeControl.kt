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
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isDateTimeControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_DISPLAY_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.booleanOption
import com.chobantonov.jsonfroms.material.renderers.controls.currentTemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import com.chobantonov.jsonfroms.material.renderers.controls.isAllowedTemporalEdit
import com.chobantonov.jsonfroms.material.renderers.controls.parseDateTimeValue
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.chobantonov.jsonfroms.material.renderers.controls.stringOption
import com.chobantonov.jsonfroms.material.renderers.controls.toPickerMillis
import kotlinx.serialization.json.JsonPrimitive

val CupertinoDateTimeControl = ComposeJsonFormsRenderer { CupertinoDateTimeControl(it) }

@Composable
fun CupertinoDateTimeControl(props: ComposeJsonFormsRendererProps) {
    val bindings = cupertinoControlBindings(props, "CupertinoDateTimeControl")
    val control = bindings.controlProps
    val options = control.config + control.uischema.options
    val displayFormat = options.stringOption("dateTimeFormat") ?: DEFAULT_DATE_TIME_DISPLAY_FORMAT
    val saveFormat = options.stringOption("dateTimeSaveFormat") ?: DEFAULT_DATE_TIME_SAVE_FORMAT
    val committed = bindings.stateProps.data.toCupertinoInputString()
    val parsed = parseDateTimeValue(
        committed,
        listOf(saveFormat, DEFAULT_DATE_TIME_SAVE_FORMAT, "YYYY-MM-DDTHH:mm:ss.SSSZ", "YYYY-MM-DDTHH:mm:ss"),
    )
    val displayed = parsed?.let { formatTemporal(it, displayFormat) } ?: committed
    var draft by remember(bindings.stateProps.path) { mutableStateOf(displayed) }
    var observed by remember(bindings.stateProps.path) { mutableStateOf(committed) }
    var showPicker by remember { mutableStateOf(false) }
    val localError = draft.takeIf { it.isNotEmpty() && parseDateTimeValue(it, listOf(displayFormat)) == null }
        ?.let { "Invalid date and time. Expected $displayFormat." }
    val preserveEmptyValue = LocalPreserveEmptyValue.current

    LaunchedEffect(committed, displayed) {
        if (committed != observed) {
            draft = displayed
            observed = committed
        }
    }

    fun commit(value: TemporalValue) {
        val saved = formatTemporal(value, saveFormat)
        observed = saved
        draft = formatTemporal(value, displayFormat)
        bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(saved))
    }

    val colors = LocalCupertinoColors.current
    CupertinoInputControl(
        props = control,
        value = draft,
        localError = localError,
        clearLabel = bindings.clearLabel,
        trailingContent = {
            Text(
                "Pick",
                Modifier.clickable(control.enabled && !control.readonly) { showPicker = true }.padding(start = 10.dp),
                color = colors.accent,
            )
        },
    ) { value ->
        if (!isAllowedTemporalEdit(draft, value, displayFormat, options.booleanOption("mask") != false)) return@CupertinoInputControl
        draft = value
        if (value.isEmpty()) {
            observed = ""
            bindings.dispatchProps.handleChange(
                bindings.stateProps.path,
                JsonPrimitive("").takeIf { preserveEmptyValue },
            )
        } else {
            parseDateTimeValue(value, listOf(displayFormat))?.let(::commit) ?: run {
                observed = value
                bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
            }
        }
    }

    if (showPicker) {
        val initial = parsed ?: currentTemporalValue()
        CupertinoDateTimePickerDialog(
            initial.date.toPickerMillis(),
            initial.hour,
            initial.minute,
            options.booleanOption("ampm") == false,
            options.stringOption("cancelLabel") ?: "Cancel",
            options.stringOption("okLabel") ?: "OK",
            { showPicker = false },
        ) { millis, hour, minute ->
            val date = pickerMillisToDate(millis)
            commit(initial.copy(year = date.year, month = date.month.ordinal + 1, day = date.day, hour = hour, minute = minute))
            showPicker = false
        }
    }
}

val cupertinoDateTimeControlTester: RankedTester = withIncreasedRank(100, rankWith(2, isDateTimeControl))
