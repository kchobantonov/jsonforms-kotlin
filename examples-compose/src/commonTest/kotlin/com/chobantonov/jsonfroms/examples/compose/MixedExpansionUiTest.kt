package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class MixedExpansionUiTest {
    @Test fun materialMixedDefaultAndLocalCollapse() = scenario(false)
    @Test fun cupertinoMixedDefaultAndLocalCollapse() = scenario(true)
    @Test fun materialNoncollapsibleStaysOpen() = noncollapsible(false)
    @Test fun cupertinoNoncollapsibleStaysOpen() = noncollapsible(true)
    @Test fun materialInvalidCollapseReportsDiagnostic() = invalid(false)
    @Test fun cupertinoInvalidCollapseReportsDiagnostic() = invalid(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":["object","array"]}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"value":{"child":1}}""")
    private fun control(
        collapsed: JsonElement? = null,
        collapsible: Boolean? = null,
    ): ControlElement {
        val options = buildMap {
            if (collapsed != null) put("collapsed", collapsed)
            if (collapsible != null) put("collapsible", JsonPrimitive(collapsible))
        }
        return ControlElement(scope = "#/properties/value", label = StringControlLabel("Value"), options = options)
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var ui by mutableStateOf(control())
        var globalCollapsed by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("collapsed" to globalCollapsed)),
                onChange = { changes++ })
        }
        onNodeWithText("Search tree…").assertExists()
        onNodeWithContentDescription("Collapse").performClick()
        onNodeWithText("Search tree…").assertDoesNotExist()
        runOnIdle { ui = control() }
        onNodeWithText("Search tree…").assertDoesNotExist()
        onNodeWithContentDescription("Expand").performClick()
        onNodeWithText("Search tree…").assertExists()
        runOnIdle { globalCollapsed = true }
        onNodeWithText("Search tree…").assertDoesNotExist()
        runOnIdle { globalCollapsed = false }
        onNodeWithText("Search tree…").assertExists()
        assertEquals(0, changes)
    }

    private fun noncollapsible(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(JsonPrimitive(true), collapsible = false),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Search tree…").assertExists()
        onNodeWithContentDescription("Collapse").assertDoesNotExist()
        onNodeWithContentDescription("Expand").assertDoesNotExist()
    }

    private fun invalid(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter { diagnostics += it }
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(JsonNull),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to reporter))
        }
        onNodeWithText("Search tree…").assertExists()
        waitForIdle()
        assertEquals(listOf("section.invalidCollapsed"), diagnostics.map { it.code })
        assertEquals("Mixed", diagnostics.single().details["elementType"])
    }
}
