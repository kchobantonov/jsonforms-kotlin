package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DemoSettingsConformanceUiTest {
    @Test fun materialReadonlySettingChangesFormWithoutChangingData() = readonlySetting(false)
    @Test fun cupertinoReadonlySettingChangesFormWithoutChangingData() = readonlySetting(true)

    private fun readonlySetting(cupertino: Boolean) = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false))
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
        ))
        val example = ExampleDescription(
            name = "settings-conformance", label = "Settings conformance",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = store,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }

        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().assertExists()
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithText("Locale").assertExists()
        onNodeWithText("English (en)").assertExists()
        onNodeWithText("Direction").assertExists()
        onNodeWithText("Validation").assertExists()
        onNodeWithText("Hide Required Asterisk").assertExists()
        onNodeWithText("Show Unfocused Description").assertExists()
        onNodeWithText("Restrict").assertExists()
        onNodeWithText("Options").assertExists()
        onNodeWithTag("setting-readonly").performScrollTo().performClick()
        onAllNodesWithContentDescription("Close settings").onFirst().performClick()
        onNodeWithText("Ada").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Ada")).assertCountEquals(0)

        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithTag("setting-readonly").performScrollTo().performClick()
        onAllNodesWithContentDescription("Close settings").onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().performTextReplacement("Grace")
        onNodeWithText("Grace").assertExists()
    }
}
