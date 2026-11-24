package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.strictControlBooleanOption

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateControlInvalidValue
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.mappers.ControlProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlProps
import com.chobantonov.jsonfroms.core.mappers.invalidStringStoredValue
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.formatIs
import com.chobantonov.jsonfroms.core.testers.isStringControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.composeWithUi
import com.chobantonov.jsonfroms.core.validation.CronField
import com.chobantonov.jsonfroms.core.validation.cronFieldValues
import com.chobantonov.jsonfroms.core.validation.cronProblem
import com.chobantonov.jsonfroms.core.validation.splitCron
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoInputControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialInputControl
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip as CupertinoActionTooltip
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip as MaterialActionTooltip
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val cronControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("cron"), optionIs("format", "cron"))),
)

internal enum class CronPeriod(val key: String, val defaultLabel: String) {
    Year("year", "Yearly"), Month("month", "Monthly"), Week("week", "Weekly"),
    Day("day", "Daily"), Hour("hour", "Hourly"), Minute("minute", "Every minute"),
    Second("second", "Every second"),
}

private val cronOrder = CronField.entries.toList()
private val periodFields = mapOf(
    CronPeriod.Minute to listOf(CronField.Second),
    CronPeriod.Second to emptyList(),
    CronPeriod.Hour to listOf(CronField.Minute, CronField.Second),
    CronPeriod.Day to listOf(CronField.Hour, CronField.Minute, CronField.Second),
    CronPeriod.Week to listOf(CronField.DayOfWeek, CronField.Hour, CronField.Minute, CronField.Second),
    CronPeriod.Month to listOf(CronField.DayOfMonth, CronField.Hour, CronField.Minute, CronField.Second),
    CronPeriod.Year to listOf(CronField.Month, CronField.DayOfMonth, CronField.Hour, CronField.Minute, CronField.Second),
)

internal fun cronPeriodFields(period: CronPeriod): List<CronField> = periodFields.getValue(period)

internal fun cronPeriodOf(expression: String): CronPeriod {
    val fields = splitCron(expression) ?: return CronPeriod.Minute
    val probes = listOf(
        CronField.Month to CronPeriod.Year, CronField.DayOfMonth to CronPeriod.Month,
        CronField.DayOfWeek to CronPeriod.Week, CronField.Hour to CronPeriod.Day,
        CronField.Minute to CronPeriod.Hour,
    )
    return probes.firstOrNull { (field, _) ->
        val values = cronFieldValues(fields[field.ordinal], field)
        values == null || values.size != (if (field == CronField.DayOfWeek) 7 else field.maximum - field.minimum + 1)
    }?.second ?: CronPeriod.Second
}

internal fun cronChoiceValues(field: CronField): List<Int> =
    (field.minimum..if (field == CronField.DayOfWeek) 6 else field.maximum).toList()

internal fun cronChoiceLabel(field: CronField, value: Int): String = when (field) {
    CronField.Month -> listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN",
        "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")[value - 1]
    CronField.DayOfWeek -> listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")[value]
    else -> value.toString()
}

internal fun cronFormatChoices(field: CronField, values: Collection<Int>): String {
    val choices = cronChoiceValues(field)
    val sorted = values.filter { it in choices }.distinct().sorted()
    if (sorted.isEmpty() || sorted.size == choices.size) return "*"
    if (field !in listOf(CronField.Month, CronField.DayOfWeek) && sorted.size > 2) {
        val step = sorted[1] - sorted[0]
        if (step > 1 && sorted.zipWithNext().all { (a, b) -> b - a == step } &&
            sorted.last() + step > choices.last()) {
            return if (sorted.first() == choices.first()) "*/$step" else "${sorted.first()}/$step"
        }
    }
    val parts = mutableListOf<String>()
    var index = 0
    while (index < sorted.size) {
        var end = index
        while (end + 1 < sorted.size && sorted[end + 1] == sorted[end] + 1) end++
        if (end - index >= 2) parts += "${cronChoiceLabel(field, sorted[index])}-${cronChoiceLabel(field, sorted[end])}"
        else for (each in index..end) parts += cronChoiceLabel(field, sorted[each])
        index = end + 1
    }
    return parts.joinToString(",")
}

internal fun cronSameExpression(left: String, right: String): Boolean {
    if (left.trim() == right.trim()) return true
    val a = splitCron(left) ?: return false
    val b = splitCron(right) ?: return false
    return cronOrder.all { field ->
        val one = cronFieldValues(a[field.ordinal], field)
        val two = cronFieldValues(b[field.ordinal], field)
        if (one == null || two == null) a[field.ordinal] == b[field.ordinal] else one == two
    }
}

internal fun cronForPeriod(expression: String, period: CronPeriod): List<String> {
    val fields = splitCron(expression) ?: List(cronOrder.size) { "*" }
    val shown = cronPeriodFields(period)
    return cronOrder.map { field -> if (field in shown) fields[field.ordinal] else "*" }
}

internal fun cronEditedField(fields: List<String>, field: CronField, value: String): List<String> {
    val edited = fields.toMutableList().also { it[field.ordinal] = value }
    val selectedValues = cronFieldValues(value, field) ?: return edited
    if (selectedValues.size ==
        (if (field == CronField.DayOfWeek) 7 else field.maximum - field.minimum + 1)) return edited
    val lower = when (field) {
        CronField.Hour -> listOf(CronField.Minute, CronField.Second)
        CronField.Minute -> listOf(CronField.Second)
        else -> emptyList()
    }
    lower.forEach { finer ->
        val selected = cronFieldValues(edited[finer.ordinal], finer)
        if (selected != null && selected.size == finer.maximum - finer.minimum + 1)
            edited[finer.ordinal] = "0"
    }
    return edited
}

private val cronBg = JsonObject(mapOf(
    "cron.open" to JsonPrimitive("Избери график"),
    "cron.title" to JsonPrimitive("График"),
    "cron.apply" to JsonPrimitive("Приложи"),
    "cron.cancel" to JsonPrimitive("Отказ"),
    "cron.invalid" to JsonPrimitive("Невалиден cron израз."),
    "cron.period" to JsonPrimitive("Период"),
    "cron.minute" to JsonPrimitive("Минути"),
    "cron.hour" to JsonPrimitive("Часове"),
    "cron.day" to JsonPrimitive("Ден"),
    "cron.week" to JsonPrimitive("Седмица"),
    "cron.month" to JsonPrimitive("Месеци"),
    "cron.year" to JsonPrimitive("Година"),
    "cron.second" to JsonPrimitive("Секунди"),
    "cron.dayOfMonth" to JsonPrimitive("Дни от месеца"),
    "cron.dayOfWeek" to JsonPrimitive("Дни от седмицата"),
    "cron.selectedPeriod" to JsonPrimitive("Избран период"),
    "cron.repeats" to JsonPrimitive("Повтаря се"),
    "cron.chooseRepeats" to JsonPrimitive("Избери честота"),
    "cron.every" to JsonPrimitive("Всеки"),
    "cron.expression" to JsonPrimitive("Израз"),
    "cron.advanced" to JsonPrimitive("Този избор използва разширен cron синтаксис."),
    "cron.period.year" to JsonPrimitive("Годишно"),
    "cron.period.month" to JsonPrimitive("Месечно"),
    "cron.period.week" to JsonPrimitive("Седмично"),
    "cron.period.day" to JsonPrimitive("Ежедневно"),
    "cron.period.hour" to JsonPrimitive("На всеки час"),
    "cron.period.minute" to JsonPrimitive("На всяка минута"),
    "cron.period.second" to JsonPrimitive("На всяка секунда"),
))

@OptIn(ExperimentalCupertinoApi::class)
@Composable
internal fun CronControl(props: ComposeJsonFormsRendererProps, cupertino: Boolean,
    compact: Boolean = false) {
    val control = props.uischema as? ControlElement ?: return
    val path = composeWithUi(control, props.path)
    val current = (JsonPath.get(props.state.jsonforms.core.data, path) as? JsonPrimitive)?.contentOrNull.orEmpty()
    val i18n = props.state.jsonforms.i18n
    val stateProps = mapStateToControlProps(props.state, OwnPropsOfControl(
        id = props.id, controlUiSchema = control,
        own = OwnPropsOfRenderer(uischema = control, schema = props.schema,
            enabled = props.enabled, path = props.path, renderers = props.renderers, cells = props.cells),
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
        i18n.translate, i18n.locale, key, fallback, mapOf("bg" to cronBg))
    val showActions = (strictControlBooleanOption(control.options, "showActions")
        ?: strictControlBooleanOption(props.config, "showActions")) != false
    var pickerOpen by remember(path, if (!showActions) null else current) {
        mutableStateOf(false)
    }
    var period by remember(path, if (!showActions) null else current) {
        mutableStateOf<CronPeriod?>(splitCron(current)?.let { cronPeriodOf(current) })
    }
    var fields by remember(path, if (!showActions) null else current) {
        mutableStateOf(splitCron(current) ?: List(cronOrder.size) { "*" })
    }
    var expressionDraft by remember(path, if (!showActions) null else current) {
        mutableStateOf(current)
    }
    var pickerDirty by remember(path, if (!showActions) null else current) { mutableStateOf(false) }
    var lastPickerCommit by remember(path) { mutableStateOf<String?>(null) }
    var periodMenuOpen by remember(path) { mutableStateOf(false) }
    var openField by remember(path) { mutableStateOf<CronField?>(null) }

    fun publish(expression: String) {
        if (cronSameExpression(expression, current)) return
        lastPickerCommit = expression
        inputProps.handleChange(path, JsonPrimitive(expression))
    }
    fun resetPicker() {
        period = splitCron(current)?.let { cronPeriodOf(current) }
        fields = splitCron(current) ?: List(cronOrder.size) { "*" }
        expressionDraft = current
        pickerDirty = false
        periodMenuOpen = false
        openField = null
    }
    fun edit(next: List<String>) {
        fields = next
        expressionDraft = next.joinToString(" ")
        pickerDirty = true
        if (!showActions && (expressionDraft.isBlank() || cronProblem(expressionDraft) == null))
            publish(expressionDraft)
    }
    fun editExpression(next: String) {
        expressionDraft = next
        splitCron(next)?.let { fields = it; period = cronPeriodOf(next) }
        pickerDirty = true
        if (!showActions && (next.isBlank() || cronProblem(next) == null)) publish(next)
    }
    fun dismiss() { pickerOpen = false; resetPicker() }
    LaunchedEffect(current) {
        if (!showActions && pickerOpen && current != lastPickerCommit) dismiss()
        lastPickerCommit = null
    }
    LaunchedEffect(stateProps.enabled, stateProps.readonly) {
        if (!stateProps.enabled || stateProps.readonly) dismiss()
    }
    fun apply() {
        if (pickerDirty && (expressionDraft.isBlank() || cronProblem(expressionDraft) == null))
            publish(expressionDraft)
        pickerOpen = false
    }
    val openPicker = {
        if (!pickerOpen) resetPicker()
        pickerOpen = !pickerOpen
    }

    @Composable fun clearButton(description: String, modifier: Modifier = Modifier,
        onClick: () -> Unit,
    ) {
        val button: @Composable () -> Unit = {
            IconButton(onClick = onClick, modifier = modifier.semantics {
                contentDescription = description
            }) { Icon(JsonFormsActionIcons.Clear, contentDescription = null) }
        }
        if (cupertino) CupertinoActionTooltip(description) { button() }
        else MaterialActionTooltip(description) { button() }
    }

    @Composable fun pickerContent(labelWidth: Dp, menuWidth: Dp) {
        val editedProblem = expressionDraft.takeIf(String::isNotBlank)?.let(::cronProblem)
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label("cron.repeats", "Repeats"), Modifier.width(labelWidth))
                Box {
                    OutlinedButton(onClick = { periodMenuOpen = true },
                        modifier = Modifier.semantics {
                            contentDescription = label("cron.repeats", "Repeats")
                            stateDescription = if (period == null)
                                label("cron.chooseRepeats", "Choose how often")
                            else label("cron.selectedPeriod", "Selected period")
                        }) {
                        Text(period?.let { label("cron.period.${it.key}", it.defaultLabel) }
                            ?: label("cron.chooseRepeats", "Choose how often"))
                        Text(" ⌄")
                    }
                    DropdownMenu(expanded = periodMenuOpen,
                        onDismissRequest = { periodMenuOpen = false }) {
                        CronPeriod.entries.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(label("cron.period.${choice.key}", choice.defaultLabel)) },
                                trailingIcon = if (choice == period) ({ Text("✓") }) else null,
                                modifier = Modifier.semantics { selected = choice == period },
                                onClick = {
                                    periodMenuOpen = false
                                    if (choice != period) {
                                        period = choice
                                        if (splitCron(expressionDraft) != null)
                                            edit(if (choice == CronPeriod.Second)
                                                List(cronOrder.size) { "*" }
                                            else cronForPeriod(expressionDraft, choice))
                                    }
                                })
                        }
                    }
                }
            }
            val visibleFields = period?.let(::cronPeriodFields).orEmpty()
            visibleFields.forEach { field ->
                val fieldLabel = label("cron.${field.key}", when (field) {
                    CronField.Second -> "Seconds"; CronField.Minute -> "Minutes"
                    CronField.Hour -> "Hours"; CronField.DayOfMonth -> "Days of month"
                    CronField.Month -> "Months"; CronField.DayOfWeek -> "Days of week"
                })
                val raw = fields[field.ordinal]
                val values = cronFieldValues(raw, field)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(fieldLabel, Modifier.width(labelWidth))
                    if (values == null) {
                        val inputInteraction = remember(field) { MutableInteractionSource() }
                        val inputFocused by inputInteraction.collectIsFocusedAsState()
                        var inputHovered by remember(field) { mutableStateOf(false) }
                        var inputClearFocused by remember(field) { mutableStateOf(false) }
                        Box(Modifier.weight(1f).pointerInput(field) {
                            awaitPointerEventScope {
                                while (true) when (awaitPointerEvent().type) {
                                    PointerEventType.Enter -> inputHovered = true
                                    PointerEventType.Exit -> inputHovered = false
                                }
                            }
                        }) {
                            OutlinedTextField(raw,
                                onValueChange = { edit(cronEditedField(fields, field, it)) },
                                singleLine = true, interactionSource = inputInteraction,
                                modifier = Modifier.fillMaxWidth().semantics {
                                    contentDescription = fieldLabel
                                },
                                trailingIcon = if (raw.isNotEmpty() && raw != "*" &&
                                    (inputFocused || inputHovered || inputClearFocused)) ({
                                    clearButton("${translateControlClear(i18n)}: $fieldLabel",
                                        modifier = Modifier.onFocusChanged {
                                            inputClearFocused = it.isFocused
                                        }) {
                                        inputClearFocused = false
                                        edit(cronEditedField(fields, field, "*"))
                                    }
                                }) else null)
                        }
                    } else {
                        val choices = cronChoiceValues(field)
                        val selectedValues = if (values.size == choices.size) emptySet() else values
                        val hoverInteraction = remember(field) { MutableInteractionSource() }
                        val buttonInteraction = remember(field) { MutableInteractionSource() }
                        val hovered by hoverInteraction.collectIsHoveredAsState()
                        val focused by buttonInteraction.collectIsFocusedAsState()
                        var resetFocused by remember(field) { mutableStateOf(false) }
                        val showReset = selectedValues.isNotEmpty() &&
                            (hovered || focused || resetFocused || openField == field)
                        Box(Modifier.weight(1f).hoverable(hoverInteraction)) {
                            OutlinedButton(onClick = { openField = if (openField == field) null else field },
                                interactionSource = buttonInteraction,
                                modifier = Modifier.fillMaxWidth().semantics {
                                    contentDescription = fieldLabel
                                }) {
                                Text(if (selectedValues.isEmpty()) label("cron.every", "Every")
                                    else selectedValues.sorted().joinToString(", ") {
                                        cronChoiceLabel(field, it)
                                    }, Modifier.weight(1f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                if (selectedValues.isNotEmpty()) Box(Modifier.width(48.dp))
                                Text("⌄")
                            }
                            if (showReset) Box(Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
                                .size(48.dp).zIndex(1f)) {
                                clearButton("${label("cron.every", "Every")}: $fieldLabel",
                                    modifier = Modifier.onFocusChanged { resetFocused = it.isFocused }) {
                                    resetFocused = false
                                    edit(cronEditedField(fields, field, "*"))
                                }
                            }
                            DropdownMenu(expanded = openField == field,
                                onDismissRequest = { openField = null },
                                modifier = Modifier.width(menuWidth).heightIn(max = 256.dp)) {
                                Column(Modifier.heightIn(max = 256.dp).verticalScroll(rememberScrollState())) {
                                    choices.forEach { choice ->
                                        DropdownMenuItem(
                                            text = { Text(cronChoiceLabel(field, choice)) },
                                            trailingIcon = if (choice in selectedValues) ({
                                                Text("✓")
                                            }) else null,
                                            modifier = Modifier.semantics {
                                                selected = choice in selectedValues
                                                contentDescription = "$fieldLabel: ${cronChoiceLabel(field, choice)}"
                                            },
                                            onClick = {
                                                val next = if (choice in selectedValues)
                                                    selectedValues - choice else selectedValues + choice
                                                edit(cronEditedField(fields, field,
                                                    cronFormatChoices(field, next)))
                                            })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (visibleFields.any { cronFieldValues(fields[it.ordinal], it) == null })
                Text(label("cron.advanced", "This selection uses advanced cron syntax."))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label("cron.expression", "Expression"), Modifier.width(labelWidth))
                val expressionInteraction = remember { MutableInteractionSource() }
                val expressionFocused by expressionInteraction.collectIsFocusedAsState()
                var expressionHovered by remember { mutableStateOf(false) }
                var expressionClearFocused by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f).pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> expressionHovered = true
                            PointerEventType.Exit -> expressionHovered = false
                        }
                    }
                }) {
                    OutlinedTextField(expressionDraft, onValueChange = ::editExpression,
                        singleLine = true, isError = pickerDirty && editedProblem != null,
                        interactionSource = expressionInteraction,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = label("cron.expression", "Expression")
                        }, trailingIcon = if (expressionDraft.isNotEmpty() &&
                            (expressionFocused || expressionHovered || expressionClearFocused)) ({
                            clearButton("${translateControlClear(i18n)}: ${label("cron.expression", "Expression")}",
                                modifier = Modifier.onFocusChanged {
                                    expressionClearFocused = it.isFocused
                                }) {
                                expressionClearFocused = false
                                editExpression("")
                            }
                        }) else null)
                }
            }
            if (pickerDirty && editedProblem != null)
                Text(label("cron.invalid", "Invalid cron expression."))
            if (showActions) Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End) {
                val applyText = (control.options["okLabel"] as? JsonPrimitive)?.contentOrNull
                    ?: label("cron.apply", "Apply")
                val cancelText = (control.options["cancelLabel"] as? JsonPrimitive)?.contentOrNull
                    ?: label("cron.cancel", "Cancel")
                if (cupertino) {
                    CupertinoButton(onClick = ::dismiss,
                        colors = CupertinoButtonDefaults.plainButtonColors()) {
                        CupertinoText(cancelText)
                    }
                    CupertinoButton(onClick = ::apply,
                        enabled = !pickerDirty || editedProblem == null,
                        colors = CupertinoButtonDefaults.filledButtonColors()) {
                        CupertinoText(applyText)
                    }
                } else {
                    TextButton(onClick = ::dismiss) { Text(cancelText) }
                    Button(onClick = ::apply,
                        enabled = !pickerDirty || editedProblem == null) {
                        Text(applyText)
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val localError = if (current.isNotEmpty() && cronProblem(current) != null)
            label("cron.invalid", "Invalid cron expression.") else null
        val onTextChange: (String) -> Unit = { next ->
            inputProps.handleChange(path, if (next.isEmpty()) null else JsonPrimitive(next))
        }
        BoxWithConstraints {
        val popupWidth = minOf(384.dp, maxWidth)
        val labelWidth = minOf(112.dp, maxOf(72.dp, popupWidth - 152.dp))
        val menuWidth = minOf(220.dp, popupWidth - labelWidth - 24.dp)
        if (cupertino) CupertinoInputControl(inputProps, current,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingContent = if (stateProps.enabled && !stateProps.readonly)
                ({ ExtendedPickerTrigger(ExtendedPickerKind.Cron, true,
                    label("cron.open", "Choose schedule"), openPicker) })
            else null,
            clearLabel = translateControlClear(i18n), onChange = onTextChange)
        else MaterialInputControl(inputProps, current,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingIcon = if (stateProps.enabled && !stateProps.readonly)
                ({ ExtendedPickerTrigger(ExtendedPickerKind.Cron, false,
                    label("cron.open", "Choose schedule"), openPicker) })
            else null,
            clearLabel = translateControlClear(i18n), onChange = onTextChange)
        DropdownMenu(expanded = pickerOpen && stateProps.enabled && !stateProps.readonly,
            onDismissRequest = {
                when {
                    openField != null -> openField = null
                    periodMenuOpen -> periodMenuOpen = false
                    else -> dismiss()
                }
            }, modifier = Modifier.width(popupWidth).testTag("cron-picker-panel")) {
            pickerContent(labelWidth, menuWidth)
        }
        }
    }
}
