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
import com.chobantonov.jsonfroms.core.testers.isDateControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_DISPLAY_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.booleanOption
import com.chobantonov.jsonfroms.material.renderers.controls.currentTemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import com.chobantonov.jsonfroms.material.renderers.controls.isAllowedTemporalEdit
import com.chobantonov.jsonfroms.material.renderers.controls.parseDateValue
import com.chobantonov.jsonfroms.material.renderers.controls.pickerMillisToDate
import com.chobantonov.jsonfroms.material.renderers.controls.stringOption
import com.chobantonov.jsonfroms.material.renderers.controls.toPickerMillis
import kotlinx.serialization.json.JsonPrimitive

val CupertinoDateControl = ComposeJsonFormsRenderer { CupertinoDateControl(it) }

@Composable
fun CupertinoDateControl(props: ComposeJsonFormsRendererProps) {
    val bindings = cupertinoControlBindings(props, "CupertinoDateControl")
    val control = bindings.controlProps
    val options = control.config + control.uischema.options
    val displayFormat = options.stringOption("dateFormat") ?: DEFAULT_DATE_DISPLAY_FORMAT
    val saveFormat = options.stringOption("dateSaveFormat") ?: DEFAULT_DATE_SAVE_FORMAT
    val committed = bindings.stateProps.data.toCupertinoInputString()
    val parsed = parseDateValue(committed, listOf(saveFormat, DEFAULT_DATE_SAVE_FORMAT))
    val displayed = parsed?.let { formatTemporal(it, displayFormat) } ?: committed
    var draft by remember(bindings.stateProps.path) { mutableStateOf(displayed) }
    var observed by remember(bindings.stateProps.path) { mutableStateOf(committed) }
    var showPicker by remember { mutableStateOf(false) }
    val localError = draft.takeIf { it.isNotEmpty() && parseDateValue(it, listOf(displayFormat)) == null }
        ?.let { "Invalid date. Expected $displayFormat." }
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
            parseDateValue(value, listOf(displayFormat))?.let(::commit) ?: run {
                observed = value
                bindings.dispatchProps.handleChange(bindings.stateProps.path, JsonPrimitive(value))
            }
        }
    }

    if (showPicker) {
        val initial = parsed?.date ?: currentTemporalValue().date
        CupertinoDatePickerDialog(
            initial.toPickerMillis(),
            options.stringOption("cancelLabel") ?: "Cancel",
            options.stringOption("okLabel") ?: "OK",
            { showPicker = false },
        ) { millis ->
            val date = pickerMillisToDate(millis)
            commit(TemporalValue(date.year, date.monthNumber, date.day))
            showPicker = false
        }
    }
}

val cupertinoDateControlTester: RankedTester = withIncreasedRank(100, rankWith(4, isDateControl))
