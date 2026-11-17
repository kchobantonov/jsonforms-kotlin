package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FileScalarPickerUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object","required":["file"],
        "properties":{"file":{"type":"string","format":"binary","title":"File",
            "description":"Attach a file."},"other":{"type":"string"}}}""").jsonObject)

    @Test fun materialPickerShowsFilenameAndClear() = pickerValue(false)
    @Test fun cupertinoPickerShowsFilenameAndClear() = pickerValue(true)
    @Test fun materialPickerShowsInvalidImportedValue() = invalidImported(false)
    @Test fun cupertinoPickerShowsInvalidImportedValue() = invalidImported(true)

    private fun invalidImported(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"file":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/file"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        assertTrue(onNodeWithText("Choose file").fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: {\"raw\":true}"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Invalid value: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"file":null}""")
        }
        onNodeWithText("Невалидна стойност: null").assertExists()
        onNodeWithText("Изчисти").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("{}"), data)
            assertEquals(1, changes)
        }
    }
    @Test fun materialPickerHelpAndError() = helpAndError(false)
    @Test fun cupertinoPickerHelpAndError() = helpAndError(true)
    @Test fun materialPickerInitialFocus() = initialFocus(false)
    @Test fun cupertinoPickerInitialFocus() = initialFocus(true)
    @Test fun materialWithoutPickerKeepsManualInput() = manualFallback(false)
    @Test fun cupertinoWithoutPickerKeepsManualInput() = manualFallback(true)
    @Test fun materialPayloadOnlyFilenameFallback() = payloadOnlyFallback(false)
    @Test fun cupertinoPayloadOnlyFilenameFallback() = payloadOnlyFallback(true)
    @Test fun materialPickerRespectsActionPermissions() = actionPermissions(false)
    @Test fun cupertinoPickerRespectsActionPermissions() = actionPermissions(true)

    private fun payloadOnlyFallback(cupertino: Boolean) = runComposeUiTest {
        val payloadSchema = JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{
            "file":{"type":"string","contentEncoding":"base64"}}}""").jsonObject)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"file":"YQ=="}"""), schema = payloadSchema,
                uischema = ControlElement("#/properties/file"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                i18n = JsonFormsI18nState(locale = locale), onChange = { changes++ })
        }
        onNodeWithContentDescription("Attachment 1").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Прикачен файл 1").assertExists()
        assertEquals(0, changes)
    }

    private fun actionPermissions(cupertino: Boolean) = runComposeUiTest {
        val original = Json.parseToJsonElement("""{"file":"data:text/plain;filename=old.txt;base64,b2xk"}""")
        var clearable by mutableStateOf(JsonPrimitive(true))
        var readonly by mutableStateOf(JsonPrimitive(false))
        var changes = 0
        setContent {
            JsonForms(data = original, schema = schema,
                uischema = ControlElement("#/properties/file", options = mapOf(
                    "clearable" to clearable, "readonly" to readonly)),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("clearable" to false,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                onChange = { changes++ })
        }
        onNodeWithText("Choose file").assertIsEnabled()
        onNodeWithText("Clear").assertIsEnabled()
        runOnIdle { readonly = JsonPrimitive(true) }
        onNodeWithText("Choose file").assertIsNotEnabled()
        onNodeWithText("Clear").assertIsNotEnabled()
        runOnIdle { readonly = JsonPrimitive(false); clearable = JsonPrimitive(false) }
        onNodeWithText("Choose file").assertIsEnabled()
        onNodeWithText("Clear").assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun pickerValue(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"file":"data:text/plain;filename=old.txt;base64,b2xk"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/file"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    onEvent(FilePickerEvent.Selected(JsonPrimitive(
                        "data:text/plain;filename=new.txt;base64,bmV3"), 3))
                }),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("old.txt").assertExists()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals(0, changes)
        onNodeWithText("Choose file").performClick()
        onNodeWithContentDescription("new.txt").assertExists()
        assertEquals(Json.parseToJsonElement(
            """{"file":"data:text/plain;filename=new.txt;base64,bmV3"}"""), data)
        onNodeWithText("Clear").performClick()
        assertEquals(Json.parseToJsonElement("{}"), data)
        onNodeWithText("No file").assertExists()
    }

    private fun helpAndError(cupertino: Boolean) = runComposeUiTest {
        var showUnfocused by mutableStateOf(JsonPrimitive(false))
        var hideMarker by mutableStateOf(JsonPrimitive(true))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"other":"x"}"""), schema = schema,
                uischema = VerticalLayout(elements = listOf(
                    ControlElement("#/properties/file", options = mapOf(
                        "showUnfocusedDescription" to showUnfocused,
                        "hideRequiredAsterisk" to hideMarker)),
                    ControlElement("#/properties/other"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("showUnfocusedDescription" to JsonPrimitive(true),
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ })
        }
        val choose = onNodeWithText("Choose file")
        onNodeWithText("Attach a file.").assertDoesNotExist()
        onNodeWithText("File*").assertDoesNotExist()
        assertEquals("Required field", choose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertTrue(choose.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        choose.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Attach a file.").assertExists()
        assertEquals("Required field. Attach a file.",
            choose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { locale = "bg-BG"; hideMarker = JsonPrimitive(false) }
        val localizedChoose = onNodeWithText("Избери файл")
        onNodeWithText("File*").assertExists()
        assertEquals("Задължително поле. Attach a file.",
            localizedChoose.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        runOnIdle { showUnfocused = JsonPrimitive("true") }
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Attach a file.").assertDoesNotExist()
        runOnIdle { showUnfocused = JsonPrimitive(true) }
        onNodeWithText("Attach a file.").assertExists()
        assertEquals(0, changes)
    }

    private fun initialFocus(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = ControlElement("#/properties/file", options = mapOf("focus" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                onChange = { changes++ })
        }
        onNodeWithText("Choose file").assertIsFocused()
        assertEquals(0, changes)
    }

    private fun manualFallback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/file"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("manual")
        assertEquals(Json.parseToJsonElement("""{"file":"manual"}"""), data)
    }
}
