package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
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
class AdornmentChoiceArrowOrderUiTest {
    @Test fun materialLtr() = order(cupertino = false, rtl = false)
    @Test fun materialRtl() = order(cupertino = false, rtl = true)
    @Test fun cupertinoLtr() = order(cupertino = true, rtl = false)
    @Test fun cupertinoRtl() = order(cupertino = true, rtl = true)

    private fun order(cupertino: Boolean, rtl: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","enum":["Red","Blue"]}}
        }""").jsonObject)
        val ui = ControlElement("#/properties/value", options = mapOf(
            "startAdornment" to JsonPrimitive("S"),
            "endAdornment" to JsonPrimitive("E"),
        ))
        var writes = 0
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                Box(Modifier.width(240.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"value":"Red"}"""),
                        schema = schema, uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                        onChange = { writes++ })
                }
            }
        }
        val start = onNodeWithText("S", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val end = onNodeWithText("E", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val arrow = onNodeWithTag("choice-dropdown-arrow", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        if (rtl) {
            assertTrue(start.left >= end.right, "Start/end reverse in RTL: $start, $end")
            if (cupertino) assertTrue(end.right <= arrow.left,
                "Cupertino end adornment should follow the arrow in RTL: $end, $arrow")
            else assertTrue(arrow.right <= end.left,
                "Material suffix should precede its trailing arrow in RTL: $end, $arrow")
        } else {
            assertTrue(start.right <= end.left, "Start/end reverse in LTR: $start, $end")
            if (cupertino) assertTrue(arrow.right <= end.left,
                "Cupertino end adornment should follow the arrow in LTR: $end, $arrow")
            else assertTrue(end.right <= arrow.left,
                "Material suffix should precede its trailing arrow in LTR: $end, $arrow")
        }
        assertEquals(0, writes)
    }
}
