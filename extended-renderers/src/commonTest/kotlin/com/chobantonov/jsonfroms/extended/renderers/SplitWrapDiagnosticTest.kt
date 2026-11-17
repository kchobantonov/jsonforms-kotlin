package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class SplitWrapDiagnosticTest {
    @Test fun materialEmptyStructuralPaneDoesNotAddDivider() = emptyStructuralPaneMounted(materialExtendedRenderers)

    @Test fun cupertinoEmptyStructuralPaneDoesNotAddDivider() = emptyStructuralPaneMounted(cupertinoExtendedRenderers)

    private fun emptyStructuralPaneMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter")),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                VerticalLayout(),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","last":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-0").assertExists()
        onNodeWithTag("split-layout-divider-1").assertDoesNotExist()
    }

    @Test fun materialSplitterFunctionRuleReceivesContext() = splitterFunctionRuleMounted(materialExtendedRenderers)

    @Test fun cupertinoSplitterFunctionRuleReceivesContext() = splitterFunctionRuleMounted(cupertinoExtendedRenderers)

    private fun splitterFunctionRuleMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter")),
            elements = listOf(
                ControlElement(scope = "#/properties/first", rule = Rule(
                    RuleEffect.SHOW,
                    ValidateFunctionCondition(scope = "#", validate = { context ->
                        context.path == "" && (context.config as? Map<*, *>)?.get("showFirst") == true
                    }),
                )),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        var showFirst by mutableStateOf(true)
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","last":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers,
                    config = mapOf("showFirst" to showFirst))
            }
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-0").assertExists()
        runOnIdle { showFirst = false }
        waitForIdle()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-0").assertDoesNotExist()
    }

    @Test fun parsesDefiniteSplitterHeight() {
        assertEquals(320f, parseSplitterHeight("20rem"))
        assertEquals(400f, parseSplitterHeight("400dp"))
        assertEquals(400f, parseSplitterHeight("400px"))
        assertNull(parseSplitterHeight("auto"))
        assertNull(parseSplitterHeight("-20dp"))
    }

    @Test fun materialVerticalSplitterUsesBoundedParentHeight() = boundedHeightMounted(materialExtendedRenderers)

    @Test fun cupertinoVerticalSplitterUsesBoundedParentHeight() = boundedHeightMounted(cupertinoExtendedRenderers)

    @Test fun materialVerticalAutoPanesKeepNaturalHeight() = verticalAutoMounted(materialExtendedRenderers)

    @Test fun cupertinoVerticalAutoPanesKeepNaturalHeight() = verticalAutoMounted(cupertinoExtendedRenderers)

    @Test fun materialOverconstrainedVerticalPaneScrolls() = overconstrainedPaneMounted(materialExtendedRenderers)

    @Test fun cupertinoOverconstrainedVerticalPaneScrolls() = overconstrainedPaneMounted(cupertinoExtendedRenderers)

    private fun overconstrainedPaneMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"string"},"c":{"type":"string"},"d":{"type":"string"},"e":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "height" to JsonPrimitive("200dp")),
            elements = listOf(
                VerticalLayout(elements = listOf("a", "b", "c", "d").map {
                    ControlElement(scope = "#/properties/$it")
                }),
                ControlElement(scope = "#/properties/e"),
            ),
        )
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement(
                    """{"a":"A","b":"B","c":"C","d":"D","e":"E"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val content = onNodeWithTag("split-layout-content")
        val before = content.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        content.performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 100f)
        }
        val after = content.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(after > before + 30f, "Expected oversized splitter to scroll from $before to $after")
    }

    private fun verticalAutoMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "height" to JsonPrimitive("400dp")),
            elements = listOf(
                VerticalLayout(elements = listOf(
                    ControlElement(scope = "#/properties/first"),
                    ControlElement(scope = "#/properties/second"),
                )),
                ControlElement(scope = "#/properties/third"),
            ),
        )
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        waitForIdle()
        val divider = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot
        val controls = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(3, controls.size)
        assertTrue(divider.top > controls[1].boundsInRoot.bottom,
            "First Auto pane should contain both controls")
        assertTrue(divider.top in 110f..185f,
            "Expected natural first-pane height rather than half the 400dp frame, got ${divider.top}")
        assertTrue(controls[2].boundsInRoot.top > divider.bottom)
    }

    @Test fun materialHorizontalSplitterFitsTallestPane() = horizontalIntrinsicMounted(materialExtendedRenderers)

    @Test fun cupertinoHorizontalSplitterFitsTallestPane() = horizontalIntrinsicMounted(cupertinoExtendedRenderers)

    private fun horizontalIntrinsicMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"third":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            VerticalLayout(elements = listOf(
                ControlElement(scope = "#/properties/first"),
                ControlElement(scope = "#/properties/second"),
            )),
            ControlElement(scope = "#/properties/third"),
        ))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B","third":"C"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val divider = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot
        val controls = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(3, controls.size)
        val tallPaneBottom = maxOf(controls[0].boundsInRoot.bottom, controls[1].boundsInRoot.bottom)
        assertTrue(divider.height in 60f..200f, "Expected content-sized divider, got ${divider.height}")
        assertTrue(divider.bottom >= tallPaneBottom - 2f)
    }

    @Test fun materialUnboundedVerticalSplitterReportsFallback() = unboundedHeightMounted(materialExtendedRenderers)

    @Test fun cupertinoUnboundedVerticalSplitterReportsFallback() = unboundedHeightMounted(cupertinoExtendedRenderers)

    private fun unboundedHeightMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/second"),
        ))
        setContent {
            Column(Modifier.width(400.dp).height(500.dp).verticalScroll(rememberScrollState())) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers,
                    config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
            }
        }
        waitForIdle()
        assertEquals(listOf("layout.indefiniteHeight"), diagnostics.map { it.code })
        val frameHeight = onNodeWithTag("split-layout-frame").fetchSemanticsNode().boundsInRoot.height
        assertTrue(frameHeight in 358f..362f, "Expected 360dp fallback frame, got $frameHeight")
        val dividerTop = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(dividerTop in 40f..140f, "Expected natural Auto pane height, got $dividerTop")
    }

    private fun boundedHeightMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/second"),
        ))
        setContent {
            Box(Modifier.width(400.dp).height(500.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val frameHeight = onNodeWithTag("split-layout-frame").fetchSemanticsNode().boundsInRoot.height
        assertTrue(frameHeight in 498f..502f, "Expected parent-sized frame, got $frameHeight")
        val dividerTop = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(dividerTop in 40f..140f, "Expected natural Auto pane height, got $dividerTop")
    }

    @Test fun horizontalInitialSizingUsesFixedWidthAndSpanFallsBackToAuto() {
        val fixed = ControlElement(scope = "#/properties/first", options = mapOf(
            "layout" to buildJsonObject { put("width", 100) },
        ))
        val automatic = ControlElement(scope = "#/properties/second")
        val span = ControlElement(scope = "#/properties/third", options = mapOf(
            "layout" to buildJsonObject { put("span", 4) },
        ))
        val weights = initialHorizontalSplitWeights(listOf(fixed, automatic), 400f, emptyMap(), emptyMap())
        assertTrue(kotlin.math.abs(weights[0] - 100f / 376f) < 0.001f)
        val globalGap = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf("gap" to 32)))
        val globalWeights = initialHorizontalSplitWeights(listOf(fixed, automatic), 400f, emptyMap(), globalGap)
        assertTrue(kotlin.math.abs(globalWeights[0] - 100f / 360f) < 0.001f)
        assertEquals(listOf(0.5f, 0.5f),
            initialHorizontalSplitWeights(listOf(span, automatic), 400f, emptyMap(), emptyMap()))
        assertEquals("span", splitSpanDiagnostics(HorizontalLayout(elements = listOf(span))).single().details["key"])
    }

    @Test fun verticalInitialSizingUsesFixedHeightAndWeight() {
        val fixed = ControlElement(scope = "#/properties/first", options = mapOf(
            "layout" to buildJsonObject { put("height", 100) },
        ))
        val weighted = ControlElement(scope = "#/properties/second", options = mapOf(
            "layout" to buildJsonObject { put("weight", 2) },
        ))
        val automatic = ControlElement(scope = "#/properties/third")
        val sizes = initialVerticalSplitWeights(
            listOf(fixed, weighted, automatic), 400f, naturalHeights = listOf(0f, 20f, 50f),
        )
        assertTrue(kotlin.math.abs(sizes[0] - 100f / 352f) < 0.001f)
        assertTrue(kotlin.math.abs(sizes[1] - 202f / 352f) < 0.001f)
        assertTrue(kotlin.math.abs(sizes[2] - 50f / 352f) < 0.001f)
    }

    @Test fun verticalInitialSizingHonorsBoundsWhenAnotherPaneCanFill() {
        val capped = ControlElement(scope = "#/properties/first", options = mapOf(
            "layout" to buildJsonObject { put("minHeight", 100); put("maxHeight", 120) },
        ))
        val flexible = ControlElement(scope = "#/properties/second", options = mapOf(
            "layout" to buildJsonObject { put("weight", 1) },
        ))
        val sizes = initialVerticalSplitWeights(
            listOf(capped, flexible), 400f, naturalHeights = listOf(200f, 20f),
        )
        assertTrue(kotlin.math.abs(sizes[0] - 120f / 376f) < 0.001f)
    }

    @Test fun verticalAllCappedPanesLeaveSpareSpace() {
        val fixed = listOf("first", "second").map { property ->
            ControlElement(scope = "#/properties/$property", options = mapOf(
                "layout" to buildJsonObject { put("height", 100) },
            ))
        }
        val sizes = initialVerticalSplitWeights(fixed, 400f)
        assertTrue(kotlin.math.abs(sizes[0] - 100f / 376f) < 0.001f)
        assertTrue(kotlin.math.abs(sizes[1] - 100f / 376f) < 0.001f)
        assertTrue(sizes.sum() < 1f)
    }

    @Test fun verticalOverconstrainedFixedPanesKeepAuthoredExtents() {
        val fixed = listOf("first", "second").map { property ->
            ControlElement(scope = "#/properties/$property", options = mapOf(
                "layout" to buildJsonObject { put("height", 200) },
            ))
        }
        val sizes = initialVerticalSplitWeights(fixed, 300f)
        assertTrue(kotlin.math.abs(sizes[0] - 200f / 276f) < 0.001f)
        assertTrue(kotlin.math.abs(sizes[1] - 200f / 276f) < 0.001f)
        assertTrue(sizes.sum() > 1f)
    }

    @Test fun materialOverconstrainedFixedPaneKeepsHeight() = fixedOverflowMounted(materialExtendedRenderers)

    @Test fun cupertinoOverconstrainedFixedPaneKeepsHeight() = fixedOverflowMounted(cupertinoExtendedRenderers)

    private fun fixedOverflowMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val fixed = listOf("first", "second").map { property ->
            ControlElement(scope = "#/properties/$property", options = mapOf(
                "layout" to buildJsonObject { put("height", 200) },
            ))
        }
        val ui = VerticalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "height" to JsonPrimitive("300dp")),
            elements = fixed,
        )
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val dividerTop = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(dividerTop in 206f..210f, "Expected 200dp first pane, got $dividerTop")
        val scroll = onNodeWithTag("split-layout-content")
            .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(scroll.maxValue() > 100f)
    }

    @Test fun materialVerticalFixedPanesDoNotStretch() = cappedHeightMounted(materialExtendedRenderers)

    @Test fun cupertinoVerticalFixedPanesDoNotStretch() = cappedHeightMounted(cupertinoExtendedRenderers)

    private fun cappedHeightMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val fixed = listOf("first", "second").map { property ->
            ControlElement(scope = "#/properties/$property", options = mapOf(
                "layout" to buildJsonObject { put("height", 100) },
            ))
        }
        val ui = VerticalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "height" to JsonPrimitive("400dp")),
            elements = fixed,
        )
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val dividerTop = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot.top
        assertTrue(dividerTop in 106f..110f, "Expected a 100dp first pane, got $dividerTop")
    }

    @Test fun materialSplitterStartsWithFixedPaneWidth() = initialSizingMounted(materialExtendedRenderers)

    @Test fun cupertinoSplitterStartsWithFixedPaneWidth() = initialSizingMounted(cupertinoExtendedRenderers)

    @Test fun materialSplitterHonorsZeroGap() = initialSizingMounted(materialExtendedRenderers, gap = 0)

    @Test fun cupertinoSplitterHonorsZeroGap() = initialSizingMounted(cupertinoExtendedRenderers, gap = 0)

    @Test fun materialVerticalSplitterHonorsWideGap() =
        initialSizingMounted(materialExtendedRenderers, vertical = true, gap = 32)

    @Test fun cupertinoVerticalSplitterHonorsWideGap() =
        initialSizingMounted(cupertinoExtendedRenderers, vertical = true, gap = 32)

    @Test fun materialSplitterReportsSpanFallback() = spanDiagnosticMounted(materialExtendedRenderers)

    @Test fun cupertinoSplitterReportsSpanFallback() = spanDiagnosticMounted(cupertinoExtendedRenderers)

    private fun spanDiagnosticMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("span", 4) },
            )),
            ControlElement(scope = "#/properties/second"),
        ))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers,
                    config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)))
            }
        }
        waitForIdle()
        assertEquals(listOf("layout.unsupportedOption"), diagnostics.map { it.code })
        assertEquals("span", diagnostics.single().details["key"])
        val dividerLeft = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot.left
        assertTrue(dividerLeft in 194f..198f, "Expected Auto fallback, got $dividerLeft")
    }

    @Test fun materialVerticalSplitterStartsWithFixedPaneHeight() =
        initialSizingMounted(materialExtendedRenderers, vertical = true)

    @Test fun cupertinoVerticalSplitterStartsWithFixedPaneHeight() =
        initialSizingMounted(cupertinoExtendedRenderers, vertical = true)

    private fun initialSizingMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        vertical: Boolean = false,
        gap: Int? = null,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put(if (vertical) "height" else "width", 100) },
            )),
            ControlElement(scope = "#/properties/second"),
        )
        val options = mapOf("variant" to JsonPrimitive("splitter")) +
            (if (gap == null) emptyMap() else mapOf("gap" to JsonPrimitive(gap)))
        val ui: UiSchemaElement = if (vertical) VerticalLayout(
            options = options + ("height" to JsonPrimitive("400dp")),
            elements = elements,
        ) else HorizontalLayout(options = options, elements = elements)
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val bounds = onNodeWithTag("split-layout-divider-0").fetchSemanticsNode().boundsInRoot
        val position = if (vertical) bounds.top else bounds.left
        val expected = 100f + (gap ?: 16) / 2f
        assertTrue(position in (expected - 2f)..(expected + 2f),
            "Expected a 100dp pane and centered handle at $expected, got $position")
    }

    @Test fun resizableDefaultsTrueAndRejectsNonBoolean() {
        assertTrue(splitResizable(emptyMap()))
        assertEquals(false, splitResizable(mapOf("resizable" to JsonPrimitive(false))))
        assertNull(splitResizableDiagnostic(mapOf("resizable" to JsonPrimitive(false))))
        assertEquals("layout.invalidOption", splitResizableDiagnostic(
            mapOf("resizable" to JsonPrimitive("false")))?.code)
    }

    @Test fun materialSplitterSupportsKeyboardResize() = resizableMounted(materialExtendedRenderers, true)

    @Test fun cupertinoSplitterSupportsKeyboardResize() = resizableMounted(cupertinoExtendedRenderers, true)

    @Test fun materialHorizontalResizeSurvivesHostWidthChange() = hostResizeMounted(materialExtendedRenderers)

    @Test fun cupertinoHorizontalResizeSurvivesHostWidthChange() = hostResizeMounted(cupertinoExtendedRenderers)

    @Test fun materialVerticalResizeSurvivesHostHeightChange() = verticalHostResizeMounted(materialExtendedRenderers)

    @Test fun cupertinoVerticalResizeSurvivesHostHeightChange() = verticalHostResizeMounted(cupertinoExtendedRenderers)

    @Test fun materialVerticalResizeSurvivesNaturalHeightChange() = naturalHeightAfterDragMounted(materialExtendedRenderers)

    @Test fun cupertinoVerticalResizeSurvivesNaturalHeightChange() = naturalHeightAfterDragMounted(cupertinoExtendedRenderers)

    @Test fun materialUntouchedVerticalPaneTracksNaturalHeight() =
        naturalHeightAfterDragMounted(materialExtendedRenderers, userDrag = false)

    @Test fun cupertinoUntouchedVerticalPaneTracksNaturalHeight() =
        naturalHeightAfterDragMounted(cupertinoExtendedRenderers, userDrag = false)

    private fun naturalHeightAfterDragMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        userDrag: Boolean = true,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"extra":{"type":"string"},"last":{"type":"string"},"showExtra":{"type":"boolean"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "height" to JsonPrimitive("400dp")),
            elements = listOf(
                VerticalLayout(elements = listOf(
                    ControlElement(scope = "#/properties/first"),
                    ControlElement(scope = "#/properties/extra", rule = Rule(
                        effect = RuleEffect.SHOW,
                        condition = SchemaBasedCondition(
                            scope = "#/properties/showExtra",
                            schema = buildJsonSchema { put("const", true) },
                        ),
                    )),
                )),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        fun data(show: Boolean) = Json.parseToJsonElement(
            """{"first":"A","extra":"B","last":"C","showExtra":$show}"""
        )
        var formData by mutableStateOf(data(false))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = formData, schema = schema, uischema = ui, renderers = renderers)
            }
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        val divider = onNodeWithTag("split-layout-divider-0")
        if (userDrag) {
            divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            divider.performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        }
        val resizedTop = divider.fetchSemanticsNode().boundsInRoot.top
        runOnIdle { formData = data(true) }
        assertEquals(3, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        val after = divider.fetchSemanticsNode().boundsInRoot.top
        if (userDrag) {
            assertTrue(kotlin.math.abs(after - resizedTop) < 2f,
                "Expected user-resized divider at $resizedTop after content grew, got $after")
        } else {
            assertTrue(after > resizedTop + 20f,
                "Expected untouched divider to follow natural content height from $resizedTop, got $after")
        }
    }

    private fun verticalHostResizeMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = VerticalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("height", 100) },
            )),
            ControlElement(scope = "#/properties/second"),
        ))
        var hostHeight by mutableStateOf(400.dp)
        setContent {
            Box(Modifier.width(400.dp).height(hostHeight)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val divider = onNodeWithTag("split-layout-divider-0")
        fun position() = divider.fetchSemanticsNode().boundsInRoot.top
        assertTrue(position() in 106f..110f)
        runOnIdle { hostHeight = 500.dp }
        assertTrue(position() in 106f..110f, "Untouched fixed pane should remain 100dp")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        val resizedShare = (position() - 8f) / 476f
        assertTrue(resizedShare > 100f / 476f)
        runOnIdle { hostHeight = 600.dp }
        val nextShare = (position() - 8f) / 576f
        assertTrue(kotlin.math.abs(nextShare - resizedShare) < 0.01f,
            "Expected resized share $resizedShare after host resize, got $nextShare")
    }

    private fun hostResizeMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first", options = mapOf(
                "layout" to buildJsonObject { put("width", 100) },
            )),
            ControlElement(scope = "#/properties/second"),
        ))
        var hostWidth by mutableStateOf(400.dp)
        setContent {
            Box(Modifier.width(hostWidth)) {
                JsonForms(data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema, uischema = ui, renderers = renderers)
            }
        }
        val divider = onNodeWithTag("split-layout-divider-0")
        fun position() = divider.fetchSemanticsNode().boundsInRoot.left
        assertTrue(position() in 106f..110f)
        runOnIdle { hostWidth = 500.dp }
        assertTrue(position() in 106f..110f, "Untouched fixed pane should remain 100dp")
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
        val resizedShare = (position() - 8f) / 476f
        assertTrue(resizedShare > 100f / 476f)
        runOnIdle { hostWidth = 600.dp }
        val nextShare = (position() - 8f) / 576f
        assertTrue(kotlin.math.abs(nextShare - resizedShare) < 0.01f,
            "Expected resized share $resizedShare after host resize, got $nextShare")
    }

    @Test fun materialResizeStateSurvivesDataReplacement() = runtimeStateMounted(materialExtendedRenderers)

    @Test fun cupertinoResizeStateSurvivesDataReplacement() = runtimeStateMounted(cupertinoExtendedRenderers)

    private fun runtimeStateMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/second"),
        ))
        val authored = ui.copy()
        var formData by mutableStateOf(Json.parseToJsonElement("""{"first":"A","second":"B"}"""))
        var changes = 0
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = formData, schema = schema, uischema = ui, renderers = renderers,
                    onChange = { changes++ })
            }
        }
        val divider = onNodeWithTag("split-layout-divider-0")
        val before = divider.fetchSemanticsNode().boundsInRoot.left
        divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        divider.performKeyInput { keyDown(Key.DirectionRight); keyUp(Key.DirectionRight) }
        val resized = divider.fetchSemanticsNode().boundsInRoot.left
        assertTrue(resized > before + 8f)
        val changeCount = changes
        runOnIdle { formData = Json.parseToJsonElement("""{"first":"Updated","second":"B"}""") }
        assertTrue(kotlin.math.abs(divider.fetchSemanticsNode().boundsInRoot.left - resized) < 1f)
        assertEquals(changeCount, changes)
        assertEquals(authored, ui)
    }

    @Test fun materialVerticalSplitterSupportsKeyboardResize() =
        resizableMounted(materialExtendedRenderers, true, vertical = true)

    @Test fun cupertinoVerticalSplitterSupportsKeyboardResize() =
        resizableMounted(cupertinoExtendedRenderers, true, vertical = true)

    @Test fun materialReadOnlySplitterKeepsVisualBoundary() = resizableMounted(materialExtendedRenderers, false)

    @Test fun cupertinoReadOnlySplitterKeepsVisualBoundary() = resizableMounted(cupertinoExtendedRenderers, false)

    @Test fun materialHiddenPaneLeavesSplitterSizing() = visibilityMounted(materialExtendedRenderers)

    @Test fun cupertinoHiddenPaneLeavesSplitterSizing() = visibilityMounted(cupertinoExtendedRenderers)

    private fun visibilityMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"middle":{"type":"string"},"last":{"type":"string"},"showMiddle":{"type":"boolean"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(options = mapOf("variant" to JsonPrimitive("splitter")), elements = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/middle", rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/showMiddle",
                    schema = buildJsonSchema { put("const", true) },
                ),
            )),
            ControlElement(scope = "#/properties/last"),
        ))
        fun data(show: Boolean) = Json.parseToJsonElement(
            """{"first":"A","middle":"B","last":"C","showMiddle":$show}"""
        )
        var formData by mutableStateOf(data(false))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = formData, schema = schema, uischema = ui, renderers = renderers)
            }
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-0").assertExists()
        onNodeWithTag("split-layout-divider-1").assertDoesNotExist()
        runOnIdle { formData = data(true) }
        assertEquals(3, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-1").assertExists()
        runOnIdle { formData = data(false) }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithTag("split-layout-divider-1").assertDoesNotExist()
    }

    private fun resizableMounted(
        renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>,
        enabled: Boolean,
        vertical: Boolean = false,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val options = mapOf("variant" to JsonPrimitive("splitter")) +
            (if (enabled) emptyMap() else mapOf("resizable" to JsonPrimitive(false)))
        val elements = listOf(
                ControlElement(scope = "#/properties/first"),
                ControlElement(scope = "#/properties/second"),
            )
        val ui: UiSchemaElement = if (vertical) VerticalLayout(
            options = options + ("height" to JsonPrimitive("400dp")), elements = elements,
        ) else HorizontalLayout(options = options, elements = elements)
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                    schema = schema,
                    uischema = ui,
                    renderers = renderers,
                )
            }
        }
        fun secondPosition(): Float = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()[1].boundsInRoot.let {
            if (vertical) it.top else it.left
        }
        val before = secondPosition()
        val divider = onNodeWithTag("split-layout-divider-0")
        divider.assertExists()
        val dividerBefore = divider.fetchSemanticsNode().boundsInRoot.let { if (vertical) it.top else it.left }
        if (enabled) {
            onNodeWithContentDescription("Resize panes").assertExists()
            divider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            divider.performKeyInput {
                val key = if (vertical) Key.DirectionDown else Key.DirectionRight
                keyDown(key)
                keyUp(key)
            }
            val dividerAfter = divider.fetchSemanticsNode().boundsInRoot.let { if (vertical) it.top else it.left }
            assertTrue(dividerAfter > dividerBefore + 8f,
                "Expected keyboard resize to move divider from $dividerBefore to $dividerAfter")
            assertTrue(secondPosition() > before,
                "Expected second pane content to move from $before to ${secondPosition()}")
        } else {
            onNodeWithContentDescription("Resize panes").assertDoesNotExist()
            divider.performTouchInput {
                down(center)
                moveTo(Offset(center.x + 50f, center.y))
                up()
            }
            assertTrue(kotlin.math.abs(secondPosition() - before) < 3f)
        }
    }

    @Test fun localFalseOverridesGlobalWrapAndTrueIsDiagnosed() {
        val config = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf("wrap" to true)))
        assertNull(splitWrapDiagnostic(mapOf("wrap" to JsonPrimitive(false)), config))
        assertEquals("config.jsonformsExtended.layoutDefaults",
            splitWrapDiagnostic(emptyMap(), config)?.details?.get("source"))
        assertEquals("options", splitWrapDiagnostic(mapOf("wrap" to JsonPrimitive(true)), emptyMap())
            ?.details?.get("source"))
    }

    @Test fun materialReportsSplitterWrapWithoutChangingPanes() = mounted(materialExtendedRenderers)

    @Test fun cupertinoReportsSplitterWrapWithoutChangingPanes() = mounted(cupertinoExtendedRenderers)

    private fun mounted(renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>>) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(
            options = mapOf("variant" to JsonPrimitive("splitter"), "wrap" to JsonPrimitive(true)),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                ControlElement(scope = "#/properties/second"),
            ),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"first":"A","second":"B"}"""),
                schema = schema,
                uischema = ui,
                renderers = renderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter(diagnostics::add)),
            )
        }
        waitForIdle()
        assertEquals(listOf("layout.unsupportedOption"), diagnostics.map { it.code })
        assertEquals("wrap", diagnostics.single().details["key"])
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }
}
