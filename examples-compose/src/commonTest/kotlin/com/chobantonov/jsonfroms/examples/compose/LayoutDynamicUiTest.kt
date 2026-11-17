package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutDynamicUiTest {
    @Test fun materialLiveWrapDefaultRespectsLocalFalse() = liveWrapDefault(false)
    @Test fun cupertinoLiveWrapDefaultRespectsLocalFalse() = liveWrapDefault(true)

    private fun liveWrapDefault(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "first":{"type":"string"},"second":{"type":"string"},
                "third":{"type":"string"},"fourth":{"type":"string"}
            }
        }""").jsonObject)
        fun child(name: String) = ControlElement("#/properties/$name", options = mapOf(
            "layout" to kotlinx.serialization.json.buildJsonObject {
                put("width", 150); put("minWidth", 150)
            },
        ))
        val ui = VerticalLayout(options = mapOf("gap" to JsonPrimitive(0)), elements = listOf(
            HorizontalLayout(options = mapOf("gap" to JsonPrimitive(10)),
                elements = listOf(child("first"), child("second"))),
            HorizontalLayout(options = mapOf("gap" to JsonPrimitive(10), "wrap" to JsonPrimitive(false)),
                elements = listOf(child("third"), child("fourth"))),
        ))
        var wrap by mutableStateOf(false)
        var changes = 0
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """{"first":"A","second":"B","third":"C","fourth":"D"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    config = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf("wrap" to wrap))),
                    onChange = { changes++ },
                )
            }
        }
        fun tops() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.top }
        val initial = tops()
        assertEquals(4, initial.size)
        assertTrue(abs(initial[1] - initial[0]) < 3f, "Inherited layout unexpectedly wrapped: $initial")
        assertTrue(abs(initial[3] - initial[2]) < 3f, "Local false layout unexpectedly wrapped: $initial")
        runOnIdle { wrap = true }
        waitForIdle()
        val changed = tops()
        assertTrue(changed[1] > changed[0] + 10f, "Inherited layout did not wrap: $changed")
        assertTrue(abs(changed[3] - changed[2]) < 3f, "Local false layout wrapped: $changed")
        assertEquals(0, changes)
    }

    @Test fun materialLiveGridDefaultRespectsLocalOverride() = liveGridDefault(false)
    @Test fun cupertinoLiveGridDefaultRespectsLocalOverride() = liveGridDefault(true)

    private fun liveGridDefault(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{
                "first":{"type":"string"},"second":{"type":"string"},
                "third":{"type":"string"},"fourth":{"type":"string"}
            }
        }""").jsonObject)
        fun child(name: String) = ControlElement("#/properties/$name", options = mapOf(
            "layout" to kotlinx.serialization.json.buildJsonObject { put("span", 1) },
        ))
        val ui = VerticalLayout(options = mapOf("gap" to JsonPrimitive(0)), elements = listOf(
            HorizontalLayout(options = mapOf("gap" to JsonPrimitive(0)),
                elements = listOf(child("first"), child("second"))),
            HorizontalLayout(options = mapOf("gap" to JsonPrimitive(0), "gridColumns" to JsonPrimitive(2)),
                elements = listOf(child("third"), child("fourth"))),
        ))
        var columns by mutableStateOf(2)
        var changes = 0
        setContent {
            Box(Modifier.width(240.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """{"first":"A","second":"B","third":"C","fourth":"D"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    config = mapOf(
                        "layoutDefaults" to mapOf("gridColumns" to 8),
                        "jsonformsExtended" to mapOf("layoutDefaults" to mapOf("gridColumns" to columns)),
                    ),
                    onChange = { changes++ },
                )
            }
        }
        fun widths() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.width }
        val initial = widths()
        assertEquals(4, initial.size)
        assertTrue(abs(initial[0] - initial[2]) < 3f, "Initial global/local spans differ: $initial")
        runOnIdle { columns = 4 }
        waitForIdle()
        val changed = widths()
        assertTrue(changed[0] < initial[0] - 40f, "Global span did not respond: $initial to $changed")
        assertTrue(abs(changed[2] - initial[2]) < 3f, "Local grid override changed: $initial to $changed")
        assertEquals(0, changes)
    }

    @Test fun materialContainerChildrenUseOnlyParentGap() = containerGap(false)

    @Test fun cupertinoContainerChildrenUseOnlyParentGap() = containerGap(true)

    @Test fun materialHorizontalContainersUseOnlyParentGap() = containerGap(false, horizontal = true)

    @Test fun cupertinoHorizontalContainersUseOnlyParentGap() = containerGap(true, horizontal = true)

    @Test fun materialWrappedContainersUseOnlyParentGap() = containerGap(false, horizontal = true, wrap = true)

    @Test fun cupertinoWrappedContainersUseOnlyParentGap() = containerGap(true, horizontal = true, wrap = true)

    private fun containerGap(
        cupertino: Boolean,
        horizontal: Boolean = false,
        wrap: Boolean = false,
    ) = runComposeUiTest {
        val containerSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"details":{"type":"object","properties":{"value":{"type":"string"}}},"items":{"type":"array","items":{"type":"string"}},"last":{"type":"string"}}}"""
        ).jsonObject)
        fun layout(gap: Int?): UiSchemaElement {
            val options = (gap?.let { mapOf("gap" to JsonPrimitive(it)) } ?: emptyMap()) +
                (if (wrap) mapOf("wrap" to JsonPrimitive(true)) else emptyMap())
            val elements = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/details"),
            ControlElement(scope = "#/properties/items"),
            ControlElement(scope = "#/properties/last"),
            )
            return if (horizontal) HorizontalLayout(options = options, elements = elements)
                else VerticalLayout(options = options, elements = elements)
        }
        var ui by mutableStateOf(layout(20))
        setContent {
            Box(Modifier.width(if (horizontal) 1200.dp else 400.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """{"first":"A","details":{"value":"B"},"items":["C"],"last":"D"}"""),
                    schema = containerSchema,
                    uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        fun gaps(): List<Float> {
            val bounds = (0..3).map { index ->
                onNodeWithTag("layout-child--$index").fetchSemanticsNode().boundsInRoot
            }
            return (1..3).map { index ->
                if (horizontal) bounds[index].left - bounds[index - 1].right
                else bounds[index].top - bounds[index - 1].bottom
            }
        }
        val configured = gaps()
        configured.forEach { gap -> assertTrue(abs(gap - 20f) < 2f, "Expected 20dp outer gap, got $configured") }
        runOnIdle { ui = layout(0) }
        val zero = gaps()
        zero.forEach { gap -> assertTrue(abs(gap) < 2f, "Expected zero outer gap, got $zero") }
        runOnIdle { ui = layout(null) }
        val default = gaps()
        default.forEach { gap -> assertTrue(abs(gap - 16f) < 2f, "Expected default 16dp gap, got $default") }
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 3)
    }

    @Test fun materialRowReflowsWhenRuleChanges() = visibility(false, horizontal = true)

    @Test fun cupertinoRowReflowsWhenRuleChanges() = visibility(true, horizontal = true)

    @Test fun materialColumnReflowsWhenRuleChanges() = visibility(false, horizontal = false)

    @Test fun cupertinoColumnReflowsWhenRuleChanges() = visibility(true, horizontal = false)

    @Test fun materialHiddenFirstRowChildReflows() = edgeVisibility(false, hiddenFirst = true)

    @Test fun cupertinoHiddenFirstRowChildReflows() = edgeVisibility(true, hiddenFirst = true)

    @Test fun materialHiddenLastRowChildReflows() = edgeVisibility(false, hiddenFirst = false)

    @Test fun cupertinoHiddenLastRowChildReflows() = edgeVisibility(true, hiddenFirst = false)

    @Test fun materialNestedRowReflowsWhenChildChanges() = nestedVisibility(false)

    @Test fun cupertinoNestedRowReflowsWhenChildChanges() = nestedVisibility(true)

    @Test fun materialDeepSingleChildNestingAddsNoOuterGap() = deepSingleChild(false)

    @Test fun cupertinoDeepSingleChildNestingAddsNoOuterGap() = deepSingleChild(true)

    @Test fun materialLocalZeroGapOverridesConfig() = gapPrecedence(false)

    @Test fun cupertinoLocalZeroGapOverridesConfig() = gapPrecedence(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"first":{"type":"string"},"middle":{"type":"string"},"last":{"type":"string"},"show":{"type":"boolean"}}}"""
    ).jsonObject)

    private fun controls(): List<UiSchemaElement> = listOf(
        ControlElement(scope = "#/properties/first"),
        ControlElement(
            scope = "#/properties/middle",
            rule = Rule(
                effect = RuleEffect.SHOW,
                condition = SchemaBasedCondition(
                    scope = "#/properties/show",
                    schema = buildJsonSchema { put("const", true) },
                ),
            ),
        ),
        ControlElement(scope = "#/properties/last"),
    )

    private fun visibility(cupertino: Boolean, horizontal: Boolean) = runComposeUiTest {
        var data by mutableStateOf(sampleData(show = false))
        val layout = if (horizontal) HorizontalLayout(elements = controls())
            else VerticalLayout(elements = controls())
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(
                    data = data,
                    schema = schema,
                    uischema = layout,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        fun bounds() = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        val hidden = bounds()
        assertEquals(2, hidden.size)
        if (horizontal) {
            assertTrue(abs((hidden[1].left - hidden[0].left) - 308f) < 4f)
        }
        runOnIdle { data = sampleData(show = true) }
        val shown = bounds()
        assertEquals(3, shown.size)
        if (horizontal) {
            assertTrue(abs((shown[1].left - shown[0].left) - 205.33333f) < 4f)
            assertTrue(abs((shown[2].left - shown[1].left) - 205.33333f) < 4f)
        } else {
            assertTrue(shown[2].top > hidden[1].top + 20f)
        }
        runOnIdle { data = sampleData(show = false) }
        val hiddenAgain = bounds()
        assertEquals(2, hiddenAgain.size)
        if (horizontal) assertTrue(abs(hiddenAgain[1].left - hidden[1].left) < 2f)
        else assertTrue(abs(hiddenAgain[1].top - hidden[1].top) < 2f)
    }

    private fun edgeVisibility(cupertino: Boolean, hiddenFirst: Boolean) = runComposeUiTest {
        val hiddenName = if (hiddenFirst) "first" else "last"
        val names = listOf("first", "middle", "last")
        val ui = HorizontalLayout(elements = names.map { name ->
            ControlElement(
                scope = "#/properties/$name",
                rule = if (name == hiddenName) Rule(
                    effect = RuleEffect.SHOW,
                    condition = SchemaBasedCondition(
                        scope = "#/properties/show",
                        schema = buildJsonSchema { put("const", true) },
                    ),
                ) else null,
            )
        })
        var data by mutableStateOf(sampleData(show = false))
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun lefts(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.left }
        val hidden = lefts()
        assertEquals(2, hidden.size)
        assertTrue(abs((hidden[1] - hidden[0]) - 308f) < 4f)
        runOnIdle { data = sampleData(show = true) }
        val shown = lefts()
        assertEquals(3, shown.size)
        assertTrue(abs((shown[1] - shown[0]) - 205.33333f) < 4f)
        assertTrue(abs((shown[2] - shown[1]) - 205.33333f) < 4f)
        runOnIdle { data = sampleData(show = false) }
        assertTrue(abs((lefts()[1] - lefts()[0]) - 308f) < 4f)
    }

    private fun nestedVisibility(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(sampleData(show = false))
        val ui = VerticalLayout(elements = listOf(HorizontalLayout(elements = controls())))
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(data = data, schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers)
            }
        }
        fun lefts(): List<Float> = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot.left }
        val hidden = lefts()
        assertEquals(2, hidden.size)
        assertTrue(abs((hidden[1] - hidden[0]) - 308f) < 4f)
        runOnIdle { data = sampleData(show = true) }
        val shown = lefts()
        assertEquals(3, shown.size)
        assertTrue(abs((shown[2] - shown[1]) - 205.33333f) < 4f)
        runOnIdle { data = sampleData(show = false) }
        assertTrue(abs((lefts()[1] - lefts()[0]) - 308f) < 4f)
    }

    private fun deepSingleChild(cupertino: Boolean) = runComposeUiTest {
        val first = ControlElement(scope = "#/properties/first")
        val last = ControlElement(scope = "#/properties/last")
        val outerOptions = mapOf("gap" to JsonPrimitive(16))
        var ui: UiSchemaElement by mutableStateOf(VerticalLayout(
            options = outerOptions,
            elements = listOf(first, last),
        ))
        setContent {
            JsonForms(data = sampleData(show = false), schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        fun gap(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(2, fields.size)
            return fields[1].top - fields[0].bottom
        }
        val direct = gap()
        runOnIdle {
            ui = VerticalLayout(options = outerOptions, elements = listOf(
                VerticalLayout(options = mapOf("gap" to JsonPrimitive(32)), elements = listOf(
                    VerticalLayout(options = mapOf("gap" to JsonPrimitive(48)), elements = listOf(first)),
                )),
                last,
            ))
        }
        assertTrue(abs(gap() - direct) < 3f, "Deep single-child nesting added an outer gap")
    }

    private fun gapPrecedence(cupertino: Boolean) = runComposeUiTest {
        val children = listOf(
            ControlElement(scope = "#/properties/first"),
            ControlElement(scope = "#/properties/last"),
        )
        var layout: UiSchemaElement by mutableStateOf(VerticalLayout(
            options = mapOf("gap" to JsonPrimitive(0)),
            elements = children,
        ))
        var config by mutableStateOf<Map<String, Any?>>(mapOf(
            "jsonformsExtended" to mapOf("layoutDefaults" to mapOf("gap" to 32)),
        ))
        setContent {
            JsonForms(
                data = sampleData(show = false),
                schema = schema,
                uischema = layout,
                config = config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        fun gap(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(2, fields.size)
            return fields[1].top - fields[0].bottom
        }
        val localZero = gap()
        runOnIdle { layout = VerticalLayout(elements = children) }
        val configured = gap()
        assertTrue(abs((configured - localZero) - 32f) < 3f)
        runOnIdle {
            config = mapOf("jsonformsExtended" to mapOf("layoutDefaults" to mapOf("gap" to 8)))
        }
        val changedConfig = gap()
        assertTrue(abs((configured - changedConfig) - 24f) < 3f)
    }

    private fun sampleData(show: Boolean) = Json.parseToJsonElement(
        """{"first":"A","middle":"B","last":"C","show":$show}"""
    )
}
