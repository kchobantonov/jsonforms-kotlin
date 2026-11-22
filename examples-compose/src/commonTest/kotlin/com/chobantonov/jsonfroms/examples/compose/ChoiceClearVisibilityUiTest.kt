package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ChoiceClearVisibilityUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{
        "status":{"type":"string","enum":["ready","done"]},
        "channel":{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}
      }
    }""").jsonObject)

    @Test fun materialEnumClearFollowsPointer() = scenario(false, false)
    @Test fun cupertinoEnumClearFollowsPointer() = scenario(true, false)
    @Test fun materialOneOfEnumClearFollowsPointer() = scenario(false, true)
    @Test fun cupertinoOneOfEnumClearFollowsPointer() = scenario(true, true)

    private fun scenario(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "status"
        val value = if (oneOf) "Yes" else "ready"
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"status":"ready","channel":"yes"}"""),
                schema = schema, uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        val clear = onNodeWithContentDescription("Clear value")
        clear.assertDoesNotExist()
        onNodeWithText(value).performMouseInput { moveTo(center) }
        waitForIdle()
        clear.assertExists()
        clear.performMouseInput { moveTo(center) }
        waitForIdle()
        clear.assertExists()
        clear.performMouseInput { moveTo(Offset(-1100f, -100f)) }
        waitForIdle()
        clear.assertDoesNotExist()
    }
}
