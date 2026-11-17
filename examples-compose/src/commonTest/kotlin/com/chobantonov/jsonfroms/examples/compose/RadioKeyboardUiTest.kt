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
