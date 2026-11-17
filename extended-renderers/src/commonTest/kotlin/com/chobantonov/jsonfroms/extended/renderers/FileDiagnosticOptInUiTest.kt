package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FileDiagnosticOptInUiTest {
    @Test fun optionPrecedenceUsesRealBooleans() {
        val control = ControlElement("#/properties/file")
        val enabled = mapOf("jsonformsExtended" to mapOf("propagateErrors" to true))
        assertEquals(false, filePropagateErrors(control, emptyMap()))
        assertEquals(true, filePropagateErrors(control, enabled))
        assertEquals(false, filePropagateErrors(ControlElement("#/properties/file",
            options = mapOf("propagateErrors" to JsonPrimitive(false))), enabled))
        assertEquals(true, filePropagateErrors(ControlElement("#/properties/file",
            options = mapOf("propagateErrors" to JsonPrimitive(true))), emptyMap()))
        assertEquals(false, filePropagateErrors(ControlElement("#/properties/file",
            options = mapOf("propagateErrors" to JsonPrimitive("true"))), enabled))
        assertEquals(true, filePropagateErrors(control, mapOf("jsonformsExtended" to
            Json.parseToJsonElement("""{"propagateErrors":true}""").jsonObject)))
    }

    @Test fun materialPublicationCanToggleWithoutErasingSchemaOrHostErrors() = publication(false)
    @Test fun cupertinoPublicationCanToggleWithoutErasingSchemaOrHostErrors() = publication(true)

    private fun publication(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["other"],"properties":{"file":{"type":"string","format":"binary"},"other":{"type":"string"}}}"""
        ).jsonObject)
        val control = ControlElement("#/properties/file", options = mapOf(
            "formatMaximum" to JsonPrimitive(3)))
        val host = listOf(ErrorObject(instancePath = "/file", keyword = "server", message = "Host rejected file"))
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        var enabled by mutableStateOf(false)
        var validity by mutableStateOf<CombinedValidity?>(null)
        var changes = 0
        val picker = ExtendedFilePickerEvents { _, _, onEvent ->
            onEvent(FilePickerEvent.Selected(JsonPrimitive("large"), 4))
        }
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false,
                    "jsonformsExtended" to mapOf("propagateErrors" to enabled),
                    ExtendedFilePickerEventsConfigKey to picker),
                additionalErrors = host,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it })
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"file":"large"}"""), data)
            assertEquals(1, changes)
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(1, validity?.additionalErrorCount)
        }
        onNodeWithText("The selected file does not meet the size limit").assertExists()

        runOnIdle { enabled = true }
        waitForIdle()
        runOnIdle {
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(2, validity?.additionalErrorCount)
            assertEquals(1, changes)
        }
        runOnIdle { enabled = false }
        waitForIdle()
        runOnIdle {
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(1, validity?.additionalErrorCount)
            assertEquals(1, changes)
        }
    }
}
