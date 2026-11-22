package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
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
class AdornmentTextVariantHoverUiTest {
    @Test fun materialMultiline() = hover(false, false)
    @Test fun cupertinoMultiline() = hover(true, false)
    @Test fun materialMasked() = hover(false, true)
    @Test fun cupertinoMasked() = hover(true, true)

    private fun hover(cupertino: Boolean, masked: Boolean) = runComposeUiTest {
        val value = if (masked) "482913" else "Ada\nMia"
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            if (masked) """{"value":"482913"}""" else """{"value":"Ada\nMia"}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.width(240.dp)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/value", options = mapOf(
                        (if (masked) "mask" else "multi") to
                            (if (masked) JsonPrimitive("###-###") else JsonPrimitive(true)),
                        "startAdornment" to JsonPrimitive("S"),
                        "endAdornment" to JsonPrimitive("E"))),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { writes++; data = it.data })
            }
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        fun edge(text: String) = onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        fun clearBounds() = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst()
        val fieldBounds = field.fetchSemanticsNode().boundsInRoot
        assertEquals(if (masked) "482-913" else value,
            field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        val start = edge("S")
        val end = edge("E")
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        field.performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = clearBounds()
        assertEquals(start, edge("S"))
        assertEquals(end, edge("E"))
        for (part in listOf(start, end, clear)) {
            assertTrue(part.left >= fieldBounds.left - 2f && part.right <= fieldBounds.right + 2f,
                "${if (masked) "masked" else "multiline"} part escapes 240 dp field: $part, $fieldBounds")
        }
        assertTrue(end.right <= clear.left || clear.right <= end.left,
            "end adornment overlaps clear: $end, $clear")
        assertEquals(0, writes)
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        assertEquals(start, edge("S"))
        assertEquals(end, edge("E"))
        field.performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{}"""), data)
        assertEquals(1, writes)
        onNodeWithText("S", useUnmergedTree = true).assertExists()
        onNodeWithText("E", useUnmergedTree = true).assertExists()
    }
}
