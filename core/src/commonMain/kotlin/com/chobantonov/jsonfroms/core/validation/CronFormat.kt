@file:OptIn(io.github.optimumcode.json.schema.ExperimentalApi::class)

package com.chobantonov.jsonfroms.core.validation

import io.github.optimumcode.json.schema.FormatValidator
import io.github.optimumcode.json.schema.model.AbstractElement
import io.github.optimumcode.json.schema.model.PrimitiveElement

/** The portable six-field cron dialect: second, minute, hour, day, month, weekday. */
enum class CronField(val key: String, val minimum: Int, val maximum: Int) {
    Second("second", 0, 59),
    Minute("minute", 0, 59),
    Hour("hour", 0, 23),
    DayOfMonth("dayOfMonth", 1, 31),
    Month("month", 1, 12),
    DayOfWeek("dayOfWeek", 0, 7),
}

private val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
private val weekdays = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
private val aliases = mapOf(
    "@yearly" to "0 0 0 1 1 *", "@annually" to "0 0 0 1 1 *",
    "@monthly" to "0 0 0 1 * *", "@weekly" to "0 0 0 * * 0",
    "@daily" to "0 0 0 * * *", "@midnight" to "0 0 0 * * *",
    "@hourly" to "0 0 * * * *",
)

/** Expand known aliases for reading. Callers retain the authored text until an actual edit. */
fun splitCron(expression: String): List<String>? {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return null
    val expanded = aliases[trimmed.lowercase()] ?: trimmed
    return expanded.split(Regex("\\s+")).takeIf { it.size == CronField.entries.size }
}

private fun rawValueOf(token: String, field: CronField): Int? {
    val name = token.uppercase()
    val named = when (field) {
        CronField.Month -> months.indexOf(name).takeIf { it >= 0 }?.plus(1)
        CronField.DayOfWeek -> weekdays.indexOf(name).takeIf { it >= 0 }
        else -> null
    }
    val value = named ?: token.takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
    return value?.takeIf { it in field.minimum..field.maximum }
}

private fun valueOf(token: String, field: CronField): Int? =
    rawValueOf(token, field)?.let { if (field == CronField.DayOfWeek && it == 7) 0 else it }

private fun positiveStep(text: String, field: CronField): Int? {
    if (text.isEmpty() || text.any { it !in '0'..'9' } || text.all { it == '0' }) return null
    val beyondRange = field.maximum - field.minimum + 1
    return text.toIntOrNull()?.coerceAtMost(beyondRange) ?: beyondRange
}

private fun parsePart(part: String, field: CronField): Set<Int>? {
    val sections = part.split('/')
    if (sections.size !in 1..2) return null
    val step = if (sections.size == 2) positiveStep(sections[1], field)
        ?: return null else 1
    val base = sections[0]
    val from: Int
    val to: Int
    if (base == "*" || (base == "?" && field in listOf(CronField.DayOfMonth, CronField.DayOfWeek))) {
        from = field.minimum
        to = field.maximum
    } else {
        val bounds = base.split('-')
        if (bounds.size !in 1..2) return null
        from = (if (bounds.size == 2) rawValueOf(bounds[0], field)
            else valueOf(bounds[0], field)) ?: return null
        to = when {
            bounds.size == 2 -> rawValueOf(bounds[1], field) ?: return null
            sections.size == 2 -> field.maximum
            else -> from
        }
        if (to < from) return null
    }
    val result = mutableSetOf<Int>()
    var each = from
    while (each <= to) {
        result += if (field == CronField.DayOfWeek && each == 7) 0 else each
        if (step > to - each) break
        each += step
    }
    return result
}

/** Numeric set for a picker, or null for special selectors and malformed text. */
fun cronFieldValues(text: String, field: CronField): Set<Int>? {
    if (text.isBlank()) return null
    val values = mutableSetOf<Int>()
    for (part in text.split(',')) values += parsePart(part, field) ?: return null
    return values
}

private fun validSpecial(part: String, field: CronField): Boolean {
    val upper = part.uppercase()
    if (field == CronField.DayOfMonth) {
        if (upper == "L" || upper == "LW") return true
        Regex("L-([0-9]+)").matchEntire(upper)?.let {
            return it.groupValues[1].toIntOrNull()?.let { offset -> offset in 0..30 } == true
        }
        Regex("([0-9]+)W").matchEntire(upper)?.let {
            return it.groupValues[1].toIntOrNull()?.let { day -> day in 1..31 } == true
        }
    }
    if (field == CronField.DayOfWeek) {
        if (upper == "L") return true
        Regex("([A-Z]{3}|[0-7])L").matchEntire(upper)?.let {
            return valueOf(it.groupValues[1], field) != null
        }
        Regex("([A-Z]{3}|[0-7])#([1-5])").matchEntire(upper)?.let {
            return valueOf(it.groupValues[1], field) != null
        }
    }
    val wrapping = Regex("([^/,-]+)-([^/,-]+)(?:/([0-9]+))?").matchEntire(part)
        ?: return false
    val start = valueOf(wrapping.groupValues[1], field) ?: return false
    val end = valueOf(wrapping.groupValues[2], field) ?: return false
    val step = wrapping.groupValues[3].takeIf(String::isNotEmpty)
    return end < start && (step == null || positiveStep(step, field) != null)
}

private fun validField(text: String, field: CronField): Boolean =
    text.isNotEmpty() && text.split(',').all { part ->
        parsePart(part, field) != null || validSpecial(part, field)
    }

/** Null when valid; otherwise the first failing field or sixFields. */
fun cronProblem(expression: String): String? {
    val fields = splitCron(expression) ?: return "sixFields"
    return CronField.entries.firstOrNull { !validField(fields[it.ordinal], it) }?.key
}

internal val cronFormatValidator = object : FormatValidator {
    override fun validate(element: AbstractElement) =
        if ((element as? PrimitiveElement)?.takeIf { it.isString }?.content
                ?.let(::cronProblem) == null) FormatValidator.Valid()
        else FormatValidator.Invalid()
}
