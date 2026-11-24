package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.strictControlBooleanOption

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
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
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip as CupertinoActionTooltip
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialInputControl
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip as MaterialActionTooltip
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
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
private const val DEFAULT_DURATION_FIELD = 3 // Hours

internal fun shownDurationFields(parts: DurationParts?): List<Int> {
    val values = (parts ?: DurationParts()).components()
    val populated = values.indices.filter { values[it] > 0L }
    return populated.ifEmpty { listOf(DEFAULT_DURATION_FIELD) }
}

private val durationBulgarian = JsonObject(mapOf(
    "duration.years" to JsonPrimitive("Години"),
    "duration.months" to JsonPrimitive("Месеци"),
    "duration.days" to JsonPrimitive("Дни"),
    "duration.hours" to JsonPrimitive("Часове"),
    "duration.minutes" to JsonPrimitive("Минути"),
    "duration.seconds" to JsonPrimitive("Секунди"),
    "duration.weeks" to JsonPrimitive("Седмици"),
    "duration.modeComponents" to JsonPrimitive("От години до секунди"),
    "duration.modeWeeks" to JsonPrimitive("Седмици"),
    "duration.addUnit" to JsonPrimitive("Добавяне на единица"),
    "duration.removeUnit" to JsonPrimitive("Премахване на „{unit}“"),
    "duration.open" to JsonPrimitive("Избери продължителност"),
    "duration.title" to JsonPrimitive("Продължителност"),
    "duration.ok" to JsonPrimitive("Прилагане"),
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
    fun label(key: String, fallback: String, values: Map<String, Any?> = emptyMap()): String =
        translateRendererText(i18n.translate, i18n.locale, key, fallback,
            mapOf("bg" to durationBulgarian), values)
    val showActions = (strictControlBooleanOption(control.options, "showActions")
        ?: strictControlBooleanOption(props.config, "showActions")) != false
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
    var editedInput by remember(path, if (!showActions) null else current) { mutableStateOf(false) }
    var shownFields by remember(path, if (!showActions) null else current) {
        mutableStateOf(shownDurationFields(parsed))
    }
    var addMenuOpen by remember(path) { mutableStateOf(false) }
    var lastPickerCommit by remember(path) { mutableStateOf<String?>(null) }
    var showPicker by remember(path, if (!showActions) null else current) {
        mutableStateOf(false)
    }

    fun resetPicker() {
        weeksMode = parsed?.weeks != null
        fields = (parsed ?: DurationParts()).components().map(Long::toString)
        weekText = (parsed?.weeks ?: 0L).toString()
        dirty = false
        editedInput = false
        shownFields = shownDurationFields(parsed)
        addMenuOpen = false
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
    fun equivalent(parts: DurationParts): Boolean = parsed != null &&
        parsed.components() == parts.components() && (parsed.weeks ?: 0L) == (parts.weeks ?: 0L)
    fun commit(parts: DurationParts) {
        val encoded = formatDuration(parts)
        lastPickerCommit = encoded
        props.dispatch(Actions.update(path = path, updater = { JsonPrimitive(encoded) }))
    }
    fun pickerEdited(): Boolean = dirty || weeksMode != (parsed?.weeks != null) ||
        fields != (parsed ?: DurationParts()).components().map(Long::toString) ||
        weekText != (parsed?.weeks ?: 0L).toString()
    fun confirmPicker() {
        if (pickerEdited()) {
            val selected = pending() ?: return
            if (editedInput || !equivalent(selected)) commit(selected)
        }
        showPicker = false
    }
    fun changed(inputEdit: Boolean = false) {
        dirty = true
        if (inputEdit) editedInput = true
        if (!showActions) pending()?.let { if (editedInput || !equivalent(it)) commit(it) }
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
        BoxWithConstraints {
        val popupWidth = minOf(320.dp, maxWidth)
        // Reserve panel padding, the remove action, row gaps and a readable unit label.
        val quantityWidth = minOf(120.dp, maxOf(72.dp, popupWidth - 152.dp))
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
                if (cupertino) Column(Modifier.width(quantityWidth)) {
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
                        compact = true,
                        onChange = onValueChange,
                    )
                    if (value.toExactComponent() == null)
                        CupertinoText(label("duration.invalidNumber", "Enter a non-negative integer."))
                } else OutlinedTextField(value, onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = value.toExactComponent() == null,
                    modifier = Modifier.width(quantityWidth).semantics { contentDescription = fieldLabel })
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    @Composable fun modeButton(weeks: Boolean, text: String) {
                        val active = weeksMode == weeks
                        val select = {
                            if (!active) {
                                weeksMode = weeks
                                fields = List(componentLabels.size) { "0" }
                                weekText = "0"
                                shownFields = listOf(DEFAULT_DURATION_FIELD)
                                editedInput = false
                                changed()
                            }
                        }
                        if (cupertino) CupertinoButton(onClick = select,
                            colors = if (active) CupertinoButtonDefaults.filledButtonColors()
                                else CupertinoButtonDefaults.plainButtonColors(),
                            modifier = Modifier.semantics { selected = active }) {
                            CupertinoText(text)
                        } else if (active) Button(onClick = select,
                            modifier = Modifier.semantics { selected = true }) { Text(text) }
                        else OutlinedButton(onClick = select,
                            modifier = Modifier.semantics { selected = false }) { Text(text) }
                    }
                    modeButton(false, label("duration.modeComponents", "Years to seconds"))
                    modeButton(true, label("duration.modeWeeks", "Weeks"))
                }
                if (weeksMode) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        numberField(weekText, label("duration.weeks", "Weeks"), "weeks") {
                            weekText = it; changed(inputEdit = true)
                        }
                        Text(label("duration.weeks", "Weeks"))
                    }
                } else {
                    shownFields.forEach { index ->
                        val (key, fallback) = componentLabels[index]
                        val unit = label(key, fallback)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            numberField(fields[index], unit, key) { next ->
                                fields = fields.toMutableList().also { it[index] = next }
                                changed(inputEdit = true)
                            }
                            Text(unit, Modifier.weight(1f))
                            if (shownFields.size > 1) {
                                val removeText = label("duration.removeUnit", "Remove {unit}",
                                    mapOf("unit" to unit)).replace("{unit}", unit)
                                @Composable fun removeButton() {
                                    IconButton(onClick = {
                                        shownFields = shownFields - index
                                        fields = fields.toMutableList().also { it[index] = "0" }
                                        changed()
                                    }, modifier = Modifier.semantics { contentDescription = removeText }) {
                                        Icon(JsonFormsActionIcons.Clear, contentDescription = null)
                                    }
                                }
                                if (cupertino) CupertinoActionTooltip(removeText) { removeButton() }
                                else MaterialActionTooltip(removeText) { removeButton() }
                            }
                        }
                    }
                    if (shownFields.size < componentLabels.size) Box {
                        OutlinedButton(onClick = { addMenuOpen = true }) {
                            Text(label("duration.addUnit", "Add unit"))
                            Text(" ⌄")
                        }
                        DropdownMenu(expanded = addMenuOpen,
                            onDismissRequest = { addMenuOpen = false }) {
                            componentLabels.indices.filterNot(shownFields::contains).forEach { index ->
                                val (key, fallback) = componentLabels[index]
                                DropdownMenuItem(text = { Text(label(key, fallback)) },
                                    onClick = {
                                        shownFields = (shownFields + index).sorted()
                                        addMenuOpen = false
                                    })
                            }
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = showPicker && stateProps.enabled && !stateProps.readonly,
            onDismissRequest = ::dismissPicker,
            modifier = Modifier.width(popupWidth).testTag("duration-picker-panel")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pickerFields()
                if (showActions) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End) {
                    val cancelText = (control.options["cancelLabel"] as? JsonPrimitive)?.contentOrNull
                        ?: label("duration.cancel", "Cancel")
                    val applyText = (control.options["okLabel"] as? JsonPrimitive)?.contentOrNull
                        ?: label("duration.ok", "Apply")
                    if (cupertino) {
                        CupertinoButton(onClick = ::dismissPicker,
                            colors = CupertinoButtonDefaults.plainButtonColors()) {
                            CupertinoText(cancelText)
                        }
                        CupertinoButton(onClick = ::confirmPicker,
                            enabled = !pickerEdited() || pending() != null,
                            colors = CupertinoButtonDefaults.filledButtonColors()) {
                            CupertinoText(applyText)
                        }
                    } else {
                        TextButton(onClick = ::dismissPicker) { Text(cancelText) }
                        Button(onClick = ::confirmPicker,
                            enabled = !pickerEdited() || pending() != null) { Text(applyText) }
                    }
                }
            }
        }
        }
    }
}
