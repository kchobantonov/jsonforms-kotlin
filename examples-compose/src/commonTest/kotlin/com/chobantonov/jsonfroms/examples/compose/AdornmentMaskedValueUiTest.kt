package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
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
class AdornmentMaskedValueUiTest {
    @Test fun materialInitiallyEmpty() = emptyValue(false, false)
    @Test fun cupertinoInitiallyEmpty() = emptyValue(true, false)
    @Test fun materialHostEmptiesUnfocusedField() = emptyValue(false, true)
    @Test fun cupertinoHostEmptiesUnfocusedField() = emptyValue(true, true)
    @Test fun materialMidStringEditAndBackspace() = edit(false)
    @Test fun cupertinoMidStringEditAndBackspace() = edit(true)
    @Test fun materialFormattedPhonePrefix() = phone(false)
    @Test fun cupertinoFormattedPhonePrefix() = phone(true)
    @Test fun materialEagerTrailingLiteral() = eager(false)
    @Test fun cupertinoEagerTrailingLiteral() = eager(true)

    private fun eager(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "mask" to JsonPrimitive("###-###"),
                    "eager" to JsonPrimitive(true),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        for (digit in "482") field.performTextInput(digit.toString())
        assertEquals(JsonPrimitive("482"), data.jsonObject["value"])
        assertEquals("482-", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performTextInput("9")
        assertEquals(JsonPrimitive("4829"), data.jsonObject["value"])
        assertEquals("482-9", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performKeyInput { keyDown(Key.Backspace); keyUp(Key.Backspace) }
        assertEquals(JsonPrimitive("482"), data.jsonObject["value"])
        assertEquals("482-", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    private fun phone(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "mask" to JsonPrimitive("+1 (###) ###-####"),
                    "returnMaskedValue" to JsonPrimitive(true),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        for (digit in "5035550142") field.performTextInput(digit.toString())
        waitForIdle()
        assertEquals(JsonPrimitive("+1 (503) 555-0142"), data.jsonObject["value"])
        assertEquals("+1 (503) 555-0142",
            field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performKeyInput { keyDown(Key.Backspace); keyUp(Key.Backspace) }
        waitForIdle()
        assertEquals(JsonPrimitive("+1 (503) 555-014"), data.jsonObject["value"])
        assertEquals(11, writes)
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
    }

    private fun edit(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"48291"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "mask" to JsonPrimitive("###-###"),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        assertEquals("482-91", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performSemanticsAction(SemanticsActions.SetSelection) { it(4, 4, false) }
        field.performTextInput("7")
        waitForIdle()
        assertEquals(JsonPrimitive("482791"), data.jsonObject["value"])
        assertEquals("482-791", field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        field.performKeyInput { keyDown(Key.Backspace); keyUp(Key.Backspace) }
        waitForIdle()
        assertEquals(JsonPrimitive("48291"), data.jsonObject["value"])
        assertEquals(2, writes)
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
    }

    private fun emptyValue(cupertino: Boolean, hostReplacement: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            if (hostReplacement) """{"value":"482913"}""" else """{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "mask" to JsonPrimitive("###-###"),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        if (hostReplacement) runOnIdle { data = Json.parseToJsonElement("""{}""") }
        waitForIdle()
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
        assertEquals(0, writes)
    }
}
