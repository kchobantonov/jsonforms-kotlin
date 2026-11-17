package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class LayoutDiagnosticsTest {
    @Test fun reportsOnlyUnsupportedAlignmentAxes() {
        val options = mapOf(
            "align" to JsonPrimitive("center"),
            "justify" to JsonPrimitive("space-evenly"),
        )
        assertEquals(emptyList(), collectLayoutDiagnostics(options, emptyMap(), emptyList(), horizontal = true))
        assertEquals(emptyList(),
            collectLayoutDiagnostics(options, emptyMap(), emptyList(), horizontal = false).map { it.code })
        assertEquals(emptyList(), collectLayoutDiagnostics(
            mapOf("align" to JsonPrimitive("stretch")), emptyMap(), emptyList(), horizontal = true,
        ).map { it.code })
        assertEquals(emptyList(), collectLayoutDiagnostics(
            mapOf("align" to JsonPrimitive("stretch")), emptyMap(), emptyList(), horizontal = false,
        ).map { it.code })
        assertEquals(listOf("layout.stretchFallback"), collectLayoutDiagnostics(
            mapOf("align" to JsonPrimitive("stretch")), emptyMap(), listOf(VerticalLayout()), horizontal = true,
        ).map { it.code })
    }

    @Test fun reportsExcludedLegacyChildColumnsWithoutChangingPortableSpan() {
        val children = listOf(
            ControlElement(scope = "#", options = mapOf("columns" to JsonPrimitive(4))),
            ControlElement(scope = "#", options = mapOf(
                "columns" to JsonPrimitive(8),
                "layout" to buildJsonObject { put("span", 2) },
            )),
        )
        val diagnostics = collectLayoutDiagnostics(
            options = mapOf("gridColumns" to JsonPrimitive(12)),
            config = emptyMap(),
            elements = children,
            horizontal = true,
        )
        assertEquals(listOf("layout.excludedOption", "layout.excludedOption"), diagnostics.map { it.code })
        assertEquals(listOf(0, 1), diagnostics.map { it.details["childIndex"] })
        assertTrue(diagnostics.all { it.details["key"] == "columns" })
        assertEquals(LayoutWidthMode.Span(2), resolveLayoutItemSizing(children[1], null).mode)
    }

    @Test fun reportsInvalidOptionsAndUnsupportedUnits() {
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("weight", 0)
            put("span", 1.5)
            put("width", "40%")
            put("minWidth", -1)
        }))
        val diagnostics = collectLayoutDiagnostics(
            options = mapOf("gap" to JsonPrimitive(-4), "gridColumns" to JsonPrimitive(2.5)),
            config = emptyMap(),
            elements = listOf(child),
            horizontal = true,
        )
        assertEquals(5, diagnostics.count { it.code == "layout.invalidOption" })
        assertEquals(1, diagnostics.count { it.code == "layout.unsupportedOption" })
        assertTrue(diagnostics.all { it.severity == UiDiagnosticSeverity.Warning })
        assertTrue(diagnostics.any { it.details["childIndex"] == 0 })
    }

    @Test fun reportsClampedSpanAndConflictingModes() {
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("width", 120)
            put("span", 5)
            put("weight", 2)
        }))
        val codes = collectLayoutDiagnostics(
            options = mapOf("gridColumns" to JsonPrimitive(4)),
            config = emptyMap(),
            elements = listOf(child),
            horizontal = true,
        ).map { it.code }
        assertEquals(listOf("layout.spanClamped", "layout.multipleSizingModes"), codes)
    }

    @Test fun reportsInvalidSpacerSizeWithChildIndex() {
        val spacer = SpacerElement(size = JsonPrimitive(-1))
        val diagnostics = collectLayoutDiagnostics(emptyMap(), emptyMap(), listOf(spacer), horizontal = true)
        assertEquals(listOf("layout.invalidOption"), diagnostics.map { it.code })
        assertEquals(0, diagnostics.single().details["childIndex"])
        assertEquals(32f, resolveSpacerSize(spacer))
    }

    @Test fun reportsUnsupportedVerticalAndContainerHints() {
        val child = ControlElement(scope = "#", options = mapOf("layout" to buildJsonObject {
            put("span", 2)
            put("weight", 1)
        }))
        val codes = collectLayoutDiagnostics(
            options = mapOf(
                "wrap" to JsonPrimitive(true),
                "align" to JsonPrimitive("center"),
                "resizable" to JsonPrimitive(true),
                "gridColumns" to JsonPrimitive(4),
            ),
            config = emptyMap(),
            elements = listOf(child),
            horizontal = false,
        ).map { it.code }
        assertTrue("layout.spanUnsupported" in codes)
        assertEquals(4, codes.count { it == "layout.unsupportedOption" || it == "layout.spanUnsupported" })
    }
}
