package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
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
class ArrayRowDetailPanelUiTest {
    @Test fun materialPanelLabelsFollowBulgarianLocale() = panelLocale(false)
    @Test fun cupertinoPanelLabelsFollowBulgarianLocale() = panelLocale(true)

    private fun panelLocale(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ui("""{"presentation":"panel","detail":{"type":"Control","scope":"#/properties/note"}}"""),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale), onChange = { writes++ })
        }
        onNodeWithText("No selection").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Няма избран елемент").assertExists()
        onNodeWithText("Скрий подробностите").performClick()
        onNodeWithText("Покажи подробностите").assertExists()
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"note":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"name":"Ada","note":"one"},{"name":"Bob","note":"two"},{"name":"Cyd","note":"three"}]}"""
    )
    private fun ui(rowDetail: String) = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "columnDefs" to Json.parseToJsonElement("""[{"field":"name"}]"""),
        "pagination" to Json.parseToJsonElement("""{"pageSize":1}"""),
        "rowDetail" to Json.parseToJsonElement(rowDetail),
    ))

    @Test fun materialRightPanelRetainsSelectionAndEditsImmediately() = rightPanel(false)
    @Test fun cupertinoRightPanelRetainsSelectionAndEditsImmediately() = rightPanel(true)
    @Test fun materialCurrentRowStateFollowsVisibleDetail() = currentRowState(false)
    @Test fun cupertinoCurrentRowStateFollowsVisibleDetail() = currentRowState(true)

    private fun currentRowState(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ui("""{"presentation":"panel","detail":{"type":"Control","scope":"#/properties/note"}}"""),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithTag("table-row-scroll-0").assertIsNotSelected()
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag("table-row-scroll-0").assertIsSelected()
        onNodeWithText("Hide details").performClick()
        onNodeWithTag("table-row-scroll-0").assertIsNotSelected()
        onNodeWithText("Show details").performClick()
        onNodeWithTag("table-row-scroll-0").assertIsSelected()
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag("table-row-scroll-1").assertIsNotSelected()
        onNodeWithTag("table-row-scroll-1").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag("table-row-scroll-1").assertIsSelected()
        assertEquals(0, changes)
    }

    private fun rightPanel(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            Box(Modifier.width(900.dp)) {
                JsonForms(
                    data = data, schema = schema,
                    uischema = ui("""{"presentation":"panel","detail":{"type":"Control","scope":"#/properties/note"}}"""),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ },
                )
            }
        }
        onNodeWithText("No selection").assertExists()
        val listWidthBefore = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        onNodeWithTag("list-detail-splitter")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.55f) }
        val listWidthAfter = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.width
        assertTrue(listWidthAfter > listWidthBefore + 100f)
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("No selection").assertDoesNotExist()
        onNodeWithTag("table-row-scroll-0").assertIsSelected()
        onAllNodes(hasSetTextAction() and hasText("one")).onFirst().performTextReplacement("updated")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada","note":"updated"},{"name":"Bob","note":"two"},{"name":"Cyd","note":"three"}]}"""
        ), data)
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("updated").assertExists()
        onNodeWithText("Hide details").performClick()
        onNodeWithText("updated").assertDoesNotExist()
        onNodeWithText("Show details").performClick()
        onNodeWithText("updated").assertExists()
        assertTrue(kotlin.math.abs(onNodeWithTag("list-detail-list-pane")
            .fetchSemanticsNode().boundsInRoot.width - listWidthAfter) < 2f)
        assertEquals(1, changes)
    }

    @Test fun collapsedPanelSelectsWithoutOpeningUntilRequested() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ui("""{"presentation":"panel","collapsed":true,"detail":{"type":"Control","scope":"#/properties/note"}}"""),
                renderers = materialRenderers,
            )
        }
        onNodeWithText("No selection").assertDoesNotExist()
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().performClick()
        onNodeWithText("one").assertDoesNotExist()
        onNodeWithText("Show details").performClick()
        onNodeWithText("one").assertExists()
        onNodeWithText("Hide details").performClick()
        onNodeWithText("one").assertDoesNotExist()
        onNodeWithText("Edit details").performClick()
        onNodeWithText("one").assertExists()
    }

    @Test fun bottomPanelResizesAndKeepsSelectedDetails() = runComposeUiTest {
        setContent {
            Box(Modifier.width(700.dp)) {
                JsonForms(
                    data = initial, schema = schema,
                    uischema = ui("""{"presentation":"panel","placement":"bottom","detail":{"type":"Control","scope":"#/properties/note"}}"""),
                    renderers = materialRenderers,
                )
            }
        }
        val splitter = onNodeWithTag("table-detail-bottom-splitter")
        val before = splitter.fetchSemanticsNode().boundsInRoot.top
        splitter.performSemanticsAction(SemanticsActions.SetProgress) { it(340f) }
        val after = onNodeWithTag("table-detail-bottom-splitter").fetchSemanticsNode().boundsInRoot.top
        assertTrue(after > before + 40f)
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("one").assertExists()
        onNodeWithText("Hide details").performClick()
        onNodeWithText("Show details").performClick()
        val reopened = onNodeWithTag("table-detail-bottom-splitter").fetchSemanticsNode().boundsInRoot.top
        assertTrue(kotlin.math.abs(reopened - after) < 2f)
    }

    @Test fun nonResizablePanelsKeepTheirLayoutWithoutResizeActions() = runComposeUiTest {
        setContent {
            Box(Modifier.width(900.dp)) {
                JsonForms(
                    data = initial, schema = schema,
                    uischema = ui("""{"presentation":"panel","resizable":false}"""),
                    renderers = materialRenderers,
                )
            }
        }
        val divider = onNodeWithTag("list-detail-splitter").fetchSemanticsNode()
        assertTrue(SemanticsActions.SetProgress !in divider.config)
        onNodeWithText("No selection").assertExists()
    }

    @Test fun deletingSelectedRowClearsThePanelSelection() = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = ui(
                    """{"presentation":"panel","detail":{"type":"Control","scope":"#/properties/note"}}"""
                ).options + ("confirmation" to Json.parseToJsonElement("""{"delete":"never"}"""))),
                renderers = materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("one").assertExists()
        onNodeWithContentDescription("Delete item").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("No selection").assertExists()
        assertEquals(2, data.jsonObject["rows"]?.let { (it as kotlinx.serialization.json.JsonArray).size })
    }

    @Test fun localMoveKeepsTheSelectedRowAndExternalReplacementClearsIt() = runComposeUiTest {
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = ui(
                    """{"presentation":"panel","detail":{"type":"Control","scope":"#/properties/note"}}"""
                ).options + ("showSortButtons" to JsonPrimitive(true))),
                renderers = materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("one").assertExists()
        onNodeWithContentDescription("Move down").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("one").assertExists()
        onNodeWithContentDescription("Next page").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithTag("table-row-scroll-1").assertIsSelected()
        runOnIdle {
            data = Json.parseToJsonElement(
                """{"rows":[{"name":"Bob","note":"two"},{"name":"Ada","note":"one"},{"name":"Cyd","note":"three"}],"sibling":"changed"}"""
            )
        }
        waitForIdle()
        onNodeWithText("No selection").assertExists()
    }
}
