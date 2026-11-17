package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FileArrayDescriptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","required":["files"],"properties":{
            "files":{"type":"array","title":"Files","description":"Attach files.",
                "minItems":1,"items":{"type":"string","format":"binary"}},
            "other":{"type":"string"}}
    }""").jsonObject)

    @Test fun materialPickerHelpAndError() = helpAndError(false, true)
    @Test fun cupertinoPickerHelpAndError() = helpAndError(true, true)
    @Test fun materialManualHelpAndError() = helpAndError(false, false)
    @Test fun cupertinoManualHelpAndError() = helpAndError(true, false)
    @Test fun materialPickerInitialFocus() = initialFocus(false, true)
    @Test fun cupertinoPickerInitialFocus() = initialFocus(true, true)
    @Test fun materialManualInitialFocus() = initialFocus(false, false)
    @Test fun cupertinoManualInitialFocus() = initialFocus(true, false)

    private val noOpPicker = ExtendedFilePicker { _, _, _ -> }

    private fun initialFocus(cupertino: Boolean, picker: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"files":[],"other":"x"}"""),
                schema = schema,
                uischema = ControlElement("#/properties/files", options = mapOf(
                    "focus" to JsonPrimitive(true))),
                config = if (picker) mapOf(ExtendedFilePickerConfigKey to noOpPicker) else emptyMap(),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        val primary = if (picker) onNodeWithText("Choose file")
            else onAllNodes(hasSetTextAction()).onFirst()
        primary.assertIsFocused()
        assertEquals(0, changes)
    }

    private fun helpAndError(cupertino: Boolean, picker: Boolean) = runComposeUiTest {
        var showUnfocused by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var hideMarker by mutableStateOf<JsonElement>(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"files":[],"other":"x"}"""),
                schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/files", options = mapOf(
                        "showUnfocusedDescription" to showUnfocused,
                        "hideRequiredAsterisk" to hideMarker)),
                    ControlElement("#/properties/other"))),
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true)) +
                    (if (picker) mapOf(ExtendedFilePickerConfigKey to noOpPicker) else emptyMap()),
                validationMode = ValidationMode.ValidateAndShow,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val primary = if (picker) onNodeWithText("Choose file")
            else onAllNodes(hasSetTextAction()).onFirst()
        onNodeWithText("Attach files.").assertExists()
        onNodeWithText("Files*").assertDoesNotExist()
        assertEquals("Required field. Attach files.",
            primary.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(primary.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        runOnIdle { showUnfocused = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Attach files.").assertDoesNotExist()
        assertEquals("Required field", primary.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Attach files.").assertDoesNotExist()
        primary.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Attach files.").assertExists()
        runOnIdle { locale = "bg-BG"; hideMarker = JsonPrimitive(false) }
        waitForIdle()
        onNodeWithText("Files*").assertExists()
        val localizedPrimary = if (picker) onNodeWithText("Избери файл") else primary
        assertEquals("Задължително поле. Attach files.",
            localizedPrimary.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { hideMarker = JsonPrimitive("true") }
        waitForIdle()
        onNodeWithText("Files*").assertExists()
        onAllNodes(hasSetTextAction()).onLast()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Attach files.").assertDoesNotExist()
        assertTrue(localizedPrimary.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
    }
}
