package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayRowDetailGeometryUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement("""{"rows":[{"name":"Ada"}]}""")
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "rowDetail" to Json.parseToJsonElement(
            """{"presentation":"dialog","dialog":{"width":620,"height":"500px","draggable":true,"resizable":true}}"""
        ),
    ))

    @Test fun materialDialogResizesMaximizesAndRestoresWithoutEditing() = geometry(false)
    @Test fun cupertinoDialogResizesMaximizesAndRestoresWithoutEditing() = geometry(true)
    @Test fun materialGeometryChangesKeepThePrivateDraft() = draft(false)
    @Test fun cupertinoGeometryChangesKeepThePrivateDraft() = draft(true)

    private fun geometry(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = initial, schema = schema, uischema = ui,
                config = mapOf("jsonformsExtended" to mapOf("dialog" to mapOf("width" to 400))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Edit details").performClick()
        val frame = onNodeWithTag("row-detail-dialog-frame")
        frame.assertIsFocused()
        val normal = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(normal.width > 600f && normal.width < 650f)
        assertTrue(normal.height > 480f && normal.height < 520f)
        onNodeWithTag("row-detail-dialog-title").performTouchInput {
            down(center)
            moveTo(center + Offset(48f, 32f))
            up()
        }
        val moved = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(moved.left > normal.left + 10f)
        assertTrue(moved.top > normal.top + 10f)
        onNodeWithTag("row-detail-dialog-resize")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(700f) }
        val resized = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(resized.width > normal.width + 60f)
        onNodeWithText("Maximize").performClick()
        val maximum = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(maximum.width > resized.width)
        assertTrue(maximum.height > resized.height)
        onNodeWithTag("row-detail-dialog-resize").assertDoesNotExist()
        onNodeWithText("Restore").performClick()
        val restored = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(restored.width - resized.width) < 2f)
        onNodeWithContentDescription("Close").performClick()
        frame.assertDoesNotExist()
        onNodeWithText("Edit details").assertIsFocused()
        onNodeWithText("Edit details").performClick()
        val reopened = frame.fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(reopened.width - normal.width) < 2f)
        assertTrue(kotlin.math.abs(reopened.height - normal.height) < 2f)
        assertTrue(kotlin.math.abs(reopened.left - normal.left) < 2f)
        assertTrue(kotlin.math.abs(reopened.top - normal.top) < 2f)
        onNodeWithText("Cancel").performClick()
        assertEquals(0, changes)
    }

    private fun draft(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Edit details").performClick()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onLast().performTextReplacement("Grace")
        onNodeWithTag("row-detail-dialog-resize")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(700f) }
        onNodeWithText("Maximize").performClick()
        onNodeWithText("Restore").performClick()
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Grace"}]}"""), data)
        assertEquals(1, changes)
        onNodeWithText("Edit details").assertIsFocused()
    }

    @Test fun localFalseHidesMaximizeAndResizeDespiteFormDefaults() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "rowDetail" to Json.parseToJsonElement(
                        """{"presentation":"dialog","dialog":{"maximizable":false,"resizable":false}}"""
                    ),
                )),
                config = mapOf("jsonformsExtended" to mapOf("dialog" to mapOf(
                    "maximizable" to true, "resizable" to true))),
                renderers = materialRenderers,
            )
        }
        onNodeWithText("Edit details").performClick()
        onNodeWithTag("row-detail-dialog-maximize").assertDoesNotExist()
        onNodeWithTag("row-detail-dialog-resize").assertDoesNotExist()
    }
}
