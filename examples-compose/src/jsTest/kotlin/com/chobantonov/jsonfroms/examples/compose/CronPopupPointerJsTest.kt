package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CronPopupPointerJsTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"schedule":{"type":"string","format":"cron"}}
    }""").jsonObject)

    @Test fun materialResetReceivesBrowserClick() = reset(false)
    @Test fun cupertinoResetReceivesBrowserClick() = reset(true)
    @Test fun materialAdvancedAndExpressionClearReceiveBrowserClick() = clearText(false)
    @Test fun cupertinoAdvancedAndExpressionClearReceiveBrowserClick() = clearText(true)

    private fun reset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Every: Hours").assertDoesNotExist()
        onNodeWithContentDescription("Hours").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Every: Hours").performClick()
        onNodeWithContentDescription("Expression").assertTextContains("0 0 * * * *")
        assertEquals(0, writes)
    }

    private fun clearText(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 L * ?"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Clear value: Days of month").assertDoesNotExist()
        onNodeWithContentDescription("Days of month").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value: Days of month").performClick()
        onNodeWithContentDescription("Expression").assertTextContains("0 0 9 * * ?")
        onNodeWithContentDescription("Expression").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value: Expression").performClick()
        assertEquals(0, writes)
    }
}
