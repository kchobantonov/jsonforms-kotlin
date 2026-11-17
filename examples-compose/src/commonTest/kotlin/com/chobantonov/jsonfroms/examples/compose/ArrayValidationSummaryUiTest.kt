package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayValidationSummaryUiTest {
    @Test fun materialArrayControlUsesBuiltInBulgarianMarkers() = builtInMarkers(false, false)
    @Test fun cupertinoArrayControlUsesBuiltInBulgarianMarkers() = builtInMarkers(true, false)
    @Test fun materialListUsesBuiltInBulgarianMarkers() = builtInMarkers(false, true)
    @Test fun cupertinoListUsesBuiltInBulgarianMarkers() = builtInMarkers(true, true)

    private fun builtInMarkers(cupertino: Boolean, list: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var showCount by mutableStateOf(true)
        var writes = 0
        setContent {
            val options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(showCount),
            )
            val ui = if (list) ListWithDetailElement("#/properties/rows", options = options)
                else ControlElement("#/properties/rows", options = options + mapOf(
                    "detail" to Json.parseToJsonElement(
                        """{"type":"Control","scope":"#/properties/name"}""").jsonObject,
                    "initCollapsed" to JsonPrimitive(true),
                ))
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/name", message = "host")),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithContentDescription("Item 1: 1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("1 грешка").assertExists()
        onNodeWithContentDescription("Елемент 1: 1 грешка").assertExists()
        runOnIdle { showCount = false }
        onNodeWithContentDescription("Съдържа грешки").assertExists()
        onNodeWithContentDescription("Елемент 1: съдържа грешки").assertExists()
        kotlin.test.assertEquals(0, writes)
    }

    @Test fun materialHideRequestsKeepOwnArrayError() = scenario(false)
    @Test fun cupertinoHideRequestsKeepOwnArrayError() = scenario(true)
    @Test fun materialTableHeaderKeepsOwnArrayError() = tableScenario(false)
    @Test fun cupertinoTableHeaderKeepsOwnArrayError() = tableScenario(true)
    @Test fun materialListWithDetailMarkersKeepOwnArrayError() = listScenario(false)
    @Test fun cupertinoListWithDetailMarkersKeepOwnArrayError() = listScenario(true)
    @Test fun materialEmptyExpandableExplainsOwnError() = emptyArray(false, "expandable")
    @Test fun cupertinoEmptyExpandableExplainsOwnError() = emptyArray(true, "expandable")
    @Test fun materialEmptyTableExplainsOwnError() = emptyArray(false, "table")
    @Test fun cupertinoEmptyTableExplainsOwnError() = emptyArray(true, "table")
    @Test fun materialEmptyListExplainsOwnError() = emptyArray(false, "list")
    @Test fun cupertinoEmptyListExplainsOwnError() = emptyArray(true, "list")

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","minItems":2,"items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"rows":[{"name":"x"}]}""")

    private fun emptyArray(cupertino: Boolean, mode: String) = runComposeUiTest {
        val empty = Json.parseToJsonElement("""{"rows":[]}""")
        var changes = 0
        setContent {
            val options = mapOf(
                "hideArraySummaryValidation" to JsonPrimitive(true),
                "showValidationIndicator" to JsonPrimitive(false),
            )
            val ui = if (mode == "list") ListWithDetailElement("#/properties/rows", options = options)
                else ControlElement("#/properties/rows", options = options +
                    if (mode == "table") mapOf("table" to JsonPrimitive(true)) else emptyMap())
            JsonForms(
                data = empty, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ },
            )
        }
        val ownError = onNodeWithText("ERR:minItems:/rows")
        ownError.assertExists()
        assertTrue(ownError.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        kotlin.test.assertEquals(0, changes)
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        fun control(indicator: Boolean? = null, count: Boolean = false, hide: Boolean = false) =
            ControlElement(
                scope = "#/properties/rows",
                options = mapOf(
                    "detail" to Json.parseToJsonElement("""{"type":"Control","scope":"#/properties/name"}""").jsonObject,
                    "hideAvatar" to JsonPrimitive(true),
                    "restrict" to JsonPrimitive(true),
                    "initCollapsed" to JsonPrimitive(true),
                    "showValidationIndicatorCount" to JsonPrimitive(count),
                    "hideArraySummaryValidation" to JsonPrimitive(hide),
                ) + (indicator?.let { mapOf("showValidationIndicator" to JsonPrimitive(it)) } ?: emptyMap()),
            )
        var ui by mutableStateOf(control())
        var globalIndicator by mutableStateOf<Boolean?>(null)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = globalIndicator?.let { mapOf("jsonformsExtended" to mapOf("showValidationIndicator" to it)) }
                    ?: emptyMap(),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/name", message = "host")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (key in setOf("array.validationIndicator", "array.item.validationIndicator") &&
                            locale == "bg"
                        ) "Грешки: $fallback" else fallback
                    },
                    translateError = { error, _, _ ->
                        "ERR:${error.keyword}:${error.instancePath}"
                    },
                ),
                onChange = { changes++ },
            )
        }
        onNodeWithText("ERR:minItems:/rows").assertExists()
        onNodeWithContentDescription("Delete item").assertIsNotEnabled()
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        runOnIdle { ui = control(count = true) }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithContentDescription("Item 1: 1 error").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Грешки: 1 грешка").assertExists()
        onNodeWithContentDescription("Грешки: Елемент 1: 1 грешка").assertExists()
        runOnIdle { locale = "en"; ui = control(count = true, hide = true) }
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: 1 error").assertExists()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { ui = control(indicator = false) }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: Contains errors").assertDoesNotExist()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { ui = control(indicator = true, hide = true) }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        onNodeWithContentDescription("Delete item").assertIsNotEnabled()
        runOnIdle { globalIndicator = false; ui = control() }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: Contains errors").assertDoesNotExist()
        runOnIdle { ui = control(indicator = true) }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        onNodeWithText("x").performClick()
        onNodeWithText("/rows/0/name", substring = true).assertExists()
        kotlin.test.assertEquals(0, changes)
    }

    private fun tableScenario(cupertino: Boolean) = runComposeUiTest {
        fun control(indicator: Boolean? = null, count: Boolean = false, hide: Boolean = false) =
            ControlElement(
                scope = "#/properties/rows",
                options = mapOf(
                    "table" to JsonPrimitive(true),
                    "restrict" to JsonPrimitive(true),
                    "showValidationIndicatorCount" to JsonPrimitive(count),
                    "hideArraySummaryValidation" to JsonPrimitive(hide),
                ) + (indicator?.let { mapOf("showValidationIndicator" to JsonPrimitive(it)) } ?: emptyMap()),
            )
        var ui by mutableStateOf(control())
        var globalIndicator by mutableStateOf<Boolean?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = globalIndicator?.let {
                    mapOf("jsonformsExtended" to mapOf("showValidationIndicator" to it))
                } ?: emptyMap(),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/name", message = "host")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ },
            )
        }
        onNodeWithText("ERR:minItems:/rows").assertExists()
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Delete item").assertIsNotEnabled()
        runOnIdle { ui = control(count = true) }
        onNodeWithContentDescription("1 error").assertExists()
        runOnIdle { ui = control(indicator = true, hide = true) }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { ui = control(indicator = false) }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { globalIndicator = false; ui = control() }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        runOnIdle { ui = control(indicator = true) }
        onNodeWithContentDescription("Contains errors").assertExists()
        kotlin.test.assertEquals(0, changes)
    }

    private fun listScenario(cupertino: Boolean) = runComposeUiTest {
        fun list(indicator: Boolean? = null, count: Boolean = false, hide: Boolean = false) =
            ListWithDetailElement(
                scope = "#/properties/rows",
                options = mapOf(
                    "showValidationIndicatorCount" to JsonPrimitive(count),
                    "hideArraySummaryValidation" to JsonPrimitive(hide),
                ) + (indicator?.let { mapOf("showValidationIndicator" to JsonPrimitive(it)) } ?: emptyMap()),
            )
        var ui by mutableStateOf(list())
        var globalIndicator by mutableStateOf<Boolean?>(null)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                config = globalIndicator?.let {
                    mapOf("jsonformsExtended" to mapOf("showValidationIndicator" to it))
                } ?: emptyMap(),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/name", message = "host")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (key in setOf("array.validationIndicator", "array.item.validationIndicator") &&
                            locale == "bg"
                        ) "Грешки: $fallback" else fallback
                    },
                    translateError = { error, _, _ -> "ERR:${error.keyword}:${error.instancePath}" },
                ),
                onChange = { changes++ },
            )
        }
        onNodeWithText("ERR:minItems:/rows").assertExists()
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        runOnIdle { ui = list(count = true) }
        onNodeWithContentDescription("1 error").assertExists()
        onNodeWithContentDescription("Item 1: 1 error").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Грешки: 1 грешка").assertExists()
        onNodeWithContentDescription("Грешки: Елемент 1: 1 грешка").assertExists()
        runOnIdle { locale = "en"; ui = list(indicator = true, hide = true) }
        onNodeWithContentDescription("1 error").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { ui = list(indicator = false) }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Item 1: Contains errors").assertDoesNotExist()
        onNodeWithText("ERR:minItems:/rows").assertExists()
        runOnIdle { globalIndicator = false; ui = list() }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        runOnIdle { ui = list(indicator = true) }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Item 1: Contains errors").assertExists()
        kotlin.test.assertEquals(0, changes)
    }
}
