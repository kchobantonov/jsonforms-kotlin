package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePicker
import com.chobantonov.jsonfroms.extended.renderers.ExtendedFilePickerConfigKey
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class UnsupportedAdornmentsUiTest {
    @Test fun materialBooleanReportsUnsupportedAdornment() = unsupported(false, "boolean")
    @Test fun cupertinoBooleanReportsUnsupportedAdornment() = unsupported(true, "boolean")
    @Test fun materialBooleanReportsUnsupportedAdornmentInBulgarian() = unsupported(false, "boolean", "bg")
    @Test fun cupertinoBooleanReportsUnsupportedAdornmentInBulgarian() = unsupported(true, "boolean", "bg")
    @Test fun materialSliderReportsUnsupportedAdornment() = unsupported(false, "slider")
    @Test fun cupertinoSliderReportsUnsupportedAdornment() = unsupported(true, "slider")
    @Test fun materialRadioReportsUnsupportedAdornment() = unsupported(false, "radio")
    @Test fun cupertinoRadioReportsUnsupportedAdornment() = unsupported(true, "radio")
    @Test fun materialOneOfRadioReportsUnsupportedAdornment() = unsupported(false, "oneOfRadio")
    @Test fun cupertinoOneOfRadioReportsUnsupportedAdornment() = unsupported(true, "oneOfRadio")
    @Test fun materialOtpReportsUnsupportedAdornment() = unsupported(false, "otp")
    @Test fun cupertinoOtpReportsUnsupportedAdornment() = unsupported(true, "otp")
    @Test fun materialChipsReportsUnsupportedAdornment() = unsupported(false, "chips")
    @Test fun cupertinoChipsReportsUnsupportedAdornment() = unsupported(true, "chips")
    @Test fun materialMultiSelectReportsUnsupportedAdornment() = unsupported(false, "multi-select")
    @Test fun cupertinoMultiSelectReportsUnsupportedAdornment() = unsupported(true, "multi-select")
    @Test fun materialNullReportsUnsupportedAdornment() = unsupported(false, "null")
    @Test fun cupertinoNullReportsUnsupportedAdornment() = unsupported(true, "null")
    @Test fun materialFileArrayReportsUnsupportedAdornment() = unsupported(false, "fileArray")
    @Test fun cupertinoFileArrayReportsUnsupportedAdornment() = unsupported(true, "fileArray")
    @Test fun materialFilePickerReportsUnsupportedAdornment() = unsupported(false, "filePicker")
    @Test fun cupertinoFilePickerReportsUnsupportedAdornment() = unsupported(true, "filePicker")
    @Test fun materialObjectReportsUnsupportedAdornment() = unsupported(false, "object")
    @Test fun cupertinoObjectReportsUnsupportedAdornment() = unsupported(true, "object")
    @Test fun materialArrayReportsUnsupportedAdornment() = unsupported(false, "array")
    @Test fun cupertinoArrayReportsUnsupportedAdornment() = unsupported(true, "array")
    @Test fun materialEnumArrayReportsUnsupportedAdornment() = unsupported(false, "enumArray")
    @Test fun cupertinoEnumArrayReportsUnsupportedAdornment() = unsupported(true, "enumArray")
    @Test fun materialTupleReportsUnsupportedAdornment() = unsupported(false, "tuple")
    @Test fun cupertinoTupleReportsUnsupportedAdornment() = unsupported(true, "tuple")
    @Test fun materialOneOfObjectReportsUnsupportedAdornment() = unsupported(false, "oneOfObject")
    @Test fun cupertinoOneOfObjectReportsUnsupportedAdornment() = unsupported(true, "oneOfObject")
    @Test fun materialAnyOfObjectReportsUnsupportedAdornment() = unsupported(false, "anyOfObject")
    @Test fun cupertinoAnyOfObjectReportsUnsupportedAdornment() = unsupported(true, "anyOfObject")
    @Test fun materialAllOfObjectReportsUnsupportedAdornment() = unsupported(false, "allOfObject")
    @Test fun cupertinoAllOfObjectReportsUnsupportedAdornment() = unsupported(true, "allOfObject")
    @Test fun materialFileScalarTextFallbackSupportsAdornment() = fileScalarFallback(false)
    @Test fun cupertinoFileScalarTextFallbackSupportsAdornment() = fileScalarFallback(true)
    @Test fun materialMixedScalarSupportsAdornment() = mixedScalarSupported(false)
    @Test fun cupertinoMixedScalarSupportsAdornment() = mixedScalarSupported(true)
    @Test fun materialTextDoesNotReportUnsupportedAdornment() = supported(false)
    @Test fun cupertinoTextDoesNotReportUnsupportedAdornment() = supported(true)
    @Test fun materialEmptyAdornmentsDoNotReport() = empty(false)
    @Test fun cupertinoEmptyAdornmentsDoNotReport() = empty(true)
    @Test fun materialMalformedAdornmentReports() = malformed(false)
    @Test fun cupertinoMalformedAdornmentReports() = malformed(true)

    private fun unsupported(cupertino: Boolean, variant: String, locale: String = "en") = runComposeUiTest {
        val property = when (variant) {
            "boolean" -> """{"type":"boolean"}"""
            "slider" -> """{"type":"integer","minimum":0,"maximum":10,"default":5}"""
            "radio" -> """{"type":"string","enum":["A","B"]}"""
            "oneOfRadio" -> """{"oneOf":[{"const":"A"},{"const":"B"}]}"""
            "chips" -> """{"type":"array","items":{"type":"string"}}"""
            "multi-select" -> """{"type":"array","uniqueItems":true,
                "items":{"type":"string","enum":["A","B"]}}"""
            "null" -> """{"type":"null"}"""
            "fileArray" -> """{"type":"array","items":{"type":"string","format":"binary"}}"""
            "filePicker" -> """{"type":"string","format":"binary"}"""
            "object" -> """{"type":"object","properties":{"child":{"type":"string"}}}"""
            "array" -> """{"type":"array","items":{"type":"object","properties":{"child":{"type":"string"}}}}"""
            "enumArray" -> """{"type":"array","uniqueItems":true,
                "items":{"type":"string","enum":["A","B"]}}"""
            "tuple" -> """{"type":"array","prefixItems":[{"type":"string"},{"type":"integer"}]}"""
            "oneOfObject" -> """{"oneOf":[{"type":"object","properties":{"a":{"type":"string"}}},
                {"type":"object","properties":{"b":{"type":"string"}}}]}"""
            "anyOfObject" -> """{"anyOf":[{"type":"object","properties":{"a":{"type":"string"}}},
                {"type":"object","properties":{"b":{"type":"string"}}}]}"""
            "allOfObject" -> """{"allOf":[{"type":"object","properties":{"a":{"type":"string"}}},
                {"type":"object","properties":{"b":{"type":"string"}}}]}"""
            else -> """{"type":"string","format":"password","minLength":2,"maxLength":4}"""
        }
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":$property}
        }""").jsonObject)
        val option = when (variant) {
            "slider" -> mapOf("slider" to JsonPrimitive(true))
            "radio", "oneOfRadio" -> mapOf("format" to JsonPrimitive("radio"))
            "otp" -> mapOf("variant" to JsonPrimitive("otp"))
            "chips", "multi-select" -> mapOf("variant" to JsonPrimitive(variant))
            else -> emptyMap()
        }
        val control = ControlElement("#/properties/value", options = option + mapOf(
            "startAdornment" to JsonPrimitive("prefix"),
            "endAdornment" to JsonPrimitive("suffix"),
        ))
        val diagnostics = mutableListOf<UiDiagnostic>()
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = control,
                renderers = if (variant == "null" || variant == "fileArray" || variant == "filePicker") {
                    if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers
                } else if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)) +
                    if (variant == "filePicker") mapOf(ExtendedFilePickerConfigKey to
                        ExtendedFilePicker { _, _, _ -> }) else emptyMap(),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        waitForIdle()
        val reported = diagnostics.filter { it.code == "adornment.unsupported" }
        assertEquals(1, reported.size)
        assertEquals(if (locale == "bg") "Украси на полета не се поддържат за избрания редактор."
            else "Input adornments are unsupported for the selected editor.",
            reported.single().message)
        assertEquals(when (variant) {
            "oneOfRadio" -> "radio"
            "fileArray", "filePicker" -> "file"
            "enumArray" -> "enum-array"
            "oneOfObject" -> "oneOf"
            "anyOfObject" -> "anyOf"
            "allOfObject" -> "allOf"
            else -> variant
        },
            reported.single().details["variant"])
        assertEquals(listOf("startAdornment", "endAdornment"), reported.single().details["edges"])
        if (variant == "filePicker") onNodeWithText("Choose file").assertExists()
        onNodeWithText("prefix").assertDoesNotExist()
        onNodeWithText("suffix").assertDoesNotExist()
        assertEquals(0, writes)
    }

    private fun fileScalarFallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"binary"}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"stored.bin"}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = { writes++ })
        }
        waitForIdle()
        onNodeWithText("prefix").assertExists()
        assertEquals(0, diagnostics.count { it.code == "adornment.unsupported" })
        assertEquals(0, writes)
    }

    private fun mixedScalarSupported(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":["string","integer"]}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"Ada"}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = { writes++ })
        }
        waitForIdle()
        onNodeWithText("prefix").assertExists()
        assertEquals(0, diagnostics.count { it.code == "adornment.unsupported" })
        assertEquals(0, writes)
    }

    private fun supported(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"Ada"}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive("prefix"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
        }
        waitForIdle()
        onNodeWithText("prefix").assertExists()
        assertEquals(0, diagnostics.count { it.code == "adornment.unsupported" })
    }

    private fun empty(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"boolean"}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":true}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive(""), "endAdornment" to JsonArray(emptyList()))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
        }
        waitForIdle()
        assertEquals(0, diagnostics.count { it.code == "adornment.unsupported" })
    }

    private fun malformed(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"boolean"}}
        }""").jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":true}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to JsonPrimitive(42))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
        }
        waitForIdle()
        val reported = diagnostics.filter { it.code == "adornment.unsupported" }
        assertEquals(1, reported.size)
        assertEquals(listOf("startAdornment"), reported.single().details["edges"])
    }
}
