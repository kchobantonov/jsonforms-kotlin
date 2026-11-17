package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ArrayChoiceDescriptionUiTest {
    @Test fun materialCheckboxHelpAndError() = helpAndError(false, "checkbox")
    @Test fun cupertinoCheckboxHelpAndError() = helpAndError(true, "checkbox")
    @Test fun materialMultiSelectHelpAndError() = helpAndError(false, "multi-select")
    @Test fun cupertinoMultiSelectHelpAndError() = helpAndError(true, "multi-select")
    @Test fun materialFiniteChipsHelpAndError() = helpAndError(false, "chips")
    @Test fun cupertinoFiniteChipsHelpAndError() = helpAndError(true, "chips")
    @Test fun materialFreeChipsHelpAndError() = helpAndError(false, "free-chips")
    @Test fun cupertinoFreeChipsHelpAndError() = helpAndError(true, "free-chips")

    private fun helpAndError(cupertino: Boolean, variant: String) = runComposeUiTest {
        val free = variant == "free-chips"
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"other":{"type":"string"},"teams":{"title":"Teams","description":"Choose two teams.",
                "type":"array","uniqueItems":true,"minItems":2,
                "items":${if (free) """{"type":"string"}""" else """{"type":"string","enum":["ops","fin"]}"""}}}
        }""").jsonObject)
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"teams":["ops"],"other":"x"}"""), schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/teams", options = mapOf(
                        "variant" to JsonPrimitive(if (free) "chips" else variant),
                        "showUnfocusedDescription" to showUnfocused)),
                    ControlElement("#/properties/other"))),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        val control = when (variant) {
            "checkbox" -> onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).onLast()
            "free-chips" -> onAllNodes(hasSetTextAction()).onFirst()
            else -> onNodeWithContentDescription("Add choice: Teams")
        }
        onNodeWithText("Choose two teams.").assertExists()
        assertEquals("Choose two teams.", control.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(control.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Choose two teams.").assertDoesNotExist()
        assertTrue(control.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Choose two teams.").assertDoesNotExist()
        control.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose two teams.").assertExists()
        assertEquals("Choose two teams.", control.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(control.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        onAllNodes(hasSetTextAction()).onLast()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        if (variant == "checkbox") mainClock.advanceTimeBy(200)
        waitForIdle()
        onNodeWithText("Choose two teams.").assertDoesNotExist()
        assertTrue(control.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
    }
}
