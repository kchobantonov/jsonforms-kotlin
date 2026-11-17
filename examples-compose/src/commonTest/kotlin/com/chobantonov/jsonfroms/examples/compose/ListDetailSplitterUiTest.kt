package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ListDetailSplitterUiTest {
    @Test fun materialResizingPersistsThroughEditAndResponsiveLayout() = scenario(false)
    @Test fun cupertinoResizingPersistsThroughEditAndResponsiveLayout() = scenario(true)
    @Test fun materialShortListKeepsNaturalHeightAndLongListScrolls() = listHeight(false)
    @Test fun cupertinoShortListKeepsNaturalHeightAndLongListScrolls() = listHeight(true)
    @Test fun materialListScrollKeepsSelectedDetail() = scrollKeepsDetail(false)
    @Test fun cupertinoListScrollKeepsSelectedDetail() = scrollKeepsDetail(true)
    @Test fun materialPageDownKeepsSelectedDetail() = scrollKeepsDetail(false, ScrollInput.Keyboard)
    @Test fun cupertinoPageDownKeepsSelectedDetail() = scrollKeepsDetail(true, ScrollInput.Keyboard)

    internal fun wheelKeepsDetail(cupertino: Boolean) = scrollKeepsDetail(cupertino, ScrollInput.MouseWheel)

    private enum class ScrollInput { Touch, MouseWheel, Keyboard }

    private fun scrollKeepsDetail(
        cupertino: Boolean,
        input: ScrollInput = ScrollInput.Touch,
    ) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        val data = Json.parseToJsonElement((1..30).joinToString(prefix = "[", postfix = "]") {
            """{"name":"Item $it"}"""
        })
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("Item 1").performClick()
        val editor = onAllNodes(hasSetTextAction() and hasText("Item 1")).onFirst()
        val editorBounds = editor.fetchSemanticsNode().boundsInRoot
        val scroll = onNodeWithTag("list-detail-navigation-scroll")
        fun offset() = scroll.fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        val before = offset()
        when (input) {
            ScrollInput.Touch -> scroll.performTouchInput { swipeUp() }
            ScrollInput.MouseWheel -> scroll.performMouseInput { scroll(240f) }
            ScrollInput.Keyboard -> {
                scroll.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
                scroll.performKeyInput {
                    keyDown(Key.PageDown)
                    keyUp(Key.PageDown)
                }
            }
        }
        waitForIdle()
        assertTrue(offset() > before, "navigation list should scroll")
        assertEquals(editorBounds, editor.fetchSemanticsNode().boundsInRoot)
        assertEquals(0, changes)
    }

    private fun listHeight(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""[{"name":"One"}]"""))
        var pixelsPerDp = 1f
        setContent {
            val density = LocalDensity.current.density
            SideEffect { pixelsPerDp = density }
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        fun listPaneHeight() = onNodeWithTag("list-detail-list-pane")
            .fetchSemanticsNode().boundsInRoot.height
        val shortHeight = listPaneHeight()
        assertTrue(shortHeight < 256f * pixelsPerDp, "one item should retain its natural height")
        runOnIdle {
            data = Json.parseToJsonElement((1..30).joinToString(prefix = "[", postfix = "]") {
                """{"name":"Item $it"}"""
            })
        }
        val longHeight = listPaneHeight()
        assertTrue(longHeight > shortHeight + 100f * pixelsPerDp)
        assertTrue(longHeight <= 384f * pixelsPerDp + 2f, "overflow list should remain capped")
        onNodeWithText("Item 30").assertExists()
    }

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bob"}]}"""))
        var paneWidth by mutableStateOf(900.dp)
        var changes = 0
        setContent {
            Box(Modifier.width(paneWidth)) {
                JsonForms(
                    data = data, schema = schema,
                    uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                        "elementLabelProp" to JsonPrimitive("name"),
                        "detail" to Json.parseToJsonElement(
                            """{"type":"Control","scope":"#/properties/name"}"""
                        ),
                    )),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ },
                )
            }
        }
        fun listWidth() = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        val initial = listWidth()
        assertTrue(initial in 280f..400f)
        assertTrue(onNodeWithTag("list-detail-splitter").fetchSemanticsNode().boundsInRoot.height >=
            onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.height)
        onNodeWithTag("list-detail-splitter").performSemanticsAction(SemanticsActions.SetProgress) {
            it(0.55f)
        }
        val resized = listWidth()
        assertTrue(resized > initial + 100f)
        onNodeWithText("Bob").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Bea")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"name":"Ada"},{"name":"Bea"}]}"""), data)
        assertTrue(kotlin.math.abs(listWidth() - resized) < 2f)
        runOnIdle { paneWidth = 500.dp }
        onNodeWithTag("list-detail-splitter").assertDoesNotExist()
        runOnIdle { paneWidth = 900.dp }
        assertTrue(kotlin.math.abs(listWidth() - resized) < 2f)
        assertEquals(1, changes)
    }

    @Test fun splitterRemainsResizableWhenFormIsReadOnly() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        var changes = 0
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"name":"Ada"}]"""), schema = schema,
                uischema = ListWithDetailElement("#"), readonly = true,
                renderers = materialRenderers, onChange = { changes++ },
            )
        }
        fun listWidth() = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        val before = listWidth()
        onNodeWithTag("list-detail-splitter").performSemanticsAction(SemanticsActions.SetProgress) {
            it(0.55f)
        }
        assertTrue(listWidth() > before + 100f)
        assertEquals(0, changes)
    }

    @Test fun pointerAndKeyboardResizeTheDivider() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"name":"Ada"}]"""), schema = schema,
                uischema = ListWithDetailElement("#"), readonly = true,
                renderers = materialRenderers,
            )
        }
        fun listWidth() = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        val before = listWidth()
        onNodeWithTag("list-detail-splitter").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 100f, center.y))
            up()
        }
        val dragged = listWidth()
        assertTrue(dragged > before + 50f)
        onNodeWithTag("list-detail-splitter").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("list-detail-splitter").performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        assertTrue(listWidth() > dragged + 8f)
        onNodeWithTag("list-detail-splitter").performKeyInput {
            keyDown(Key.MoveHome)
            keyUp(Key.MoveHome)
        }
        assertTrue(listWidth() in 279f..282f)
        onNodeWithTag("list-detail-splitter").performKeyInput {
            keyDown(Key.MoveEnd)
            keyUp(Key.MoveEnd)
        }
        assertTrue(listWidth() > 650f)
    }

    @Test fun rightToLeftDragUsesTheLogicalListSideAndLocalizedName() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                JsonForms(
                    data = Json.parseToJsonElement("""[{"name":"Ada"}]"""), schema = schema,
                    uischema = ListWithDetailElement("#"), renderers = materialRenderers,
                    i18n = JsonFormsI18nState(translate = { key, fallback, _ ->
                        if (key.endsWith("resizeAriaLabel")) "Промяна на панелите" else fallback
                    }),
                )
            }
        }
        fun listWidth() = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        val before = listWidth()
        onNodeWithContentDescription("Промяна на панелите").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 50f, center.y))
            up()
        }
        assertTrue(listWidth() < before - 20f)
    }
}
