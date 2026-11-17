package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
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
class RadioDescriptionUiTest {
    @Test fun materialHelpAndErrorStayAssociated() = helpAndError(false)
    @Test fun cupertinoHelpAndErrorStayAssociated() = helpAndError(true)

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","required":["choice"],"properties":{
                "choice":{"type":"string","enum":["Red","Blue"],"description":"Pick a color."},
                "other":{"type":"string"}}
        }""").jsonObject)
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":"Unknown","other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/choice", options = mapOf(
                        "format" to JsonPrimitive("radio"),
                        "showUnfocusedDescription" to showUnfocused)),
                    ControlElement("#/properties/other"))),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val red = onNodeWithContentDescription("Red")
        onNodeWithText("Pick a color.").assertExists()
        assertEquals("Required field. Pick a color.",
            red.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(red.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Pick a color.").assertDoesNotExist()
        assertEquals("Required field", red.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(red.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Pick a color.").assertDoesNotExist()
        red.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Pick a color.").assertExists()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertEquals("Задължително поле. Pick a color.",
            red.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(red.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Pick a color.").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
