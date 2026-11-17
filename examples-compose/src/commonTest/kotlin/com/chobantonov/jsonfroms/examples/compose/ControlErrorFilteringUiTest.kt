package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ControlErrorFilteringUiTest {
    @Test fun materialDefaultShowsRequired() = scenario(false, "default")
    @Test fun cupertinoDefaultShowsRequired() = scenario(true, "default")
    @Test fun materialLocalFalseRestoresRequired() = scenario(false, "localOff")
    @Test fun cupertinoLocalFalseRestoresRequired() = scenario(true, "localOff")
    @Test fun materialEnabledWithoutKeywordsHidesAllInline() = scenario(false, "all")
    @Test fun cupertinoEnabledWithoutKeywordsHidesAllInline() = scenario(true, "all")
    @Test fun materialKeywordFilterIncludesHostErrors() = scenario(false, "keywords")
    @Test fun cupertinoKeywordFilterIncludesHostErrors() = scenario(true, "keywords")

    private fun scenario(cupertino: Boolean, mode: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"string"}}}"""
        ).jsonObject)
        val ui = GroupElement(
            label = "Details",
            options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            ),
            elements = listOf(ControlElement(
                scope = "#/properties/a",
                options = if (mode == "localOff") mapOf("enableFilterErrorsBeforeTouch" to JsonPrimitive(false)) else emptyMap(),
            )),
        )
        val host = when (mode) {
            "all" -> listOf(ErrorObject(instancePath = "/a", keyword = "server", message = "host server"))
            "keywords" -> listOf(
                ErrorObject(instancePath = "/a", keyword = "required", message = "host required"),
                ErrorObject(instancePath = "/a", keyword = "server", message = "host server"),
            )
            else -> emptyList()
        }
        val config = when (mode) {
            "all", "localOff" -> mapOf<String, Any?>("enableFilterErrorsBeforeTouch" to true)
            "keywords" -> mapOf<String, Any?>(
                "enableFilterErrorsBeforeTouch" to true,
                "filterErrorKeywordsBeforeTouch" to listOf("required"),
            )
            else -> emptyMap()
        }
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = config, additionalErrors = host,
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required" && error.message != "host required") "schema required"
                    else error.message
                }),
            )
        }
        onNodeWithContentDescription(when (mode) {
            "default", "localOff" -> "1 error"
            "all" -> "2 errors"
            else -> "3 errors"
        }).assertExists()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(0, changes)
        when (mode) {
            "default", "localOff" -> onNodeWithText("schema required").assertExists()
            "all" -> {
                onNodeWithText("schema required").assertDoesNotExist()
                onNodeWithText("host server").assertDoesNotExist()
            }
            else -> {
                onNodeWithText("schema required").assertDoesNotExist()
                onNodeWithText("host required").assertDoesNotExist()
                onNodeWithText("host server").assertExists()
            }
        }
    }
}
