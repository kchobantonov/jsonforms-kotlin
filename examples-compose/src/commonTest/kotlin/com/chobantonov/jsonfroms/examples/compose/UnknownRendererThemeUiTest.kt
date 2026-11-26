package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.cupertino.renderers.util.cupertinoColors
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UnknownRendererThemeUiTest {
    @Test fun materialLightFallbackUsesThemeForeground() = fallback(false, false)
    @Test fun materialDarkFallbackUsesThemeForeground() = fallback(false, true)
    @Test fun cupertinoLightFallbackUsesThemeForeground() = fallback(true, false)
    @Test fun cupertinoDarkFallbackUsesThemeForeground() = fallback(true, true)

    private fun fallback(cupertino: Boolean, dark: Boolean) = runComposeUiTest {
        val materialScheme = if (dark) darkColorScheme() else lightColorScheme()
        val cupertinoScheme = cupertinoColors(dark)
        val background = if (cupertino) cupertinoScheme.background else materialScheme.background
        val foreground = if (cupertino) cupertinoScheme.label else materialScheme.onSurface
        val unknown = Json.parseToJsonElement("""{"type":"VendorWidget"}""").jsonObject.toUiSchemaElement()
        var writes = 0
        setContent {
            if (cupertino) {
                CompositionLocalProvider(LocalCupertinoColors provides cupertinoScheme) {
                    Box(Modifier.size(280.dp, 64.dp).background(background).testTag("fallback-host")) {
                        JsonForms(data = Json.parseToJsonElement("{}"),
                            schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
                            uischema = unknown, renderers = cupertinoRenderers,
                            onChange = { writes++ })
                    }
                }
            } else {
                MaterialTheme(colorScheme = materialScheme) {
                    Box(Modifier.size(280.dp, 64.dp).background(background).testTag("fallback-host")) {
                        JsonForms(data = Json.parseToJsonElement("{}"),
                            schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
                            uischema = unknown, renderers = materialRenderers,
                            onChange = { writes++ })
                    }
                }
            }
        }
        val image = onNodeWithTag("fallback-host").captureToImage()
        val pixels = image.toPixelMap()
        fun close(a: Color, b: Color): Boolean = abs(a.red - b.red) < 0.2f &&
            abs(a.green - b.green) < 0.2f && abs(a.blue - b.blue) < 0.2f
        var foregroundPixels = 0
        for (y in 2 until image.height - 2) for (x in 2 until image.width - 2) {
            if (close(pixels[x, y], foreground)) foregroundPixels++
        }
        assertTrue(foregroundPixels > 10,
            "${if (cupertino) "Cupertino" else "Material"} ${if (dark) "dark" else "light"} fallback has no themed foreground pixels")
        assertEquals(0, writes)
    }
}
