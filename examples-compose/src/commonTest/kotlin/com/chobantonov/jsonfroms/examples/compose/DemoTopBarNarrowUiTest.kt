package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeUp
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
    fun compactHeaderStaysFixedWhileWorkspaceScrolls() = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = DemoUiStyle.Material))
        val example = ExampleDescription(
            name = "scroll-sample", label = "Scroll sample",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent {
            Box(Modifier.width(320.dp).height(400.dp)) {
                ExamplesComposeApp(examples = listOf(example), settingsStore = store)
            }
        }
        val headerBefore = onNodeWithContentDescription("Show example menu")
            .fetchSemanticsNode().boundsInRoot
        val panelBefore = onNodeWithTag("demo-tab-panel").fetchSemanticsNode().boundsInRoot
        onNodeWithTag("compact-workspace").performTouchInput { swipeUp() }
        val headerAfter = onNodeWithContentDescription("Show example menu")
            .fetchSemanticsNode().boundsInRoot
        val panelAfter = onNodeWithTag("demo-tab-panel").fetchSemanticsNode().boundsInRoot
        assertTrue(panelAfter.top < panelBefore.top,
            "The compact workspace did not scroll: $panelBefore, $panelAfter")
        assertEquals(headerBefore.top, headerAfter.top)
        assertEquals(headerBefore.bottom, headerAfter.bottom)
    }

    @Test fun materialExpandedSidebarShiftsWorkspace() = expandedSidebar(
        DemoUiStyle.Material, DemoLocale.English,
        "Hide example menu", "Show example menu")

    @Test fun cupertinoBulgarianExpandedSidebarShiftsWorkspace() = expandedSidebar(
        DemoUiStyle.Cupertino, DemoLocale.Bulgarian,
        "Скрий менюто с примери", "Покажи менюто с примери")

    private fun expandedSidebar(style: DemoUiStyle, locale: DemoLocale,
        hideLabel: String, showLabel: String,
    ) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = style, locale = locale))
        val example = ExampleDescription(
            name = "sidebar-sample", label = "Sidebar sample",
            data = Json.parseToJsonElement("""{"name":"Ada"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent {
            Box(Modifier.width(1000.dp).height(560.dp)) {
                ExamplesComposeApp(examples = List(30) { index ->
                    example.copy(name = "sidebar-$index", label = "Sidebar sample $index")
                }, settingsStore = store)
            }
        }
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst()
            .performTextReplacement("Grace")
        val panel = onNodeWithTag("demo-tab-panel").fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(field.left >= panel.left && field.right <= panel.right &&
            field.top >= panel.top && field.bottom <= panel.bottom,
            "Desktop form must sit inside the tab panel: $field, $panel")
        val navigation = onNodeWithTag("expanded-navigation").fetchSemanticsNode().boundsInRoot
        val shifted = onNodeWithTag("expanded-workspace").fetchSemanticsNode().boundsInRoot
        assertTrue(navigation.top >= 56f && shifted.left > navigation.right,
            "Sidebar must sit below the header and shift the workspace: $navigation, $shifted")
        onNodeWithText("Sidebar sample 29").performScrollTo()
        assertEquals(navigation, onNodeWithTag("expanded-navigation")
            .fetchSemanticsNode().boundsInRoot)
        assertEquals(shifted, onNodeWithTag("expanded-workspace")
            .fetchSemanticsNode().boundsInRoot)
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().assertExists()
        onNodeWithContentDescription(hideLabel).performClick()
        onNodeWithTag("expanded-navigation").assertDoesNotExist()
        val full = onNodeWithTag("expanded-workspace").fetchSemanticsNode().boundsInRoot
        assertTrue(full.left < shifted.left && full.width > shifted.width,
            "Closing the sidebar must give the workspace its width back: $shifted, $full")
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().assertExists()
        onNodeWithContentDescription(showLabel).performClick()
        onNodeWithTag("expanded-navigation").assertExists()
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().assertExists()
    }

    @Test fun materialCompactDrawerOverlaysWorkspace() = compactDrawer(
        DemoUiStyle.Material, DemoLocale.English, "Show example menu")

    @Test fun cupertinoBulgarianCompactDrawerOverlaysWorkspace() = compactDrawer(
        DemoUiStyle.Cupertino, DemoLocale.Bulgarian, "Покажи менюто с примери")

    private fun compactDrawer(style: DemoUiStyle, locale: DemoLocale,
        menuLabel: String,
    ) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(uiStyle = style, locale = locale))
        val example = ExampleDescription(
            name = "drawer-sample", label = "Drawer sample",
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
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst()
            .performTextReplacement("Grace")
        val panel = onNodeWithTag("demo-tab-panel").fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(field.left >= panel.left && field.right <= panel.right && field.top >= panel.top,
            "Compact form must sit inside the tab panel: $field, $panel")
        onNodeWithContentDescription(menuLabel).performClick()
        val workspace = onNodeWithTag("compact-workspace").fetchSemanticsNode().boundsInRoot
        val drawer = onNodeWithTag("example-drawer").fetchSemanticsNode().boundsInRoot
        val backdrop = onNodeWithTag("example-drawer-backdrop").fetchSemanticsNode().boundsInRoot
        assertTrue(drawer.top >= 56f && drawer.bottom <= workspace.bottom + 1f,
            "Drawer must start below the fixed header: $drawer")
        assertTrue(drawer.width < workspace.width && backdrop.width == workspace.width,
            "Drawer must leave a dismissible backdrop beside the workspace: $drawer, $backdrop")
        onAllNodes(hasSetTextAction() and hasText("Grace")).assertCountEquals(0)
        onNodeWithTag("example-drawer-backdrop").performTouchInput {
            click(Offset(backdrop.width - 10f, center.y))
        }
        onNodeWithTag("example-drawer").assertDoesNotExist()
        onAllNodes(hasSetTextAction() and hasText("Grace")).onFirst().assertExists()
    }

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
        val brand = onNodeWithContentDescription("Go to welcome page")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(brand.left >= controls.first().right && brand.right <= controls[1].left,
            "The narrow brand mark must fit between menu and actions: $brand, $controls")
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
        onNodeWithContentDescription("Go to welcome page").performClick()
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
