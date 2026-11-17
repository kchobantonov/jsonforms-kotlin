package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.actions.SetConfigAction
import com.chobantonov.jsonfroms.core.reducers.configReducer
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestrictPolicyTest {
    @Test fun authoredElementThenHostThenTrueDefault() {
        assertTrue(resolveRestrict(null, emptyMap()))
        assertFalse(resolveRestrict(null, mapOf("restrict" to false)))
        assertTrue(resolveRestrict(JsonPrimitive(true), mapOf("restrict" to false)))
        assertFalse(resolveRestrict(JsonPrimitive(false), mapOf("restrict" to true)))
        assertFalse(resolveRestrict(null, mapOf("restrict" to JsonPrimitive(false))))
        assertTrue(resolveRestrict(JsonPrimitive("invalid"), emptyMap()))
    }

    @Test fun coreDefaultsDoNotOverridePreferredRestrictDefault() {
        val absent = configReducer(null, SetConfigAction(emptyMap()))
        assertTrue(resolveRestrict(null, absent))
        val nestedAlias = configReducer(null, SetConfigAction(mapOf(
            "jsonformsExtended" to mapOf("restrict" to false),
        )))
        assertTrue(resolveRestrict(null, nestedAlias))
        val explicit = configReducer(null, SetConfigAction(mapOf("restrict" to false)))
        assertFalse(resolveRestrict(null, explicit))
    }
}
