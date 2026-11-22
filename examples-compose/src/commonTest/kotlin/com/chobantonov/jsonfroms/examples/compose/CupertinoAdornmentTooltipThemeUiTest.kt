package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.util.cupertinoColors
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CupertinoAdornmentTooltipThemeUiTest {
    @Test fun lightTooltipUsesCupertinoSurface() = tooltip(false)
    @Test fun darkTooltipUsesCupertinoSurface() = tooltip(true)

    private fun tooltip(dark: Boolean) = runComposeUiTest {
        val explanation = "This unit explanation stays readable in a narrow input column."
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        val adornment = Json.parseToJsonElement("""{
          "kind":"text","text":"Unit","tooltip":"$explanation"
        }""")
        var writes = 0
        setContent {
            CompositionLocalProvider(LocalCupertinoColors provides cupertinoColors(dark)) {
                Box(Modifier.width(240.dp).testTag("tooltip-host")) {
                    JsonForms(data = Json.parseToJsonElement("""{"value":"42"}"""), schema = schema,
                        uischema = ControlElement("#/properties/value", options = mapOf(
                            "startAdornment" to adornment)),
                        renderers = cupertinoRenderers, onChange = { writes++ })
                }
            }
        }
        val before = onNodeWithTag("tooltip-host").fetchSemanticsNode().boundsInRoot
        onNodeWithText("Unit").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText(explanation).assertExists()
        val image = onNodeWithTag("cupertino-tooltip", useUnmergedTree = true).captureToImage()
        val after = onNodeWithTag("tooltip-host").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        assertTrue(image.width <= after.width, "Tooltip exceeds the 240 dp input column")
        val pixels = image.toPixelMap()
        val actual = pixels[4, image.height / 2]
        val colors = cupertinoColors(dark)
        val expected = colors.secondaryBackground
        fun close(a: Color, b: Color) = abs(a.red - b.red) < 0.08f &&
            abs(a.green - b.green) < 0.08f && abs(a.blue - b.blue) < 0.08f
        assertTrue(close(actual, expected), "Tooltip surface $actual differs from Cupertino $expected")
        var foregroundPixels = 0
        for (y in 6 until image.height - 6) for (x in 6 until image.width - 6) {
            val pixel = pixels[x, y]
            if (abs(pixel.red - colors.label.red) < 0.18f &&
                abs(pixel.green - colors.label.green) < 0.18f &&
                abs(pixel.blue - colors.label.blue) < 0.18f) foregroundPixels++
        }
        assertTrue(foregroundPixels > 2, "Tooltip text did not use the Cupertino label color")
        fun linear(channel: Float): Double = if (channel <= 0.04045f) channel / 12.92
            else ((channel + 0.055) / 1.055).pow(2.4)
        fun luminance(color: Color): Double = 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
        val foreground = luminance(colors.label)
        val background = luminance(colors.secondaryBackground)
        val contrast = (max(foreground, background) + 0.05) /
            (min(foreground, background) + 0.05)
        assertTrue(contrast >= 7.0, "Tooltip palette contrast $contrast is below 7:1")
        assertEquals(0, writes)
    }
}
