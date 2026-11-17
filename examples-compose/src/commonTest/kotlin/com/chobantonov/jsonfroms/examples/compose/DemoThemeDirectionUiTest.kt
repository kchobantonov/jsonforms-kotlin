package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DemoThemeDirectionUiTest {
    @Test fun materialProviderChangesPaletteAndDirection() = themeProvider(false)
    @Test fun cupertinoProviderChangesPaletteAndDirection() = themeProvider(true)

    private fun themeProvider(cupertino: Boolean) = runComposeUiTest {
        var dark by mutableStateOf(false)
        var direction by mutableStateOf(DemoDirection.Ltr)
        var background = Color.Unspecified
        var surface = Color.Unspecified
        var renderedDirection = LayoutDirection.Ltr
        setContent {
            DemoAppTheme(
                style = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
                locale = DemoLocale.English,
                direction = direction,
                darkTheme = dark,
                materialTheme = MaterialThemePreset.Purple,
                cupertinoTheme = CupertinoThemePreset.Blue,
            ) {
                val palette = MaterialTheme.colorScheme
                val layout = LocalLayoutDirection.current
                SideEffect {
                    background = palette.background
                    surface = palette.surface
                    renderedDirection = layout
                }
            }
        }
        val lightBackground = background
        val lightSurface = surface
        assertEquals(LayoutDirection.Ltr, renderedDirection)
        runOnIdle { dark = true; direction = DemoDirection.Rtl }
        waitForIdle()
        assertNotEquals(lightBackground, background)
        assertNotEquals(lightSurface, surface)
        assertEquals(LayoutDirection.Rtl, renderedDirection)
    }

    @Test fun materialThemeAndRtlSettingsKeepTheFormData() = themeAndDirection(false)
    @Test fun cupertinoThemeAndRtlSettingsKeepTheFormData() = themeAndDirection(true)

    private fun themeAndDirection(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
        ))
        val example = ExampleDescription(
            name = "theme-direction", label = "Theme and direction",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store) }
        val originalSidebarX = onNodeWithText("Example source")
            .fetchSemanticsNode().boundsInRoot.center.x
        onNodeWithContentDescription("Open settings").performClick()
        onAllNodesWithText("System").onLast().assertIsSelected()
        onNodeWithText("Dark").performClick()
        onNodeWithText("Dark").assertIsSelected()
        onNodeWithText("Light").performClick()
        onNodeWithText("Light").assertIsSelected()
        onNodeWithText("LTR").performClick()
        onNodeWithText("RTL").performClick()
        onAllNodesWithContentDescription("Close settings").onFirst().performClick()
        val rtlSidebarX = onNodeWithText("Example source")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(rtlSidebarX > originalSidebarX + 100f)
        onNodeWithText("Ada").assertExists()
    }
}
