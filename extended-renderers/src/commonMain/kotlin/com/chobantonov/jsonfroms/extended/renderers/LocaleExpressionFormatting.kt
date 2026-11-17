package com.chobantonov.jsonfroms.extended.renderers

/** Host-locale formatters used only by registered interpolation functions. */
internal expect fun formatExpressionNumber(value: Double, locale: String, fractionDigits: Int?): String
internal expect fun formatExpressionCurrency(value: Double, currencyCode: String, locale: String): String
internal expect fun formatExpressionPercent(value: Double, locale: String): String
internal expect fun formatExpressionTemporal(epochMillis: Long, locale: String, kind: TemporalFormat, fixedUtc: Boolean): String
internal expect fun formatExpressionLocalTemporal(localDateTime: String, locale: String, kind: TemporalFormat): String
internal expect fun viewerOffsetMillis(epochMillis: Long): Int

internal enum class TemporalFormat { DATE, TIME, DATE_TIME }
internal data class ExpressionInstant(
    val epochMillis: Long,
    val fixedUtc: Boolean,
    /** A validated wall time interpreted in the viewer's local time zone. */
    val localDateTime: String? = null,
)

private val calendarPattern = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
private val clockPattern = Regex("(\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d+))?)?")
private val instantPattern = Regex("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?)(Z|[+-]\\d{2}:\\d{2})")
private val localDateTimePattern = Regex("(\\d{4}-\\d{2}-\\d{2})T(\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?)")

/** Calendar and clock values retain their wall fields; offset date-times denote instants. */
internal fun parseExpressionInstant(value: String): ExpressionInstant {
    val instant = instantPattern.matchEntire(value)
    val local = localDateTimePattern.matchEntire(value)
    val dateText: String
    val clockText: String
    val zoneText: String
    val fixedUtc: Boolean
    when {
        calendarPattern.matches(value) -> {
            dateText = value; clockText = "00:00:00"; zoneText = "Z"; fixedUtc = true
        }
        clockPattern.matches(value) -> {
            dateText = "1970-01-01"; clockText = value; zoneText = "Z"; fixedUtc = true
        }
        instant != null -> {
            dateText = instant.groupValues[1]; clockText = instant.groupValues[2]
            zoneText = instant.groupValues[3]; fixedUtc = false
        }
        local != null -> {
            dateText = local.groupValues[1]; clockText = local.groupValues[2]
            zoneText = "Z"; fixedUtc = false
        }
        else -> throw IllegalArgumentException("temporal value must be an ISO date, time or offset date-time")
    }
    val date = calendarPattern.matchEntire(dateText)!!
    val year = date.groupValues[1].toInt()
    val month = date.groupValues[2].toInt()
    val day = date.groupValues[3].toInt()
    require(year in 1..9999 && month in 1..12 && day in 1..daysInMonth(year, month)) { "invalid calendar date" }
    val clock = clockPattern.matchEntire(clockText)!!
    val hour = clock.groupValues[1].toInt()
    val minute = clock.groupValues[2].toInt()
    val second = clock.groupValues[3].ifEmpty { "0" }.toInt()
    require(hour in 0..23 && minute in 0..59 && second in 0..59) { "invalid clock time" }
    val millis = clock.groupValues[4].take(3).padEnd(3, '0').ifEmpty { "0" }.toInt()
    val offsetMinutes = if (zoneText == "Z") 0 else {
        val hours = zoneText.substring(1, 3).toInt()
        val minutes = zoneText.substring(4, 6).toInt()
        require(hours <= 23 && minutes <= 59) { "invalid time-zone offset" }
        (hours * 60 + minutes) * if (zoneText[0] == '+') 1 else -1
    }
    val epochDay = daysBeforeYear(year) - daysBeforeYear(1970) +
        listOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)[month - 1] +
        (if (month > 2 && leapYear(year)) 1 else 0) + day - 1
    val epochMillis = epochDay * 86_400_000L +
        ((hour * 60L + minute - offsetMinutes) * 60 + second) * 1000 + millis
    val normalizedLocal = if (local == null) null else
        "$dateText" + "T" + clock.groupValues[1] + ":" + clock.groupValues[2] + ":" +
            clock.groupValues[3].ifEmpty { "00" } + "." + millis.toString().padStart(3, '0')
    return ExpressionInstant(epochMillis, fixedUtc, normalizedLocal)
}

private fun leapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
private fun daysBeforeYear(year: Int): Long {
    val previous = year - 1L
    return 365 * previous + previous / 4 - previous / 100 + previous / 400
}
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (leapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Selects the earlier occurrence of an ambiguous local wall time, as JavaScript Date does. */
internal fun resolveLocalExpressionInstant(wallEpochMillis: Long, offsetAt: (Long) -> Int): Long? {
    val offsets = listOf(-172_800_000L, 0L, 172_800_000L)
        .map { offsetAt(wallEpochMillis + it) }.distinct()
    return offsets.map { wallEpochMillis - it }
        .filter { candidate -> candidate + offsetAt(candidate) == wallEpochMillis }
        .minOrNull()
}

private fun formatExpressionTemporalValue(value: String, locale: String, kind: TemporalFormat): String =
    parseExpressionInstant(value).let { parsed ->
        if (parsed.localDateTime != null) {
            val resolved = resolveLocalExpressionInstant(parsed.epochMillis, ::viewerOffsetMillis)
            if (resolved != null) formatExpressionTemporal(resolved, locale, kind, fixedUtc = false)
            else formatExpressionLocalTemporal(parsed.localDateTime, locale, kind)
        } else formatExpressionTemporal(parsed.epochMillis, locale, kind, parsed.fixedUtc)
    }

internal fun formatExpressionDate(value: String, locale: String): String =
    formatExpressionTemporalValue(value, locale, TemporalFormat.DATE)
internal fun formatExpressionTime(value: String, locale: String): String =
    formatExpressionTemporalValue(value, locale, TemporalFormat.TIME)
internal fun formatExpressionDateTime(value: String, locale: String): String =
    formatExpressionTemporalValue(value, locale, TemporalFormat.DATE_TIME)
