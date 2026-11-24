package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DemoRuntimeSwitchUiTest {
    private val example = ExampleDescription(
        name = "runtime-sample", label = "Runtime sample",
        data = Json.parseToJsonElement("""{"name":"Ada"}"""),
        schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject),
        uischema = ControlElement("#/properties/name"),
    )

    @Test fun materialHomeShowsSpecifiedTitleAndTagline() = homeCopy(
        DemoUiStyle.Material, DemoLocale.English,
        "JSON Forms Kotlin Material 3", "More Forms. Less Code.",
    )

    @Test fun cupertinoBulgarianHomeShowsSpecifiedTitleAndTagline() = homeCopy(
        DemoUiStyle.Cupertino, DemoLocale.Bulgarian,
        "JSON Forms Kotlin Cupertino", "Повече формуляри. По-малко код.",
    )

    private fun homeCopy(style: DemoUiStyle, locale: DemoLocale,
        title: String, tagline: String,
    ) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = style, locale = locale))
        setContent {
            ExamplesComposeApp(examples = listOf(example), settingsStore = store,
                showLandingWhenUnselected = true, browserRuntime = DemoBrowserRuntime.Js)
        }
        onNodeWithText(title).assertExists()
        onNodeWithText(tagline).assertExists()
        onNodeWithText("Kotlin/JS").assertExists()
        onNodeWithText("Kotlin/Wasm").assertExists()
    }

    @Test fun welcomeOffersBothRuntimesAndOpensSelectedExample() = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        var selected: String? = null
        var target: DemoBrowserRuntime? = null
        var transferred: DemoRuntimeHandoff? = null
        setContent {
            ExamplesComposeApp(examples = listOf(example), settingsStore = store,
                selectedExampleName = selected, showLandingWhenUnselected = true,
                onExampleSelected = { selected = it }, browserRuntime = DemoBrowserRuntime.Js,
                onBrowserRuntimeSwitch = { runtime, handoff ->
                    target = runtime
                    transferred = handoff
                })
        }
        onNodeWithText("Choose a web runtime").assertExists()
        onNodeWithText("Kotlin/Wasm").performClick()
        assertEquals(DemoBrowserRuntime.Wasm, target)
        assertEquals("runtime-sample", transferred?.exampleName)
        assertEquals(example.data, transferred?.data)
        onNodeWithText("Kotlin/JS").performClick()
        assertEquals("runtime-sample", selected)
    }

    @Test fun settingsSwitchCarriesCurrentEditedDataFromInitialHandoff() = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        var consumed = 0
        var target: DemoBrowserRuntime? = null
        var transferred: DemoRuntimeHandoff? = null
        setContent {
            ExamplesComposeApp(examples = listOf(example), settingsStore = store,
                selectedExampleName = example.name, browserRuntime = DemoBrowserRuntime.Js,
                initialRuntimeHandoff = DemoRuntimeHandoff(DemoBrowserRuntime.Js, example.name,
                    Json.parseToJsonElement("""{"name":"Grace"}"""), true),
                onRuntimeHandoffConsumed = { consumed++ },
                onBrowserRuntimeSwitch = { runtime, handoff ->
                    target = runtime
                    transferred = handoff
                })
        }
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().performTextReplacement("Marie")
        onNodeWithText("Marie").assertExists()
        waitForIdle()
        onNodeWithContentDescription("Open settings").performClick()
        waitForIdle()
        onNodeWithText("Kotlin/JS").performClick()
        onNodeWithText("Kotlin/Wasm").performClick()
        assertEquals(1, consumed)
        assertEquals(DemoBrowserRuntime.Wasm, target)
        assertEquals(Json.parseToJsonElement("""{"name":"Marie"}"""), transferred?.data)
        assertEquals(true, transferred?.dataProvided)
    }

    @Test fun failedHandoffKeepsCurrentFormAndExplainsTheProblem() = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        setContent {
            ExamplesComposeApp(examples = listOf(example), settingsStore = store,
                selectedExampleName = example.name, browserRuntime = DemoBrowserRuntime.Js,
                onBrowserRuntimeSwitch = { _, _ -> error("Storage unavailable") })
        }
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithText("Kotlin/JS").performClick()
        onNodeWithText("Kotlin/Wasm").performClick()
        onNodeWithText("Unable to switch runtime").assertExists()
        onNodeWithText("The browser could not preserve the current form data. Free browser storage and try again.")
            .assertExists()
        onNodeWithText("OK").performClick()
        onNodeWithContentDescription("Close settings").performClick()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().assertExists()
    }
}
