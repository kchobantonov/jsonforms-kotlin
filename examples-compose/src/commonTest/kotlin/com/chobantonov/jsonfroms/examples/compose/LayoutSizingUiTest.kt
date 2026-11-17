package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.mappers.gridSpanWidth
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutSizingUiTest {
    @Test fun materialUnboundedRowUsesViewportSpanWidth() = unboundedRowUsesViewportSpanWidth(false)

    @Test fun cupertinoUnboundedRowUsesViewportSpanWidth() = unboundedRowUsesViewportSpanWidth(true)

    private fun unboundedRowUsesViewportSpanWidth(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf(
            "gap" to JsonPrimitive(10), "gridColumns" to JsonPrimitive(4),
        ), elements = listOf("first", "second").map { name ->
            ControlElement("#/properties/$name", options = mapOf(
                "layout" to buildJsonObject { put("span", 2) },
            ))
        })
        var viewportPx = 0
        var densityScale = 1f
        var writes = 0
        setContent {
            viewportPx = LocalWindowInfo.current.containerSize.width
            densityScale = LocalDensity.current.density
            Box(Modifier.width(320.dp).horizontalScroll(rememberScrollState())) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { writes++ })
            }
        }
        waitForIdle()
        assertTrue(viewportPx > 0)
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        val children = (0..1).map { index ->
            onNodeWithTag("layout-child--$index").fetchSemanticsNode()
        }
        val expectedPx = gridSpanWidth(viewportPx / densityScale, 10f, 4, 2) * densityScale
        children.forEach { child ->
            assertTrue(abs(child.size.width - expectedPx) < 6f,
                "Expected viewport span $expectedPx px, got ${child.size.width} px")
        }
        assertTrue(abs(children[0].boundsInRoot.top - children[1].boundsInRoot.top) < 3f)
        assertEquals(0, writes)
    }

    @Test fun materialReservedLayoutHintsKeepGeometry() = reservedLayoutHints(false)

    @Test fun cupertinoReservedLayoutHintsKeepGeometry() = reservedLayoutHints(true)

    private fun reservedLayoutHints(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        var reserved by mutableStateOf(false)
        var changes = 0
        val diagnostics = mutableListOf<UiDiagnostic>()
        setContent {
            val layout = buildJsonObject {
                put("width", 100)
                if (reserved) {
                    put("start", 3)
                    put("responsive", buildJsonObject { put("small", 1) })
                }
            }
            val ui = HorizontalLayout(
                options = mapOf("gap" to JsonPrimitive(12)),
                elements = listOf(
                    ControlElement("#/properties/first", options = mapOf("layout" to layout)),
                    ControlElement("#/properties/second"),
                ),
            )
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { changes++ })
            }
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val baseline = fields()
        assertEquals(2, baseline.size)
        runOnIdle { reserved = true }
        waitForIdle()
        val after = fields()
        assertEquals(2, after.size)
        baseline.zip(after).forEach { (before, current) ->
            assertTrue(abs(before.left - current.left) < 1f)
            assertTrue(abs(before.width - current.width) < 1f)
        }
        assertEquals(emptyList(), diagnostics.map { it.code })
        assertEquals(0, changes)
    }

    @Test fun materialParentMinimumWrapsUntilChildOverridesIt() = parentMinimumOverride(false)

    @Test fun cupertinoParentMinimumWrapsUntilChildOverridesIt() = parentMinimumOverride(true)

    private fun parentMinimumOverride(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        var childOverride by mutableStateOf(false)
        var changes = 0
        setContent {
            val second = ControlElement(
                scope = "#/properties/second",
                options = if (childOverride) mapOf("layout" to buildJsonObject { put("minWidth", 60) })
                    else emptyMap(),
            )
            val ui = HorizontalLayout(
                options = mapOf(
                    "wrap" to JsonPrimitive(true),
                    "gap" to JsonPrimitive(10),
                    "minItemWidth" to JsonPrimitive(140),
                ),
                elements = listOf(ControlElement("#/properties/first"), second),
            )
            Box(Modifier.width(260.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { changes++ })
            }
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val wrapped = fields()
        assertEquals(2, wrapped.size)
        assertTrue(wrapped[1].top > wrapped[0].bottom, "Parent minimum should move the second field")
        runOnIdle { childOverride = true }
        waitForIdle()
        val singleRow = fields()
        assertTrue(abs(singleRow[1].top - singleRow[0].top) < 3f,
            "Child minimum override should permit both fields on one row")
        assertEquals(0, changes)
    }

    @Test fun materialNarrowRowShrinksThenScrolls() = narrowRow(false, false)

    @Test fun cupertinoNarrowRowShrinksThenScrolls() = narrowRow(true, false)

    @Test fun materialNarrowWrappedRowMovesFlexibleChild() = narrowRow(false, true)

    @Test fun cupertinoNarrowWrappedRowMovesFlexibleChild() = narrowRow(true, true)

    @Test fun materialNarrowStretchRowScrolls() = narrowRow(false, false, stretch = true)

    @Test fun cupertinoNarrowStretchRowScrolls() = narrowRow(true, false, stretch = true)

    @Test fun materialOversizedWrappedChildScrolls() = oversizedWrappedChild(false)

    @Test fun cupertinoOversizedWrappedChildScrolls() = oversizedWrappedChild(true)

    private fun oversizedWrappedChild(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("wrap" to JsonPrimitive(true)), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 300); put("minWidth", 260) },
            )),
        ))
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val scroll = onNodeWithTag("horizontal-layout-scroll")
        val range = scroll.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue(range.maxValue() > 30f)
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(50f, 0f) }
        assertTrue(scroll.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value() > 20f)
    }

    private fun narrowRow(cupertino: Boolean, wrap: Boolean, stretch: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf(
            "gap" to JsonPrimitive(10), "wrap" to JsonPrimitive(wrap),
        ) + (if (stretch) mapOf("align" to JsonPrimitive("stretch")) else emptyMap()), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 200); put("minWidth", 150) },
            )),
            ControlElement(scope = "#/properties/second", options = mapOf(
                "layout" to buildJsonObject { put("minWidth", 80) },
            )),
        ))
        var hostWidth by mutableStateOf(260.dp)
        setContent {
            Box(Modifier.width(hostWidth)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val wide = fields()
        assertEquals(2, wide.size)
        if (wrap) {
            assertTrue(wide[1].top > wide[0].bottom)
        } else {
            onNodeWithTag("horizontal-layout-scroll").assertDoesNotExist()
        }
        runOnIdle { hostWidth = 220.dp }
        val narrow = fields()
        if (wrap) {
            assertTrue(narrow[1].top > narrow[0].bottom)
            onNodeWithTag("horizontal-layout-scroll").assertDoesNotExist()
        } else {
            assertTrue(abs((wide[0].width - narrow[0].width) - 20f) < 3f,
                "Fixed child should shrink 20dp: ${wide[0].width} to ${narrow[0].width}")
            assertTrue(abs((wide[1].left - narrow[1].left) - 20f) < 3f)
            val scroll = onNodeWithTag("horizontal-layout-scroll")
            val before = scroll.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
            scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(50f, 0f) }
            val after = scroll.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].value()
            assertTrue(after > before + 5f, "Expected overflowing row to scroll from $before to $after")
        }
    }

    @Test fun materialAppliesHorizontalCrossHeightBounds() = horizontalCrossHeight(false, false)

    @Test fun cupertinoAppliesHorizontalCrossHeightBounds() = horizontalCrossHeight(true, false)

    @Test fun materialReflowsWrappedCrossHeight() = horizontalCrossHeight(false, true)

    @Test fun cupertinoReflowsWrappedCrossHeight() = horizontalCrossHeight(true, true)

    private fun horizontalCrossHeight(cupertino: Boolean, wrap: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(maxHeight: Int) = HorizontalLayout(options = mapOf(
            "gap" to JsonPrimitive(10), "align" to JsonPrimitive("end"),
            "wrap" to JsonPrimitive(wrap),
        ), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 180); put("height", 120); put("maxHeight", maxHeight) },
            )),
            ControlElement(scope = "#/properties/second", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            )),
        ) + if (wrap) listOf(ControlElement(scope = "#/properties/third", options = mapOf(
            "layout" to buildJsonObject { put("width", 180) },
        ))) else emptyList())
        var ui by mutableStateOf(layout(90))
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val bounded = fields()
        assertEquals(if (wrap) 3 else 2, bounded.size)
        runOnIdle { ui = layout(120) }
        val full = fields()
        val shifted = if (wrap) full[2].top - bounded[2].top else full[1].top - bounded[1].top
        assertTrue(abs(shifted - 30f) < 3f, "Cross-height shift=$shifted")
    }

    @Test fun materialAlignsFixedWidthColumnChildren() = verticalAlign(false)

    @Test fun cupertinoAlignsFixedWidthColumnChildren() = verticalAlign(true)

    @Test fun materialJustifiesBoundedColumn() = verticalJustify(false)

    @Test fun cupertinoJustifiesBoundedColumn() = verticalJustify(true)

    @Test fun materialClampsCenteredVerticalChildWidth() = centeredWidthBounds(false)

    @Test fun cupertinoClampsCenteredVerticalChildWidth() = centeredWidthBounds(true)

    @Test fun materialPlacesCappedWeightedColumnAtEnd() = cappedWeightedPlacement(false)

    @Test fun cupertinoPlacesCappedWeightedColumnAtEnd() = cappedWeightedPlacement(true)

    private fun cappedWeightedPlacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(align: String, justify: String) = VerticalLayout(options = mapOf(
            "align" to JsonPrimitive(align), "justify" to JsonPrimitive(justify), "gap" to JsonPrimitive(20),
        ), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            )),
            SpacerElement(size = JsonPrimitive(0), options = mapOf(
                "layout" to buildJsonObject { put("weight", 1); put("maxHeight", 80) },
            )),
            ControlElement(scope = "#/properties/second", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            )),
        ))
        var ui by mutableStateOf(layout("start", "start"))
        setContent {
            Box(Modifier.width(300.dp).height(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun fields() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val start = fields()
        assertEquals(2, start.size)
        runOnIdle { ui = layout("end", "end") }
        val end = fields()
        assertEquals(2, end.size)
        assertTrue(end[0].top - start[0].top > 100f)
        assertTrue(abs((end[0].left - start[0].left) - 200f) < 3f)
    }

    private fun centeredWidthBounds(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(maxWidth: Int) = VerticalLayout(options = mapOf("align" to JsonPrimitive("center")), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 250); put("minWidth", 100); put("maxWidth", maxWidth) },
            )),
        ))
        var ui by mutableStateOf(layout(180))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun field() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().single().boundsInRoot
        val bounded = field()
        runOnIdle { ui = layout(250) }
        val full = field()
        assertTrue(abs((full.width - bounded.width) - 70f) < 3f,
            "Width change=${full.width - bounded.width}")
        assertTrue(abs((bounded.left - full.left) - 35f) < 3f,
            "Center offset=${bounded.left - full.left}")
    }

    private fun verticalAlign(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(align: String) = VerticalLayout(options = mapOf("align" to JsonPrimitive(align)),
            elements = listOf(
                ControlElement(scope = "#/properties/first", options = mapOf(
                    "layout" to buildJsonObject { put("width", 100) },
                )),
                ControlElement(scope = "#/properties/second", options = mapOf(
                    "layout" to buildJsonObject { put("width", 200) },
                )),
            ))
        var ui by mutableStateOf(layout("start"))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun lefts(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.left }
        val origin = lefts().first()
        val expected = mapOf(
            "start" to listOf(0f, 0f),
            "center" to listOf(150f, 100f),
            "end" to listOf(300f, 200f),
        )
        expected.forEach { (mode, positions) ->
            runOnIdle { ui = layout(mode) }
            val actual = lefts().map { it - origin }
            assertEquals(2, actual.size)
            actual.zip(positions).forEach { (x, target) ->
                assertTrue(abs(x - target) < 3f, "$mode expected $target, got $x")
            }
        }
    }

    private fun verticalJustify(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(justify: String) = VerticalLayout(options = mapOf(
            "gap" to JsonPrimitive(20), "justify" to JsonPrimitive(justify),
        ), elements = listOf("first", "second").map { name ->
            ControlElement(scope = "#/properties/$name", options = mapOf(
                "layout" to buildJsonObject { put("height", 80) },
            ))
        })
        var ui by mutableStateOf(layout("start"))
        setContent {
            Box(Modifier.width(300.dp).height(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun tops(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.top }
        val origin = tops().first()
        val expected = mapOf(
            "start" to listOf(0f, 100f),
            "center" to listOf(110f, 210f),
            "end" to listOf(220f, 320f),
            "space-between" to listOf(0f, 320f),
            "space-around" to listOf(55f, 265f),
            "space-evenly" to listOf(73f, 247f),
        )
        expected.forEach { (mode, positions) ->
            runOnIdle { ui = layout(mode) }
            val actual = tops().map { it - origin }
            assertEquals(2, actual.size)
            actual.zip(positions).forEach { (y, target) ->
                assertTrue(abs(y - target) < 3f, "$mode expected $target, got $y")
            }
        }
    }

    @Test fun materialDistributesSpaceAroundFromRtlStart() = rtlSpaceAround(false)

    @Test fun cupertinoDistributesSpaceAroundFromRtlStart() = rtlSpaceAround(true)

    @Test fun materialAlignsVerticalCrossAxisFromRtlStart() = rtlVerticalAlign(false)

    @Test fun cupertinoAlignsVerticalCrossAxisFromRtlStart() = rtlVerticalAlign(true)

    private fun rtlVerticalAlign(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"name":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(align: String) = VerticalLayout(options = mapOf("align" to JsonPrimitive(align)),
            elements = listOf(ControlElement(scope = "#/properties/name", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            ))))
        var ui by mutableStateOf(layout("start"))
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(400.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"name":"Ada"}"""),
                        schema = schema, uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers)
                }
            }
        }
        fun left() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().single().boundsInRoot.left
        val start = left()
        runOnIdle { ui = layout("center") }
        val center = left()
        runOnIdle { ui = layout("end") }
        val end = left()
        assertTrue(start > center + 100f, "RTL start should be right of center: $start, $center")
        assertTrue(center > end + 100f, "RTL end should be left of center: $center, $end")
    }

    private fun rtlSpaceAround(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf(
            "justify" to JsonPrimitive("space-around"), "gap" to JsonPrimitive(20),
        ), elements = listOf("first", "second").map { name ->
            ControlElement(scope = "#/properties/$name", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            ))
        })
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(400.dp)) {
                    JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                        schema = schema, uischema = ui,
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers)
                }
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        assertTrue(abs((fields[0].left - fields[1].left) - 210f) < 3f)
    }

    @Test fun materialJustifiesFixedChildrenWithMinimumGap() = justifyFixed(false)

    @Test fun cupertinoJustifiesFixedChildrenWithMinimumGap() = justifyFixed(true)

    @Test fun materialAlignsUnequalHeightChildren() = alignUnequal(false)

    @Test fun cupertinoAlignsUnequalHeightChildren() = alignUnequal(true)

    @Test fun materialJustifiesEachWrappedRow() = justifiedWrap(false)

    @Test fun cupertinoJustifiesEachWrappedRow() = justifiedWrap(true)

    private fun justifiedWrap(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        fun fixed(name: String, width: Int) = ControlElement(scope = "#/properties/$name", options = mapOf(
            "layout" to buildJsonObject { put("width", width) },
        ))
        val ui = HorizontalLayout(options = mapOf(
            "wrap" to JsonPrimitive(true), "justify" to JsonPrimitive("end"), "gap" to JsonPrimitive(10),
        ), elements = listOf(fixed("first", 180), fixed("second", 180), fixed("third", 100)))
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, fields.size)
        assertTrue(fields[1].top > fields[0].top)
        assertTrue(abs((fields[0].left - fields[1].left) - 110f) < 3f)
    }


    private fun justifyFixed(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(justify: String) = HorizontalLayout(options = mapOf(
            "gap" to JsonPrimitive(20), "justify" to JsonPrimitive(justify),
        ), elements = listOf("first", "second").map { name ->
            ControlElement(scope = "#/properties/$name", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            ))
        })
        var ui by mutableStateOf(layout("start"))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun lefts(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.left }
        val origin = lefts().first()
        val expected = mapOf(
            "start" to listOf(0f, 120f),
            "center" to listOf(90f, 210f),
            "end" to listOf(180f, 300f),
            "space-between" to listOf(0f, 300f),
            "space-around" to listOf(45f, 255f),
            "space-evenly" to listOf(60f, 240f),
        )
        expected.forEach { (mode, positions) ->
            runOnIdle { ui = layout(mode) }
            val actual = lefts().map { it - origin }
            assertEquals(2, actual.size)
            actual.zip(positions).forEach { (x, target) ->
                assertTrue(abs(x - target) < 3f, "$mode expected $target, got $x")
            }
        }
    }

    private fun alignUnequal(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(align: String) = HorizontalLayout(options = mapOf(
            "gap" to JsonPrimitive(0), "align" to JsonPrimitive(align),
        ), elements = listOf(
            VerticalLayout(options = mapOf("layout" to buildJsonObject { put("width", 180) }),
                elements = listOf(ControlElement(scope = "#/properties/first"))),
            VerticalLayout(options = mapOf("layout" to buildJsonObject { put("width", 180) }),
                elements = listOf(
                    ControlElement(scope = "#/properties/second"),
                    ControlElement(scope = "#/properties/third"),
                )),
        ))
        var ui by mutableStateOf(layout("start"))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
            }
        }
        fun tops(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.top }
        val start = tops()
        assertEquals(3, start.size)
        runOnIdle { ui = layout("end") }
        val end = tops()
        assertTrue(end[0] > start[0] + 20f)
        assertTrue(abs(end[1] - start[1]) < 3f)
        runOnIdle { ui = layout("center") }
        val center = tops()
        assertTrue(center[0] > start[0] && center[0] < end[0])
        runOnIdle { ui = layout("stretch") }
        assertEquals(3, tops().size)
        waitForIdle()
        assertEquals(listOf("layout.stretchFallback", "layout.stretchFallback"), diagnostics.map { it.code })
    }

    @Test fun materialWrapsNonfittingFixedChildren() = wrappedFixed(false)

    @Test fun cupertinoWrapsNonfittingFixedChildren() = wrappedFixed(true)

    @Test fun materialWrapsAutoChildrenAtMinimumWidth() = wrappedAuto(false)

    @Test fun cupertinoWrapsAutoChildrenAtMinimumWidth() = wrappedAuto(true)

    @Test fun materialReflowsWrappedChildrenOnResize() = wrappedResize(false)

    @Test fun cupertinoReflowsWrappedChildrenOnResize() = wrappedResize(true)

    private fun wrappedResize(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf(
            "wrap" to JsonPrimitive(true), "gap" to JsonPrimitive(10), "minItemWidth" to JsonPrimitive(120),
        ), elements = listOf("first", "second", "third").map { ControlElement(scope = "#/properties/$it") })
        var width by mutableStateOf(260.dp)
        setContent {
            Box(Modifier.width(width)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun tops(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.top }
        val narrow = tops()
        assertEquals(3, narrow.size)
        assertTrue(narrow[2] > narrow[1])
        runOnIdle { width = 400.dp }
        val wide = tops()
        assertEquals(3, wide.size)
        assertTrue(abs(wide[2] - wide[0]) < 3f)
    }

    private fun wrappedFixed(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        fun fixed(name: String, width: Int) = ControlElement(scope = "#/properties/$name", options = mapOf(
            "layout" to buildJsonObject { put("width", width) },
        ))
        val ui = HorizontalLayout(options = mapOf("wrap" to JsonPrimitive(true), "gap" to JsonPrimitive(10)),
            elements = listOf(fixed("first", 180), fixed("second", 180), fixed("third", 100)))
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, fields.size)
        assertTrue(fields[1].top > fields[0].bottom, "Second child did not wrap")
        assertTrue(abs(fields[2].top - fields[1].top) < 3f)
        assertTrue(fields[2].left > fields[1].left)
    }

    private fun wrappedAuto(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf(
            "wrap" to JsonPrimitive(true), "gap" to JsonPrimitive(10), "minItemWidth" to JsonPrimitive(120),
        ), elements = listOf("first", "second", "third").map { ControlElement(scope = "#/properties/$it") })
        setContent {
            Box(Modifier.width(260.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, fields.size)
        assertTrue(abs(fields[1].top - fields[0].top) < 3f)
        assertTrue(fields[2].top > fields[1].bottom)
    }

    @Test fun materialVerticalWeightKeepsNaturalHeightInScroll() = unboundedWeightedControl(false)

    @Test fun cupertinoVerticalWeightKeepsNaturalHeightInScroll() = unboundedWeightedControl(true)

    private fun unboundedWeightedControl(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(weighted: Boolean) = VerticalLayout(options = mapOf("gap" to JsonPrimitive(0)), elements = listOf(
            ControlElement(scope = "#/properties/first", options = if (weighted) mapOf(
                "layout" to buildJsonObject { put("weight", 2) },
            ) else emptyMap()),
            ControlElement(scope = "#/properties/second"),
        ))
        var ui by mutableStateOf(layout(false))
        setContent {
            Column(Modifier.width(300.dp).verticalScroll(rememberScrollState())) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun secondTop(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(2, fields.size)
            return fields[1].top
        }
        val naturalTop = secondTop()
        runOnIdle { ui = layout(true) }
        assertTrue(abs(secondTop() - naturalTop) < 3f)
    }

    @Test fun materialWeightsAVerticalControl() = weightedColumn(false)

    @Test fun cupertinoWeightsAVerticalControl() = weightedColumn(true)

    @Test fun materialWeightedColumnRetainsSpacerBases() = weightedBases(false)

    @Test fun cupertinoWeightedColumnRetainsSpacerBases() = weightedBases(true)

    private fun weightedColumn(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(options = mapOf("gap" to JsonPrimitive(0)), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("weight", 1) },
            )),
            ControlElement(scope = "#/properties/second"),
        ))
        setContent {
            Box(Modifier.width(300.dp).height(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        assertTrue(fields[1].top > 300f, "Weighted second field top=${fields[1].top}")
    }

    private fun weightedBases(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"middle":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        fun control(name: String) = ControlElement(scope = "#/properties/$name")
        fun spacer(size: Int) = SpacerElement(size = JsonPrimitive(size), options = mapOf(
            "layout" to buildJsonObject { put("weight", 1) },
        ))
        val ui = VerticalLayout(options = mapOf("gap" to JsonPrimitive(0)), elements = listOf(
            control("first"), spacer(50), control("middle"), spacer(100), control("last"),
        ))
        setContent {
            Box(Modifier.width(300.dp).height(500.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","middle":"B","last":"C"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, fields.size)
        val firstSlot = fields[1].top - fields[0].bottom
        val secondSlot = fields[2].top - fields[1].bottom
        assertTrue(abs((secondSlot - firstSlot) - 50f) < 3f,
            "Weighted spacer basis difference=${secondSlot - firstSlot}")
    }

    @Test fun materialUsesFixedVerticalControlHeight() = fixedColumn(false)

    @Test fun cupertinoUsesFixedVerticalControlHeight() = fixedColumn(true)

    @Test fun materialAppliesVerticalMinimumHeight() = boundedColumn(false, minimum = 160, maximum = 90, expected = 170f)

    @Test fun cupertinoAppliesVerticalMinimumHeight() = boundedColumn(true, minimum = 160, maximum = 90, expected = 170f)

    @Test fun materialAppliesVerticalMaximumHeight() = boundedColumn(false, minimum = null, maximum = 90, expected = 100f)

    @Test fun cupertinoAppliesVerticalMaximumHeight() = boundedColumn(true, minimum = null, maximum = 90, expected = 100f)

    private fun boundedColumn(cupertino: Boolean, minimum: Int?, maximum: Int, expected: Float) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(10)),
            elements = listOf(
                ControlElement(scope = "#/properties/first", options = mapOf("layout" to buildJsonObject {
                    put("height", 120)
                    if (minimum != null) put("minHeight", minimum)
                    put("maxHeight", maximum)
                })),
                ControlElement(scope = "#/properties/second"),
            ),
        )
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema,
                    uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        assertTrue(abs((fields[1].top - fields[0].top) - expected) < 3f)
    }

    private fun fixedColumn(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(10)),
            elements = listOf(
                ControlElement(scope = "#/properties/first", options = mapOf(
                    "layout" to buildJsonObject { put("height", 120) },
                )),
                ControlElement(scope = "#/properties/second"),
            ),
        )
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema,
                    uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        assertTrue(abs((fields[1].top - fields[0].top) - 130f) < 3f)
    }

    @Test fun materialResolvesFixedSpanAndWeightedRemainder() = sizedRow(false)

    @Test fun cupertinoResolvesFixedSpanAndWeightedRemainder() = sizedRow(true)

    private fun sizedRow(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"fixed":{"type":"string"},"span":{"type":"string"},"weighted":{"type":"string"},"auto":{"type":"string"}}}"""
        ).jsonObject)
        fun item(name: String, layout: kotlinx.serialization.json.JsonObject?) = ControlElement(
            scope = "#/properties/$name",
            options = if (layout == null) emptyMap() else mapOf("layout" to layout),
        )
        val ui = HorizontalLayout(
            options = mapOf("gridColumns" to JsonPrimitive(4), "gap" to JsonPrimitive(0)),
            elements = listOf(
                item("fixed", buildJsonObject { put("width", 100) }),
                item("span", buildJsonObject { put("span", 1) }),
                item("weighted", buildJsonObject { put("weight", 2) }),
                item("auto", null),
            ),
        )
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """{"fixed":"A","span":"B","weighted":"C","auto":"D"}"""
                    ),
                    schema = schema,
                    uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(4, fields.size)
        fun near(actual: Float, expected: Float) {
            assertTrue(abs(actual - expected) < 3f, "Expected $expected px, got $actual px")
        }
        near(fields[1].left - fields[0].left, 100f)
        near(fields[2].left - fields[1].left, 150f)
        near(fields[3].left - fields[2].left, 350f * 2f / 3f)
        near(fields[1].width - fields[0].width, 50f)
        near(fields[2].width - fields[1].width, 350f * 2f / 3f - 150f)
    }
}
