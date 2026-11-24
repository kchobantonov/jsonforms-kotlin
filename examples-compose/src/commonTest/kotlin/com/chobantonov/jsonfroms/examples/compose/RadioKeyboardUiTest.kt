package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class RadioKeyboardUiTest {
    @Test fun materialHorizontalRadioUsesArrowAndSpace() = navigate(false, false)
    @Test fun cupertinoHorizontalRadioUsesArrowAndSpace() = navigate(true, false)
    @Test fun materialVerticalRadioUsesArrowAndSpace() = navigate(false, true)
    @Test fun cupertinoVerticalRadioUsesArrowAndSpace() = navigate(true, true)
    @Test fun materialRtlRadioUsesLogicalLeftArrow() = navigate(false, false, rtl = true)
    @Test fun cupertinoRtlRadioUsesLogicalLeftArrow() = navigate(true, false, rtl = true)
    @Test fun materialRadioClearActivatesWithEnter() = clearWithKeyboard(false)
    @Test fun cupertinoRadioClearActivatesWithEnter() = clearWithKeyboard(true)
    @Test fun materialRadioUsesGlobalClearable() = globalClearable(false)
    @Test fun cupertinoRadioUsesGlobalClearable() = globalClearable(true)
    @Test fun materialEnumRadioClearFollowsTouchSelection() = touchClear(false, false)
    @Test fun cupertinoEnumRadioClearFollowsTouchSelection() = touchClear(true, false)
    @Test fun materialOneOfRadioClearFollowsTouchSelection() = touchClear(false, true)
    @Test fun cupertinoOneOfRadioClearFollowsTouchSelection() = touchClear(true, true)

    private fun touchClear(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "choice"
        val choice = if (oneOf) "Yes" else "Red"
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "choice":{"enum":["Red","Blue"]},
            "channel":{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}
          }
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"Red","channel":"yes"}"""))
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field", options = mapOf(
                    "format" to JsonPrimitive("radio"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithContentDescription(choice).performTouchInput { click() }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle { assertFalse(field in data.jsonObject) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }

    private fun globalClearable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"type":"string","enum":["Red","Blue"]}}
        }""").jsonObject)
        val data = Json.parseToJsonElement("""{"choice":"Red"}""")
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "clearable" to local)),
                config = mapOf("clearable" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = true }
        onNodeWithText("Red").performMouseInput { moveTo(Offset(-100f, -100f)); moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { local = JsonPrimitive(false) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = false; local = JsonPrimitive(true) }
        onNodeWithText("Red").performMouseInput { moveTo(Offset(-100f, -100f)); moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
    }

    private fun clearWithKeyboard(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"enum":["Red","Blue"]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"Red"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Red")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        val clear = onNodeWithContentDescription("Clear value")
        clear.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        clear.assertIsFocused()
        clear.performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        waitForIdle()
        assertFalse("choice" in data.jsonObject)
        clear.assertDoesNotExist()
    }

    private fun navigate(cupertino: Boolean, vertical: Boolean, rtl: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"type":"string","enum":["Red","Blue","Green"]}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"Red"}"""))
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides
                (if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/choice", options = mapOf(
                        "format" to JsonPrimitive("radio"),
                        "vertical" to JsonPrimitive(vertical),
                    )),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data })
            }
        }
        onNodeWithContentDescription("Red")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Red").assertIsFocused()
        onNodeWithContentDescription("Red").performKeyInput {
            val arrow = if (vertical) Key.DirectionDown
                else if (rtl) Key.DirectionLeft else Key.DirectionRight
            keyDown(arrow); keyUp(arrow)
        }
        onNodeWithContentDescription("Blue").assertIsFocused()
        onNodeWithContentDescription("Blue").performKeyInput {
            keyDown(Key.Spacebar); keyUp(Key.Spacebar)
        }
        waitForIdle()
        assertEquals(JsonPrimitive("Blue"), data.jsonObject["choice"])
        onNodeWithText("Green").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("Green"), data.jsonObject["choice"])
        onNodeWithText("Green").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("Green"), data.jsonObject["choice"])
    }
}
