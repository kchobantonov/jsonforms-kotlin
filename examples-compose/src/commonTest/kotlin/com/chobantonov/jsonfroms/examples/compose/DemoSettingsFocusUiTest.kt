package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DemoSettingsFocusUiTest {
    @Test
    fun settingsFocusMovesInsideAndReturnsOnEscapeAndBackdrop() =
        settingsFocusFlow(DemoUiStyle.Material)
}

@OptIn(ExperimentalTestApi::class)
internal fun settingsFocusFlow(style: DemoUiStyle) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = style))
        val example = ExampleDescription(
            name = "settings-focus", label = "Settings focus",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store) }

        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithTag("settings-background").assertExists()
        onNodeWithContentDescription("Open settings").assertDoesNotExist()
        val panel = onNodeWithTag("settings-panel")
        panel.assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Settings"))
        panel.assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        panel.performTouchInput { click(Offset(4f, 4f)) }
        panel.assertExists()
        val close = onAllNodesWithContentDescription("Close settings").onLast()
        close.assertIsFocused()
        close.performKeyInput {
            keyDown(Key.ShiftLeft); keyDown(Key.Tab); keyUp(Key.Tab); keyUp(Key.ShiftLeft)
        }
        val lastSwitch = onNodeWithContentDescription("Allow Additional Properties By Default")
        lastSwitch.assertIsFocused()
        lastSwitch.performKeyInput { keyDown(Key.Tab); keyUp(Key.Tab) }
        close.assertIsFocused()
        close.performKeyInput { keyDown(Key.Escape); keyUp(Key.Escape) }
        onNodeWithContentDescription("Open settings").assertIsFocused()
        onNodeWithTag("settings-background").assertDoesNotExist()

        onNodeWithContentDescription("Open settings").performClick()
        onAllNodesWithContentDescription("Close settings").onLast().assertIsFocused()
        onNodeWithTag("settings-backdrop").performClick()
        onNodeWithContentDescription("Open settings").assertIsFocused()
}
