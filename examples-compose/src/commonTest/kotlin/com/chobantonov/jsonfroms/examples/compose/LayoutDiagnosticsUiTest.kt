package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.cupertino.renderers.layouts.CupertinoHorizontalLayout
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.layouts.MaterialHorizontalLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutDiagnosticsUiTest {
    @Test fun materialReportsOptionsOnEmptyRootLayout() = emptyRootLayoutOptions(false)

    @Test fun cupertinoReportsOptionsOnEmptyRootLayout() = emptyRootLayoutOptions(true)

    private fun emptyRootLayoutOptions(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = HorizontalLayout(options = mapOf("gridColumns" to JsonPrimitive(0))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
            )
        }
        waitForIdle()
        assertEquals(listOf("layout.invalidOption"), received.map { it.code })
        assertEquals("gridColumns", received.single().details["key"])
    }

    @Test fun materialReportsEmptyNestedLayoutOptions() = emptyNestedLayoutOptions(false)

    @Test fun cupertinoReportsEmptyNestedLayoutOptions() = emptyNestedLayoutOptions(true)

    private fun emptyNestedLayoutOptions(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(elements = listOf(
            VerticalLayout(options = mapOf("gridColumns" to JsonPrimitive(4))),
            ControlElement(scope = "#/properties/name"),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
            )
        }
        waitForIdle()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(listOf("layout.unsupportedOption"), received.map { it.code })
        assertEquals("gridColumns", received.single().details["key"])
        assertEquals(listOf(0), received.single().details["structuralPath"])
    }

    @Test fun excludedColumnsDoNotSelectALegacyLayoutRenderer() {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"object"}""").jsonObject)
        val layout = HorizontalLayout(elements = listOf(ControlElement(
            scope = "#",
            options = mapOf("columns" to JsonPrimitive(4)),
        )))
        val context = TesterContext(rootSchema = schema)
        val material = materialRenderers.maxBy { it.tester(layout, schema, context) }
        val cupertino = cupertinoRenderers.maxBy { it.tester(layout, schema, context) }
        assertSame(MaterialHorizontalLayout, material.renderer)
        assertSame(CupertinoHorizontalLayout, cupertino.renderer)
    }

    @Test fun materialReportsAuthorLayoutDiagnostics() = reports(false)

    @Test fun cupertinoReportsAuthorLayoutDiagnostics() = reports(true)

    @Test fun materialReportsUnsupportedVerticalSpan() = verticalSpan(false)

    @Test fun cupertinoReportsUnsupportedVerticalSpan() = verticalSpan(true)

    @Test fun materialReportsExcludedLegacyColumns() = legacyColumns(false)

    @Test fun cupertinoReportsExcludedLegacyColumns() = legacyColumns(true)

    @Test fun materialReportsCollapsedGroupHintsOnce() = collapsedGroup(false)

    @Test fun cupertinoReportsCollapsedGroupHintsOnce() = collapsedGroup(true)

    private fun collapsedGroup(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        val layout = GroupElement(
            label = "Section",
            options = mapOf("collapsible" to JsonPrimitive(true), "collapsed" to JsonPrimitive(true)),
            elements = listOf(ControlElement(
                scope = "#/properties/name",
                options = mapOf(
                    "columns" to JsonPrimitive(4),
                    "layout" to buildJsonObject { put("span", 2) },
                ),
            )),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
            )
        }
        waitForIdle()
        assertEquals(listOf("layout.excludedOption", "layout.spanUnsupported"), received.map { it.code })
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithText("Section").performClick()
        waitForIdle()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(2, received.size)
    }

    private fun legacyColumns(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema,
                uischema = HorizontalLayout(elements = listOf(ControlElement(
                    scope = "#/properties/name",
                    options = mapOf("columns" to JsonPrimitive(4)),
                ))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { received += it }),
            )
        }
        waitForIdle()
        assertEquals(listOf("layout.excludedOption"), received.map { it.code })
        assertEquals("columns", received.single().details["key"])
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun verticalSpan(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter { received += it }
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        val layout = VerticalLayout(
            options = mapOf("gridColumns" to JsonPrimitive(4)),
            elements = listOf(ControlElement(
                scope = "#/properties/name",
                options = mapOf("layout" to buildJsonObject { put("span", 2) }),
            )),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to reporter),
            )
        }
        waitForIdle()
        assertEquals(
            listOf("layout.unsupportedOption", "layout.spanUnsupported"),
            received.map { it.code },
        )
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun reports(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter { received += it }
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        val layout = HorizontalLayout(
            options = mapOf("gridColumns" to JsonPrimitive(4), "wrap" to JsonPrimitive(true)),
            elements = listOf(ControlElement(
                scope = "#/properties/name",
                options = mapOf("layout" to buildJsonObject {
                    put("span", 5)
                    put("weight", 2)
                }),
            )),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to reporter),
            )
        }
        waitForIdle()
        assertEquals(
            listOf("layout.spanClamped", "layout.multipleSizingModes"),
            received.map { it.code },
        )
        assertEquals(0, received[0].details["childIndex"])
        assertTrue(received.all { it.details.containsKey("path") })
        onNodeWithText("options.layout.span 5 exceeds gridColumns 4; clamped.").assertDoesNotExist()
    }
}
