package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class LayoutStretchUiTest {
    @Test fun materialStretchesShortChildToTallRow() = mounted(false, false)

    @Test fun cupertinoStretchesShortChildToTallRow() = mounted(true, false)

    @Test fun materialStretchesEachWrappedRowIndependently() = mounted(false, true)

    @Test fun cupertinoStretchesEachWrappedRowIndependently() = mounted(true, true)

    @Test fun materialKeepsFixedCrossHeightDuringStretch() = mounted(false, false, fixedShort = true)

    @Test fun cupertinoKeepsFixedCrossHeightDuringStretch() = mounted(true, false, fixedShort = true)

    @Test fun materialCapsStretchedChildHeight() = mounted(false, false, maxShort = 60)

    @Test fun cupertinoCapsStretchedChildHeight() = mounted(true, false, maxShort = 60)

    private fun mounted(
        cupertino: Boolean,
        wrap: Boolean,
        fixedShort: Boolean = false,
        maxShort: Int? = null,
    ) = runComposeUiTest {
        val custom = JsonFormsRendererRegistryEntry(
            tester = { ui, _, _ -> if (ui is LabelElement) 1000 else -1 },
            renderer = ComposeJsonFormsRenderer { props ->
                val label = props.uischema as LabelElement
                Box(Modifier.fillMaxWidth().fillMaxHeight().testTag(label.text)) {
                    Spacer(Modifier.height(if (label.text == "tall") 100.dp else 30.dp))
                }
            },
        )
        fun label(name: String) = LabelElement(name, options = mapOf("layout" to buildJsonObject {
            put("width", if (wrap) 130 else 100)
            if (fixedShort && name == "short") put("height", 40)
            if (maxShort != null && name == "short") put("maxHeight", maxShort)
        }))
        val ui = HorizontalLayout(options = mapOf(
            "align" to JsonPrimitive("stretch"),
            "wrap" to JsonPrimitive(wrap),
            "gap" to JsonPrimitive(10),
        ), elements = if (wrap) listOf(label("short"), label("tall"), label("last"))
            else listOf(label("short"), label("tall")))
        setContent {
            Box(Modifier.width(300.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("{}"),
                    schema = JsonSchema(),
                    uischema = ui,
                    renderers = listOf(custom) + if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
        }
        val short = onNodeWithTag("short").fetchSemanticsNode().boundsInRoot
        val tall = onNodeWithTag("tall").fetchSemanticsNode().boundsInRoot
        val expectedShort = if (fixedShort) 40f else maxShort?.toFloat() ?: 100f
        assertTrue(abs(short.height - expectedShort) < 3f, "Short height=${short.height}")
        assertTrue(abs(tall.height - 100f) < 3f, "Tall height=${tall.height}")
        if (wrap) {
            val last = onNodeWithTag("last").fetchSemanticsNode().boundsInRoot
            assertTrue(last.top > tall.bottom)
            assertTrue(abs(last.height - 30f) < 3f, "Last height=${last.height}")
        }
    }
}
