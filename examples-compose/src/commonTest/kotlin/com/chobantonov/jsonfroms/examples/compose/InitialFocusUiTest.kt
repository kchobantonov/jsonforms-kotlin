package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
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
class InitialFocusUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
    ).jsonObject)

    @Test fun materialInitialFocusDoesNotReclaimOnUpdates() = initialFocus(false)
    @Test fun cupertinoInitialFocusDoesNotReclaimOnUpdates() = initialFocus(true)
    @Test fun materialReadonlyInputCanTakeInitialFocus() = readonlyFocus(false)
    @Test fun cupertinoReadonlyInputCanTakeInitialFocus() = readonlyFocus(true)
    @Test fun materialLocalFalseSuppressesGlobalFocus() = localFalse(false)
    @Test fun cupertinoLocalFalseSuppressesGlobalFocus() = localFalse(true)
    @Test fun materialStringTrueDoesNotRequestFocus() = stringFocus(false)
    @Test fun cupertinoStringTrueDoesNotRequestFocus() = stringFocus(true)
    @Test fun materialRadioFocusesSelectedChoiceOnce() = radioFocus(false)
    @Test fun cupertinoRadioFocusesSelectedChoiceOnce() = radioFocus(true)
    @Test fun materialRadioWithoutSelectionFocusesFirstChoice() = unselectedRadioFocus(false)
    @Test fun cupertinoRadioWithoutSelectionFocusesFirstChoice() = unselectedRadioFocus(true)
    @Test fun materialRadioLocalFalseOverridesGlobalFocus() = radioSuppressedFocus(false)
    @Test fun cupertinoRadioLocalFalseOverridesGlobalFocus() = radioSuppressedFocus(true)
    @Test fun materialRadioStringTrueDoesNotRequestFocus() = radioStringFocus(false)
    @Test fun cupertinoRadioStringTrueDoesNotRequestFocus() = radioStringFocus(true)
    @Test fun materialOneOfRadioFocusesSelectedChoice() = oneOfRadioFocus(false)
    @Test fun cupertinoOneOfRadioFocusesSelectedChoice() = oneOfRadioFocus(true)

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"first":"A","second":"B"}"""))
        var request by mutableStateOf(true)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/first", options = mapOf("focus" to JsonPrimitive(request))),
                    ControlElement("#/properties/second"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        onAllNodes(hasSetTextAction()).onLast().performClick()
        onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        runOnIdle {
            data = Json.parseToJsonElement("""{"first":"Changed","second":"B"}""")
            request = false
            locale = "bg"
        }
        waitForIdle()
        runOnIdle { request = true }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onFirst().assertIsNotFocused()
        onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        assertEquals(0, changes)
    }

    private fun readonlyFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"first":"A"}"""), schema = schema,
                uischema = ControlElement("#/properties/first",
                    options = mapOf("focus" to JsonPrimitive(true), "readonly" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("separateReadonlyFromDisabled" to true),
            )
        }
        waitForIdle()
        onNodeWithText("A").assertIsFocused()
    }

    private fun localFalse(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"first":"A"}"""), schema = schema,
                uischema = ControlElement("#/properties/first",
                    options = mapOf("focus" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("focus" to true),
            )
        }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onFirst().assertIsNotFocused()
    }

    private fun stringFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"first":"A"}"""), schema = schema,
                uischema = ControlElement("#/properties/first",
                    options = mapOf("focus" to JsonPrimitive("true"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onFirst().assertIsNotFocused()
    }

    private val radioSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"choice":{"type":"string","enum":["Red","Blue"]},"other":{"type":"string"}}}"""
    ).jsonObject)

    private fun radioFocus(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choice":"Blue","other":"X"}"""))
        var request by mutableStateOf(true)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = radioSchema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/choice", options = mapOf(
                        "format" to JsonPrimitive("radio"), "focus" to JsonPrimitive(request))),
                    ControlElement("#/properties/other"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        waitForIdle()
        onNodeWithContentDescription("Blue").assertIsFocused()
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        runOnIdle {
            data = Json.parseToJsonElement("""{"choice":"Red","other":"X"}""")
            request = false
            locale = "bg"
        }
        waitForIdle()
        runOnIdle { request = true }
        waitForIdle()
        onNodeWithContentDescription("Blue").assertIsNotFocused()
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        assertEquals(0, changes)
    }

    private fun unselectedRadioFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"other":"X"}"""), schema = radioSchema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNodeWithContentDescription("Red").assertIsFocused()
    }

    private fun radioSuppressedFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":"Blue"}"""), schema = radioSchema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "focus" to JsonPrimitive(false))),
                config = mapOf("focus" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNodeWithContentDescription("Blue").assertIsNotFocused()
    }

    private fun oneOfRadioFocus(cupertino: Boolean) = runComposeUiTest {
        val oneOfSchema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"choice":{"oneOf":[
                {"const":"red","title":"Red"},{"const":"blue","title":"Blue"}
            ]}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":"blue"}"""), schema = oneOfSchema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNodeWithContentDescription("Blue").assertIsFocused()
    }

    private fun radioStringFocus(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"choice":"Blue"}"""), schema = radioSchema,
                uischema = ControlElement("#/properties/choice", options = mapOf(
                    "format" to JsonPrimitive("radio"), "focus" to JsonPrimitive("true"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        waitForIdle()
        onNodeWithContentDescription("Blue").assertIsNotFocused()
    }
}
