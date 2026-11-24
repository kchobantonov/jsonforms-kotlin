package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MixedTypeClearUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"value":{"type":["string","number"]}}
    }""").jsonObject)

    @Test fun materialTypeClearFollowsHoverAndTouchSelection() = visibility(false)
    @Test fun cupertinoTypeClearFollowsHoverAndTouchSelection() = visibility(true)
    @Test fun materialTypeClearUsesStrictOptionPrecedence() = precedence(false)
    @Test fun cupertinoTypeClearUsesStrictOptionPrecedence() = precedence(true)

    private fun visibility(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":"hello"}"""))
        var writes = 0
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"typeChange":"never"}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithText("String").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithText("String").performMouseInput { moveTo(Offset(-100f, -100f)) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onNodeWithText("String").performTouchInput { click() }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":null}"""), data)
            assertEquals(1, writes)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }

    private fun precedence(cupertino: Boolean) = runComposeUiTest {
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        val data = Json.parseToJsonElement("""{"value":"hello"}""")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf("clearable" to local)),
                config = mapOf("clearable" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("String").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { global = true }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { local = JsonPrimitive(false) }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { local = JsonPrimitive(true); global = false }
        onNodeWithContentDescription("Clear value").assertExists()
    }
}
