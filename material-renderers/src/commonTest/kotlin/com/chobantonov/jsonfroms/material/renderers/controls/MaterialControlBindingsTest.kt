package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaterialControlBindingsTest {
    @Test
    fun laysOutMultipleErrorsUsingAvailableHorizontalSpace() {
        assertEquals(
            "New error #1 New error #2 New error #3",
            "New error #1\nNew error #2\nNew error #3".toMaterialSupportingText(),
        )
    }

    @Test
    fun unmatchedEnumQueryIsReportedAsInvalid() {
        assertEquals("Select a valid option.", unmatchedEnumQueryError("Unknown", "Known"))
        assertEquals("Select a valid option.", unmatchedEnumQueryError("Unknown", null))
        assertEquals("Select a valid option.", unmatchedEnumQueryError(" ", null))
        assertNull(unmatchedEnumQueryError("Known", "Known"))
        assertNull(unmatchedEnumQueryError("", null))
    }
}
