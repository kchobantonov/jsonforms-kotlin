package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
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
class TemporalSaveFormatUiTest {
    @Test fun materialDateRejectsCoarseSaveFormat() = verify(false, "date")
    @Test fun cupertinoDateRejectsCoarseSaveFormat() = verify(true, "date")
    @Test fun materialTimeRejectsShortSaveFormat() = verify(false, "time")
    @Test fun cupertinoTimeRejectsShortSaveFormat() = verify(true, "time")
    @Test fun materialDateTimeRejectsUnzonedSaveFormat() = verify(false, "date-time")
    @Test fun cupertinoDateTimeRejectsUnzonedSaveFormat() = verify(true, "date-time")

    private fun verify(cupertino: Boolean, format: String) = runComposeUiTest {
        val (displayKey, saveKey, displayFormat, rejectedFormat, input) = when (format) {
            "date" -> listOf("dateFormat", "dateSaveFormat", "YYYY-MM-DD", "YYYY-MM", "2024-06-15")
            "time" -> listOf("timeFormat", "timeSaveFormat", "HH:mm:ssZ", "HH:mm", "10:30:15Z")
            else -> listOf("dateTimeFormat", "dateTimeSaveFormat", "YYYY-MM-DDTHH:mm:ssZ",
                "YYYY-MM-DD HH:mm", "2024-06-15T10:30:15Z")
        }
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format"}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    displayKey to JsonPrimitive(displayFormat),
                    saveKey to JsonPrimitive(rejectedFormat),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = { data = it.data; writes++ })
        }
        waitForIdle()
        assertEquals(listOf("temporal.saveFormatIncompatible"), diagnostics.map { it.code })
        assertEquals(0, writes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(input)
        runOnIdle {
            assertEquals(1, writes)
            val stored = (data.jsonObject["value"] as JsonPrimitive).content
            when (format) {
                "date" -> assertEquals("2024-06-15", stored)
                "time" -> assertEquals("10:30:15Z", stored)
                else -> assertTrue(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(stored), stored)
            }
        }
        assertEquals(1, diagnostics.size)
    }
}
