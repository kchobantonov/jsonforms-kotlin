package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DemoTopBarNarrowUiTest {
    @Test
    fun settingsRemainsReachableInNarrowWindow() = runComposeUiTest {
        var width by mutableStateOf(240.dp)
        var homeCalls = 0
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        val example = ExampleDescription(
            name = "narrow-topbar", label = "Narrow topbar",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent {
            Box(Modifier.width(width).height(560.dp)) {
                ExamplesComposeApp(examples = listOf(example), settingsStore = store,
                    onHomeSelected = { homeCalls++ })
            }
        }
        val controls = buildList {
            add("Show example menu")
            add("Show form only")
            if (supportsWebComponentView) add("Show web component")
            add("Open GitHub")
            add("Open settings")
        }
            .map { onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot }
        assertTrue(controls.all { it.left >= 0f && it.right <= 240f },
            "Toolbar controls leave the 240 dp viewport: $controls")
        assertTrue(controls.zipWithNext().all { (left, right) -> left.right <= right.left },
            "Toolbar controls overlap: $controls")
        val settings = onNodeWithContentDescription("Open settings")
        onNodeWithText("JSON Forms Kotlin").assertDoesNotExist()
        onNodeWithContentDescription("Go to welcome page").assertDoesNotExist()
        val bounds = settings.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.left >= 0f && bounds.right <= 240f,
            "Settings button leaves the 240 dp viewport: $bounds")
        settings.performClick()
        onAllNodesWithContentDescription("Close settings").onLast().performClick()
        runOnIdle { width = 500.dp }
        onNodeWithContentDescription("Go to welcome page").assertExists()
        val title = onNodeWithText("JSON Forms Kotlin").fetchSemanticsNode().boundsInRoot
        assertTrue(title.width > 0f && title.height <= 56f, "Wide toolbar title is clipped: $title")
        runOnIdle { width = 240.dp }
        onNodeWithContentDescription("Show example menu").performClick()
        onNodeWithText("Home").performClick()
        assertEquals(1, homeCalls)
    }

    @Test
    fun bulgarianToolbarControlsFitAtPhoneWidth() = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material,
            locale = DemoLocale.Bulgarian))
        val example = ExampleDescription(
            name = "bulgarian-topbar", label = "Bulgarian topbar",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent {
            Box(Modifier.width(320.dp).height(560.dp)) {
                ExamplesComposeApp(examples = listOf(example), settingsStore = store)
            }
        }
        val controls = buildList {
            add("Покажи менюто с примери")
            add("Покажи само формуляра")
            if (supportsWebComponentView) add("Покажи уеб компонента")
            add("Отвори GitHub")
            add("Отвори настройките")
        }
            .map { onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot }
        assertTrue(controls.all { it.left >= 0f && it.right <= 320f },
            "Bulgarian toolbar controls leave the phone width: $controls")
        assertTrue(controls.zipWithNext().all { (left, right) -> left.right <= right.left },
            "Bulgarian toolbar controls overlap: $controls")
        onNodeWithContentDescription("Лого на Kotlin").assertExists()
        onNodeWithText("JSON Forms Kotlin").assertDoesNotExist()
        onNodeWithContentDescription("Отвори настройките").performClick()
        onAllNodesWithContentDescription("Затвори настройките").onLast().assertExists()
    }
}
