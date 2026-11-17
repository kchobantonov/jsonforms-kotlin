package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutSpacerUiTest {
    @Test fun materialImportedSpacersKeepDefaultExplicitAndZeroSizes() = importedSizes(false)

    @Test fun cupertinoImportedSpacersKeepDefaultExplicitAndZeroSizes() = importedSizes(true)

    private fun importedSizes(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-presentation" }
        val sections = assertIs<VerticalLayout>(fixture.uischema).elements
        val spacing = assertIs<CategorizationElement>(sections[1]).elements
            .filterIsInstance<CategoryElement>().first { it.name == "spacing" }
        val spacers = spacing.elements.filterIsInstance<SpacerElement>()
        assertEquals(3, spacers.size)
        assertEquals(null, spacers[0].size)
        assertEquals(JsonPrimitive(64), spacers[1].size)
        assertEquals(JsonPrimitive(0), spacers[2].size)
        var selected by mutableStateOf(0)
        var changes = 0
        setContent {
            Column {
                JsonForms(data = fixture.data, schema = fixture.schema!!,
                    uischema = spacers[selected],
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { changes++ })
                Box(Modifier.width(1.dp).height(1.dp).testTag("after-imported-spacer"))
            }
        }
        fun markerTop() = onNodeWithTag("after-imported-spacer").fetchSemanticsNode().boundsInRoot.top
        val defaultTop = markerTop()
        runOnIdle { selected = 1 }
        val explicitTop = markerTop()
        runOnIdle { selected = 2 }
        val zeroTop = markerTop()
        assertTrue(abs((defaultTop - zeroTop) - 32f) < 3f)
        assertTrue(abs((explicitTop - zeroTop) - 64f) < 3f)
        assertEquals(0, changes)
    }

    @Test fun materialRowSpacerUsesWidth() = row(false, weighted = false)

    @Test fun cupertinoRowSpacerUsesWidth() = row(true, weighted = false)

    @Test fun materialRowSpacerKeepsSurroundingGaps() = row(false, weighted = false, gap = 16)

    @Test fun cupertinoRowSpacerKeepsSurroundingGaps() = row(true, weighted = false, gap = 16)

    @Test fun materialEmbeddedSpacerNeverChangesFormData() = embeddedNoChanges(false)

    @Test fun cupertinoEmbeddedSpacerNeverChangesFormData() = embeddedNoChanges(true)

    @Test fun materialWeightedSpacerPushesLastChild() = row(false, weighted = true)

    @Test fun cupertinoWeightedSpacerPushesLastChild() = row(true, weighted = true)

    @Test fun materialColumnSpacerUsesHeight() = column(false)

    @Test fun cupertinoColumnSpacerUsesHeight() = column(true)

    @Test fun materialColumnSpacerHonorsMaximumHeight() = boundedSpacer(false)

    @Test fun cupertinoColumnSpacerHonorsMaximumHeight() = boundedSpacer(true)

    private fun boundedSpacer(cupertino: Boolean) = runComposeUiTest {
        fun layout(maxHeight: Int) = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                SpacerElement(size = JsonPrimitive(100), options = mapOf(
                    "layout" to buildJsonObject { put("maxHeight", maxHeight) },
                )),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        var ui by mutableStateOf(layout(0))
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun gap(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(2, fields.size)
            return fields[1].top - fields[0].bottom
        }
        val baseline = gap()
        runOnIdle { ui = layout(40) }
        assertTrue(abs((gap() - baseline) - 40f) < 3f,
            "Spacer added ${gap() - baseline} px")
    }

    @Test fun materialWeightedColumnSpacerFillsBoundedHeight() = weightedColumn(false)

    @Test fun cupertinoWeightedColumnSpacerFillsBoundedHeight() = weightedColumn(true)

    @Test fun materialWeightedColumnUsesIntrinsicHeightWhenUnbounded() = unboundedWeightedColumn(false)

    @Test fun cupertinoWeightedColumnUsesIntrinsicHeightWhenUnbounded() = unboundedWeightedColumn(true)

    @Test fun materialStandaloneSpacerDefaultsToHeightAndReportsInvalidSize() = standalone(false)

    @Test fun cupertinoStandaloneSpacerDefaultsToHeightAndReportsInvalidSize() = standalone(true)

    private fun standalone(cupertino: Boolean) = runComposeUiTest {
        val received = mutableListOf<UiDiagnostic>()
        val reporter = UiDiagnosticReporter { received += it }
        var changes = 0
        var ui by mutableStateOf(SpacerElement(size = JsonPrimitive(-1)))
        setContent {
            Column {
                JsonForms(
                    data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    config = mapOf(UiDiagnosticReporterConfigKey to reporter),
                    onChange = { changes++ },
                )
                Box(Modifier.width(1.dp).height(1.dp).testTag("after-spacer"))
            }
        }
        fun markerTop() = onNodeWithTag("after-spacer").fetchSemanticsNode().boundsInRoot.top
        waitForIdle()
        val defaultTop = markerTop()
        assertEquals(listOf("layout.invalidOption"), received.map { it.code })
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        assertEquals(0, onAllNodes(isFocusable()).fetchSemanticsNodes().size)
        runOnIdle { ui = SpacerElement(size = JsonPrimitive(0)) }
        val zeroTop = markerTop()
        assertTrue(abs((defaultTop - zeroTop) - 32f) < 3f, "Default top=$defaultTop, zero top=$zeroTop")
        assertEquals(0, changes)
    }

    private fun embeddedNoChanges(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        var value by mutableStateOf(data)
        fun layout(size: Int) = HorizontalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = listOf(fixed("first"), SpacerElement(size = JsonPrimitive(size)), fixed("last")),
        )
        var ui by mutableStateOf(layout(0))
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(data = value, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { changes++; value = it.data })
            }
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { ui = layout(48) }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(data, value)
        assertEquals(0, changes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
    ).jsonObject)
    private val data = Json.parseToJsonElement("""{"first":"A","last":"B"}""")

    private fun fixed(name: String, width: Int = 100) = ControlElement(
        scope = "#/properties/$name",
        options = mapOf("layout" to buildJsonObject { put("width", width) }),
    )

    private fun row(cupertino: Boolean, weighted: Boolean, gap: Int = 0) = runComposeUiTest {
        val spacer = if (weighted) SpacerElement(
            size = JsonPrimitive(0),
            options = mapOf("layout" to buildJsonObject { put("weight", 1) }),
        ) else SpacerElement(size = JsonPrimitive(48))
        val ui = HorizontalLayout(
            options = mapOf("gap" to JsonPrimitive(gap)),
            elements = listOf(fixed("first"), spacer, if (weighted) fixed("last") else ControlElement(scope = "#/properties/last")),
        )
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(
                    data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        val expected = if (weighted) 500f else 148f + gap * 2f
        assertTrue(abs((fields[1].left - fields[0].left) - expected) < 3f)
    }

    private fun weightedColumn(cupertino: Boolean) = runComposeUiTest {
        val ui = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                SpacerElement(size = JsonPrimitive(0), options = mapOf("layout" to buildJsonObject { put("weight", 1) })),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        setContent {
            Box(Modifier.width(300.dp).height(400.dp)) {
                JsonForms(data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, fields.size)
        assertTrue(fields[1].top > 300f, "Last field top=${fields[1].top}")
    }

    private fun unboundedWeightedColumn(cupertino: Boolean) = runComposeUiTest {
        fun layout(size: Int) = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                SpacerElement(size = JsonPrimitive(size), options = mapOf("layout" to buildJsonObject { put("weight", 1) })),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        var ui by mutableStateOf(layout(0))
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                JsonForms(data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun lastTop(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            assertEquals(2, fields.size)
            return fields[1].boundsInRoot.top
        }
        val zero = lastTop()
        runOnIdle { ui = layout(48) }
        val sized = lastTop()
        assertTrue(abs((sized - zero) - 48f) < 3f, "Unbounded weighted spacer: zero=$zero, sized=$sized")
    }

    private fun column(cupertino: Boolean) = runComposeUiTest {
        fun layout(size: Int) = VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = listOf(
                ControlElement(scope = "#/properties/first"),
                SpacerElement(size = JsonPrimitive(size)),
                ControlElement(scope = "#/properties/last"),
            ),
        )
        var ui by mutableStateOf(layout(0))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        fun lastTop(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            assertEquals(2, fields.size)
            return fields[1].boundsInRoot.top
        }
        val zero = lastTop()
        runOnIdle { ui = layout(48) }
        assertTrue(abs((lastTop() - zero) - 48f) < 3f)
    }
}
