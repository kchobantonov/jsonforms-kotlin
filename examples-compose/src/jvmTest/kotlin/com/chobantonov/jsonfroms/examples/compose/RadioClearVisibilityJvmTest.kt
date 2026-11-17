package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RadioClearVisibilityJvmTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{
        "status":{"enum":["ready","done"]},
        "channel":{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]}
      }
    }""").jsonObject)

    @Test fun materialEnumRadioClearFollowsPointerAndFocus() = scenario(false, false)
    @Test fun cupertinoEnumRadioClearFollowsPointerAndFocus() = scenario(true, false)
    @Test fun materialOneOfRadioClearFollowsPointerAndFocus() = scenario(false, true)
    @Test fun cupertinoOneOfRadioClearFollowsPointerAndFocus() = scenario(true, true)

    private fun scenario(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "status"
        val choice = if (oneOf) "Yes" else "ready"
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"status":"ready","channel":"yes"}"""),
                schema = schema, uischema = ControlElement("#/properties/$field",
                    options = mapOf("format" to JsonPrimitive("radio"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        val clear = onNodeWithContentDescription("Clear value")
        val hidden = clear.captureToImage().toPixelMap()
        onNodeWithText(choice).performMouseInput { moveTo(center) }
        waitForIdle()
        val hovered = clear.captureToImage().toPixelMap()
        assertTrue((0 until hovered.width).any { x ->
            (0 until hovered.height).any { y -> hidden[x, y] != hovered[x, y] }
        })
        clear.performMouseInput { moveTo(center) }
        waitForIdle()
        val overButton = clear.captureToImage().toPixelMap()
        assertTrue((0 until overButton.width).any { x ->
            (0 until overButton.height).any { y -> hidden[x, y] != overButton[x, y] }
        })
        clear.performMouseInput { moveTo(Offset(-1100f, -100f)) }
        waitForIdle()
        val afterExit = clear.captureToImage().toPixelMap()
        assertTrue((0 until afterExit.width).all { x ->
            (0 until afterExit.height).all { y -> hidden[x, y] == afterExit[x, y] }
        })
        clear.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        val focused = clear.captureToImage().toPixelMap()
        assertTrue((0 until focused.width).any { x ->
            (0 until focused.height).any { y -> afterExit[x, y] != focused[x, y] }
        })
    }
}
