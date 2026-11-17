package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OneOfClearVisibilityJvmTest {
    @Test fun clearIconFollowsHoverAndKeyboardFocus() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choice":{"oneOf":[
            {"title":"Email contact","type":"object","properties":{"kind":{"const":"email"}},"required":["kind"]},
            {"title":"Phone contact","type":"object","properties":{"kind":{"const":"phone"}},"required":["kind"]}
          ]}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":{"kind":"email"}}"""),
                schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = materialRenderers)
        }
        val clear = onNodeWithTag("oneof-selector-clear")
        val hidden = clear.captureToImage().toPixelMap()
        onNodeWithText("Email contact").performMouseInput { moveTo(center) }
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
        onRoot().performMouseInput { moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        val afterExit = clear.captureToImage().toPixelMap()
        assertTrue((0 until afterExit.width).all { x ->
            (0 until afterExit.height).all { y -> hidden[x, y] == afterExit[x, y] }
        })
        onNodeWithText("Email contact")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        val visible = clear.captureToImage().toPixelMap()
        assertTrue((0 until visible.width).any { x ->
            (0 until visible.height).any { y -> afterExit[x, y] != visible[x, y] }
        })
    }
}
