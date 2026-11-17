package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ControlScopeUiTest {
    @Test fun materialRejectsUnresolvedScope() = rejectsUnresolvedScope(false)

    @Test fun cupertinoRejectsUnresolvedScope() = rejectsUnresolvedScope(true)

    @Test fun materialKeepsEscapedPropertyScope() = keepsEscapedPropertyScope(false)

    @Test fun cupertinoKeepsEscapedPropertyScope() = keepsEscapedPropertyScope(true)

    private fun keepsEscapedPropertyScope(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        var changes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"a/b":{"type":"string","title":"Slash key"}}}"""
        ).jsonObject)
        val ui = (Json.parseToJsonElement(
            """{"type":"Control","scope":"#/properties/a~1b"}"""
        ) as JsonObject).toUiSchemaElement()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a/b":"value"}"""),
                schema = schema,
                uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
                onChange = { changes++ },
            )
        }
        waitForIdle()
        onNodeWithText("No applicable scope found.").assertDoesNotExist()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(emptyList(), received)
        assertEquals(0, changes)
    }

    private fun rejectsUnresolvedScope(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        var changes = 0
        var locale by mutableStateOf("en")
        val unresolvedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        val resolvedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"},"missing":{"type":"string"}}}"""
        ).jsonObject)
        var schema by mutableStateOf(unresolvedSchema)
        val ui = (Json.parseToJsonElement(
            """{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/name"},{"type":"Control","scope":"#/properties/missing"}]}"""
        ) as JsonObject).toUiSchemaElement()
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema,
                uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { changes++ },
            )
        }
        waitForIdle()
        onNodeWithText("No applicable scope found.").assertExists()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(listOf("control.scopeUnresolved"), received.map { it.code })
        assertEquals("#/properties/missing", received.single().details["scope"])
        assertEquals(0, changes)

        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onNodeWithText("Няма приложимо поле за този път в схемата.").assertExists()
        assertEquals(0, changes)
        runOnIdle { locale = "en" }
        waitForIdle()
        onNodeWithText("No applicable scope found.").assertExists()

        runOnIdle { schema = resolvedSchema }
        waitForIdle()
        onNodeWithText("No applicable scope found.").assertDoesNotExist()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)

        runOnIdle { schema = unresolvedSchema }
        waitForIdle()
        onNodeWithText("No applicable scope found.").assertExists()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }
}
