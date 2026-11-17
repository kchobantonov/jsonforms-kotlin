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
class SimpleControlDescriptionUiTest {
    @Test fun materialCheckboxHelpAndError() = helpAndError(false, "checkbox")
    @Test fun cupertinoCheckboxHelpAndError() = helpAndError(true, "checkbox")
    @Test fun materialSwitchHelpAndError() = helpAndError(false, "switch")
    @Test fun cupertinoSwitchHelpAndError() = helpAndError(true, "switch")
    @Test fun materialSliderHelpAndError() = helpAndError(false, "slider")
    @Test fun cupertinoSliderHelpAndError() = helpAndError(true, "slider")
    @Test fun materialTextFieldUsesVisibleLabelAsAccessibleName() = textFieldName(false)
    @Test fun cupertinoTextFieldUsesVisibleLabelAsAccessibleName() = textFieldName(true)

    private fun textFieldName(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"legalName":{"type":"string","title":"Legal name"}}
        }""").jsonObject)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = schema,
                uischema = ControlElement("#/properties/legalName"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        val input = onAllNodes(hasSetTextAction()).onFirst()
        assertEquals(listOf("Legal name"),
            input.fetchSemanticsNode().config[SemanticsProperties.ContentDescription])
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean, kind: String) = runComposeUiTest {
        val slider = kind == "slider"
        val path = if (slider) "value" else "flag"
        val description = if (slider) "Choose a rating." else "Choose a state."
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","required":["$path"],"properties":{
                "$path":${if (slider) """{"type":"integer","minimum":0,"maximum":4,"default":2,"description":"$description"}"""
                    else """{"type":"boolean","description":"$description"}"""},
                "other":{"type":"string"}}
        }""").jsonObject)
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(if (slider) """{"value":8,"other":"x"}"""
                    else """{"other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/$path", options = mapOf(
                        "showUnfocusedDescription" to showUnfocused) + when (kind) {
                        "switch" -> mapOf("toggle" to JsonPrimitive(true))
                        "slider" -> mapOf("slider" to JsonPrimitive(true))
                        else -> emptyMap()
                    }),
                    ControlElement("#/properties/other"))),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        val input = when (kind) {
            "checkbox" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            "switch" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            else -> onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        }
        onNodeWithText(description).assertExists()
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.StateDescription].contains(description))
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText(description).assertDoesNotExist()
        assertTrue(!input.fetchSemanticsNode().config[SemanticsProperties.StateDescription].contains(description))
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText(description).assertDoesNotExist()
        input.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText(description).assertExists()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertTrue(input.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
            .startsWith("Задължително поле"))
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText(description).assertDoesNotExist()
        assertEquals(0, changes)
    }
}
