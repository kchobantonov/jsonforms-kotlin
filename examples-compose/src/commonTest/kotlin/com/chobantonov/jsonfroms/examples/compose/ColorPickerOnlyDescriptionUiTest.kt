package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsFocused
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
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ColorPickerOnlyDescriptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","required":["shade"],"properties":{
            "shade":{"type":"string","title":"Shade","format":"color",
                "description":"Choose a shade.","minLength":9},
            "other":{"type":"string"}}
    }""").jsonObject)

    @Test fun materialHelpAndError() = helpAndError(false)
    @Test fun cupertinoHelpAndError() = helpAndError(true)
    @Test fun materialInitialFocus() = initialFocus(false)
    @Test fun cupertinoInitialFocus() = initialFocus(true)
    @Test fun materialInvalidImportedColorIsAssociated() = invalidImportedColor(false)
    @Test fun cupertinoInvalidImportedColorIsAssociated() = invalidImportedColor(true)

    private fun invalidImportedColor(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"garbage","other":"x"}"""),
                schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val choose = onNodeWithContentDescription("Choose color")
        onNodeWithText("Invalid color.").assertExists()
        assertTrue(choose.fetchSemanticsNode().config[SemanticsProperties.Error]
            .contains("Invalid color."))
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onNodeWithText("Невалиден цвят.").assertExists()
        assertTrue(onNodeWithContentDescription("Избери цвят")
            .fetchSemanticsNode().config[SemanticsProperties.Error].contains("Невалиден цвят."))
        assertEquals(0, changes)
    }

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000","other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/shade", options = mapOf(
                        "colorTextEntry" to JsonPrimitive(false), "focus" to JsonPrimitive(true))),
                    ControlElement("#/properties/other"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Choose color").assertIsFocused()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000","other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/shade", options = mapOf(
                        "colorTextEntry" to JsonPrimitive(false),
                        "showUnfocusedDescription" to showUnfocused,
                        "hideRequiredAsterisk" to hideMarker)),
                    ControlElement("#/properties/other"))),
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true)),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val choose = onNodeWithContentDescription("Choose color")
        onNodeWithText("Choose a shade.").assertExists()
        onNodeWithText("Shade*").assertDoesNotExist()
        assertEquals("Required field. Choose a shade.",
            choose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(choose.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Choose a shade.").assertDoesNotExist()
        assertEquals("Required field", choose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Choose a shade.").assertDoesNotExist()
        choose.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a shade.").assertExists()
        runOnIdle { locale = "bg-BG"; hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Shade*").assertExists()
        val localizedChoose = onNodeWithContentDescription("Избери цвят")
        assertEquals("Задължително поле. Choose a shade.",
            localizedChoose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Shade*").assertExists()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Choose a shade.").assertDoesNotExist()
        assertTrue(localizedChoose.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
    }
}
