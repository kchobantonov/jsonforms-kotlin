package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive

class AccordionOptionsTest {
    @Test fun localFalseOverridesGlobalTrue() {
        val config = mapOf("jsonformsExtended" to mapOf("collapsed" to true))
        assertFalse(resolveAccordionCollapsed(mapOf("collapsed" to JsonPrimitive(false)), config))
        assertTrue(resolveAccordionCollapsed(emptyMap(), config))
        assertFalse(resolveAccordionCollapsed(emptyMap(), emptyMap()))
    }
}
