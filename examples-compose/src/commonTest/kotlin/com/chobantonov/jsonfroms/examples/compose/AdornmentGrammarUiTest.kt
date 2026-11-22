package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AdornmentGrammarUiTest {
    @Test fun materialRendersOnlyTypedAdornmentContent() = grammar(false)
    @Test fun cupertinoRendersOnlyTypedAdornmentContent() = grammar(true)
    @Test fun materialReportsMalformedAdornmentInBulgarian() = grammar(false, "bg")
    @Test fun cupertinoReportsMalformedAdornmentInBulgarian() = grammar(true, "bg")

    private fun grammar(cupertino: Boolean, locale: String = "en") = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"original"}"""))
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "startAdornment" to Json.parseToJsonElement("""[
                      {"kind":"text","text":42},
                      {"kind":"icon","icon":"help","label":"quoted flag","decorative":"true"},
                      {"kind":"text","text":"typed start"}]
                    """),
                    "endAdornment" to Json.parseToJsonElement("""{
                      "kind":"icon","icon":"help","label":"typed end","decorative":false
                    }"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
                onChange = { changes++; data = it.data })
        }
        waitForIdle()
        val invalid = diagnostics.filter { it.code == "adornment.invalid" }
        assertEquals(2, invalid.size)
        assertEquals(listOf(0, 1), invalid.map { it.details["index"] })
        assertEquals(listOf("startAdornment", "startAdornment"),
            invalid.map { it.details["edge"] })
        assertEquals(if (locale == "bg") "Невалидна украса на поле; проверете посочения край и индекс."
            else "Invalid input adornment; check the authored edge and index.",
            invalid.first().message)
        onNodeWithText("typed start", useUnmergedTree = true).assertExists()
        onNodeWithText("typed end", useUnmergedTree = true).assertExists()
        onNodeWithText("42", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("quoted flag", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("changed")
        assertEquals(Json.parseToJsonElement("""{"value":"changed"}"""), data)
        assertEquals(1, changes)
        assertEquals(2, diagnostics.count { it.code == "adornment.invalid" })
    }
}
