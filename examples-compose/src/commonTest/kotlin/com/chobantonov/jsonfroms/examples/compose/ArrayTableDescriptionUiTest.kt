package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
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
class ArrayTableDescriptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","required":["items"],"properties":{
            "items":{"type":"array","title":"Items","description":"Add an item.",
                "minItems":1,"items":{"type":"object","properties":{"name":{"type":"string"}}}},
            "other":{"type":"string"}}
    }""").jsonObject)

    @Test fun materialTableHelpAndError() = helpAndError(false)
    @Test fun cupertinoTableHelpAndError() = helpAndError(true)
    @Test fun materialTableInitialFocus() = initialFocus(false)
    @Test fun cupertinoTableInitialFocus() = initialFocus(true)

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"items":[],"other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/items", options = mapOf(
                        "table" to JsonPrimitive(true), "focus" to JsonPrimitive(true))),
                    ControlElement("#/properties/other"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("Add item").assertIsFocused()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"items":[],"other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/items", options = mapOf(
                        "table" to JsonPrimitive(true),
                        "showUnfocusedDescription" to showUnfocused,
                        "hideRequiredAsterisk" to hideMarker)),
                    ControlElement("#/properties/other"))),
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true)),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        val add = onNodeWithContentDescription("Add item")
        onNodeWithText("Add an item.").assertExists()
        onNodeWithText("Items*").assertDoesNotExist()
        assertEquals("Required field. Add an item.",
            add.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(add.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Add an item.").assertDoesNotExist()
        assertEquals("Required field", add.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Add an item.").assertDoesNotExist()
        add.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Add an item.").assertExists()
        runOnIdle { locale = "bg-BG"; hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Items*").assertExists()
        assertEquals("Задължително поле. Add an item.",
            add.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Items*").assertExists()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Add an item.").assertDoesNotExist()
        assertTrue(add.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
    }
}
