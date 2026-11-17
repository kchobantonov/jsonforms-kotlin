package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
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
class SelectDescriptionUiTest {
    @Test fun materialEnumHelpAndError() = helpAndError(false, "enum")
    @Test fun cupertinoEnumHelpAndError() = helpAndError(true, "enum")
    @Test fun materialOneOfHelpAndError() = helpAndError(false, "oneOf")
    @Test fun cupertinoOneOfHelpAndError() = helpAndError(true, "oneOf")
    @Test fun materialSearchableHelpAndError() = helpAndError(false, "search")
    @Test fun cupertinoSearchableHelpAndError() = helpAndError(true, "search")

    private fun helpAndError(cupertino: Boolean, kind: String) = runComposeUiTest {
        val choices = if (kind == "oneOf") """"oneOf":[
            {"const":"Red","title":"Red"},{"const":"Blue","title":"Blue"}
        ]""" else """"type":"string","enum":["Red","Blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","required":["choice"],"properties":{
                "choice":{"title":"Color","description":"Choose a color.",$choices},
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
                        "showUnfocusedDescription" to showUnfocused) +
                        (if (kind == "search") mapOf("autocomplete" to JsonPrimitive(true)) else emptyMap())),
                    ControlElement("#/properties/other"))),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val selector = if (kind == "search") onAllNodes(hasSetTextAction()).onFirst()
            else onNodeWithText("Unknown")
        onNodeWithText("Choose a color.").assertExists()
        assertEquals("Required field. Current value outside choices: \"Unknown\". Choose a color.",
            selector.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(selector.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        onNodeWithContentDescription("Blue").assertDoesNotExist()
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        assertEquals("Required field. Current value outside choices: \"Unknown\"",
            selector.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(selector.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        selector.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a color.").assertExists()
        onNodeWithContentDescription("Blue").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertEquals("Задължително поле. Текуща стойност извън опциите: \"Unknown\". Choose a color.",
            selector.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onAllNodes(hasSetTextAction()).onLast()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a color.").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
