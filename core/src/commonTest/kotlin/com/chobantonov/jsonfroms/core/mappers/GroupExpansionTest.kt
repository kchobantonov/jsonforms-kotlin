package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

class GroupExpansionTest {
    @Test fun mixedDefaultsToCollapsibleAndOpen() {
        val result = resolveMixedExpansionOptions(emptyMap(), emptyMap())
        assertTrue(result.collapsible)
        assertFalse(result.collapsed)
    }

    @Test fun mixedLocalFalseOverridesGlobalTrue() {
        val result = resolveMixedExpansionOptions(
            options = mapOf("collapsible" to JsonPrimitive(false), "collapsed" to JsonPrimitive(false)),
            config = mapOf("jsonformsExtended" to mapOf("collapsible" to true, "collapsed" to true)),
        )
        assertFalse(result.collapsible)
        assertFalse(result.collapsed)
    }

    @Test fun mixedInvalidCollapsedIdentifiesItsFrame() {
        val result = resolveMixedExpansionOptions(mapOf("collapsed" to JsonNull), emptyMap())
        assertFalse(result.collapsed)
        assertEquals("section.invalidCollapsed", result.diagnostic?.code)
        assertEquals("Mixed", result.diagnostic?.details?.get("elementType"))
    }

    @Test fun defaultsAreOpenAndNotCollapsible() {
        val result = resolveGroupExpansionOptions(emptyMap(), emptyMap())
        assertFalse(result.collapsible)
        assertFalse(result.collapsed)
        assertFalse(result.showDataIndicator)
        assertNull(result.diagnostic)
    }

    @Test fun localFalseOverridesGlobalTrue() {
        val result = resolveGroupExpansionOptions(
            options = mapOf("collapsible" to JsonPrimitive(false), "collapsed" to JsonPrimitive(false)),
            config = mapOf("jsonformsExtended" to mapOf("collapsible" to true, "collapsed" to true)),
        )
        assertFalse(result.collapsible)
        assertFalse(result.collapsed)
        assertNull(result.diagnostic)
    }

    @Test fun namespacedConfigAppliesWithoutLocalValue() {
        val result = resolveGroupExpansionOptions(
            options = emptyMap(),
            config = mapOf("jsonformsExtended" to mapOf("collapsible" to true, "collapsed" to true)),
        )
        assertTrue(result.collapsible)
        assertTrue(result.collapsed)
    }

    @Test fun dataIndicatorLocalFalseOverridesNamespacedTrue() {
        val result = resolveGroupExpansionOptions(
            options = mapOf("showDataIndicator" to JsonPrimitive(false)),
            config = mapOf("jsonformsExtended" to mapOf("showDataIndicator" to true)),
        )
        assertFalse(result.showDataIndicator)
        assertTrue(resolveGroupExpansionOptions(emptyMap(),
            mapOf("jsonformsExtended" to mapOf("showDataIndicator" to true))).showDataIndicator)
    }

    @Test fun invalidCollapsedIsDiagnosedWithoutTruthyCoercion() {
        for (invalid in listOf(JsonNull, JsonPrimitive("true"), JsonPrimitive(1))) {
            val result = resolveGroupExpansionOptions(
                options = mapOf("collapsed" to invalid),
                config = mapOf("jsonformsExtended" to mapOf("collapsible" to true, "collapsed" to true)),
            )
            assertFalse(result.collapsed)
            assertEquals("section.invalidCollapsed", result.diagnostic?.code)
        }
    }
}
