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
import androidx.compose.ui.test.onNodeWithContentDescription
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
class RequiredArrayChoiceSemanticsUiTest {
    @Test fun materialCheckboxRequiredCue() = requiredCue(false, "checkbox")
    @Test fun cupertinoCheckboxRequiredCue() = requiredCue(true, "checkbox")
    @Test fun materialMultiSelectRequiredCue() = requiredCue(false, "multi-select")
    @Test fun cupertinoMultiSelectRequiredCue() = requiredCue(true, "multi-select")
    @Test fun materialFiniteChipsRequiredCue() = requiredCue(false, "chips")
    @Test fun cupertinoFiniteChipsRequiredCue() = requiredCue(true, "chips")
    @Test fun materialFreeChipsRequiredCue() = requiredCue(false, "free-chips")
    @Test fun cupertinoFreeChipsRequiredCue() = requiredCue(true, "free-chips")

    private fun requiredCue(cupertino: Boolean, variant: String) = runComposeUiTest {
        val free = variant == "free-chips"
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"teams":{"title":"Teams","type":"array","uniqueItems":true,
                "items":${if (free) """{"type":"string"}""" else """{"type":"string","enum":["ops","fin"]}"""}}},
            "required":["teams"]
        }""").jsonObject)
        val initial = Json.parseToJsonElement("""{"teams":["ops"]}""")
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ControlElement("#/properties/teams", options = mapOf(
                    "variant" to JsonPrimitive(if (free) "chips" else variant),
                    "hideRequiredAsterisk" to hideMarker)),
                config = mapOf("hideRequiredAsterisk" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val control = when (variant) {
            "checkbox" -> onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).onFirst()
            "free-chips" -> onAllNodes(hasSetTextAction()).onFirst()
            else -> onNodeWithContentDescription("Add choice: Teams")
        }
        waitForIdle()
        assertEquals("Required field", control.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Teams*").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Задължително поле"))
            .onFirst().assertExists()
        runOnIdle { hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Teams*").assertExists()
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Teams*").assertExists()
        assertEquals(0, changes)
    }
}
