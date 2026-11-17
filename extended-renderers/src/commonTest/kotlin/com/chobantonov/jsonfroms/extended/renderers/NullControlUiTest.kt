package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.DispatchCell
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsChange
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class NullControlUiTest {
    @Test fun materialDistinguishesMissingAndNullWithRequiredError() = scenario(false)
    @Test fun cupertinoDistinguishesMissingAndNullWithRequiredError() = scenario(true)
    @Test fun materialNullCellUsesCellRegistry() = cellScenario(false)
    @Test fun cupertinoNullCellUsesCellRegistry() = cellScenario(true)
    @Test fun materialNullFiltersRequiredButKeepsHostError() = filteredError(false)
    @Test fun cupertinoNullFiltersRequiredButKeepsHostError() = filteredError(true)
    @Test fun materialNullInitialFocus() = initialFocus(false)
    @Test fun cupertinoNullInitialFocus() = initialFocus(true)
    @Test fun materialNullHelpAndError() = helpAndError(false)
    @Test fun cupertinoNullHelpAndError() = helpAndError(true)
    @Test fun materialTableUsesNullCell() = tableCell(false)
    @Test fun cupertinoTableUsesNullCell() = tableCell(true)
    @Test fun materialNullBuiltinLocales() = builtinLocales(false)
    @Test fun cupertinoNullBuiltinLocales() = builtinLocales(true)
    @Test fun materialInvalidImportedNullValue() = invalidImported(false)
    @Test fun cupertinoInvalidImportedNullValue() = invalidImported(true)

    private fun invalidImported(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"a":{"type":"null"}}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        val checkbox = onAllNodes(hasClickAction()).onFirst()
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        assertTrue(checkbox.fetchSemanticsNode().config[SemanticsProperties.Error]
            .contains("Invalid value: {\"raw\":true}"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("Invalid value: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"a":42}""")
        }
        onNodeWithText("Невалидна стойност: 42").assertExists()
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithText("Зададено на null").assertExists()
        runOnIdle {
            assertEquals(1, changes)
            assertEquals(JsonNull, (data as JsonObject)["a"])
        }
    }

    private fun builtinLocales(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"a":{"type":"null"}}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Not set").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Не е зададено").assertExists()
        assertEquals(0, changes)
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithText("Зададено на null").assertExists()
        assertEquals(Json.parseToJsonElement("""{"a":null}"""), data)
        runOnIdle { locale = "de-DE" }
        onNodeWithText("Set to null").assertExists()
        assertEquals(1, changes)
    }

    private fun tableCell(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{
            "rows":{"type":"array","items":{"type":"object","required":["a"],
                "properties":{"a":{"type":"null"}}}}}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{}]}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required error" else error.message
                }),
                onChange = { data = it.data })
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithContentDescription("required error").assertExists()
        onNodeWithText("required error").assertDoesNotExist()
        onNode(hasClickAction() and hasContentDescription("Not set", substring = true)).performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"a":null}]}"""), data)
        onNodeWithText("Set to null").assertExists()
    }

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = JsonSchema(Json.parseToJsonElement(
                    """{"type":"object","properties":{"a":{"type":"null"}}}""").jsonObject),
                uischema = ControlElement("#/properties/a", options = mapOf("focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ },
            )
        }
        onAllNodes(hasClickAction()).onFirst().assertIsFocused()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object","required":["a"],
            "properties":{"a":{"type":"null","title":"Null value","description":"Set an explicit null."},
            "other":{"type":"string"}}}""").jsonObject)
        var showUnfocused by mutableStateOf(JsonPrimitive(false))
        var hideMarker by mutableStateOf(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"other":"x"}"""), schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/a", options = mapOf(
                        "showUnfocusedDescription" to showUnfocused,
                        "hideRequiredAsterisk" to hideMarker)),
                    ControlElement("#/properties/other"))),
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true)),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        val checkbox = onAllNodes(hasClickAction()).onFirst()
        onNodeWithText("Set an explicit null.").assertDoesNotExist()
        onNodeWithText("Null value*").assertDoesNotExist()
        assertEquals("Required field", checkbox.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(checkbox.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        checkbox.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Set an explicit null.").assertExists()
        assertEquals("Required field. Set an explicit null.",
            checkbox.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { locale = "bg-BG"; hideMarker = JsonPrimitive(false) }
        onNodeWithText("Null value*").assertExists()
        assertEquals("Задължително поле. Set an explicit null.",
            checkbox.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        onAllNodes(hasSetTextAction()).onLast()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Set an explicit null.").assertDoesNotExist()
        runOnIdle { showUnfocused = JsonPrimitive(true) }
        onNodeWithText("Set an explicit null.").assertExists()
        assertEquals(0, changes)
    }

    private fun filteredError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"null"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("required"),
                ),
                additionalErrors = listOf(ErrorObject(instancePath = "/a", keyword = "server", message = "server error")),
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required error" else error.message
                }),
            )
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("server error").assertExists()
        onNodeWithText("required error").assertDoesNotExist()
    }

    private fun cellScenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"null"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        val cellHost = ComposeJsonFormsRenderer { props ->
            DispatchCell(ComposeJsonFormsCellProps(
                state = props.state,
                dispatch = props.dispatch,
                uischema = props.uischema as ControlElement,
                schema = props.schema,
                rootSchema = props.rootSchema,
                path = props.path,
                enabled = props.enabled,
                renderers = props.renderers,
                cells = props.cells,
                id = props.id,
                config = props.config,
            ))
        }
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = listOf(JsonFormsRendererRegistryEntry({ _, _, _ -> 1000 }, cellHost)),
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("ERR:required:").assertDoesNotExist()
        assertTrue(onAllNodes(hasClickAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].any { it.contains("ERR:required:") })
        onNodeWithContentDescription("ERR:required:").assertExists()
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithText("Set to null").assertExists()
        assertEquals(JsonNull, (data as JsonObject)["a"])
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"null"}}}"""
        ).jsonObject)
        val changes = mutableListOf<JsonFormsChange>()
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (locale == "bg" && key.endsWith("null.set")) "HOST NULL" else fallback
                    },
                    translateError = { error, _, _ -> "ERR:${error.keyword}:${error.instancePath}" },
                ),
                onChange = { change ->
                    changes += change
                    data = change.data
                },
            )
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("ERR:required:").assertExists()
        assertTrue(changes.isEmpty())
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithText("Set to null").assertExists()
        onNodeWithText("ERR:required:").assertDoesNotExist()
        assertEquals(JsonNull, (changes.last().data as JsonObject)["a"])
        runOnIdle { locale = "bg" }
        onNodeWithText("HOST NULL").assertExists()
        onAllNodes(hasClickAction()).onFirst().performClick()
        onNodeWithText("Не е зададено").assertExists()
        assertTrue("a" !in (changes.last().data as JsonObject))
        onNodeWithText("ERR:required:").assertExists()
    }
}
