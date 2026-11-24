package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SliderConformanceUiTest {
    private val hasProgress = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"value":{"type":"integer","minimum":1,"maximum":6,"default":2,"multipleOf":2}}
    }""").jsonObject)
    private val control = ControlElement("#/properties/value", options = mapOf("slider" to JsonPrimitive(true)))

    @Test fun materialSliderUsesZeroBasedMultiples() = multiples(false)
    @Test fun cupertinoSliderUsesZeroBasedMultiples() = multiples(true)
    @Test fun materialMissingSliderKeepsDefaultProvisionalAndClears() = missingAndClear(false)
    @Test fun cupertinoMissingSliderKeepsDefaultProvisionalAndClears() = missingAndClear(true)
    @Test fun materialSliderUsesGlobalClearable() = globalClearable(false)
    @Test fun cupertinoSliderUsesGlobalClearable() = globalClearable(true)
    @Test fun materialSliderClearFollowsHoverAndTouchSelection() = clearVisibility(false)
    @Test fun cupertinoSliderClearFollowsHoverAndTouchSelection() = clearVisibility(true)

    private fun clearVisibility(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":4}"""))
        var changes = 0
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        val slider = onAllNodes(hasProgress).onFirst()
        slider.performMouseInput { moveTo(Offset(-100f, -100f)); moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        slider.performMouseInput { moveTo(Offset(-100f, -100f)) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { assertEquals(0, changes) }
        slider.performTouchInput { click() }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertFalse("value" in data.jsonObject)
            assertTrue(changes >= 1)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }

    private fun globalClearable(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"value":4}""")
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "slider" to JsonPrimitive(true), "clearable" to local)),
                config = mapOf("clearable" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = true }
        onAllNodes(hasProgress).onFirst().performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { local = JsonPrimitive(false) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = false; local = JsonPrimitive(true) }
        onNodeWithContentDescription("Clear value").assertExists()
    }
    @Test fun materialInvalidSliderShowsActualValue() = invalidIncoming(false)
    @Test fun cupertinoInvalidSliderShowsActualValue() = invalidIncoming(true)
    @Test fun materialInvalidNullSliderHasAccessibleError() = invalidNull(false)
    @Test fun cupertinoInvalidNullSliderHasAccessibleError() = invalidNull(true)

    private fun invalidNull(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid current value: null").assertExists()
        assertTrue(onAllNodes(hasProgress).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid current value: null"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("Invalid current value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"value":null}""")
        }
        onNodeWithText("Невалидна текуща стойност: null").assertExists()
        onAllNodes(hasProgress).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":4}"""), data)
            assertEquals(1, changes)
        }
    }
    @Test fun materialIncompatibleRangeReportsDiagnostic() = noPosition(false)
    @Test fun cupertinoIncompatibleRangeReportsDiagnostic() = noPosition(true)
    @Test fun materialSinglePositionCanBeSelected() = singlePosition(false)
    @Test fun cupertinoSinglePositionCanBeSelected() = singlePosition(true)

    private fun singlePosition(cupertino: Boolean) = runComposeUiTest {
        val onlyTwo = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"integer","minimum":2,"maximum":2,"default":2,"multipleOf":2}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = onlyTwo, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithText("Select value").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":2}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithText("Current value: 2").assertExists()
    }

    private fun multiples(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":2}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value: 2").assertExists()
        val slider = onAllNodes(hasProgress).onFirst()
        assertEquals("Current value: 2",
            slider.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":4}"""), data)
            assertEquals(1, changes)
        }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(2f) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":6}"""), data)
            assertEquals(2, changes)
        }
        onNodeWithText("Current value: 6").assertExists()
    }

    private fun missingAndClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Not set").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{}"""), data)
            assertEquals(0, changes)
        }
        onAllNodes(hasProgress).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":4}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{}"""), data)
            assertEquals(2, changes)
        }
        onNodeWithText("Not set").assertExists()
    }

    private fun invalidIncoming(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":"3"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid current value: \"3\"").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":"3"}"""), data)
            assertEquals(0, changes)
        }
        onAllNodes(hasProgress).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":4}"""), data)
            assertEquals(1, changes)
        }
    }

    private fun noPosition(cupertino: Boolean) = runComposeUiTest {
        val incompatible = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"integer","minimum":1,"maximum":1,"default":1,"multipleOf":2}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":1}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = incompatible, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("No selectable value in slider range.").assertExists()
        assertEquals(0, onAllNodes(hasProgress).fetchSemanticsNodes().size)
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":1}"""), data)
            assertEquals(0, changes)
        }
    }
}
