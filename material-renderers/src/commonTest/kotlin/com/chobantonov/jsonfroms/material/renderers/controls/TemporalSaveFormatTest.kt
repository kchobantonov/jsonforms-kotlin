package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalSaveFormatTest {
    @Test fun schemaFormatsRejectIncompatibleStoragePatterns() {
        assertEquals(TemporalSaveFormatResolution(DEFAULT_DATE_SAVE_FORMAT, true),
            resolveTemporalSaveFormat("date", "YYYY-MM", DEFAULT_DATE_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution(DEFAULT_TIME_SAVE_FORMAT, true),
            resolveTemporalSaveFormat("time", "HH:mm", DEFAULT_TIME_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution(DEFAULT_DATE_TIME_SAVE_FORMAT, true),
            resolveTemporalSaveFormat("date-time", "YYYY-MM-DD HH:mm", DEFAULT_DATE_TIME_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution(DEFAULT_DATE_SAVE_FORMAT, true),
            resolveTemporalSaveFormat("date", "YYYY-MM-[31]", DEFAULT_DATE_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution(DEFAULT_TIME_SAVE_FORMAT, true),
            resolveTemporalSaveFormat("time", "HH:mm:ss[+25:99]", DEFAULT_TIME_SAVE_FORMAT))
    }

    @Test fun compatiblePatternsAndUiOnlyCoarseStorageRemainAuthored() {
        assertEquals(TemporalSaveFormatResolution("YYYY-MM-DD", false),
            resolveTemporalSaveFormat("date", "YYYY-MM-DD", DEFAULT_DATE_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution("HH:mm:ssZ", false),
            resolveTemporalSaveFormat("time", "HH:mm:ssZ", DEFAULT_TIME_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution("YYYY-MM-DDTHH:mm:ssZ", false),
            resolveTemporalSaveFormat("date-time", "YYYY-MM-DDTHH:mm:ssZ", DEFAULT_DATE_TIME_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution("YYYY-MM", false),
            resolveTemporalSaveFormat(null, "YYYY-MM", DEFAULT_DATE_SAVE_FORMAT))
        assertEquals(TemporalSaveFormatResolution(DEFAULT_DATE_SAVE_FORMAT, false),
            resolveTemporalSaveFormat("date", null, DEFAULT_DATE_SAVE_FORMAT))
    }
}
