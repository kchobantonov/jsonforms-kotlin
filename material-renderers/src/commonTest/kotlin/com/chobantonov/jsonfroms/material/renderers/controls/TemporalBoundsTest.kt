package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemporalBoundsTest {
    private fun schema(source: String) = JsonSchema(Json.parseToJsonElement(source).jsonObject)

    @Test fun boundFeedbackUsesDisplayFormat() {
        assertEquals("10.01.2025", formatTemporalBound("2025-01-10", TemporalBoundKind.Date,
            "DD.MM.YYYY"))
        assertEquals("09:30:30", formatTemporalBound("09:30:30", TemporalBoundKind.Time,
            "HH:mm:ss"))
        assertEquals("10.01.2025 09:30:30", formatTemporalBound("2025-01-10T09:30:30",
            TemporalBoundKind.DateTime, "DD.MM.YYYY HH:mm:ss"))
    }

    @Test fun dateBoundsPreserveInclusiveAndExclusiveDays() {
        val bounds = schema("""{"type":"string","format":"date","formatMinimum":"2026-10-01","formatExclusiveMaximum":"2026-10-19"}""")
        assertNull(temporalRangeViolation(TemporalValue(2026, 10, 1), bounds, TemporalBoundKind.Date))
        assertNull(temporalRangeViolation(TemporalValue(2026, 10, 18), bounds, TemporalBoundKind.Date))
        assertEquals("formatMinimum", temporalRangeViolation(
            TemporalValue(2026, 9, 30), bounds, TemporalBoundKind.Date)?.keyword)
        assertEquals("formatExclusiveMaximum", temporalRangeViolation(
            TemporalValue(2026, 10, 19), bounds, TemporalBoundKind.Date)?.keyword)
    }

    @Test fun minutePickerMustRespectSecondPrecisionInTimeBounds() {
        val bounds = schema("""{"type":"string","format":"time","formatMinimum":"09:30:30Z","formatExclusiveMaximum":"17:00:00Z"}""")
        assertEquals("formatMinimum", temporalRangeViolation(
            TemporalValue(hour = 9, minute = 30), bounds, TemporalBoundKind.Time)?.keyword)
        assertNull(temporalRangeViolation(TemporalValue(hour = 9, minute = 31), bounds, TemporalBoundKind.Time))
        assertEquals("formatExclusiveMaximum", temporalRangeViolation(
            TemporalValue(hour = 17), bounds, TemporalBoundKind.Time)?.keyword)
    }

    @Test fun offsetTimeBoundsAgreeWithSchemaComparison() {
        val bounds = schema("""{"type":"string","format":"time",
            "formatExclusiveMinimum":"10:30:00+01:00"}""")
        assertEquals("formatExclusiveMinimum", temporalRangeViolation(
            TemporalValue(hour = 9, minute = 30), bounds, TemporalBoundKind.Time)?.keyword)
        assertNull(temporalRangeViolation(TemporalValue(hour = 9, minute = 31),
            bounds, TemporalBoundKind.Time))
        assertEquals("09:30:00", formatTemporalBound("10:30:00+01:00",
            TemporalBoundKind.Time, "HH:mm:ss"))

        val fractional = schema("""{"type":"string","format":"time",
            "formatMinimum":"09:30:00.5Z"}""")
        assertEquals("formatMinimum", temporalRangeViolation(
            TemporalValue(hour = 9, minute = 30), fractional, TemporalBoundKind.Time)?.keyword)
        assertNull(temporalRangeViolation(TemporalValue(hour = 9, minute = 30, second = 1),
            fractional, TemporalBoundKind.Time))
    }

    @Test fun offsetDateTimeBoundUsesSameInstantAsValidator() {
        val bounds = schema("""{"type":"string","format":"date-time",
            "formatExclusiveMinimum":"2025-01-10T11:30:00+02:00"}""")
        val equal = parseDateTimeValue("2025-01-10T09:30:00Z",
            listOf(DEFAULT_DATE_TIME_SAVE_FORMAT))!!
        val later = parseDateTimeValue("2025-01-10T09:31:00Z",
            listOf(DEFAULT_DATE_TIME_SAVE_FORMAT))!!
        assertEquals("formatExclusiveMinimum", temporalRangeViolation(
            equal, bounds, TemporalBoundKind.DateTime)?.keyword)
        assertNull(temporalRangeViolation(later, bounds, TemporalBoundKind.DateTime))
    }

    @Test fun dateTimeChecksWholeCandidateAndDoesNotLeakBoundaryTime() {
        val bounds = schema("""{"type":"string","format":"date-time","formatMinimum":"2026-06-15T12:00:00Z"}""")
        val before = parseDateTimeValue("2026-06-15T09:00:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT))!!
        val after = parseDateTimeValue("2026-06-16T09:00:00Z", listOf(DEFAULT_DATE_TIME_SAVE_FORMAT))!!
        assertEquals("formatMinimum", temporalRangeViolation(before, bounds, TemporalBoundKind.DateTime)?.keyword)
        assertNull(temporalRangeViolation(after, bounds, TemporalBoundKind.DateTime))
    }

    @Test fun dateTimeDatePickerRequiresASelectableMinute() {
        val bounds = schema("""{"type":"string","format":"date-time",
            "formatMinimum":"2026-06-15T23:59:30","formatMaximum":"2026-06-17T00:00:30"}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15), bounds))
        assertEquals(true, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 16), bounds))
        assertEquals(true, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 17), bounds))
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 18), bounds))
        val noWholeMinute = schema("""{"type":"string","format":"date-time",
            "formatMinimum":"2026-06-15T09:30:30","formatMaximum":"2026-06-15T09:30:40"}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15), noWholeMinute))
        assertEquals(true, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15),
            noWholeMinute, retainedSecond = 37))
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15),
            noWholeMinute, retainedSecond = 45))
        val exclusiveEdge = schema("""{"type":"string","format":"date-time",
            "formatExclusiveMinimum":"2026-06-15T23:59:00"}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15), exclusiveEdge))

        val lastMinute = TemporalValue(2026, 6, 15, 23, 59)
        val fractionalMinimum = schema("""{"type":"string","format":"date-time",
            "formatMinimum":"${formatTemporal(lastMinute, DEFAULT_DATE_TIME_SAVE_FORMAT).removeSuffix("Z")}.5Z"}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2026, 6, 15),
            fractionalMinimum))
    }

    @Test fun dataReferencesAreNotMisrepresentedAsLiteralBounds() {
        val bounds = schema("""{"type":"string","format":"date","formatMinimum":{"${'$'}data":"1/from"}}""")
        assertNull(temporalRangeViolation(TemporalValue(2026, 1, 1), bounds, TemporalBoundKind.Date))
    }

    @Test fun absoluteAndRelativeDataBoundsResolveAgainstCurrentDocument() {
        val document = Json.parseToJsonElement("""{"start":"2025-01-10",
            "items":[{"start":"2025-02-10","end":"2025-02-09"}]}""")
        val absolute = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"/start"}}""")
        val relative = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"1/start"}}""")
        assertEquals("formatMinimum", temporalRangeViolation(TemporalValue(2025, 1, 9),
            absolute, TemporalBoundKind.Date, document, "items.0.end")?.keyword)
        assertEquals("2025-02-10", temporalRangeViolation(TemporalValue(2025, 2, 9),
            relative, TemporalBoundKind.Date, document, "items.0.end")?.bound)
        assertNull(temporalRangeViolation(TemporalValue(2025, 2, 10),
            relative, TemporalBoundKind.Date, document, "items.0.end"))
    }

    @Test fun nonStringDataBoundReportsKeywordAndDisablesPicker() {
        val document = Json.parseToJsonElement("""{"start":42}""")
        val nonString = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"1/start"}}""")
        val missing = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"1/missing"}}""")
        val outside = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"2/start"}}""")
        val value = TemporalValue(2025, 1, 1)
        val violation = temporalRangeViolation(value, nonString, TemporalBoundKind.Date,
            document, "end")
        assertEquals("formatMinimum", violation?.keyword)
        assertEquals(true, violation?.invalidBoundType)
        assertNull(temporalRangeViolation(value, missing, TemporalBoundKind.Date,
            document, "end"))
        assertEquals(true, temporalRangeViolation(value, outside, TemporalBoundKind.Date,
            document, "end")?.invalidBoundReference)
        val dateTime = schema("""{"type":"string","format":"date-time",
            "formatMinimum":{"${'$'}data":"1/start"}}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2025, 1, 1),
            dateTime, document, "end"))
    }

    @Test fun malformedDataReferenceHasDistinctRestrictedPickerFeedback() {
        val document = Json.parseToJsonElement("""{"start":"2025-01-10"}""")
        val malformed = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"/sta~2rt"}}""")
        val violation = temporalRangeViolation(TemporalValue(2025, 1, 10), malformed,
            TemporalBoundKind.Date, document, "end")
        assertEquals("formatMinimum", violation?.keyword)
        assertEquals(true, violation?.invalidBoundReference)
        assertEquals(false, violation?.invalidBoundType)
        val missing = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"/missing"}}""")
        assertNull(temporalRangeViolation(TemporalValue(2025, 1, 10), missing,
            TemporalBoundKind.Date, document, "end"))
    }

    @Test fun dataPointersPreserveEscapesEmptyKeysAndRelativePropertyNames() {
        val document = Json.parseToJsonElement("""{"":"2025-01-10",
            "a/b":"2025-01-12","dates":{"2025-01-14":{"end":"2025-01-13"}}}""")
        fun dynamic(reference: String) = schema("""{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"$reference"}}""")
        assertEquals("2025-01-10", temporalRangeViolation(TemporalValue(2025, 1, 9),
            dynamic("/"), TemporalBoundKind.Date, document, "dates.2025-01-14.end")?.bound)
        assertEquals("2025-01-12", temporalRangeViolation(TemporalValue(2025, 1, 9),
            dynamic("/a~1b"), TemporalBoundKind.Date, document, "dates.2025-01-14.end")?.bound)
        assertEquals("2025-01-14", temporalRangeViolation(TemporalValue(2025, 1, 13),
            dynamic("1#"), TemporalBoundKind.Date, document, "dates.2025-01-14.end")?.bound)
        assertEquals(true, temporalRangeViolation(TemporalValue(2025, 1, 9),
            dynamic("/a~2b"), TemporalBoundKind.Date, document,
            "dates.2025-01-14.end")?.invalidBoundReference)
    }

    @Test fun dynamicDateTimeBoundAlsoRestrictsCalendarStep() {
        val bounds = schema("""{"type":"string","format":"date-time",
            "formatMinimum":{"${'$'}data":"1/start"}}""")
        val document = Json.parseToJsonElement("""{"start":"2025-01-10T23:59:30",
            "end":"2025-01-10T09:00:00"}""")
        assertEquals(false, temporalDateHasSelectableMinute(TemporalValue(2025, 1, 10),
            bounds, document, "end"))
        assertEquals(true, temporalDateHasSelectableMinute(TemporalValue(2025, 1, 11),
            bounds, document, "end"))
    }

    @Test fun everyFormatBoundRejectsResolvedNonStringTypes() {
        val document = Json.parseToJsonElement("""{"number":42,"nothing":null,
            "array":[],"object":{}}""")
        for (keyword in listOf("formatMinimum", "formatExclusiveMinimum",
            "formatMaximum", "formatExclusiveMaximum")) {
            for (field in listOf("number", "nothing", "array", "object")) {
                val bounds = schema("""{"type":"string","format":"date",
                    "$keyword":{"${'$'}data":"/$field"}}""")
                val violation = temporalRangeViolation(TemporalValue(2025, 1, 7), bounds,
                    TemporalBoundKind.Date, document, "end")
                assertEquals(keyword, violation?.keyword)
                assertEquals(true, violation?.invalidBoundType)
            }
        }
    }
}
