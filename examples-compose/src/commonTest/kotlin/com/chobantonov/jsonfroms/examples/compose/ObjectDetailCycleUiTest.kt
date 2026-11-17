package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ObjectDetailCycleUiTest {
    @Test fun materialFallsBackFromSelfMatchingRegistry() = scenario(false)
    @Test fun cupertinoFallsBackFromSelfMatchingRegistry() = scenario(true)
    @Test fun materialKeepsRegisteredControlWithInlineDetail() = inline(false)
    @Test fun cupertinoKeepsRegisteredControlWithInlineDetail() = inline(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"title":"Person","type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        var unrelated by mutableStateOf(false)
        var changes = 0
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { candidate, _, _ -> if (candidate?.title == "Person") 10 else -1 },
            uischema = ControlElement("#"),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""),
                schema = schema, uischema = ControlElement("#/properties/person"),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it },
                    "unrelated" to unrelated),
                onChange = { changes++ },
            )
        }
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        waitForIdle()
        assertEquals(listOf("uischema.registryCycle"), diagnostics.map { it.code })
        assertEquals("person", diagnostics.single().details["path"])
        runOnIdle { unrelated = true }
        waitForIdle()
        assertEquals(1, diagnostics.size)
        assertEquals(0, changes)
    }

    private fun inline(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"person":{"title":"Person","type":"object","properties":{"name":{"type":"string"}}}}}"""
        ).jsonObject)
        val diagnostics = mutableListOf<UiDiagnostic>()
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { candidate, _, _ -> if (candidate?.title == "Person") 10 else -1 },
            uischema = ControlElement("#", options = mapOf("detail" to Json.parseToJsonElement(
                """{"type":"Label","text":"Authored detail"}"""))),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"person":{"name":"Ada"}}"""),
                schema = schema, uischema = ControlElement("#/properties/person"),
                uischemas = registry,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
            )
        }
        onNodeWithText("Authored detail").assertExists()
        waitForIdle()
        assertTrue(diagnostics.isEmpty())
    }
}
