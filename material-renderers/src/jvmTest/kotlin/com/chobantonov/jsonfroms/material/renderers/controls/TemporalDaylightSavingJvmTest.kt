package com.chobantonov.jsonfroms.material.renderers.controls

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TemporalDaylightSavingJvmTest {
    @Test
    fun selectedDateKeepsWallTimeAcrossSpringTransition() {
        val zone = TimeZone.of("America/New_York")
        val imported = assertNotNull(parseDateTimeValue(
            "2026-03-07T15:30:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT), zone,
        ))
        assertEquals(TemporalValue(2026, 3, 7, 10, 30), imported)

        val selected = imported.copy(day = 8)
        assertEquals("2026-03-08T14:30:00Z", formatTemporal(selected, DEFAULT_DATE_TIME_SAVE_FORMAT, zone))
        assertEquals("2026-03-07 10:30 -05:00", formatTemporal(imported, "YYYY-MM-DD HH:mm Z", zone))
        assertEquals("2026-03-08 10:30 -04:00", formatTemporal(selected, "YYYY-MM-DD HH:mm Z", zone))
        assertEquals(selected, parseDateTimeValue(
            "2026-03-08T14:30:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT), zone,
        ))
    }

    @Test
    fun selectedDateKeepsWallTimeAcrossAutumnTransition() {
        val zone = TimeZone.of("America/New_York")
        val imported = assertNotNull(parseDateTimeValue(
            "2026-10-31T14:30:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT), zone,
        ))
        assertEquals(TemporalValue(2026, 10, 31, 10, 30), imported)

        val selected = imported.copy(month = 11, day = 1)
        assertEquals("2026-11-01T15:30:00Z", formatTemporal(selected, DEFAULT_DATE_TIME_SAVE_FORMAT, zone))
        assertEquals("2026-10-31 10:30 -04:00", formatTemporal(imported, "YYYY-MM-DD HH:mm Z", zone))
        assertEquals("2026-11-01 10:30 -05:00", formatTemporal(selected, "YYYY-MM-DD HH:mm Z", zone))
        assertEquals(selected, parseDateTimeValue(
            "2026-11-01T15:30:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT), zone,
        ))
    }
}
