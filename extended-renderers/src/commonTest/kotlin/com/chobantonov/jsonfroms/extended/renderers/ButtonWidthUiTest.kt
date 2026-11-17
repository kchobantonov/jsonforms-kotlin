package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class ButtonWidthUiTest {
    @Test fun materialButtonFillsOnlyWhenFullWidthIsTrue() = checkWidth(false)
    @Test fun cupertinoButtonFillsOnlyWhenFullWidthIsTrue() = checkWidth(true)
    @Test fun materialFullWidthKeepsGridCellAllocation() = checkAllocatedCell(false, weighted = false)
    @Test fun cupertinoFullWidthKeepsGridCellAllocation() = checkAllocatedCell(true, weighted = false)
    @Test fun materialFullWidthKeepsFlexCellAllocation() = checkAllocatedCell(false, weighted = true)
    @Test fun cupertinoFullWidthKeepsFlexCellAllocation() = checkAllocatedCell(true, weighted = true)

    private fun checkAllocatedCell(cupertino: Boolean, weighted: Boolean) = runComposeUiTest {
        var fullWidth by mutableStateOf(false)
        val neighbor = UnknownUiSchemaElement(
            type = "Button",
            properties = buildJsonObject { put("label", "Neighbor"); put("action", "neighbor") },
        )
        setContent {
            Box(Modifier.width(400.dp).testTag("grid-container")) {
                JsonForms(
                    data = buildJsonObject {},
                    schema = JsonSchema(buildJsonObject { put("type", "object") }),
                    uischema = HorizontalLayout(
                        options = mapOf("gridColumns" to JsonPrimitive(4), "gap" to JsonPrimitive(8)),
                        elements = listOf(
                            UnknownUiSchemaElement(
                                type = "Button",
                                options = mapOf(
                                    "layout" to buildJsonObject {
                                        if (weighted) put("weight", 1) else put("span", 1)
                                    },
                                    "fullWidth" to JsonPrimitive(fullWidth),
                                ),
                                properties = buildJsonObject { put("label", "Go"); put("action", "go") },
                            ),
                            neighbor.copy(options = mapOf("layout" to buildJsonObject {
                                if (weighted) put("weight", 3) else put("span", 3)
                            })),
                        ),
                    ),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { _, _ -> }),
                )
            }
        }

        fun bounds() = Pair(
            onNodeWithTag("layout-child--0").fetchSemanticsNode().boundsInRoot,
            onNodeWithTag("layout-child--1").fetchSemanticsNode().boundsInRoot,
        )
        val (firstCell, secondCell) = bounds()
        val before = onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, before.size)
        assertTrue(before[0].width < firstCell.width)
        assertTrue(before[1].width < secondCell.width)

        runOnIdle { fullWidth = true }
        val (filledCell, unchangedCell) = bounds()
        val after = onAllNodes(hasClickAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(firstCell, filledCell)
        assertEquals(secondCell, unchangedCell)
        assertEquals(filledCell.width, after[0].width, 1f)
        assertEquals(before[1], after[1])
    }

    private fun checkWidth(cupertino: Boolean) = runComposeUiTest {
        var options by mutableStateOf(emptyMap<String, JsonPrimitive>())
        var secondary by mutableStateOf(false)
        setContent {
            Box(Modifier.width(320.dp).testTag("button-cell")) {
                JsonForms(
                    data = buildJsonObject {},
                    schema = JsonSchema(buildJsonObject { put("type", "object") }),
                    uischema = UnknownUiSchemaElement(
                        type = "Button",
                        options = options,
                        properties = buildJsonObject {
                            put("label", "Run")
                            put("action", "run")
                            if (secondary) put("color", "secondary")
                        },
                    ),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    config = mapOf(ExtendedActionHandlerConfigKey to ExtendedActionHandler { _, _ -> }),
                )
            }
        }

        fun widths(): Pair<Float, Float> =
            onNodeWithTag("button-cell").fetchSemanticsNode().boundsInRoot.width to
                onNode(hasClickAction()).fetchSemanticsNode().boundsInRoot.width

        val (cellWidth, intrinsicWidth) = widths()
        assertTrue(intrinsicWidth < cellWidth * 0.8f)

        runOnIdle { options = mapOf("block" to JsonPrimitive(true)) }
        assertEquals(intrinsicWidth, widths().second, 1f)

        runOnIdle { options = mapOf("fullWidth" to JsonPrimitive("true")) }
        assertEquals(intrinsicWidth, widths().second, 1f)

        runOnIdle { options = mapOf("fullWidth" to JsonPrimitive(true)) }
        assertEquals(cellWidth, widths().second, 1f)

        runOnIdle { secondary = true }
        assertEquals(cellWidth, widths().second, 1f)

        runOnIdle { options = mapOf("fullWidth" to JsonPrimitive(false)) }
        assertTrue(widths().second < cellWidth * 0.8f)
        assertEquals(cellWidth, widths().first, 1f)
    }
}
