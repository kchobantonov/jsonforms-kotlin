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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
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
class AdornmentStepperHoverUiTest {
    @Test fun materialInteger() = hover(false, false)
    @Test fun cupertinoInteger() = hover(true, false)
    @Test fun materialDecimal() = hover(false, true)
    @Test fun cupertinoDecimal() = hover(true, true)

    private fun hover(cupertino: Boolean, decimal: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (decimal) "number" else "integer"}"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            if (decimal) """{"value":7.5}""" else """{"value":7}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.width(240.dp)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/value", options = mapOf(
                        "startAdornment" to JsonPrimitive("S"),
                        "endAdornment" to JsonPrimitive("E"))),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { writes++; data = it.data })
            }
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        fun bounds(text: String) = onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        fun actionBounds(text: String) = onNodeWithContentDescription(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val start = bounds("S")
        val end = bounds("E")
        val decrease = actionBounds("Decrease value")
        val increase = actionBounds("Increase value")
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = actionBounds("Clear value")
        assertEquals(start, bounds("S"))
        assertEquals(end, bounds("E"))
        assertEquals(decrease, actionBounds("Decrease value"))
        assertEquals(increase, actionBounds("Increase value"))
        for (part in listOf(start, end, clear, decrease, increase)) {
            assertTrue(part.left >= field.left - 2f && part.right <= field.right + 2f,
                "stepper part escapes 240 dp field: $part, $field")
        }
        for (step in listOf(decrease, increase)) {
            assertTrue(clear.right <= step.left || step.right <= clear.left,
                "clear overlaps stepper action: $clear, $step")
            assertTrue(end.right <= step.left || step.right <= end.left,
                "end adornment overlaps stepper action: $end, $step")
        }
        assertTrue(end.right <= clear.left || clear.right <= end.left,
            "end adornment overlaps clear: $end, $clear")
        assertEquals(0, writes)
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        assertEquals(start, bounds("S"))
        assertEquals(end, bounds("E"))
        assertEquals(decrease, actionBounds("Decrease value"))
        assertEquals(increase, actionBounds("Increase value"))
        onNodeWithContentDescription("Increase value").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(if (decimal) 7.6 else 8), data.jsonObject["value"])
        assertEquals(1, writes)
    }
}
