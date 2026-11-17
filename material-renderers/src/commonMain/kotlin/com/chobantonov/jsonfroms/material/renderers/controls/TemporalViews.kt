package com.chobantonov.jsonfroms.material.renderers.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.datetime.LocalDate

enum class TimeView { Hours, Minutes, Seconds }
enum class DateViewMode { Day, Month, Year }

fun yearPageStart(year: Int): Int = ((year.coerceIn(1, 9999) - 1) / 12) * 12 + 1

fun yearsOnPage(start: Int): List<Int> = (start..(start + 11)).filter { it in 1..9999 }

/** An authored date view selects the finest named panel; otherwise the display format does. */
fun resolveDateViewMode(raw: Any?, displayFormat: String): DateViewMode {
    val authored = when (raw) {
        is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is List<*> -> raw.filterIsInstance<String>()
        else -> emptyList()
    }
    if ("day" in authored) return DateViewMode.Day
    if ("month" in authored) return DateViewMode.Month
    if ("year" in authored) return DateViewMode.Year
    val tokens = temporalFormatTokens(displayFormat)
    return when {
        tokens.any { it == "D" || it == "DD" } -> DateViewMode.Day
        tokens.any { it == "M" || it == "MM" } -> DateViewMode.Month
        else -> DateViewMode.Year
    }
}

class DateViewsDraft(private val initial: TemporalValue) {
    var year by mutableStateOf(initial.year.toString().padStart(4, '0'))
    var month by mutableStateOf(initial.month.toString().padStart(2, '0'))

    fun changeYear(text: String) {
        if (text.length <= 4 && text.all(Char::isDigit)) year = text
    }

    fun changeMonth(text: String) {
        if (text.length <= 2 && text.all(Char::isDigit)) month = text
    }

    fun shiftYear(delta: Int) {
        val next = (year.toIntOrNull() ?: return) + delta
        if (next in 1..9999) year = next.toString().padStart(4, '0')
    }

    fun selected(mode: DateViewMode): TemporalValue? {
        val selectedYear = year.toIntOrNull()?.takeIf { it in 1..9999 } ?: return null
        val selectedMonth = if (mode == DateViewMode.Year) initial.month
            else month.toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val selectedDay = (1..initial.day).lastOrNull { day ->
            runCatching { LocalDate(selectedYear, selectedMonth, day) }.isSuccess
        } ?: return null
        return initial.copy(year = selectedYear, month = selectedMonth, day = selectedDay)
    }
}

@Composable
fun rememberDateViewsDraft(initial: TemporalValue): DateViewsDraft =
    remember(initial) { DateViewsDraft(initial) }

/** Seconds in the display format need an editable column even without authored views. */
fun resolveTimeViews(raw: Any?, displayFormat: String): List<TimeView>? {
    val authored = when (raw) {
        is JsonArray -> raw.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is List<*> -> raw.filterIsInstance<String>()
        else -> return defaultTimeViews(displayFormat).takeIf { TimeView.Seconds in it }
    }
    val selected = authored.mapNotNull { when (it) {
        "hours" -> TimeView.Hours
        "minutes" -> TimeView.Minutes
        "seconds" -> TimeView.Seconds
        else -> null
    } }.distinct()
    if (selected.isNotEmpty()) return selected
    return defaultTimeViews(displayFormat)
}

fun defaultTimeViews(displayFormat: String): List<TimeView> {
    val tokens = temporalFormatTokens(displayFormat)
    val fromFormat = buildList {
        if (tokens.any { it == "H" || it == "HH" || it == "h" || it == "hh" }) add(TimeView.Hours)
        if (tokens.any { it == "m" || it == "mm" }) add(TimeView.Minutes)
        if (tokens.any { it == "s" || it == "ss" }) add(TimeView.Seconds)
    }
    return fromFormat.ifEmpty { listOf(TimeView.Hours, TimeView.Minutes) }
}

class TimeViewsDraft(private val initial: TemporalValue, private val is24Hour: Boolean) {
    var hours by mutableStateOf((if (is24Hour) initial.hour else (initial.hour % 12).takeIf { it != 0 } ?: 12)
        .toString().padStart(2, '0'))
    var minutes by mutableStateOf(initial.minute.toString().padStart(2, '0'))
    var seconds by mutableStateOf(initial.second.toString().padStart(2, '0'))
    var isPm by mutableStateOf(initial.hour >= 12)

    fun change(view: TimeView, text: String) {
        if (text.length > 2 || text.any { !it.isDigit() }) return
        when (view) {
            TimeView.Hours -> hours = text
            TimeView.Minutes -> minutes = text
            TimeView.Seconds -> seconds = text
        }
    }

    fun value(view: TimeView): String = when (view) {
        TimeView.Hours -> hours
        TimeView.Minutes -> minutes
        TimeView.Seconds -> seconds
    }

    fun selected(views: List<TimeView>): TemporalValue? {
        fun component(view: TimeView, text: String, original: Int, range: IntRange): Int? =
            if (view !in views) original else text.toIntOrNull()?.takeIf { it in range }
        val displayedHour = component(TimeView.Hours, hours, initial.hour,
            if (is24Hour) 0..23 else 1..12) ?: return null
        val hour = if (TimeView.Hours !in views || is24Hour) displayedHour
            else displayedHour % 12 + if (isPm) 12 else 0
        val minute = component(TimeView.Minutes, minutes, initial.minute, 0..59) ?: return null
        val second = component(TimeView.Seconds, seconds, initial.second, 0..59) ?: return null
        return initial.copy(hour = hour, minute = minute, second = second)
    }
}

@Composable
fun rememberTimeViewsDraft(initial: TemporalValue, is24Hour: Boolean): TimeViewsDraft =
    remember(initial, is24Hour) { TimeViewsDraft(initial, is24Hour) }
