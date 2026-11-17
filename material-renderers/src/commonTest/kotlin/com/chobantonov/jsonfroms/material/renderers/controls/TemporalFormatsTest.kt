package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonPrimitive

class TemporalFormatsTest {
    @Test
    fun formatsJsonSchemaDateForDisplay() {
        val value = assertNotNull(parseDateValue("2026-08-16", listOf(DEFAULT_DATE_SAVE_FORMAT)))

        assertEquals("08/16/2026", formatTemporal(value, DEFAULT_DATE_DISPLAY_FORMAT))
    }

    @Test
    fun formatsJsonSchemaTimeForDisplay() {
        val value = assertNotNull(parseTimeValue("13:37:00Z", listOf(DEFAULT_TIME_SAVE_FORMAT)))

        assertEquals("1:37 PM", formatTemporal(value, DEFAULT_TIME_DISPLAY_FORMAT))
        assertEquals("13:37:00Z", formatTemporal(value, DEFAULT_TIME_SAVE_FORMAT))
    }

    @Test fun normalizesImportedOffsetTimeForUtcWallClockPicker() {
        val value = assertNotNull(parseTimeValue("10:30:00+01:00", listOf(DEFAULT_TIME_SAVE_FORMAT)))
        assertEquals("9:30 AM", formatTemporal(value, DEFAULT_TIME_DISPLAY_FORMAT))
        assertEquals("09:30:00Z", formatTemporal(value, DEFAULT_TIME_SAVE_FORMAT))
    }

    @Test
    fun convertsJsonSchemaDateTimeToLocalDisplay() {
        // Fixed offsets are supported without shipping the optional JS timezone database.
        val newYorkSummerOffset = FixedOffsetTimeZone(UtcOffset(hours = -4))
        val value = assertNotNull(
            parseDateTimeValue("2026-08-16T23:31:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT), newYorkSummerOffset),
        )

        assertEquals("08/16/2026 7:31 PM", formatTemporal(value, DEFAULT_DATE_TIME_DISPLAY_FORMAT, newYorkSummerOffset))
    }

    @Test
    fun supportsUiSchemaDisplayAndSaveFormats() {
        val value = assertNotNull(parseDateTimeValue("1999/12/11 10:05 am", listOf("YYYY/MM/DD h:mm a")))

        assertEquals("11-12-99 10:05:am", formatTemporal(value, "DD-MM-YY hh:mm:a"))
    }

    @Test
    fun maskedInputRejectsUnrelatedCharacters() {
        assertEquals(false, isAllowedTemporalInput("dfgdfgd", DEFAULT_DATE_DISPLAY_FORMAT))
        assertEquals(false, isAllowedTemporalInput("1:37 XM", DEFAULT_TIME_DISPLAY_FORMAT))
        assertEquals(true, isAllowedTemporalInput("08/16/2026", DEFAULT_DATE_DISPLAY_FORMAT))
        assertEquals(true, isAllowedTemporalInput("1:37 PM", DEFAULT_TIME_DISPLAY_FORMAT))
    }

    @Test
    fun maskCanBeDisabledForFreeFormInput() {
        assertEquals(true, isAllowedTemporalInput("not yet a date", DEFAULT_DATE_DISPLAY_FORMAT, mask = false))
    }

    @Test
    fun temporalBooleanOptionsRequireBooleanJsonValues() {
        assertEquals(false, mapOf("mask" to JsonPrimitive(false)).booleanOption("mask"))
        assertEquals(true, mapOf("mask" to JsonPrimitive(true)).booleanOption("mask"))
        assertEquals(null, mapOf("mask" to JsonPrimitive("false")).booleanOption("mask"))
        assertEquals(null, mapOf("mask" to JsonPrimitive("true")).booleanOption("mask"))
    }

    @Test
    fun invalidExistingValuesCanStillBeDeleted() {
        assertEquals(true, isAllowedTemporalEdit("bad value", "bad valu", DEFAULT_DATE_DISPLAY_FORMAT))
    }

    @Test fun bracketLiteralsDoNotBecomeStoredBracketsOrFormatTokens() {
        val value = TemporalValue(2025, 10, 14, 9, 30, 37)
        val format = "YYYY/MM/DD [at] HH:mm:ss[Z]"
        val encoded = "2025/10/14 at 09:30:37Z"
        assertEquals(encoded, formatTemporal(value, format, TimeZone.UTC))
        assertEquals(value, parseDateTimeValue(encoded, listOf(format), TimeZone.UTC))
        assertEquals(true, isAllowedTemporalInput(encoded, format))
        assertEquals(false, isAllowedTemporalInput("2025/10/14 [at] 09:30:37[Z]", format))
    }
}
