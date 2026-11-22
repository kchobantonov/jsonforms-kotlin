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
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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
class AdornmentChoiceClearVisibilityUiTest {
    @Test fun materialChoiceClearFollowsHover() = hover(false)
    @Test fun cupertinoChoiceClearFollowsHover() = hover(true)
    @Test fun materialAutocompleteClearFollowsHover() = hover(false, true)
    @Test fun cupertinoAutocompleteClearFollowsHover() = hover(true, true)
    @Test fun materialChoiceMenuStillSelects() = menu(false, false)
    @Test fun cupertinoChoiceMenuStillSelects() = menu(true, false)
    @Test fun materialAutocompleteMenuStillSelects() = menu(false, true)
    @Test fun cupertinoAutocompleteMenuStillSelects() = menu(true, true)

    private fun menu(cupertino: Boolean, autocomplete: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","enum":["Ada","Mia"]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"Ada"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "autocomplete" to JsonPrimitive(autocomplete),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        if (autocomplete) onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Mi")
        else onNodeWithText("Ada", useUnmergedTree = true).performClick()
        onNodeWithText("Mia").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("Mia"), data.jsonObject["value"])
        assertEquals(1, writes)
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
    }

    private fun hover(cupertino: Boolean, autocomplete: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","enum":["Ada","Mia"]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"Ada"}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "autocomplete" to JsonPrimitive(autocomplete),
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++; data = it.data })
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        fun bounds(text: String) = onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val prefix = bounds("prefix")
        val suffix = bounds("suffix")
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithText("Ada", useUnmergedTree = true).performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertEquals(prefix, bounds("prefix"))
        assertEquals(suffix, bounds("suffix"))
        assertTrue(suffix.right <= clear.left || clear.right <= suffix.left,
            "choice adornment overlaps clear action: $suffix, $clear")
        assertEquals(0, writes)
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        assertEquals(prefix, bounds("prefix"))
        assertEquals(suffix, bounds("suffix"))
        onNodeWithText("Ada", useUnmergedTree = true).performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{}"""), data)
        assertEquals(1, writes)
        onNodeWithText("prefix", useUnmergedTree = true).assertExists()
        onNodeWithText("suffix", useUnmergedTree = true).assertExists()
    }
}
