package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
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
class SelectPlaceholderUiTest {
    @Test fun materialEnumHint() = hint(false, "enum")
    @Test fun cupertinoEnumHint() = hint(true, "enum")
    @Test fun materialOneOfHint() = hint(false, "oneOf")
    @Test fun cupertinoOneOfHint() = hint(true, "oneOf")
    @Test fun materialSearchHint() = hint(false, "search")
    @Test fun cupertinoSearchHint() = hint(true, "search")
    @Test fun materialStoredEmptyChoiceDoesNotShowHint() = storedEmptyChoice(false)
    @Test fun cupertinoStoredEmptyChoiceDoesNotShowHint() = storedEmptyChoice(true)

    private fun storedEmptyChoice(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"type":"string","enum":["","Red"],"title":"Color"}}}"""
        ).jsonObject)
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":""}"""), schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "placeholder" to JsonPrimitive("Pick a color"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithTag("choice-dropdown-arrow", useUnmergedTree = true).assertExists()
        if (!cupertino) onNodeWithText("Color").performClick()
        onNodeWithText("Pick a color").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun hint(cupertino: Boolean, kind: String) = runComposeUiTest {
        val choices = if (kind == "oneOf")
            """"oneOf":[{"const":"Red","title":"Red"},{"const":"Blue","title":"Blue"}]"""
        else """"type":"string","enum":["Red","Blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"choice":{"title":"Color",$choices}}}"""
        ).jsonObject)
        var hint by mutableStateOf(JsonPrimitive("Pick a color"))
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = ControlElement("#/properties/choice", options =
                    mapOf("placeholder" to hint) +
                        if (kind == "search") mapOf("autocomplete" to JsonPrimitive(true)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("Color").assertExists()
        if (!cupertino) onNodeWithText("Color").performClick()
        onNodeWithText("Pick a color").assertExists()
        if (kind == "search") assertTrue(onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("Pick a color"))
        runOnIdle { hint = JsonPrimitive("Choose a shade") }
        onNodeWithText("Choose a shade").assertExists()
        if (kind == "search") assertTrue(onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .contains("Choose a shade"))
        runOnIdle { hint = JsonPrimitive("") }
        onNodeWithText("Choose a shade").assertDoesNotExist()
        runOnIdle { hint = JsonPrimitive(42) }
        onNodeWithText("42").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
