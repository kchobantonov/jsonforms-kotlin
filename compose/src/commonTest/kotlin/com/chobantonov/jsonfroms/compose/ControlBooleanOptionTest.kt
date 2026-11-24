package com.chobantonov.jsonfroms.compose

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ControlBooleanOptionTest {
    @Test fun validLocalValueOverridesGlobal() {
        assertEquals(false, resolvedControlBooleanOption(
            mapOf("clearable" to JsonPrimitive(false)),
            mapOf("clearable" to JsonPrimitive(true)), "clearable"))
        assertEquals(true, resolvedControlBooleanOption(
            mapOf("clearable" to true), mapOf("clearable" to false), "clearable"))
    }

    @Test fun invalidLocalValueFallsThroughToGlobal() {
        assertEquals(false, resolvedControlBooleanOption(
            mapOf("clearable" to JsonPrimitive("false")),
            mapOf("clearable" to JsonPrimitive(false)), "clearable"))
        assertEquals(true, resolvedControlBooleanOption(
            mapOf("clearable" to JsonPrimitive(0)),
            mapOf("clearable" to true), "clearable"))
        assertNull(resolvedControlBooleanOption(
            mapOf("clearable" to JsonPrimitive("false")),
            mapOf("clearable" to JsonPrimitive("true")), "clearable"))
    }
}
