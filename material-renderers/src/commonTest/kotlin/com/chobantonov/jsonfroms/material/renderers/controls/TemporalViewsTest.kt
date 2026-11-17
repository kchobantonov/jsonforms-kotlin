package com.chobantonov.jsonfroms.material.renderers.controls

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalViewsTest {
    @Test fun authoredTimeColumnsKeepOrderAndIgnoreOtherViews() {
        val raw = Json.parseToJsonElement("""["year","seconds","hours","seconds","day"]""")
        assertEquals(listOf(TimeView.Seconds, TimeView.Hours), resolveTimeViews(raw, "h:mm A"))
    }

    @Test fun dateOnlyArrayUsesTimeDisplayFormatForTimeHalf() {
        val raw = Json.parseToJsonElement("""["year","month","day"]""")
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes, TimeView.Seconds),
            resolveTimeViews(raw, "MM/DD/YYYY HH:mm:ss"))
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes),
            resolveTimeViews(raw, DEFAULT_TIME_DISPLAY_FORMAT))
    }

    @Test fun absentAuthoredViewsCanUseDisplayFallback() {
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes, TimeView.Seconds),
            resolveTimeViews(null, "HH:mm:ss"))
        assertEquals(null, resolveTimeViews(null, DEFAULT_TIME_DISPLAY_FORMAT))
        assertEquals(null, resolveTimeViews(null, "[seconds] HH:mm"))
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes),
            defaultTimeViews(DEFAULT_DATE_TIME_DISPLAY_FORMAT))
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes, TimeView.Seconds),
            defaultTimeViews("YYYY-MM-DD HH:mm:ss"))
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes), defaultTimeViews("YYYY-MM-DD"))
        assertEquals(listOf(TimeView.Hours, TimeView.Minutes),
            defaultTimeViews("[seconds] HH:mm"))
    }

    @Test fun finestAuthoredDateViewWinsAndUnusableViewsFallBackToDisplayFormat() {
        assertEquals(DateViewMode.Year,
            resolveDateViewMode(Json.parseToJsonElement("""["year","hours"]"""), "MM/DD/YYYY"))
        assertEquals(DateViewMode.Month,
            resolveDateViewMode(Json.parseToJsonElement("""["year","month"]"""), "MM/DD/YYYY"))
        assertEquals(DateViewMode.Day,
            resolveDateViewMode(Json.parseToJsonElement("""["year","day"]"""), "YYYY-MM"))
        assertEquals(DateViewMode.Month,
            resolveDateViewMode(Json.parseToJsonElement("""["hours"]"""), "YYYY-MM"))
        assertEquals(DateViewMode.Year, resolveDateViewMode(null, "YYYY"))
        assertEquals(DateViewMode.Year, resolveDateViewMode(null, "YYYY [MM/DD]"))
    }

    @Test fun coarseDateDraftPreservesFinerDateAndClampsInvalidDay() {
        val draft = DateViewsDraft(TemporalValue(2024, 2, 29))
        draft.changeYear("2025")
        assertEquals(TemporalValue(2025, 2, 28), draft.selected(DateViewMode.Year))
        draft.changeMonth("04")
        assertEquals(TemporalValue(2025, 4, 29), draft.selected(DateViewMode.Month))
        draft.changeMonth("13")
        assertEquals(null, draft.selected(DateViewMode.Month))
    }

    @Test fun yearPagesStayWithinFourDigitDateRange() {
        assertEquals(2017, yearPageStart(2025))
        assertEquals((2017..2028).toList(), yearsOnPage(yearPageStart(2025)))
        assertEquals(1, yearPageStart(1))
        assertEquals(9997, yearPageStart(9999))
        assertEquals(listOf(9997, 9998, 9999), yearsOnPage(yearPageStart(9999)))
    }
}
