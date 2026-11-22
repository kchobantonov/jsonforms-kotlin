package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedCells
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CompactAdornmentGeometryUiTest {
    @Test fun materialDurationLtr() = geometry(false, "duration", false)
    @Test fun cupertinoDurationLtr() = geometry(true, "duration", false)
    @Test fun materialDurationRtl() = geometry(false, "duration", true)
    @Test fun cupertinoDurationRtl() = geometry(true, "duration", true)
    @Test fun materialCronLtr() = geometry(false, "cron", false)
    @Test fun cupertinoCronLtr() = geometry(true, "cron", false)
    @Test fun materialCronRtl() = geometry(false, "cron", true)
    @Test fun cupertinoCronRtl() = geometry(true, "cron", true)
    @Test fun materialColorLtr() = geometry(false, "color", false)
    @Test fun cupertinoColorLtr() = geometry(true, "color", false)
    @Test fun materialColorRtl() = geometry(false, "color", true)
    @Test fun cupertinoColorRtl() = geometry(true, "color", true)

    private fun geometry(cupertino: Boolean, kind: String, rtl: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"rows":{"type":"array","items":{
            "type":"object","properties":{"value":{"type":"string","format":"$kind"}}
          }}}
        }""").jsonObject)
        val value = when (kind) {
            "duration" -> "PT1H"
            "cron" -> "0 * * * *"
            else -> "#ff0000"
        }
        val action = when (kind) {
            "duration" -> "Choose duration"
            "cron" -> "Choose schedule"
            else -> "Choose color"
        }
        var writes = 0
        setContent {
            TestTouchInputMode()
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(360.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"rows":[{"value":"$value"}]}"""),
                        schema = schema,
                        uischema = ControlElement("#/properties/rows", options = mapOf(
                            "table" to JsonPrimitive(true),
                            "columnDefs" to Json.parseToJsonElement("""[{"field":"value","width":240}]"""),
                            "cells" to Json.parseToJsonElement("""{"value":{
                              "startAdornment":"S","endAdornment":"E"}}"""))),
                        renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                        cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                        onChange = { writes++ })
                }
            }
        }
        val start = onNodeWithText("S", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("E", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val picker = onNodeWithContentDescription(action, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performMouseInput { moveTo(center) }
        waitForIdle()
        val clear = onNodeWithContentDescription("Clear value", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(field.width <= 242f, "$kind compact field exceeds its 240 dp column: $field")
        if (rtl) assertTrue(start.left >= end.right, "$kind RTL edges overlap or reverse: $start, $end")
        else assertTrue(start.right <= end.left, "$kind LTR edges overlap or reverse: $start, $end")
        listOf(start, end, picker, clear).forEach { bounds ->
            assertTrue(bounds.left >= field.left - 2f && bounds.right <= field.right + 2f,
                "$kind compact part escapes the field: $bounds, $field")
        }
        listOf(picker, clear).forEach { actionBounds ->
            assertTrue(end.right <= actionBounds.left || actionBounds.right <= end.left,
                "$kind end adornment overlaps a compact action: $end, $actionBounds")
        }
        assertEquals(0, writes)
    }
}
