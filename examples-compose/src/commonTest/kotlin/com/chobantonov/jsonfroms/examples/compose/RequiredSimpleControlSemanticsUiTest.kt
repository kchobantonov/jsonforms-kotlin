package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RequiredSimpleControlSemanticsUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{
            "flag":{"type":"boolean","title":"Active"},
            "value":{"type":"integer","title":"Rating","minimum":0,"maximum":4,"default":2}
        },"required":["flag","value"]
    }""").jsonObject)

    @Test fun materialCheckboxKeepsRequiredState() = requiredState(false, "checkbox")
    @Test fun cupertinoCheckboxKeepsRequiredState() = requiredState(true, "checkbox")

    @Test fun materialRequiredMarkerReadsTopLevelConfig() = topLevelMarker(false)
    @Test fun cupertinoRequiredMarkerReadsTopLevelConfig() = topLevelMarker(true)

    private fun topLevelMarker(cupertino: Boolean) = runComposeUiTest {
        var hide by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"flag":true,"value":2}"""), schema = schema,
                uischema = ControlElement("#/properties/flag"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(
                    "hideRequiredAsterisk" to hide,
                    "jsonformsExtended" to mapOf("hideRequiredAsterisk" to !hide),
                ),
                onChange = { changes++ },
            )
        }
        onNodeWithText("Active*").assertDoesNotExist()
        runOnIdle { hide = false }
        waitForIdle()
        onNodeWithText("Active*").assertExists()
        assertEquals(0, changes)
    }
    @Test fun materialSwitchKeepsRequiredState() = requiredState(false, "switch")
    @Test fun cupertinoSwitchKeepsRequiredState() = requiredState(true, "switch")
    @Test fun materialSliderKeepsRequiredState() = requiredState(false, "slider")
    @Test fun cupertinoSliderKeepsRequiredState() = requiredState(true, "slider")
    @Test fun materialSingleValueSliderButtonKeepsRequiredState() = singleValueSlider(false)
    @Test fun cupertinoSingleValueSliderButtonKeepsRequiredState() = singleValueSlider(true)
    @Test fun materialMissingCheckboxCombinesRequiredAndUnset() = missingBoolean(false, false)
    @Test fun cupertinoMissingCheckboxCombinesRequiredAndUnset() = missingBoolean(true, false)
    @Test fun materialMissingSwitchCombinesRequiredAndUnset() = missingBoolean(false, true)
    @Test fun cupertinoMissingSwitchCombinesRequiredAndUnset() = missingBoolean(true, true)

    private fun requiredState(cupertino: Boolean, kind: String) = runComposeUiTest {
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            val path = if (kind == "slider") "value" else "flag"
            val variant = when (kind) {
                "switch" -> mapOf("toggle" to JsonPrimitive(true))
                "slider" -> mapOf("slider" to JsonPrimitive(true))
                else -> emptyMap()
            }
            JsonForms(data = Json.parseToJsonElement("""{"flag":true,"value":2}"""), schema = schema,
                uischema = ControlElement("#/properties/$path", options = variant +
                    ("hideRequiredAsterisk" to hideMarker)),
                config = mapOf("hideRequiredAsterisk" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val input = when (kind) {
            "checkbox" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            "switch" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            else -> onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        }
        val label = if (kind == "slider") "Rating" else "Active"
        waitForIdle()
        val english = input.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue(english.startsWith("Required field"))
        onNodeWithText("$label*").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        val bulgarian = input.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue(bulgarian.startsWith("Задължително поле"))
        runOnIdle { hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("$label*").assertExists()
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("$label*").assertExists()
        assertEquals(0, changes)
    }

    private fun singleValueSlider(cupertino: Boolean) = runComposeUiTest {
        val singleSchema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"value":{
                "type":"integer","title":"Rating","minimum":2,"maximum":2,"default":2
            }},"required":["value"]
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = singleSchema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "slider" to JsonPrimitive(true), "hideRequiredAsterisk" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        val button = onNodeWithText("Select value")
        assertEquals("Required field. Not set",
            button.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Rating*").assertDoesNotExist()
    }

    private fun missingBoolean(cupertino: Boolean, toggle: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":2}"""), schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "toggle" to JsonPrimitive(toggle),
                    "hideRequiredAsterisk" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale))
        }
        val role = if (toggle) Role.Switch else Role.Checkbox
        val input = onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, role))
        waitForIdle()
        assertEquals("Required field. Not set",
            input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { locale = "bg" }
        waitForIdle()
        assertEquals("Задължително поле. Не е зададено",
            input.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }
}
