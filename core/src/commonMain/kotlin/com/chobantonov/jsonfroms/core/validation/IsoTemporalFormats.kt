@file:OptIn(io.github.optimumcode.json.schema.ExperimentalApi::class)

package com.chobantonov.jsonfroms.core.validation

import io.github.optimumcode.json.schema.FormatValidator
import io.github.optimumcode.json.schema.model.AbstractElement
import io.github.optimumcode.json.schema.model.PrimitiveElement
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val isoTimePattern = Regex(
    """^(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|[+-]\d{2}(?::?\d{2})?)?$""",
    RegexOption.IGNORE_CASE,
)

private data class IsoClock(
    val hour: Int,
    val minute: Int,
    val second: Int,
    val fraction: String,
    val offsetSeconds: Int?,
    val dateParserCompatible: Boolean,
)

private fun parseIsoClock(text: String): IsoClock? {
    val match = isoTimePattern.matchEntire(text) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    val second = match.groupValues[3].toInt()
    val zone = match.groupValues[5]
    val offset = when {
        zone.isEmpty() -> null
        zone.equals("Z", ignoreCase = true) -> 0
        else -> {
            val digits = zone.drop(1).replace(":", "")
            val offsetHour = digits.take(2).toInt()
            val offsetMinute = digits.drop(2).takeIf(String::isNotEmpty)?.toInt() ?: 0
            if (offsetHour > 23 || offsetMinute > 59) return null
            (offsetHour * 3600 + offsetMinute * 60) * if (zone[0] == '+') 1 else -1
        }
    }
    if (hour > 23 || minute > 59) return null
    if (second > 60) return null
    if (second == 60) {
        val sign = if (offset == null || offset >= 0) 1 else -1
        val absoluteOffset = kotlin.math.abs(offset ?: 0)
        val offsetHour = absoluteOffset / 3600
        val offsetMinute = (absoluteOffset % 3600) / 60
        val utcMinute = minute - offsetMinute * sign
        val utcHour = hour - offsetHour * sign - if (utcMinute < 0) 1 else 0
        if ((utcHour != 23 && utcHour != -1) || (utcMinute != 59 && utcMinute != -1))
            return null
    }
    val fraction = match.groupValues[4].trimEnd('0')
    return IsoClock(hour, minute, second, fraction, offset,
        second < 60 && !Regex("""[+-]\d{2}""").matches(zone))
}

private data class IsoDateTime(val date: LocalDate, val clock: IsoClock)

private fun parseIsoDateTime(text: String): IsoDateTime? {
    val parts = text.split(Regex("[Tt\\s]"))
    if (parts.size != 2 || !Regex("""\d{4}-\d{2}-\d{2}""").matches(parts[0])) return null
    val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return null
    return IsoDateTime(date, parseIsoClock(parts[1]) ?: return null)
}

internal val isoTimeFormatValidator = object : FormatValidator {
    override fun validate(element: AbstractElement) =
        if ((element as? PrimitiveElement)?.takeIf { it.isString }?.content
                ?.let(::parseIsoClock) != null) FormatValidator.Valid()
        else FormatValidator.Invalid()
}

internal val isoDateTimeFormatValidator = object : FormatValidator {
    override fun validate(element: AbstractElement) =
        if ((element as? PrimitiveElement)?.takeIf { it.isString }?.content
                ?.let(::parseIsoDateTime) != null) FormatValidator.Valid()
        else FormatValidator.Invalid()
}

/** ajv-formats compares iso-time by local clock text and ignores its optional zone. */
internal fun compareIsoTime(left: String, right: String): Int? {
    val a = isoTimePattern.matchEntire(left) ?: return null
    val b = isoTimePattern.matchEntire(right) ?: return null
    fun key(match: MatchResult): String = match.groupValues[1] + match.groupValues[2] +
        match.groupValues[3] + match.groupValues[4].takeIf(String::isNotEmpty)
            ?.let { ".$it" }.orEmpty()
    return key(a).compareTo(key(b))
}

/** ajv-formats compares calendar dates first, then the time portion. */
internal fun compareIsoDateTime(left: String, right: String): Int? {
    val aParts = left.split(Regex("[Tt\\s]"))
    val bParts = right.split(Regex("[Tt\\s]"))
    val aDate = aParts.firstOrNull()?.takeIf(String::isNotEmpty) ?: return null
    val bDate = bParts.firstOrNull()?.takeIf(String::isNotEmpty) ?: return null
    val dateComparison = aDate.compareTo(bDate)
    if (dateComparison != 0) return dateComparison
    val a = aParts.getOrNull(1)?.let(::parseIsoClock) ?: return null
    val b = bParts.getOrNull(1)?.let(::parseIsoClock) ?: return null
    if (!a.dateParserCompatible || !b.dateParserCompatible) return null
    val aSecond = comparableSecond(a)
    val bSecond = comparableSecond(b)
    val secondComparison = aSecond.compareTo(bSecond)
    if (secondComparison != 0) return secondComparison
    val aMillis = a.fraction.padEnd(3, '0').take(3).toIntOrNull() ?: 0
    val bMillis = b.fraction.padEnd(3, '0').take(3).toIntOrNull() ?: 0
    return aMillis.compareTo(bMillis)
}

private fun comparableSecond(clock: IsoClock): Long {
    val offset = clock.offsetSeconds ?: run {
        val local = LocalDateTime(2020, 1, 1, clock.hour, clock.minute,
            minOf(clock.second, 59))
        val utc = local.toInstant(TimeZone.UTC).epochSeconds
        (utc - local.toInstant(TimeZone.currentSystemDefault()).epochSeconds).toInt()
    }
    return clock.hour * 3600L + clock.minute * 60L + clock.second - offset
}
