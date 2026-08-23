package com.chobantonov.jsonfroms.material.renderers.controls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class NumericInputsTest {
    @Test
    fun validIntegerIsStoredAsJsonNumber() {
        val result = parseIntegerInput("45646")

        assertEquals(JsonPrimitive(45646L), result.data)
        assertFalse((result.data as JsonPrimitive).isString)
        assertNull(result.error)
    }

    @Test
    fun invalidIntegerIsStoredAsJsonStringAndReportedLocally() {
        val result = parseIntegerInput("not an integer")

        assertEquals(JsonPrimitive("not an integer"), result.data)
        assertTrue((result.data as JsonPrimitive).isString)
        assertEquals("Invalid integer.", result.error)
    }

    @Test
    fun validNumberIsStoredAsJsonNumber() {
        val result = parseNumberInput("50.5")

        assertEquals(JsonPrimitive(50.5), result.data)
        assertFalse((result.data as JsonPrimitive).isString)
        assertNull(result.error)
    }

    @Test
    fun invalidNumberIsStoredAsJsonStringAndReportedLocally() {
        val result = parseNumberInput("not a number")

        assertEquals(JsonPrimitive("not a number"), result.data)
        assertTrue((result.data as JsonPrimitive).isString)
        assertEquals("Invalid number.", result.error)
    }

    @Test
    fun blankNumericInputRemovesThePropertyValue() {
        assertNull(parseIntegerInput("").data)
        assertNull(parseNumberInput("").data)
    }

    @Test
    fun visibleWhitespaceIsInvalidDataRatherThanAnImplicitClear() {
        assertEquals(JsonPrimitive(" "), parseIntegerInput(" ").data)
        assertEquals("Invalid integer.", parseIntegerInput(" ").error)
        assertEquals(JsonPrimitive(" "), parseNumberInput(" ").data)
        assertEquals("Invalid number.", parseNumberInput(" ").error)
    }
}
