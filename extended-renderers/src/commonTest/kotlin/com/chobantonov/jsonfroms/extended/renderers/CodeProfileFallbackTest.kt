package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsChange
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CodeProfileFallbackTest {
    private val control = ControlElement("#/properties/source", options = mapOf(
        "format" to JsonPrimitive("code"), "language" to JsonPrimitive("json"),
    ))
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"source":{"type":"string"}}
    }""") as JsonObject)

    @Test fun materialReportsMonacoUnavailableAndKeepsNativeEditing() = mounted(materialExtendedRenderers)

    @Test fun cupertinoReportsMonacoUnavailableAndKeepsNativeEditing() = mounted(cupertinoExtendedRenderers)

    @Test fun materialTextFallbackKeepsAdornmentsOutsideStoredCode() = adornedFallback(materialExtendedRenderers)

    @Test fun cupertinoTextFallbackKeepsAdornmentsOutsideStoredCode() = adornedFallback(cupertinoExtendedRenderers)

    private fun adornedFallback(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"source":"first"}"""),
                schema = schema,
                uischema = control.copy(options = control.options + mapOf(
                    "startAdornment" to JsonPrimitive("prefix"),
                    "endAdornment" to JsonPrimitive("suffix"),
                )),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = changes::add,
            )
        }
        onNodeWithText("prefix").assertExists()
        onNodeWithText("suffix").assertExists()
        onNodeWithText("first").performTextReplacement("second")
        waitForIdle()
        assertEquals(JsonPrimitive("second"), (changes.single().data as JsonObject)["source"])
        assertEquals(listOf("editor.monacoUnavailable"), diagnostics.map { it.code })
    }

    private fun mounted(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"source":"first"}"""),
                schema = schema,
                uischema = control,
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = changes::add,
            )
        }
        onNodeWithText("Monaco editor is unavailable on this host").assertExists()
        waitForIdle()
        assertEquals(listOf("editor.monacoUnavailable"), diagnostics.map { it.code })
        assertTrue(changes.isEmpty())
        onNodeWithText("first").performTextReplacement("second")
        waitForIdle()
        assertEquals(JsonPrimitive("second"), (changes.last().data as JsonObject)["source"])
    }

    @Test fun convertedJsonRequestDoesNotCommitOnMount() = runComposeUiTest {
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"settings":{"name":"Ada"}}"""),
                schema = JsonSchema(Json.parseToJsonElement("""{
                  "type":"object","properties":{"settings":{"type":"object","properties":{"name":{"type":"string"}}}}
                }""") as JsonObject),
                uischema = ControlElement("#/properties/settings", options = mapOf(
                    "format" to JsonPrimitive("code"), "language" to JsonPrimitive("json"),
                    "convertJson" to JsonPrimitive(true),
                )),
                renderers = materialExtendedRenderers,
                onChange = changes::add,
            )
        }
        onNodeWithText("Monaco editor is unavailable on this host").assertExists()
        waitForIdle()
        assertTrue(changes.isEmpty())
    }

    @Test fun materialObjectFallbackReportsUnsupportedAdornments() = objectAdornmentFallback(materialExtendedRenderers)
    @Test fun cupertinoObjectFallbackReportsUnsupportedAdornments() = objectAdornmentFallback(cupertinoExtendedRenderers)

    private fun objectAdornmentFallback(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val changes = mutableListOf<JsonFormsChange>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"settings":{"name":"Ada"}}"""),
                schema = JsonSchema(Json.parseToJsonElement("""{
                  "type":"object","properties":{"settings":{"type":"object","properties":{"name":{"type":"string"}}}}
                }""") as JsonObject),
                uischema = ControlElement("#/properties/settings", options = mapOf(
                    "format" to JsonPrimitive("code"),
                    "language" to JsonPrimitive("json"),
                    "convertJson" to JsonPrimitive(true),
                    "startAdornment" to JsonPrimitive("prefix"),
                )),
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = changes::add,
            )
        }
        waitForIdle()
        onNodeWithText("prefix").assertDoesNotExist()
        assertEquals(setOf("editor.monacoUnavailable", "adornment.unsupported"), diagnostics.map { it.code }.toSet())
        assertTrue(changes.isEmpty())
    }

    @Test fun materialNoCatalogCodeNoticeFollowsLocale() = noCatalogLocale(materialExtendedRenderers)
    @Test fun cupertinoNoCatalogCodeNoticeFollowsLocale() = noCatalogLocale(cupertinoExtendedRenderers)

    private fun noCatalogLocale(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val locale = mutableStateOf("en-US")
        val changes = mutableListOf<JsonFormsChange>()
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"source":"first"}"""),
                schema = schema,
                uischema = control,
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                i18n = JsonFormsI18nState(locale = locale.value),
                onChange = changes::add,
            )
        }
        onNodeWithText("Monaco editor is unavailable on this host").assertExists()
        waitForIdle()
        assertEquals("Monaco editor is unavailable on this host", diagnostics.last().message)
        runOnIdle { locale.value = "bg-BG" }
        onNodeWithText("Редакторът Monaco не е наличен на този хост").assertExists()
        waitForIdle()
        assertEquals("Редакторът Monaco не е наличен на този хост", diagnostics.last().message)
        assertTrue(changes.isEmpty())
    }
}
