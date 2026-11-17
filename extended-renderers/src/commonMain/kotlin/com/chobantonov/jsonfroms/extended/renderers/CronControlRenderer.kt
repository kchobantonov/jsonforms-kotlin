package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
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

val cronControlTester: RankedTester = rankWith(
    3, and(isStringControl, or(formatIs("cron"), optionIs("format", "cron"))),
)

internal enum class CronPeriod(val key: String, val defaultLabel: String) {
    Minute("minute", "Minute"), Hour("hour", "Hour"), Day("day", "Day"),
    Week("week", "Week"), Month("month", "Month"), Year("year", "Year"),
}

private val cronOrder = CronField.entries.toList()
private val periodFields = mapOf(
    CronPeriod.Minute to listOf(CronField.Second),
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
    }?.second ?: CronPeriod.Minute
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
    "cron.minute" to JsonPrimitive("Минута"),
    "cron.hour" to JsonPrimitive("Час"),
    "cron.day" to JsonPrimitive("Ден"),
    "cron.week" to JsonPrimitive("Седмица"),
    "cron.month" to JsonPrimitive("Месец"),
    "cron.year" to JsonPrimitive("Година"),
    "cron.second" to JsonPrimitive("Секунда"),
    "cron.dayOfMonth" to JsonPrimitive("Ден от месеца"),
    "cron.dayOfWeek" to JsonPrimitive("Ден от седмицата"),
    "cron.selectedPeriod" to JsonPrimitive("Избран период"),
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
    val showActions = (control.options["showActions"] as? JsonPrimitive)?.booleanOrNull != false
    var pickerOpen by remember(path, if (compact && !showActions) null else current) {
        mutableStateOf(false)
    }
    var period by remember(path, current) { mutableStateOf(cronPeriodOf(current)) }
    var fields by remember(path, current) {
        mutableStateOf(splitCron(current) ?: List(cronOrder.size) { "*" })
    }
    var pickerDirty by remember(path, current) { mutableStateOf(false) }

    fun publish(expression: String) = inputProps.handleChange(path, JsonPrimitive(expression))
    fun resetPicker() {
        period = cronPeriodOf(current)
        fields = splitCron(current) ?: List(cronOrder.size) { "*" }
        pickerDirty = false
    }
    fun edit(next: List<String>) {
        fields = next
        pickerDirty = true
        if (!showActions && cronProblem(next.joinToString(" ")) == null) publish(next.joinToString(" "))
    }
    fun dismiss() { pickerOpen = false; resetPicker() }
    LaunchedEffect(stateProps.enabled, stateProps.readonly) {
        if (!stateProps.enabled || stateProps.readonly) dismiss()
    }
    fun apply() {
        if (pickerDirty && cronProblem(fields.joinToString(" ")) == null) publish(fields.joinToString(" "))
        pickerOpen = false
    }
    val openPicker = {
        if (!pickerOpen) resetPicker()
        pickerOpen = !pickerOpen
    }

    @Composable fun pickerContent() {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cupertino) CupertinoText(label("cron.period", "Period"))
            else Text(label("cron.period", "Period"))
            @Composable fun periodButton(choice: CronPeriod) {
                    val periodSemantics = Modifier.semantics {
                        selected = choice == period
                        if (choice == period) stateDescription =
                            label("cron.selectedPeriod", "Selected period")
                    }
                    val choose = {
                        if (choice != period) {
                            period = choice
                            edit(cronForPeriod(fields.joinToString(" "), choice))
                        }
                    }
                    if (cupertino) CupertinoButton(modifier = periodSemantics, onClick = choose,
                        colors = CupertinoButtonDefaults.plainButtonColors()) {
                        CupertinoText(label("cron.${choice.key}", choice.defaultLabel))
                    } else OutlinedButton(modifier = periodSemantics, onClick = choose) {
                        Text(label("cron.${choice.key}", choice.defaultLabel))
                    }
            }
            if (cupertino) Column { CronPeriod.entries.forEach { periodButton(it) } }
            else Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CronPeriod.entries.forEach { periodButton(it) }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              cronPeriodFields(period).forEach { field ->
                val fieldLabel = label("cron.${field.key}", when (field) {
                    CronField.Second -> "Second"; CronField.Minute -> "Minute"; CronField.Hour -> "Hour"
                    CronField.DayOfMonth -> "Day of month"; CronField.Month -> "Month"
                    CronField.DayOfWeek -> "Day of week"
                })
                val onChange: (String) -> Unit = { next -> edit(cronEditedField(fields, field, next)) }
                if (cupertino) Box(Modifier.width(115.dp)) { CupertinoInputControl(
                    props = inputProps.copy(
                        config = inputProps.config - "focus",
                        uischema = control.copy(options = control.options - "focus" +
                            ("clearable" to JsonPrimitive(false))),
                        path = "$path.__cron_${field.key}", label = fieldLabel,
                        description = null, required = false, errors = "", errorsBeforeTouch = "",
                    ), value = fields[field.ordinal], onChange = onChange,
                ) } else OutlinedTextField(
                    value = fields[field.ordinal], onValueChange = onChange,
                    label = { Text(fieldLabel) }, singleLine = true,
                    isError = cronProblem(fields.joinToString(" ")) != null,
                    modifier = Modifier.width(115.dp),
                )
              }
            }
            if (pickerDirty && cronProblem(fields.joinToString(" ")) != null) {
                if (cupertino) CupertinoText(label("cron.invalid", "Invalid cron expression."))
                else Text(label("cron.invalid", "Invalid cron expression."))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val localError = if (current.isNotEmpty() && cronProblem(current) != null)
            label("cron.invalid", "Invalid cron expression.") else null
        val onTextChange: (String) -> Unit = { next ->
            inputProps.handleChange(path, if (next.isEmpty()) null else JsonPrimitive(next))
        }
        if (cupertino) CupertinoInputControl(inputProps, current,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingContent = if (compact && stateProps.enabled && !stateProps.readonly)
                ({ CupertinoButton(onClick = openPicker,
                    modifier = Modifier.semantics { contentDescription = label("cron.open", "Choose schedule") },
                    colors = CupertinoButtonDefaults.plainButtonColors()) { CupertinoText("⋯") } })
            else null,
            clearLabel = translateControlClear(i18n), onChange = onTextChange)
        else MaterialInputControl(inputProps, current,
            localError = localError, storedValueStatus = invalidStatus,
            compact = compact,
            trailingIcon = if (compact && stateProps.enabled && !stateProps.readonly)
                ({ IconButton(onClick = openPicker,
                    modifier = Modifier.semantics { contentDescription = label("cron.open", "Choose schedule") }) {
                    Text("⋯")
                } })
            else null,
            clearLabel = translateControlClear(i18n), onChange = onTextChange)
        if (stateProps.enabled && !stateProps.readonly) {
            if (showActions && !compact) {
                if (cupertino) CupertinoButton(onClick = openPicker,
                    colors = CupertinoButtonDefaults.plainButtonColors()) {
                    CupertinoText(label("cron.open", "Choose schedule"))
                } else OutlinedButton(onClick = openPicker) { Text(label("cron.open", "Choose schedule")) }
            } else if (!showActions && (!compact || pickerOpen)) pickerContent()
        }
        if (stateProps.enabled && !stateProps.readonly && pickerOpen && showActions) {
            val applyText = (control.options["okLabel"] as? JsonPrimitive)?.contentOrNull
                ?: label("cron.apply", "Apply")
            val cancelText = (control.options["cancelLabel"] as? JsonPrimitive)?.contentOrNull
                ?: label("cron.cancel", "Cancel")
            if (cupertino) CupertinoAlertDialog(
                onDismissRequest = ::dismiss,
                title = { CupertinoText(label("cron.title", "Schedule")) },
                message = { pickerContent() },
                buttons = {
                    cancel(onClick = ::dismiss) { CupertinoText(cancelText) }
                    default(enabled = !pickerDirty || cronProblem(fields.joinToString(" ")) == null,
                        onClick = ::apply) { CupertinoText(applyText) }
                },
            ) else AlertDialog(
                onDismissRequest = ::dismiss,
                title = { Text(label("cron.title", "Schedule")) },
                text = { pickerContent() },
                confirmButton = {
                    TextButton(onClick = ::apply,
                        enabled = !pickerDirty || cronProblem(fields.joinToString(" ")) == null) {
                        Text(applyText)
                    }
                },
                dismissButton = { TextButton(onClick = ::dismiss) { Text(cancelText) } },
            )
        }
    }
}
