package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DemoHostValidationJvmTest {
    @Test
    fun sessionOptionsStartFromAuthoredConfigAndCanBeOverridden() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-options-test", label = "Options test",
            data = Json.parseToJsonElement("{}"),
            schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
            uischema = ControlElement("#"),
            config = mapOf(
                "enableFilterErrorsBeforeTouch" to true,
                "filterErrorKeywordsBeforeTouch" to listOf("required"),
                "allowAdditionalPropertiesIfMissing" to true,
            ),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithTag("setting-filter-before-touch").assertIsOn()
        onNodeWithTag("setting-additional-properties-default").assertIsOn()
        onNodeWithTag("setting-filter-before-touch").performScrollTo().performClick()
        onNodeWithTag("setting-additional-properties-default").performScrollTo().performClick()
        onNodeWithTag("setting-error-keywords").performScrollTo()
            .performTextReplacement("required, pattern")
        onNodeWithTag("setting-filter-before-touch").assertIsOff()
        onNodeWithTag("setting-additional-properties-default").assertIsOff()
        onAllNodesWithContentDescription("Close settings").onLast().performClick()
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithTag("setting-filter-before-touch").assertIsOff()
        onNodeWithTag("setting-additional-properties-default").assertIsOff()
        assertTrue(settingsStore.keys.none { it.contains("filter", true) || it.contains("additional", true) })
    }

    @Test
    fun directionSwitchMirrorsTheShellWithoutResettingTheForm() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-direction-test", label = "Direction test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        val before = onNodeWithContentDescription("Hide example menu")
            .fetchSemanticsNode().boundsInRoot.center.x
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithText("LTR").performClick()
        onNodeWithText("RTL").performClick()
        onAllNodesWithContentDescription("Close settings").onLast().performClick()
        val after = onNodeWithContentDescription("Hide example menu")
            .fetchSemanticsNode().boundsInRoot.center.x
        assertTrue(after > before + 500f)
        onNodeWithText("Demo (1)").assertExists()
        assertTrue(settingsStore.keys.none { it.contains("direction", ignoreCase = true) })
    }

    @Test
    fun changingDemoLocaleUpdatesChromeAndKeepsTheActiveForm() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-locale-test", label = "Locale test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithContentDescription("Open settings").performClick()
        onNodeWithText("English (en)").performClick()
        onNodeWithText("Bulgarian (bg)").performClick()
        onNodeWithText("Настройки").assertExists()
        onAllNodesWithContentDescription("Затвори настройките").onLast().performClick()
        onNodeWithText("Демо (1)").assertExists()
        onNodeWithContentDescription("Отвори настройките").assertExists()
        onNodeWithText("JSON Forms").assertExists()
    }

    @Test
    fun cupertinoBulgarianEditorActionsHaveLocalizedAccessibleNames() = runComposeUiTest {
        val settingsStore = MapSettings()
        DemoPreferences(settingsStore).save(DemoSettings(
            uiStyle = DemoUiStyle.Cupertino,
            locale = DemoLocale.Bulgarian,
        ))
        val example = ExampleDescription(
            name = "demo-cupertino-editor-test", label = "Cupertino editor test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithText("Данни").performClick()
        onNodeWithContentDescription("Презареди данни").assertExists()
        onNodeWithContentDescription("Приложи данни").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("""{"name":"ok"}""")
        onNodeWithContentDescription("Приложи данни").performClick()
        onNodeWithText("Demo (1)").assertDoesNotExist()
    }

    @Test
    fun dataEditorReloadDiscardsDraftWithoutChangingTheForm() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-reload-test", label = "Reload test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithText("Data").performClick()
        onNodeWithContentDescription("Reload Data").assertExists()
        onNodeWithContentDescription("Apply Data").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("{ invalid")
        onNodeWithContentDescription("Apply Data").performClick()
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithContentDescription("Reload Data").performClick()
        onAllNodes(hasSetTextAction() and hasText("\"name\": \"x\"", substring = true))
            .onLast().assertExists()
        onNodeWithText("Demo (1)").assertExists()
    }

    @Test
    fun combinedDemoUsesAResizableSplit() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-split-test", label = "Split test",
            data = Json.parseToJsonElement("""{"name":"ok"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithContentDescription("Open settings").performClick()
        onAllNodes(hasText("Demo Layout", substring = true)).onFirst().performClick()
        onNodeWithText("Demo and Data").performClick()
        onAllNodesWithContentDescription("Close settings").onLast().performClick()
        onNodeWithTag("demo-splitter").assertExists()
        val form = onNodeWithTag("demo-form-pane").fetchSemanticsNode().boundsInRoot
        val data = onNodeWithTag("demo-data-pane").fetchSemanticsNode().boundsInRoot
        assertTrue(form.right <= data.left)
    }

    @Test
    fun dataEditorApplyUpdatesCountWhileDemoPaneIsHidden() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-editor-count-test",
            label = "Editor count test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithText("Demo (1)").assertExists()
        onNodeWithText("Data").performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("""{"name":"ok"}""")
        onNodeWithContentDescription("Apply Data").performClick()
        onNodeWithText("Demo").assertExists()
        onNodeWithText("Demo (1)").assertDoesNotExist()
    }

    @Test
    fun committedFormActionUpdatesTheDemoTabCount() = runComposeUiTest {
        val settingsStore = MapSettings()
        val example = ExampleDescription(
            name = "demo-count-test",
            label = "Count test",
            data = Json.parseToJsonElement("""{"name":"x"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"name":{"type":"string","minLength":2}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/name"),
            actions = listOf(ExampleAction("Repair") { state ->
                state.copy(data = buildJsonObject { put("name", "ok") })
            }),
        )
        setContent { ExamplesComposeApp(examples = listOf(example), settingsStore = settingsStore) }
        onNodeWithText("Demo (1)").assertExists()
        val heading = onNodeWithText("JSON Forms").fetchSemanticsNode().boundsInRoot.center.y
        val action = onNodeWithText("Repair").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue(abs(heading - action) < 30f)
        onNodeWithText("Repair").performClick()
        onNodeWithText("Demo").assertExists()
        onNodeWithText("Demo (1)").assertDoesNotExist()
    }
}
