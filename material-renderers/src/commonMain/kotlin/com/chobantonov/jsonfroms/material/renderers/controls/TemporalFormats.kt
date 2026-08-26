@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

const val DEFAULT_DATE_DISPLAY_FORMAT = "MM/DD/YYYY"
const val DEFAULT_TIME_DISPLAY_FORMAT = "h:mm A"
const val DEFAULT_DATE_TIME_DISPLAY_FORMAT = "MM/DD/YYYY h:mm A"
const val DEFAULT_DATE_SAVE_FORMAT = "YYYY-MM-DD"
const val DEFAULT_TIME_SAVE_FORMAT = "HH:mm:ss"
const val DEFAULT_DATE_TIME_SAVE_FORMAT = "YYYY-MM-DDTHH:mm:ssZ"

data class TemporalValue(
    val year: Int = 1970,
    val month: Int = 1,
    val day: Int = 1,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0,
) {
    val date: LocalDate get() = LocalDate(year, month, day)
    val time: LocalTime get() = LocalTime(hour, minute, second)
    val dateTime: LocalDateTime get() = LocalDateTime(date, time)
}

fun Map<String, Any?>.stringOption(name: String): String? = when (val value = this[name]) {
    is String -> value
    is JsonPrimitive -> value.contentOrNull
    else -> null
}

fun Map<String, Any?>.booleanOption(name: String): Boolean? = when (val value = this[name]) {
    is Boolean -> value
    is JsonPrimitive -> value.contentOrNull?.toBooleanStrictOrNull()
    else -> null
}

/**
 * Restricts masked temporal inputs to digits, their configured separators, and AM/PM markers.
 * Semantic checks (for example, February 31) remain the parser's responsibility.
 */
fun isAllowedTemporalInput(value: String, format: String, mask: Boolean = true): Boolean {
    if (!mask || value.isEmpty()) return true
    val literalCharacters = buildSet {
        var index = 0
        while (index < format.length) {
            val token = tokens.firstOrNull { format.startsWith(it, index) }
            if (token != null) {
                index += token.length
            } else {
                add(format[index++])
            }
        }
    }
    val allowsPeriod = tokens.any { token ->
        (token == "A" || token == "a") && format.contains(token)
    }
    val maximumLength = format.sumOfTokens { token ->
        when (token) {
            "YYYY" -> 4
            "YY", "MM", "DD", "HH", "hh", "mm", "ss" -> 2
            "M", "D", "H", "h", "m", "s" -> 2
            "A", "a" -> 2
            else -> token.length
        }
    }
    return value.length <= maximumLength && value.all { character ->
        character.isDigit() ||
            character in literalCharacters ||
            (allowsPeriod && character.uppercaseChar() in setOf('A', 'P', 'M'))
    }
}

fun isAllowedTemporalEdit(previous: String, next: String, format: String, mask: Boolean = true): Boolean =
    next.length < previous.length || isAllowedTemporalInput(next, format, mask)

private fun String.sumOfTokens(tokenLength: (String) -> Int): Int {
    var result = 0
    var index = 0
    while (index < length) {
        val token = tokens.firstOrNull { startsWith(it, index) }
        if (token != null) {
            result += tokenLength(token)
            index += token.length
        } else {
            result += 1
            index += 1
        }
    }
    return result
}

fun parseDateValue(value: String, formats: List<String>, timeZone: TimeZone = TimeZone.currentSystemDefault()): TemporalValue? {
    parseIsoInstant(value, timeZone)?.let { return it }
    return formats.firstNotNullOfOrNull { parseTemporal(value, it) }
}

fun parseTimeValue(value: String, formats: List<String>): TemporalValue? =
    formats.firstNotNullOfOrNull { parseTemporal(value, it) }

fun parseDateTimeValue(value: String, formats: List<String>, timeZone: TimeZone = TimeZone.currentSystemDefault()): TemporalValue? {
    parseIsoInstant(value, timeZone)?.let { return it }
    return formats.firstNotNullOfOrNull { parseTemporal(value, it) }
}

private fun parseIsoInstant(value: String, timeZone: TimeZone): TemporalValue? = runCatching {
    Instant.parse(value).toLocalDateTime(timeZone).toTemporalValue()
}.getOrNull()

fun formatTemporal(value: TemporalValue, format: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    if (format == DEFAULT_DATE_TIME_SAVE_FORMAT) {
        val utc = value.dateTime.toInstant(timeZone).toLocalDateTime(TimeZone.UTC)
        return buildString {
            append(utc.year.pad(4)); append('-'); append(utc.monthNumber.pad()); append('-'); append(utc.day.pad())
            append('T'); append(utc.hour.pad()); append(':'); append(utc.minute.pad()); append(':'); append(utc.second.pad()); append('Z')
        }
    }

    val hour12 = ((value.hour + 11) % 12) + 1
    val replacements = mapOf(
        "YYYY" to value.year.pad(4), "YY" to (value.year % 100).pad(),
        "MM" to value.month.pad(), "M" to value.month.toString(),
        "DD" to value.day.pad(), "D" to value.day.toString(),
        "HH" to value.hour.pad(), "H" to value.hour.toString(),
        "hh" to hour12.pad(), "h" to hour12.toString(),
        "mm" to value.minute.pad(), "m" to value.minute.toString(),
        "ss" to value.second.pad(), "s" to value.second.toString(),
        "A" to if (value.hour < 12) "AM" else "PM",
        "a" to if (value.hour < 12) "am" else "pm",
    )
    return replaceTokens(format, replacements)
}

fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

fun pickerMillisToDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date

fun currentTemporalValue(timeZone: TimeZone = TimeZone.currentSystemDefault()): TemporalValue =
    Clock.System.now().toLocalDateTime(timeZone).toTemporalValue()

private val tokens = listOf("YYYY", "YY", "MM", "DD", "HH", "hh", "M", "D", "H", "h", "mm", "ss", "m", "s", "A", "a")

private fun parseTemporal(value: String, format: String): TemporalValue? {
    val captures = mutableListOf<String>()
    val pattern = buildString {
        append('^')
        var index = 0
        while (index < format.length) {
            val token = tokens.firstOrNull { format.startsWith(it, index) }
            if (token != null) {
                captures += token
                append(
                    when (token) {
                        "YYYY" -> "(\\d{4})"
                        "YY" -> "(\\d{2})"
                        "A", "a" -> "([AaPp][Mm])"
                        else -> "(\\d{1,2})"
                    },
                )
                index += token.length
            } else {
                append(Regex.escape(format[index].toString()))
                index++
            }
        }
        append('$')
    }
    val match = Regex(pattern).matchEntire(value.trim()) ?: return null
    var year = 1970
    var month = 1
    var day = 1
    var hour = 0
    var minute = 0
    var second = 0
    var period: String? = null
    captures.forEachIndexed { index, token ->
        val captured = match.groupValues[index + 1]
        when (token) {
            "YYYY" -> year = captured.toInt()
            "YY" -> year = 2000 + captured.toInt()
            "MM", "M" -> month = captured.toInt()
            "DD", "D" -> day = captured.toInt()
            "HH", "H", "hh", "h" -> hour = captured.toInt()
            "mm", "m" -> minute = captured.toInt()
            "ss", "s" -> second = captured.toInt()
            "A", "a" -> period = captured.uppercase()
        }
    }
    if (period != null) {
        if (hour !in 1..12) return null
        hour = (hour % 12) + if (period == "PM") 12 else 0
    }
    return runCatching { TemporalValue(year, month, day, hour, minute, second).also { it.dateTime } }.getOrNull()
}

private fun replaceTokens(format: String, replacements: Map<String, String>): String = buildString {
    var index = 0
    while (index < format.length) {
        val token = tokens.firstOrNull { format.startsWith(it, index) }
        if (token != null) {
            append(replacements.getValue(token))
            index += token.length
        } else {
            append(format[index++])
        }
    }
}

private fun Int.pad(length: Int = 2): String = toString().padStart(length, '0')

private fun LocalDateTime.toTemporalValue() = TemporalValue(year, monthNumber, day, hour, minute, second)
