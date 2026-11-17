package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class UnknownRendererLocaleUiTest {
    @Test fun materialExplainsUnsupportedElementInEnglishAndBulgarian() = unsupportedElement(false)

    @Test fun cupertinoExplainsUnsupportedElementInEnglishAndBulgarian() = unsupportedElement(true)

    private fun unsupportedElement(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        val authored = Json.parseToJsonElement(
            """{"type":"VendorWidget","name":"stable","options":{"vendor":{"mode":"wide"}},"payload":{"enabled":true},"elements":[]}"""
        ).jsonObject
        val unknown = authored.toUiSchemaElement()
        assertEquals(authored, unknown.toJsonObject())
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject),
                uischema = unknown,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        onNodeWithText("No applicable renderer found.").assertExists()
        waitForIdle()
        assertEquals(listOf("renderer.unavailable"), diagnostics.map { it.code })
        assertEquals("VendorWidget", diagnostics.single().details["elementType"])
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Няма приложим визуализатор.").assertExists()
        assertEquals(1, diagnostics.size)
        assertEquals(0, changes)
    }
}
