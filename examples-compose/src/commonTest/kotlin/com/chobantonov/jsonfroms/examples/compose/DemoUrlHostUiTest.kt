package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleAction
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DemoUrlHostUiTest {
    @Test
    fun externalViewStatePreservesWorkingDataAcrossFormOnlyAndTabChanges() = runComposeUiTest {
        var view by mutableStateOf(DemoUrlViewState(drawer = false))
        val settingsStore = MapSettings()
        DemoPreferences(settingsStore).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        val example = ExampleDescription(
            name = "url-view-test", label = "URL view test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
            actions = listOf(ExampleAction("Repair") { state ->
                state.copy(data = buildJsonObject { put("name", "ok") })
            }),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore,
            browserViewState = view, onBrowserViewStateChange = { view = it }) }
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithText("Repair").performClick()
        onNodeWithText("Demo (1)").assertDoesNotExist()
        runOnIdle { view = view.copy(formOnly = true, activePane = DemoPane.Schema) }
        onNodeWithText("JSON Forms").assertExists()
        onNodeWithText("URL view test").assertDoesNotExist()
        onNodeWithContentDescription("Open settings").assertDoesNotExist()
        onNodeWithContentDescription("Show full UI").performClick()
        assertTrue(!view.formOnly)
        onAllNodes(hasText("Schema")).onFirst().assertIsSelected()
        runOnIdle { view = view.copy(activePane = DemoPane.Demo) }
        onNodeWithText("Demo").assertExists()
        onNodeWithText("Demo (1)").assertDoesNotExist()
        runOnIdle { view = view.copy(readonly = true) }
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithTag("setting-readonly").assertIsOn()
        onNodeWithContentDescription("Read-Only").assertIsOn()
        onNodeWithTag("setting-readonly").performScrollTo().performClick()
        assertTrue(!view.readonly)
    }
}
