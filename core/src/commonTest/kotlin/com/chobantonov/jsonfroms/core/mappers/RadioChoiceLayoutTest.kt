package com.chobantonov.jsonfroms.core.mappers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RadioChoiceLayoutTest {
    @Test fun nestedRadioDefaultAndLocalFalseHaveDefinedPrecedence() {
        val config = mapOf("vertical" to true,
            "jsonformsExtended" to mapOf("radio" to mapOf("vertical" to true)))
        assertTrue(radioChoiceVertical(config, emptyMap()))
        assertFalse(radioChoiceVertical(config, mapOf("vertical" to JsonPrimitive(false))))
        assertFalse(checkboxChoiceVertical(emptyMap()))
    }

    @Test fun plainGlobalAndStringAliasesDoNotStackChoices() {
        val config = mapOf("vertical" to true)
        assertFalse(radioChoiceVertical(config, emptyMap()))
        assertFalse(radioChoiceVertical(config, mapOf("vertical" to JsonPrimitive("true"))))
        assertFalse(checkboxChoiceVertical(mapOf("vertical" to "true")))
        assertTrue(checkboxChoiceVertical(mapOf("vertical" to JsonPrimitive(true))))
    }

    @Test fun jsonObjectNestedConfigurationIsReadWithoutTupleOrPlainGlobalFallback() {
        val config = mapOf("jsonformsExtended" to Json.parseToJsonElement(
            """{"radio":{"vertical":true},"tuple":{"vertical":false}}""").jsonObject)
        assertTrue(radioChoiceVertical(config, emptyMap()))
        assertFalse(checkboxChoiceVertical(emptyMap()))
    }
}
