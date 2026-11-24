package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.ValidateFunctionCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutVisibilityUiTest {
    @Test fun materialHiddenAncestorSuppressesVisibleChild() = hiddenAncestorSuppressesChild(false)
    @Test fun cupertinoHiddenAncestorSuppressesVisibleChild() = hiddenAncestorSuppressesChild(true)

    private fun hiddenAncestorSuppressesChild(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"outer":{"type":"boolean"},"inner":{"type":"boolean"},"value":{"type":"string"}}}"""
        ).jsonObject)
        fun showFlag(property: String) = Rule(
            effect = RuleEffect.SHOW,
            condition = SchemaBasedCondition(
                scope = "#/properties/$property",
                schema = buildJsonSchema { put("const", true) },
                failWhenUndefined = true,
            ),
        )
        val ui = VerticalLayout(elements = listOf(
            VerticalLayout(
                rule = showFlag("outer"),
                elements = listOf(ControlElement("#/properties/value", rule = showFlag("inner"))),
            ),
        ))
        var data by mutableStateOf(Json.parseToJsonElement("""{"outer":false,"inner":true,"value":"kept"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement("""{"outer":true,"inner":true,"value":"kept"}""") }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement("""{"outer":true,"inner":false,"value":"kept"}""") }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }

    @Test fun materialEmptyStructuralChildDoesNotAddColumnGap() = emptyStructuralColumnGap(false)
    @Test fun cupertinoEmptyStructuralChildDoesNotAddColumnGap() = emptyStructuralColumnGap(true)

    private fun emptyStructuralColumnGap(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        var withEmpty by mutableStateOf(false)
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"first":"A","last":"B"}"""),
                    schema = schema,
                    uischema = VerticalLayout(
                        options = mapOf("gap" to JsonPrimitive(12)),
                        elements = listOf(ControlElement("#/properties/first")) +
                            (if (withEmpty) listOf(HorizontalLayout()) else emptyList()) +
                            ControlElement("#/properties/last"),
                    ),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        fun gap(): Float {
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            assertEquals(2, fields.size)
            return fields[1].boundsInRoot.top - fields[0].boundsInRoot.bottom
        }
        val baseline = gap()
        runOnIdle { withEmpty = true }
        waitForIdle()
        assertTrue(kotlin.math.abs(gap() - baseline) < 1f,
            "Empty nested layout must not add another gap")
    }

    @Test fun materialEmptyStructuralChildDoesNotReserveColumn() = emptyStructuralChild(false)
    @Test fun cupertinoEmptyStructuralChildDoesNotReserveColumn() = emptyStructuralChild(true)

    private fun emptyStructuralChild(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(elements = listOf(
            ControlElement("#/properties/first"),
            VerticalLayout(),
            ControlElement("#/properties/last"),
        ))
        setContent {
            Box(Modifier.width(600.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""{"first":"A","last":"B"}"""),
                    schema = schema, uischema = ui,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(2, fields.size)
        assertTrue(fields[0].boundsInRoot.width > 250f, "Empty layout reserved a third column")
        assertTrue(fields[1].boundsInRoot.width > 250f, "Empty layout reserved a third column")
    }

    @Test fun materialFunctionRuleReceivesLayoutContext() = functionRuleContext(false)
    @Test fun cupertinoFunctionRuleReceivesLayoutContext() = functionRuleContext(true)

    private fun functionRuleContext(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"last":{"type":"string"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(elements = listOf(
            ControlElement("#/properties/first", rule = Rule(RuleEffect.SHOW,
                ValidateFunctionCondition(scope = "#", validate = { context ->
                    context.path == "" && (context.config as? Map<*, *>)?.get("showFirst") == true
                }))),
            ControlElement("#/properties/last"),
        ))
        var show by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"first":"A","last":"B"}"""),
                schema = schema, uischema = ui,
                config = mapOf("showFirst" to show),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { show = false }
        waitForIdle()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }

    @Test fun materialHiddenMiddleChildDoesNotReserveRowWidth() = hiddenMiddleChild(false)

    @Test fun cupertinoHiddenMiddleChildDoesNotReserveRowWidth() = hiddenMiddleChild(true)

    private fun hiddenMiddleChild(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"first":{"type":"string"},"middle":{"type":"string"},"last":{"type":"string"},"showMiddle":{"type":"boolean"}}}"""
        ).jsonObject)
        val ui = HorizontalLayout(elements = listOf(
            ControlElement(scope = "#/properties/first", label = StringControlLabel("First")),
            ControlElement(
                scope = "#/properties/middle",
                label = StringControlLabel("Middle"),
                rule = Rule(
                    effect = RuleEffect.SHOW,
                    condition = SchemaBasedCondition(
                        scope = "#/properties/showMiddle",
                        schema = buildJsonSchema { put("const", true) },
                    ),
                ),
            ),
            ControlElement(scope = "#/properties/last", label = StringControlLabel("Last")),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(
                    """{"first":"one","middle":"two","last":"three","showMiddle":false}"""
                ),
                schema = schema,
                uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(2, fields.size)
        val first = fields[0].boundsInRoot
        val last = fields[1].boundsInRoot
        assertTrue(last.left > first.left)
        assertTrue(
            last.left - first.left < last.width * 1.5f,
            "Hidden middle child must not keep a third row column",
        )
    }
}
