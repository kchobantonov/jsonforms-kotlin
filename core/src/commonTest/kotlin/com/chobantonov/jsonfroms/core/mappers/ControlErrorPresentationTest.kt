package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class ControlErrorPresentationTest {
    private val required = ErrorObject(keyword = "required", message = "missing")
    private val minimum = ErrorObject(keyword = "minimum", message = "too small")
    private val hostRequired = ErrorObject(keyword = "required", message = "server")
    private val errors = listOf(required, minimum, hostRequired)

    @Test fun filteringIsDisabledByDefaultAndLocalFalseOverridesGlobalTrue() {
        assertEquals(errors, filterControlErrorsBeforeTouch(errors, emptyMap()))
        assertEquals(errors, filterControlErrorsBeforeTouch(errors,
            mapOf("enableFilterErrorsBeforeTouch" to true) +
                mapOf("enableFilterErrorsBeforeTouch" to JsonPrimitive(false))))
    }

    @Test fun enabledWithoutKeywordsSuppressesAllInlineText() {
        assertEquals(emptyList(), filterControlErrorsBeforeTouch(errors,
            mapOf("enableFilterErrorsBeforeTouch" to true)))
        assertEquals(emptyList(), filterControlErrorsBeforeTouch(errors,
            mapOf(
                "enableFilterErrorsBeforeTouch" to JsonPrimitive(true),
                "filterErrorKeywordsBeforeTouch" to JsonArray(emptyList()),
            )))
    }

    @Test fun quotedEnableFlagKeepsInlineErrorsVisible() {
        assertEquals(errors, filterControlErrorsBeforeTouch(errors,
            mapOf("enableFilterErrorsBeforeTouch" to JsonPrimitive("true"))))
    }

    @Test fun keywordListSuppressesMatchingSchemaAndHostErrorsOnly() {
        val options = mapOf<String, Any?>(
            "enableFilterErrorsBeforeTouch" to JsonPrimitive(true),
            "filterErrorKeywordsBeforeTouch" to JsonArray(listOf(JsonPrimitive("required"))),
        )
        assertEquals(listOf(minimum), filterControlErrorsBeforeTouch(errors, options))
        assertEquals(listOf(required, hostRequired), filterControlErrorsBeforeTouch(errors,
            mapOf("enableFilterErrorsBeforeTouch" to true,
                "filterErrorKeywordsBeforeTouch" to listOf("minimum"))))
    }
}
