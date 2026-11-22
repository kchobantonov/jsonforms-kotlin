package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AdornmentClearVisibilityUiTest {
    @Test fun materialClearAppearsOnlyWhenFieldIsFocused() = visibility(false)
    @Test fun cupertinoClearAppearsOnlyWhenFieldIsFocused() = visibility(true)
    @Test fun materialClearFollowsPointerHover() = visibility(false, hover = true)
    @Test fun cupertinoClearFollowsPointerHover() = visibility(true, hover = true)
    @Test fun materialPasswordActionsKeepAdornmentOrder() = passwordActions(false)
    @Test fun cupertinoPasswordActionsKeepAdornmentOrder() = passwordActions(true)

    private fun passwordActions(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"Secret"}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "format" to JsonPrimitive("password"),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        val prefix = onNodeWithText("prefix", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val suffix = onNodeWithText("suffix", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val clear = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val reveal = onNodeWithContentDescription("Show password", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(prefix.right <= suffix.left, "prefix must precede suffix")
        assertTrue(suffix.right <= clear.left || clear.right <= suffix.left,
            "suffix and clear must not overlap")
        assertTrue(clear.right <= reveal.left, "clear must precede reveal")

        onNodeWithContentDescription("Show password").performClick()
        waitForIdle()
        assertEquals(0, writes)
        onNodeWithContentDescription("Hide password").assertExists()
        assertEquals(prefix, onNodeWithText("prefix", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(suffix, onNodeWithText("suffix", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(clear, onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(reveal, onNodeWithContentDescription("Hide password", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot)
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{}"""), data)
        assertEquals(1, writes)
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithContentDescription("Hide password").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
    }

    private fun visibility(cupertino: Boolean, hover: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"Ada"}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        onNodeWithText("prefix").assertExists()
        onNodeWithText("suffix").assertExists()
        val prefixBefore = onNodeWithText("prefix").fetchSemanticsNode().boundsInRoot
        val suffixBefore = onNodeWithText("suffix").fetchSemanticsNode().boundsInRoot
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        if (hover) {
            onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
            waitForIdle()
            onNodeWithContentDescription("Clear value").assertExists()
            assertEquals(prefixBefore, onNodeWithText("prefix").fetchSemanticsNode().boundsInRoot)
            assertEquals(suffixBefore, onNodeWithText("suffix").fetchSemanticsNode().boundsInRoot)
            onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
            waitForIdle()
            onNodeWithContentDescription("Clear value").assertDoesNotExist()
            assertEquals(0, writes)
            return@runComposeUiTest
        }
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        assertEquals(prefixBefore, onNodeWithText("prefix").fetchSemanticsNode().boundsInRoot)
        assertEquals(suffixBefore, onNodeWithText("suffix").fetchSemanticsNode().boundsInRoot)
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{}"""), data)
        assertEquals(1, writes)
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        onNodeWithText("prefix").assertExists()
        onNodeWithText("suffix").assertExists()
    }
}
