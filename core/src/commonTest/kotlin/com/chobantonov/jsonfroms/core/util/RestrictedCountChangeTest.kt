package com.chobantonov.jsonfroms.core.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class RestrictedCountChangeTest {
    @Test fun boundsCheckResultAndPermitRepair() {
        assertFalse(acceptsRestrictedCountChange(1, 0, 1, 3, true))
        assertFalse(acceptsRestrictedCountChange(3, 4, 1, 3, true))
        assertTrue(acceptsRestrictedCountChange(4, 3, 1, 3, true))
        assertTrue(acceptsRestrictedCountChange(0, 1, 2, 3, true))
        assertTrue(acceptsRestrictedCountChange(3, 3, 3, 3, true))
        assertTrue(acceptsRestrictedCountChange(1, 0, 1, 3, false))
    }

    @Test fun appendRechecksSnapshotAndMaximum() {
        val one = JsonArray(listOf(JsonPrimitive("a")))
        val two = appendArrayItemIfAllowed(one, one, JsonPrimitive("b"), 2, true) as JsonArray
        assertEquals(2, two.size)
        assertEquals(two, appendArrayItemIfAllowed(two, two, JsonPrimitive("c"), 2, true))
        assertEquals(two, appendArrayItemIfAllowed(two, one, JsonPrimitive("c"), 2, true))
        assertEquals(two, appendArrayItemIfAllowed(JsonArray(two.toList()), two,
            JsonPrimitive("c"), 3, true))
        assertEquals(3, (appendArrayItemIfAllowed(two, two, JsonPrimitive("c"), 2, false) as JsonArray).size)
        assertEquals(JsonArray(listOf(JsonPrimitive("a"))),
            appendArrayItemIfAllowed(null, null, JsonPrimitive("a"), 1, true))
        val invalid = JsonPrimitive(7)
        assertEquals(JsonArray(listOf(JsonPrimitive("a"))),
            appendArrayItemIfAllowed(invalid, invalid, JsonPrimitive("a"), 1, true))
        assertEquals(invalid, appendArrayItemIfAllowed(invalid, invalid, JsonPrimitive("a"), 0, true))
    }
}
