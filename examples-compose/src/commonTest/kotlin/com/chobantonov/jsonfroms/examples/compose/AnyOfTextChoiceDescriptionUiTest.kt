package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AnyOfTextChoiceDescriptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","required":["choice"],"properties":{
            "choice":{"title":"Color","description":"Choose a color.","minLength":4,
                "anyOf":[{"type":"string","enum":["Red","Blue"]},{"type":"string"}]},
            "other":{"type":"string"}}
    }""").jsonObject)

    @Test fun materialHelpAndError() = helpAndError(false)
    @Test fun cupertinoHelpAndError() = helpAndError(true)
    @Test fun materialInitialFocus() = initialFocus(false)
    @Test fun cupertinoInitialFocus() = initialFocus(true)
    @Test fun materialSuggestionsStillCommitChoice() = suggestionsCommitChoice(false)
    @Test fun cupertinoSuggestionsStillCommitChoice() = suggestionsCommitChoice(true)
    @Test fun cupertinoHostReplacementClosesSuggestionsWithoutWriting() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"x"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice"),
                renderers = cupertinoRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("B")
        onNodeWithText("Blue").assertExists()
        runOnIdle {
            assertEquals(JsonPrimitive("B"), data.jsonObject["choice"])
            assertEquals(1, changes)
            data = Json.parseToJsonElement("""{"choice":"Red"}""")
        }
        waitForIdle()
        onNodeWithText("Blue").assertDoesNotExist()
        assertEquals("Red", onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        runOnIdle { assertEquals(1, changes) }
    }
    @Test fun materialImportedNonStringStaysUntilRepair() = importedNonString(false)
    @Test fun cupertinoImportedNonStringStaysUntilRepair() = importedNonString(true)

    private fun importedNonString(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: {\"raw\":true}"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Invalid value: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"choice":null}""")
        }
        onNodeWithText("Невалидна стойност: null").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Blue")
        runOnIdle {
            assertEquals(JsonPrimitive("Blue"), data.jsonObject["choice"])
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
    }

    private fun suggestionsCommitChoice(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"x","other":"ok"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("B")
        onNodeWithText("Blue").performClick()
        assertEquals("Blue", (data.jsonObject["choice"] as JsonPrimitive).content)
    }

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"choice":"x","other":"ok"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/choice", options = mapOf(
                        "focus" to JsonPrimitive(true),
                        "hideRequiredAsterisk" to JsonPrimitive(true))),
                    ControlElement("#/properties/other"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        onNodeWithText("Color*").assertDoesNotExist()
        onNodeWithText("Red").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"choice":"x","other":"ok"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/choice", options = mapOf(
                        "showUnfocusedDescription" to showUnfocused)),
                    ControlElement("#/properties/other"))),
                validationMode = ValidationMode.ValidateAndShow,
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        val input = onAllNodes(hasSetTextAction()).onFirst()
        onNodeWithText("Choose a color.").assertExists()
        assertEquals("Required field. Choose a color.",
            input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        assertEquals("Required field", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        input.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a color.").assertExists()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertEquals("Задължително поле. Choose a color.",
            input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onAllNodes(hasSetTextAction()).onLast()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
