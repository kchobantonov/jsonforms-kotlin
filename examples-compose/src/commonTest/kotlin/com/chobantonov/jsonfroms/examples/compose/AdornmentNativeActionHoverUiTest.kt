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
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AdornmentNativeActionHoverUiTest {
    @Test fun materialDate() = hover(false, "date")
    @Test fun cupertinoDate() = hover(true, "date")
    @Test fun materialTime() = hover(false, "time")
    @Test fun cupertinoTime() = hover(true, "time")
    @Test fun materialDateTime() = hover(false, "date-time")
    @Test fun cupertinoDateTime() = hover(true, "date-time")
    @Test fun materialDuration() = hover(false, "duration")
    @Test fun cupertinoDuration() = hover(true, "duration")
    @Test fun materialCron() = hover(false, "cron")
    @Test fun cupertinoCron() = hover(true, "cron")
    @Test fun materialColor() = hover(false, "color")
    @Test fun cupertinoColor() = hover(true, "color")

    private fun hover(cupertino: Boolean, kind: String) = runComposeUiTest {
        val value = when (kind) {
            "date" -> "2025-01-10"
            "time" -> "09:30:00"
            "date-time" -> "2025-01-10T09:30:00Z"
            "duration" -> "PT1H"
            "cron" -> "0 * * * *"
            else -> "#ff0000"
        }
        val pickerLabel = when (kind) {
            "date" -> "Choose date"
            "time" -> "Choose time"
            "date-time" -> "Choose date and time"
            "duration" -> "Choose duration"
            "cron" -> "Choose schedule"
            else -> "Choose color"
        }
        val extended = kind == "duration" || kind == "cron" || kind == "color"
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$kind"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"$value"}"""))
        var writes = 0
        var inputModeManager: InputModeManager? = null
        setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.width(240.dp)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/value", options = mapOf(
                        "startAdornment" to JsonPrimitive("S"),
                        "endAdornment" to JsonPrimitive("E"))),
                    renderers = when {
                        extended && cupertino -> cupertinoExtendedRenderers
                        extended -> materialExtendedRenderers
                        cupertino -> cupertinoRenderers
                        else -> materialRenderers
                    },
                    onChange = { writes++; data = it.data })
            }
        }
        runOnIdle { inputModeManager!!.requestInputMode(InputMode.Keyboard) }
        fun edge(text: String) = onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        fun action(text: String) = onNodeWithContentDescription(text, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val start = edge("S")
        val end = edge("E")
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        val picker = if (extended) null else action(pickerLabel)
        if (extended) onNodeWithText(pickerLabel).assertExists()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = action("Clear value")
        assertEquals(start, edge("S"))
        assertEquals(end, edge("E"))
        if (picker != null) assertEquals(picker, action(pickerLabel))
        for (part in listOfNotNull(start, end, clear, picker)) {
            assertTrue(part.left >= field.left - 2f && part.right <= field.right + 2f,
                "$kind part escapes 240 dp field: $part, $field")
        }
        assertTrue(end.right <= clear.left || clear.right <= end.left,
            "$kind end adornment overlaps clear: $end, $clear")
        if (picker != null) {
            assertTrue(end.right <= picker.left || picker.right <= end.left,
                "$kind end adornment overlaps picker: $end, $picker")
            assertTrue(clear.right <= picker.left || picker.right <= clear.left,
                "$kind clear overlaps picker: $clear, $picker")
        }
        assertEquals(0, writes)
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        assertEquals(start, edge("S"))
        assertEquals(end, edge("E"))
        if (picker != null) assertEquals(picker, action(pickerLabel))
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
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
