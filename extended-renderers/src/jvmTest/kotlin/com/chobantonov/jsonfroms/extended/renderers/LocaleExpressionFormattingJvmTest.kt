package com.chobantonov.jsonfroms.extended.renderers

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleExpressionFormattingJvmTest {
    @Test fun calendarDateDoesNotShiftWithViewerTimeZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val utc = formatExpressionDate("2026-10-01", "en")
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(utc, formatExpressionDate("2026-10-01", "en"))
            assertEquals("Oct 1, 2026", utc)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun offsetInstantUsesViewerTimeZoneAndTimeOnlyDoesNot() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            assertEquals("Oct 1, 2026", formatExpressionDate("2026-10-01T02:30:00+02:00", "en"))
            val utcTime = formatExpressionTime("14:30:00", "en")
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals("Sep 30, 2026", formatExpressionDate("2026-10-01T02:30:00+02:00", "en"))
            assertEquals(utcTime, formatExpressionTime("14:30:00", "en"))
            assertEquals("5:30 PM", formatExpressionTime("2026-10-01T02:30:00+02:00", "en"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun offsetFreeDateTimeKeepsItsWallClockInEachViewerZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val utc = formatExpressionDateTime("2026-10-01T14:30", "en")
            assertEquals("2:30 PM", formatExpressionTime("2026-10-01T14:30", "en"))
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(utc, formatExpressionDateTime("2026-10-01T14:30", "en"))
            assertEquals("2:30 PM", formatExpressionTime("2026-10-01T14:30", "en"))
            assertEquals("7:30 AM", formatExpressionTime("2026-10-01T14:30Z", "en"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun localDaylightSavingOverlapUsesEarlierInstantAndGapNormalizesForward() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val overlap = parseExpressionInstant("2026-11-01T01:30")
            assertEquals(parseExpressionInstant("2026-11-01T01:30-04:00").epochMillis,
                resolveLocalExpressionInstant(overlap.epochMillis, ::viewerOffsetMillis))
            assertEquals("3:30 AM", formatExpressionTime("2026-03-08T02:30", "en"))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
