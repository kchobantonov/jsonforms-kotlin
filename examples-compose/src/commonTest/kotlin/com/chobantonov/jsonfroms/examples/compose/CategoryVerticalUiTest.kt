package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class CategoryVerticalUiTest {
    @Test fun materialVerticalTabsHaveSideNavigation() = scenario(false, false)
    @Test fun cupertinoVerticalTabsHaveSideNavigation() = scenario(true, false)
    @Test fun materialVerticalStepsHaveSideNavigation() = scenario(false, true)
    @Test fun cupertinoVerticalStepsHaveSideNavigation() = scenario(true, true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"string"}}}"""
    ).jsonObject)
    private fun ui(stepper: Boolean) = CategorizationElement(
        options = mapOf("vertical" to JsonPrimitive(true)) +
            (if (stepper) mapOf(
                "variant" to JsonPrimitive("stepper"),
                "showNavButtons" to JsonPrimitive(true),
            ) else emptyMap()),
        elements = listOf(
            CategoryElement(label = "First", name = "first",
                elements = listOf(ControlElement(scope = "#/properties/a"))),
            CategoryElement(label = "Second", name = "second",
                elements = listOf(ControlElement(scope = "#/properties/b"))),
        ),
    )
    private fun scenario(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        var changes = 0
        var paneWidth by mutableStateOf(600.dp)
        setContent {
            Box(Modifier.width(paneWidth)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"a":"Stored","b":"Other"}"""),
                    schema = schema, uischema = ui(stepper),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    config = mapOf("jsonformsExtended" to mapOf("showDataIndicator" to true)),
                    onChange = { changes++ },
                )
            }
        }
        val first = onNodeWithContentDescription("First, Contains data").fetchSemanticsNode().boundsInRoot
        val second = onNodeWithContentDescription("Second, Contains data").fetchSemanticsNode().boundsInRoot
        val field = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(second.top >= first.bottom - 2f, "Vertical navigation must stack categories")
        assertTrue(field.left >= first.right - 2f, "The selected panel must be beside the navigation")
        if (stepper) {
            onNodeWithText("Next").performClick()
        } else {
            onNodeWithContentDescription("Second, Contains data").performClick()
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        runOnIdle { paneWidth = 320.dp }
        val narrowNavigation = onNodeWithContentDescription("First, Contains data")
            .fetchSemanticsNode().boundsInRoot
        val narrowField = onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(narrowField.left >= narrowNavigation.right - 2f)
        assertTrue(narrowField.right <= 322f, "Selected field must fit a 320 dp pane")
        assertEquals(0, changes)
    }
}
