package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SimpleControlInitialFocusUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{
            "flag":{"type":"boolean"},
            "value":{"type":"integer","minimum":0,"maximum":4,"default":2},
            "other":{"type":"string"}
        }
    }""").jsonObject)

    @Test fun materialCheckboxFocusesOnce() = focus(false, "checkbox")
    @Test fun cupertinoCheckboxFocusesOnce() = focus(true, "checkbox")
    @Test fun materialSwitchFocusesOnce() = focus(false, "switch")
    @Test fun cupertinoSwitchFocusesOnce() = focus(true, "switch")
    @Test fun materialSliderFocusesOnce() = focus(false, "slider")
    @Test fun cupertinoSliderFocusesOnce() = focus(true, "slider")
    @Test fun materialDisabledCheckboxDoesNotFocusLater() = disabledCheckbox(false)
    @Test fun cupertinoDisabledCheckboxDoesNotFocusLater() = disabledCheckbox(true)
    @Test fun materialSwitchRejectsStringFocus() = stringSwitch(false)
    @Test fun cupertinoSwitchRejectsStringFocus() = stringSwitch(true)
    @Test fun materialSingleValueSliderButtonFocuses() = singleValueSlider(false)
    @Test fun cupertinoSingleValueSliderButtonFocuses() = singleValueSlider(true)

    private fun focus(cupertino: Boolean, kind: String) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"flag":true,"value":2,"other":"X"}"""))
        var request by mutableStateOf(true)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            val path = if (kind == "slider") "value" else "flag"
            val mode = when (kind) {
                "switch" -> mapOf("toggle" to JsonPrimitive(true))
                "slider" -> mapOf("slider" to JsonPrimitive(true))
                else -> emptyMap()
            }
            JsonForms(data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/$path", options = mode + ("focus" to JsonPrimitive(request))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        val target = when (kind) {
            "checkbox" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            "switch" -> onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            else -> onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        }
        waitForIdle()
        target.assertIsFocused()
        val other = onAllNodes(hasSetTextAction()).onFirst()
        other.performClick()
        other.assertIsFocused()
        runOnIdle {
            data = Json.parseToJsonElement("""{"flag":false,"value":3,"other":"X"}""")
            request = false
            locale = "bg"
        }
        waitForIdle()
        runOnIdle { request = true }
        waitForIdle()
        target.assertIsNotFocused()
        other.assertIsFocused()
        assertEquals(0, changes)
    }

    private fun disabledCheckbox(cupertino: Boolean) = runComposeUiTest {
        var readonly by mutableStateOf(true)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"flag":true}"""), schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "focus" to JsonPrimitive(true), "readonly" to JsonPrimitive(readonly))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        val checkbox = onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        waitForIdle()
        checkbox.assertIsNotEnabled()
        runOnIdle { readonly = false }
        waitForIdle()
        checkbox.assertIsNotFocused()
    }

    private fun stringSwitch(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"flag":true}"""), schema = schema,
                uischema = ControlElement("#/properties/flag", options = mapOf(
                    "toggle" to JsonPrimitive(true), "focus" to JsonPrimitive("true"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertIsNotFocused()
    }

    private fun singleValueSlider(cupertino: Boolean) = runComposeUiTest {
        val singleSchema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"value":{
                "type":"integer","minimum":2,"maximum":2,"default":2
            }}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = singleSchema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "slider" to JsonPrimitive(true), "focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNodeWithText("Select value").assertIsFocused()
    }
}
