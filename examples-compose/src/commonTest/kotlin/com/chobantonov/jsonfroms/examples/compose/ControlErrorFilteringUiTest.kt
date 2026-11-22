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
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
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
    @Test fun materialCategoryCountsUntouchedErrors() = categoryCountsUntouchedErrors(false)
    @Test fun cupertinoCategoryCountsUntouchedErrors() = categoryCountsUntouchedErrors(true)
    @Test fun materialLiveHostErrorsKeepValidityWhileFiltering() = liveHostErrors(false)
    @Test fun cupertinoLiveHostErrorsKeepValidityWhileFiltering() = liveHostErrors(true)
    @Test fun materialDefaultShowsRequired() = scenario(false, "default")
    @Test fun cupertinoDefaultShowsRequired() = scenario(true, "default")
    @Test fun materialLocalFalseRestoresRequired() = scenario(false, "localOff")
    @Test fun cupertinoLocalFalseRestoresRequired() = scenario(true, "localOff")
    @Test fun materialEnabledWithoutKeywordsHidesAllInline() = scenario(false, "all")
    @Test fun cupertinoEnabledWithoutKeywordsHidesAllInline() = scenario(true, "all")
    @Test fun materialKeywordFilterIncludesHostErrors() = scenario(false, "keywords")
    @Test fun cupertinoKeywordFilterIncludesHostErrors() = scenario(true, "keywords")

    private fun categoryCountsUntouchedErrors(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"string"},"b":{"type":"string"}}}"""
        ).jsonObject)
        val indicator = mapOf(
            "showValidationIndicator" to JsonPrimitive(true),
            "showValidationIndicatorCount" to JsonPrimitive(true),
        )
        val ui = CategorizationElement(elements = listOf(
            CategoryElement(label = "First", options = indicator,
                elements = listOf(ControlElement("#/properties/a"))),
            CategoryElement(label = "Second", options = indicator,
                elements = listOf(ControlElement("#/properties/b"))),
        ))
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var host by mutableStateOf(listOf(ErrorObject(instancePath = "/b", keyword = "server", message = "server error")))
        var locale by mutableStateOf("en")
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = host, validationMode = mode,
                config = mapOf("enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("required")),
                i18n = JsonFormsI18nState(locale = locale,
                    translateError = { error, _, _ -> "ERR:${error.keyword}" }),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("First, 1 error").assertExists()
        onNodeWithContentDescription("Second, 1 error").assertExists()
        onNodeWithText("ERR:required").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("First, 1 грешка").assertExists()
        onNodeWithContentDescription("Second, 1 грешка").assertExists()
        runOnIdle { mode = ValidationMode.ValidateAndHide }
        onNodeWithContentDescription("First, 1 грешка").assertDoesNotExist()
        onNodeWithContentDescription("Second, 1 грешка").assertExists()
        runOnIdle { mode = ValidationMode.NoValidation }
        onNodeWithContentDescription("First, 1 грешка").assertDoesNotExist()
        onNodeWithContentDescription("Second, 1 грешка").assertExists()
        runOnIdle { mode = ValidationMode.ValidateAndShow; host = emptyList() }
        onNodeWithContentDescription("First, 1 грешка").assertExists()
        onNodeWithContentDescription("Second, 1 грешка").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"a":"ok"}""") }
        onNodeWithContentDescription("First, 1 грешка").assertDoesNotExist()
        assertEquals(0, writes)
    }

    private fun liveHostErrors(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"string"}}}"""
        ).jsonObject)
        val ui = GroupElement(label = "Details", options = mapOf(
            "showValidationIndicator" to JsonPrimitive(true),
            "showValidationIndicatorCount" to JsonPrimitive(true),
        ), elements = listOf(ControlElement("#/properties/a")))
        var host by mutableStateOf(listOf(
            ErrorObject(instancePath = "/a", keyword = "required", message = "host required"),
            ErrorObject(instancePath = "/a", keyword = "server", message = "server one"),
            ErrorObject(instancePath = "/a", keyword = "remote", message = "remote two"),
        ))
        var locale by mutableStateOf("en")
        var filtering by mutableStateOf(true)
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var validityStatus: CombinedValidityStatus? = null
        var schemaErrors = -1
        var additionalErrors = -1
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = host, validationMode = mode,
                config = mapOf("enableFilterErrorsBeforeTouch" to filtering,
                    "filterErrorKeywordsBeforeTouch" to listOf("required")),
                i18n = JsonFormsI18nState(locale = locale, translateError = { error, _, _ ->
                    if (error.keyword == "required" && error.message != "host required" &&
                        error.message != "new required") "$locale:schema required"
                    else "$locale:${error.message}"
                }),
                onChange = { writes++ },
                onValidityChange = {
                    validityStatus = it.status
                    schemaErrors = it.schemaErrorCount
                    additionalErrors = it.additionalErrorCount
                })
        }
        onNodeWithContentDescription("4 errors").assertExists()
        onNodeWithText("en:server one", substring = true).assertExists()
        onNodeWithText("en:remote two", substring = true).assertExists()
        onNodeWithText("en:host required", substring = true).assertDoesNotExist()
        onNodeWithText("en:schema required", substring = true).assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Invalid, validityStatus)
        assertEquals(1, schemaErrors)
        assertEquals(3, additionalErrors)
        runOnIdle { locale = "bg" }
        onNodeWithText("bg:server one", substring = true).assertExists()
        onNodeWithText("bg:host required", substring = true).assertDoesNotExist()
        runOnIdle {
            host = listOf(
                ErrorObject(instancePath = "/a", keyword = "required", message = "new required"),
                ErrorObject(instancePath = "/a", keyword = "server", message = "new server"),
            )
        }
        onNodeWithContentDescription("3 грешки").assertExists()
        onNodeWithText("bg:new server", substring = true).assertExists()
        onNodeWithText("bg:new required", substring = true).assertDoesNotExist()
        onNodeWithText("en:server one", substring = true).assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Invalid, validityStatus)
        assertEquals(1, schemaErrors)
        assertEquals(2, additionalErrors)
        runOnIdle { filtering = false }
        onNodeWithText("bg:schema required", substring = true).assertExists()
        onNodeWithText("bg:new required", substring = true).assertExists()
        onNodeWithText("bg:new server", substring = true).assertExists()
        runOnIdle { mode = ValidationMode.ValidateAndHide }
        onNodeWithText("bg:schema required", substring = true).assertDoesNotExist()
        onNodeWithText("bg:new required", substring = true).assertExists()
        onNodeWithText("bg:new server", substring = true).assertExists()
        assertEquals(1, schemaErrors)
        assertEquals(2, additionalErrors)
        runOnIdle { filtering = true }
        onNodeWithText("bg:new required", substring = true).assertDoesNotExist()
        onNodeWithText("bg:new server", substring = true).assertExists()
        runOnIdle { mode = ValidationMode.NoValidation }
        onNodeWithText("bg:new required", substring = true).assertDoesNotExist()
        onNodeWithText("bg:new server", substring = true).assertExists()
        assertEquals(CombinedValidityStatus.Invalid, validityStatus)
        assertEquals(0, schemaErrors)
        assertEquals(2, additionalErrors)
        assertEquals(0, writes)
    }

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
