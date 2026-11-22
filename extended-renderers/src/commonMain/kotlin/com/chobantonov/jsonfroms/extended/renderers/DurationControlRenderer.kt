package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.composeWithUi
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoInputControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialInputControl
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.default
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val durationControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("duration"), optionIs("format", "duration"))),
)

internal const val MAX_EXACT_DURATION_COMPONENT = 9007199254740991L

internal data class DurationParts(
    val years: Long = 0, val months: Long = 0, val days: Long = 0,
    val hours: Long = 0, val minutes: Long = 0, val seconds: Long = 0,
    val weeks: Long? = null,
) {
    fun components(): List<Long> = listOf(years, months, days, hours, minutes, seconds)
}

private val componentsPattern = Regex("^P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?$")
private val weeksPattern = Regex("^P(\\d+)W$")

internal fun parseDuration(value: String): DurationParts? {
    val weeks = weeksPattern.matchEntire(value)?.groupValues?.get(1)
    if (weeks != null) return weeks.toExactComponent()?.let { DurationParts(weeks = it) }
    val groups = componentsPattern.matchEntire(value)?.groupValues ?: return null
    if (groups.drop(1).all(String::isEmpty) || value.endsWith('T')) return null
    if (groups.drop(1).any { it.isNotEmpty() && it.toExactComponent() == null }) return null
    val values = groups.drop(1).map { it.takeIf(String::isNotEmpty)?.toExactComponent() ?: 0L }
    return DurationParts(values[0], values[1], values[2], values[3], values[4], values[5])
}

private fun String.toExactComponent(): Long? =
    toLongOrNull()?.takeIf { it in 0..MAX_EXACT_DURATION_COMPONENT }

internal fun formatDuration(parts: DurationParts): String {
    parts.weeks?.let { return if (it == 0L) "P0D" else "P${it}W" }
    val date = buildString {
        if (parts.years != 0L) append("${parts.years}Y")
        if (parts.months != 0L) append("${parts.months}M")
        if (parts.days != 0L) append("${parts.days}D")
    }
    val time = buildString {
        if (parts.hours != 0L) append("${parts.hours}H")
        if (parts.minutes != 0L) append("${parts.minutes}M")
        if (parts.seconds != 0L) append("${parts.seconds}S")
    }
    return if (date.isEmpty() && time.isEmpty()) "P0D"
        else "P$date${if (time.isNotEmpty()) "T$time" else ""}"
}

private val componentLabels = listOf(
    "duration.years" to "Years", "duration.months" to "Months",
    "duration.days" to "Days", "duration.hours" to "Hours",
    "duration.minutes" to "Minutes", "duration.seconds" to "Seconds",
)
private val durationBulgarian = JsonObject(mapOf(
    "duration.years" to JsonPrimitive("Години"),
    "duration.months" to JsonPrimitive("Месеци"),
    "duration.days" to JsonPrimitive("Дни"),
    "duration.hours" to JsonPrimitive("Часове"),
    "duration.minutes" to JsonPrimitive("Минути"),
    "duration.seconds" to JsonPrimitive("Секунди"),
    "duration.weeks" to JsonPrimitive("Седмици"),
    "duration.components" to JsonPrimitive("Компоненти"),
    "duration.open" to JsonPrimitive("Избери продължителност"),
    "duration.title" to JsonPrimitive("Продължителност"),
    "duration.ok" to JsonPrimitive("Потвърди"),
    "duration.cancel" to JsonPrimitive("Отказ"),
    "duration.invalid" to JsonPrimitive("Невалидна продължителност."),
    "duration.invalidNumber" to JsonPrimitive("Въведете неотрицателно цяло число."),
))

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun DurationControl(props: ComposeJsonFormsRendererProps, cupertino: Boolean,
    compact: Boolean = false) {
    val control = props.uischema as? ControlElement ?: return
    val path = composeWithUi(control, props.path)
    val current = (com.chobantonov.jsonfroms.core.util.JsonPath.get(
        props.state.jsonforms.core.data, path,
    ) as? JsonPrimitive)?.contentOrNull.orEmpty()
    val parsed = parseDuration(current)
    val i18n = props.state.jsonforms.i18n
    val stateProps = mapStateToControlProps(props.state, OwnPropsOfControl(
        id = props.id, controlUiSchema = control,
        own = OwnPropsOfRenderer(uischema = control, schema = props.schema,
            enabled = props.enabled, path = props.path, renderers = props.renderers,
            cells = props.cells),
    ))
    val invalidStatus = invalidStringStoredValue(stateProps.data, stateProps.dataProvided)
        ?.let { "${translateControlInvalidValue(i18n)}: $it" }
    val inputProps = ControlProps(
        config = stateProps.config, uischema = stateProps.uischema,
        schema = stateProps.schema, data = stateProps.data,
        enabled = stateProps.enabled, readonly = stateProps.readonly,
        visible = stateProps.visible, path = stateProps.path,
        renderers = stateProps.renderers, cells = stateProps.cells,
        errors = stateProps.errors, errorsBeforeTouch = stateProps.errorsBeforeTouch,
        rootSchema = stateProps.rootSchema, id = stateProps.id,
        label = stateProps.label, description = stateProps.description,
        required = stateProps.required, i18nKeyPrefix = stateProps.i18nKeyPrefix,
        handleChange = { changedPath, value ->
            props.dispatch(Actions.update(path = changedPath, updater = { value }))
        },
    )
    fun label(key: String, fallback: String): String = translateRendererText(
        i18n.translate, i18n.locale, key, fallback, mapOf("bg" to durationBulgarian))
    val showActions = (control.options["showActions"] as? JsonPrimitive)?.booleanOrNull != false
    var textDraft by remember(path, props.schema, control) { mutableStateOf(current) }
    var observedCurrent by remember(path, props.schema, control) { mutableStateOf(current) }
    var textDraftDirty by remember(path, props.schema, control) { mutableStateOf(false) }
    val setPending = rememberPendingEditOwner("duration:$path:${props.id}")
    var weeksMode by remember(path, if (!showActions) null else current) { mutableStateOf(parsed?.weeks != null) }
    var fields by remember(path, if (!showActions) null else current) {
        mutableStateOf((parsed ?: DurationParts()).components().map(Long::toString))
    }
    var weekText by remember(path, if (!showActions) null else current) {
        mutableStateOf((parsed?.weeks ?: 0L).toString())
    }
    var dirty by remember(path, if (!showActions) null else current) { mutableStateOf(false) }
    var lastPickerCommit by remember(path) { mutableStateOf<String?>(null) }
    var showPicker by remember(path, if (!showActions) null else current) {
        mutableStateOf(false)
    }

    fun resetPicker() {
        weeksMode = parsed?.weeks != null
        fields = (parsed ?: DurationParts()).components().map(Long::toString)
        weekText = (parsed?.weeks ?: 0L).toString()
        dirty = false
    }
    fun dismissPicker() {
        showPicker = false
        resetPicker()
    }
    LaunchedEffect(stateProps.enabled, stateProps.readonly) {
        if (!stateProps.enabled || stateProps.readonly) dismissPicker()
    }

    LaunchedEffect(current) {
        if (!showActions && showPicker && current != lastPickerCommit) dismissPicker()
        lastPickerCommit = null
        if (current != observedCurrent) {
            textDraft = current
            observedCurrent = current
            textDraftDirty = false
        }
    }

    fun pending(): DurationParts? {
        if (weeksMode) return weekText.toExactComponent()?.let { DurationParts(weeks = it) }
        val values = fields.map { it.toExactComponent() ?: return null }
        return DurationParts(values[0], values[1], values[2], values[3], values[4], values[5])
    }
    fun commit(parts: DurationParts) {
        val encoded = formatDuration(parts)
        lastPickerCommit = encoded
        props.dispatch(Actions.update(path = path, updater = { JsonPrimitive(encoded) }))
    }
    fun pickerEdited(): Boolean = dirty || weeksMode != (parsed?.weeks != null) ||
        fields != (parsed ?: DurationParts()).components().map(Long::toString) ||
        weekText != (parsed?.weeks ?: 0L).toString()
    fun confirmPicker() {
        if (pickerEdited()) commit(pending() ?: return)
        showPicker = false
    }
    fun changed() {
        dirty = true
        if (!showActions) pending()?.let(::commit)
    }
    LaunchedEffect(textDraft, textDraftDirty, dirty, weeksMode, fields, weekText) {
        setPending((textDraftDirty && textDraft.isNotEmpty() && parseDuration(textDraft) == null) ||
            (dirty && pending() == null))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val localError = if (textDraft.isNotEmpty() && parseDuration(textDraft) == null)
            label("duration.invalid", "Invalid duration.") else null
        val onTextChange: (String) -> Unit = { next ->
            textDraft = next
            if (next.isEmpty()) {
                textDraftDirty = false
                observedCurrent = ""
                inputProps.handleChange(path, null)
            } else if (parseDuration(next) != null) {
                textDraftDirty = false
                observedCurrent = next
                inputProps.handleChange(path, JsonPrimitive(next))
            } else {
                textDraftDirty = true
            }
        }
        val openPicker = {
            if (!showPicker) resetPicker()
            showPicker = !showPicker
        }
        if (cupertino) CupertinoInputControl(inputProps, textDraft,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingContent = if (stateProps.enabled && !stateProps.readonly)
                ({ ExtendedPickerTrigger(ExtendedPickerKind.Duration, true,
                    label("duration.open", "Choose duration"), openPicker) })
            else null,
            clearLabel = translateControlClear(i18n),
            onChange = onTextChange)
        else MaterialInputControl(inputProps, textDraft,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingIcon = if (stateProps.enabled && !stateProps.readonly)
                ({ ExtendedPickerTrigger(ExtendedPickerKind.Duration, false,
                    label("duration.open", "Choose duration"), openPicker) })
            else null,
            clearLabel = translateControlClear(i18n),
            onChange = onTextChange)
        @Composable fun pickerFields() {
            @Composable fun numberField(
                value: String, fieldLabel: String, key: String, onValueChange: (String) -> Unit,
            ) {
                if (cupertino) Box(Modifier.width(115.dp)) {
                    CupertinoInputControl(
                        props = inputProps.copy(
                            config = inputProps.config - "focus",
                            uischema = control.copy(options = control.options - "focus" +
                                ("clearable" to JsonPrimitive(false))),
                            path = "$path.__duration_$key", label = fieldLabel,
                            description = null, required = false, errors = "", errorsBeforeTouch = "",
                        ),
                        value = value,
                        keyboardType = KeyboardType.Number,
                        localError = if (value.toExactComponent() == null)
                            label("duration.invalidNumber", "Enter a non-negative integer.") else null,
                        onChange = onValueChange,
                    )
                } else OutlinedTextField(value, onValueChange = onValueChange,
                    label = { Text(fieldLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = value.toExactComponent() == null,
                    modifier = Modifier.width(115.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val componentsClick = {
                    if (weeksMode) { weeksMode = false; changed() }
                }
                val weeksClick = {
                    if (!weeksMode) { weeksMode = true; changed() }
                }
                if (cupertino) {
                    CupertinoButton(onClick = componentsClick,
                        colors = CupertinoButtonDefaults.plainButtonColors()) {
                        CupertinoText(label("duration.components", "Components"))
                    }
                    CupertinoButton(onClick = weeksClick,
                        colors = CupertinoButtonDefaults.plainButtonColors()) {
                        CupertinoText(label("duration.weeks", "Weeks"))
                    }
                } else {
                    OutlinedButton(onClick = componentsClick) {
                        Text(label("duration.components", "Components"))
                    }
                    OutlinedButton(onClick = weeksClick) {
                        Text(label("duration.weeks", "Weeks"))
                    }
                }
            }
            Row(Modifier.widthIn(max = 320.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (weeksMode) {
                    numberField(weekText, label("duration.weeks", "Weeks"), "weeks") {
                        weekText = it; changed()
                    }
                } else componentLabels.forEachIndexed { index, (key, fallback) ->
                    numberField(fields[index], label(key, fallback), key) { next ->
                        fields = fields.toMutableList().also { it[index] = next }
                        changed()
                    }
                }
            }
        }
        if (stateProps.enabled && !stateProps.readonly && !showActions && showPicker) pickerFields()
        if (stateProps.enabled && !stateProps.readonly && showPicker && showActions) {
            val okText = (control.options["okLabel"] as? JsonPrimitive)?.contentOrNull
                ?: label("duration.ok", "OK")
            val cancelText = (control.options["cancelLabel"] as? JsonPrimitive)?.contentOrNull
                ?: label("duration.cancel", "Cancel")
            val title = label("duration.title", "Duration")
            if (cupertino) CupertinoAlertDialog(
                onDismissRequest = ::dismissPicker,
                title = { CupertinoText(title) },
                message = { pickerFields() },
                buttons = {
                    cancel(onClick = ::dismissPicker) { CupertinoText(cancelText) }
                    default(enabled = !pickerEdited() || pending() != null, onClick = ::confirmPicker) {
                        CupertinoText(okText)
                    }
                },
            ) else AlertDialog(
                onDismissRequest = ::dismissPicker,
                title = { Text(title) },
                text = { pickerFields() },
                confirmButton = {
                    TextButton(onClick = ::confirmPicker, enabled = !pickerEdited() || pending() != null) {
                        Text(okText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = ::dismissPicker) { Text(cancelText) }
                },
            )
        }
    }
}
