package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayTableGeometryUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"email":{"type":"string"}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """[{"name":"Ada","email":"a@example.com"},{"name":"Bob","email":"b@example.com"},{"name":"Cyd","email":"c@example.com"},{"name":"Dee","email":"d@example.com"}]"""
    )
    private val options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement(
            """[{"field":"name","width":170,"minWidth":120,"maxWidth":320},{"field":"email","width":220}]"""
        ),
        "pagination" to Json.parseToJsonElement("""{"pageSize":2}"""),
    )

    @Test fun materialResizePersistsThroughEditAndPaging() = resizeAndPersist(false)
    @Test fun cupertinoResizePersistsThroughEditAndPaging() = resizeAndPersist(true)
    @Test fun materialNarrowCompositeCellKeepsErrorOnSummaryLine() = narrowCompositeCell(false)
    @Test fun cupertinoNarrowCompositeCellKeepsErrorOnSummaryLine() = narrowCompositeCell(true)
    @Test fun materialScalarCellKeepsErrorBesideEditor() = scalarCellError(false)
    @Test fun cupertinoScalarCellKeepsErrorBesideEditor() = scalarCellError(true)

    private fun scalarCellError(cupertino: Boolean) = runComposeUiTest {
        val scalarSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""[{"name":"Ada"}]"""),
                    schema = scalarSchema,
                    uischema = ControlElement("#", options = mapOf(
                        "table" to JsonPrimitive(true),
                        "showValidationIndicator" to JsonPrimitive(false),
                        "columnDefs" to Json.parseToJsonElement("""[{"field":"name","width":170}]"""),
                    )),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    additionalErrors = listOf(ErrorObject(
                        instancePath = "/0/name", message = "server error")),
                )
            }
        }
        val editor = onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst()
            .fetchSemanticsNode().boundsInRoot
        val marker = onNodeWithContentDescription("Name contains errors", substring = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(marker.left >= editor.left && marker.left >= editor.right - 5f)
        assertTrue(marker.center.y in editor.top..editor.bottom)
    }

    private fun narrowCompositeCell(cupertino: Boolean) = runComposeUiTest {
        val nestedSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}}"""
        ).jsonObject)
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement("""[{"contact":{"city":"X"}}]"""),
                    schema = nestedSchema,
                    uischema = ControlElement("#", options = mapOf(
                        "table" to JsonPrimitive(true),
                        "columnDefs" to Json.parseToJsonElement("""[{"field":"contact","width":90}]"""),
                        "cells" to Json.parseToJsonElement("""{"contact":{"summary":{"type":"Label","text":"Preview"},"detail":{"type":"Control","scope":"#"}}}"""),
                    )),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    additionalErrors = listOf(ErrorObject(
                        instancePath = "/0/contact/city", message = "invalid city")),
                )
            }
        }
        val summary = onNodeWithText("Preview").fetchSemanticsNode().boundsInRoot
        val indicator = onNodeWithContentDescription("Contact contains errors", substring = true)
            .fetchSemanticsNode().boundsInRoot
        val edit = onNodeWithContentDescription("Edit Contact").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(summary.center.y - indicator.center.y) < 12f)
        assertTrue(kotlin.math.abs(edit.center.y - indicator.center.y) < 12f)
        assertTrue(indicator.left >= summary.left && edit.left > indicator.left)
        assertTrue(indicator.width > 0f && edit.width > 0f)
    }

    private fun resizeAndPersist(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            Box(Modifier.width(700.dp)) {
                JsonForms(
                    data = data, schema = schema,
                    uischema = ControlElement("#", options = options),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ },
                )
            }
        }
        fun nameWidth() = onNodeWithText("Name").fetchSemanticsNode().boundsInRoot.width
        val before = nameWidth()
        onNodeWithTag("table-column-resize-name")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(260f) }
        val resized = nameWidth()
        assertTrue(resized > before + 70f)
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().performTextReplacement("Ava")
        waitForIdle()
        assertTrue(kotlin.math.abs(nameWidth() - resized) < 2f)
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(kotlin.math.abs(nameWidth() - resized) < 2f)
        onNodeWithTag("table-column-resize-name")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("table-column-resize-name").performKeyInput {
            keyDown(Key.DirectionRight); keyUp(Key.DirectionRight)
        }
        assertTrue(nameWidth() > resized + 8f)
        onNodeWithTag("table-column-resize-name").performKeyInput {
            keyDown(Key.MoveHome); keyUp(Key.MoveHome)
        }
        assertTrue(nameWidth() < before - 35f)
        onNodeWithTag("table-column-resize-name").performKeyInput {
            keyDown(Key.MoveEnd); keyUp(Key.MoveEnd)
        }
        assertTrue(nameWidth() > resized + 40f)
        assertEquals(1, changes)
        assertEquals(JsonPrimitive("Ava"), (data as JsonArray)[0].jsonObject["name"])
    }

    @Test fun rightToLeftPointerResizeUsesLogicalColumnEdge() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.width(700.dp)) {
                    JsonForms(
                        data = initial, schema = schema,
                        uischema = ControlElement("#", options = options),
                        renderers = materialRenderers,
                        i18n = JsonFormsI18nState(translate = { key, fallback, _ ->
                            if (key == "array.columnResize") "Промяна на колона" else fallback
                        }),
                    )
                }
            }
        }
        fun nameWidth() = onNodeWithText("Name").fetchSemanticsNode().boundsInRoot.width
        val before = nameWidth()
        onNodeWithTag("table-column-resize-name").assertContentDescriptionEquals("Промяна на колона")
        onNodeWithTag("table-column-resize-name").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 50f, center.y))
            up()
        }
        assertTrue(nameWidth() < before - 20f)
    }

    @Test fun wideHeaderAndRowsShareHorizontalScroll() = runComposeUiTest {
        val wideSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"email":{"type":"string"},"role":{"type":"string"},"city":{"type":"string"}}}}"""
        ).jsonObject)
        setContent {
            Box(Modifier.width(400.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """[{"name":"Ada","email":"a@example.com","role":"Engineer","city":"Sofia"}]"""
                    ),
                    schema = wideSchema,
                    uischema = ControlElement("#", options = mapOf(
                        "table" to JsonPrimitive(true),
                        "columnDefs" to Json.parseToJsonElement(
                            """[{"field":"name","width":250},{"field":"email","width":250},{"field":"role","width":250},{"field":"city","width":250}]"""
                        ),
                    )),
                    renderers = materialRenderers,
                )
            }
        }
        val headerBefore = onNodeWithText("City").fetchSemanticsNode().boundsInRoot.left
        val rowBefore = onAllNodes(hasSetTextAction() and hasText("Sofia")).onFirst()
            .fetchSemanticsNode().boundsInRoot.left
        onNodeWithTag("table-header-scroll")
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(600f, 0f) }
        waitForIdle()
        val headerAfter = onNodeWithText("City").fetchSemanticsNode().boundsInRoot.left
        val rowAfter = onAllNodes(hasSetTextAction() and hasText("Sofia")).onFirst()
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(headerAfter > headerBefore + 100f)
        assertTrue(kotlin.math.abs((headerAfter - headerBefore) - (rowAfter - rowBefore)) < 2f)
        onNodeWithTag("table-header-scroll")
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(1000f, 0f) }
        waitForIdle()
        val action = onNodeWithContentDescription("Delete item").fetchSemanticsNode().boundsInRoot
        assertTrue(action.left >= 0f && action.right <= 400f)
    }
}
