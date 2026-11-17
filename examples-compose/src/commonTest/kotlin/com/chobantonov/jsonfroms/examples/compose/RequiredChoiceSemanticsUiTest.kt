package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RequiredChoiceSemanticsUiTest {
    @Test fun materialRadioKeepsRequiredState() = requiredState(false, "radio")
    @Test fun cupertinoRadioKeepsRequiredState() = requiredState(true, "radio")
    @Test fun materialEnumSelectKeepsRequiredState() = requiredState(false, "enum")
    @Test fun cupertinoEnumSelectKeepsRequiredState() = requiredState(true, "enum")
    @Test fun materialOneOfSelectKeepsRequiredState() = requiredState(false, "oneOf")
    @Test fun cupertinoOneOfSelectKeepsRequiredState() = requiredState(true, "oneOf")
    @Test fun materialSearchableSelectKeepsRequiredState() = requiredState(false, "search")
    @Test fun cupertinoSearchableSelectKeepsRequiredState() = requiredState(true, "search")

    private fun requiredState(cupertino: Boolean, kind: String) = runComposeUiTest {
        val oneOf = kind == "oneOf"
        val choices = if (oneOf) """"oneOf":[
            {"const":"red","title":"Red"},{"const":"blue","title":"Blue"}
        ]""" else """"type":"string","enum":["Red","Blue"]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{"title":"Choice",$choices}},
            "required":["choice"]
        }""").jsonObject)
        val selected = if (oneOf) "blue" else "Blue"
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            val options = when (kind) {
                "radio" -> mapOf("format" to JsonPrimitive("radio"))
                "search" -> mapOf("autocomplete" to JsonPrimitive(true))
                else -> emptyMap()
            }
            JsonForms(data = Json.parseToJsonElement("""{"choice":"$selected"}"""), schema = schema,
                uischema = ControlElement("#/properties/choice", options = options +
                    ("hideRequiredAsterisk" to hideMarker)),
                config = mapOf("hideRequiredAsterisk" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val input = when (kind) {
            "radio" -> onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).onFirst()
            "search" -> onAllNodes(hasSetTextAction()).onFirst()
            else -> onNodeWithText("Blue")
        }
        waitForIdle()
        assertEquals("Required field", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Choice*").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        assertEquals("Задължително поле", input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Choice*").assertExists()
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Choice*").assertExists()
        assertEquals(0, changes)
    }
}
