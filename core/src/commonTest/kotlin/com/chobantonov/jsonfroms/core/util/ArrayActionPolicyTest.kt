package com.chobantonov.jsonfroms.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive

class ArrayActionPolicyTest {
    @Test fun defaultsAndTopLevelHostFlags() {
        assertEquals(ArrayActionPolicy(false, false), resolveArrayActionPolicy(emptyMap(), emptyMap()))
        assertEquals(ArrayActionPolicy(true, false), resolveArrayActionPolicy(emptyMap(),
            mapOf("disableAdd" to true, "disableRemove" to false)))
        assertEquals(ArrayActionPolicy(false, true), resolveArrayActionPolicy(emptyMap(),
            mapOf("jsonformsExtended" to mapOf("disableAdd" to true), "disableRemove" to true)))
    }

    @Test fun explicitLocalFalseOverridesHostTrue() {
        assertEquals(ArrayActionPolicy(false, false), resolveArrayActionPolicy(
            mapOf("disableAdd" to JsonPrimitive(false), "disableRemove" to JsonPrimitive(false)),
            mapOf("disableAdd" to true, "disableRemove" to true)))
    }

    @Test fun invalidLocalValueFallsBackToTypedHostFlag() {
        assertEquals(ArrayActionPolicy(true, false), resolveArrayActionPolicy(
            mapOf("disableAdd" to JsonPrimitive("maybe")),
            mapOf("disableAdd" to JsonPrimitive(true))))
    }
}
