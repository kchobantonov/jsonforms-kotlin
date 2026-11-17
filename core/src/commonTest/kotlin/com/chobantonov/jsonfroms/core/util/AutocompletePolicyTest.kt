package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutocompletePolicyTest {
    @Test fun localThenHostThenOptInDefault() {
        assertFalse(resolveAutocomplete(null, emptyMap()))
        assertTrue(resolveAutocomplete(JsonPrimitive(true), emptyMap()))
        assertFalse(resolveAutocomplete(JsonPrimitive(false), mapOf("autocomplete" to true)))
        assertTrue(resolveAutocomplete(null, mapOf("autocomplete" to true)))
        assertFalse(resolveAutocomplete(JsonPrimitive("true"), emptyMap()))
        assertFalse(resolveAutocomplete(null, mapOf("autocomplete" to "true")))
    }
}
