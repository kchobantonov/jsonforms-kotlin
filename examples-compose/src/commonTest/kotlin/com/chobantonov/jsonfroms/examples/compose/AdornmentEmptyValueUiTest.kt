package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AdornmentEmptyValueUiTest {
    @Test fun materialInitiallyEmpty() = emptyValue(false, false)
    @Test fun cupertinoInitiallyEmpty() = emptyValue(true, false)
    @Test fun materialHostEmptiesUnfocusedField() = emptyValue(false, true)
    @Test fun cupertinoHostEmptiesUnfocusedField() = emptyValue(true, true)
    @Test fun materialPasswordInitiallyEmpty() = emptyValue(false, false, true)
    @Test fun cupertinoPasswordInitiallyEmpty() = emptyValue(true, false, true)
    @Test fun materialHostEmptiesUnfocusedPassword() = emptyValue(false, true, true)
    @Test fun cupertinoHostEmptiesUnfocusedPassword() = emptyValue(true, true, true)
    @Test fun materialControlledEditAndHostReplacement() = controlledEdit(false)
    @Test fun cupertinoControlledEditAndHostReplacement() = controlledEdit(true)
    @Test fun materialPasswordMaskAndRevealKeepRawValue() = passwordMask()

    private fun passwordMask() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "format" to JsonPrimitive("password"),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = materialRenderers,
                onChange = { writes++; data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextReplacement("Ada")
        waitForIdle()
        assertEquals(JsonPrimitive("Ada"), data.jsonObject["value"])
        assertEquals(1, writes)
        assertEquals("•••", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        onNodeWithContentDescription("Show password").performClick()
        assertEquals("Ada", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        onNodeWithContentDescription("Hide password").performClick()
        assertEquals("•••", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        assertEquals(1, writes)
    }

    private fun controlledEdit(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","maxLength":4}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextReplacement("Ada")
        waitForIdle()
        assertEquals(JsonPrimitive("Ada"), data.jsonObject["value"])
        assertEquals(1, writes)
        field.performTextReplacement("Longer")
        waitForIdle()
        assertEquals(JsonPrimitive("Ada"), data.jsonObject["value"])
        assertEquals(1, writes)
        assertEquals("Ada", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        runOnIdle { data = Json.parseToJsonElement("""{"value":"Mia"}""") }
        assertEquals("Mia", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        assertEquals(1, writes)
        field.performSemanticsAction(SemanticsActions.SetSelection) { it(1, 1, false) }
        field.performTextInput("o")
        waitForIdle()
        assertEquals(JsonPrimitive("Moia"), data.jsonObject["value"])
        assertEquals("Moia", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        assertEquals(2, field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].start)
        assertEquals(2, writes)
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        waitForIdle()
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
        assertEquals(2, writes)
    }

    private fun emptyValue(cupertino: Boolean, hostReplacement: Boolean,
                           password: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            if (hostReplacement) """{"value":"Ada"}""" else """{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix")) +
                    (if (password) mapOf("format" to JsonPrimitive("password")) else emptyMap())),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        if (hostReplacement) runOnIdle { data = Json.parseToJsonElement("""{}""") }
        waitForIdle()
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
        if (password) {
            onNodeWithContentDescription("Show password").performClick()
            onNodeWithContentDescription("Hide password").assertExists()
            onNodeWithText("prefix", useUnmergedTree = true).assertExists()
            onNodeWithText("suffix", useUnmergedTree = true).assertExists()
        }
        assertEquals(0, writes)
    }
}
