package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ListDetailPaginationUiTest {
    @Test fun materialPagedDeleteKeepsShiftedDetailTarget() = pagedDeleteKeepsDetail(false)
    @Test fun cupertinoPagedDeleteKeepsShiftedDetailTarget() = pagedDeleteKeepsDetail(true)

    private fun pagedDeleteKeepsDetail(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Dee").performClick()
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Dee"},{"name":"Eve"}]}"""
        ), data)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Daria")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Daria"},{"name":"Eve"}]}"""
        ), data)
        assertEquals(2, writes)
    }

    @Test fun materialCrossPageMoveKeepsSelectedDetailTarget() = crossPageMoveKeepsDetail(false)
    @Test fun cupertinoCrossPageMoveKeepsSelectedDetailTarget() = crossPageMoveKeepsDetail(true)

    private fun crossPageMoveKeepsDetail(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options +
                    ("showSortButtons" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Cyd").performClick()
        onAllNodes(hasContentDescription("Move up")).onFirst().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Cyd"},{"name":"Bob"},{"name":"Dee"},{"name":"Eve"}]}"""
        ), data)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Clara")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Clara"},{"name":"Bob"},{"name":"Dee"},{"name":"Eve"}]}"""
        ), data)
        assertEquals(2, writes)
    }

    @Test fun materialHostReplacementClearsPagedSelection() = hostReplacementClearsPagedSelection(false)
    @Test fun cupertinoHostReplacementClearsPagedSelection() = hostReplacementClearsPagedSelection(true)

    private fun hostReplacementClearsPagedSelection(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Dee").performClick()
        onNodeWithText("No selection").assertDoesNotExist()
        runOnIdle {
            data = Json.parseToJsonElement(
                """{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Cyd"},{"name":"Dana"},{"name":"Eve"}]}"""
            )
        }
        onNodeWithText("No selection").assertExists()
        onNodeWithText("Dana").assertExists()
        assertEquals(0, writes)
    }

    @Test fun cupertinoListPagerNavigatesWithKeyboard() = runComposeUiTest {
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                renderers = cupertinoRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Next page")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Next page").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        onNodeWithText("Cyd").assertExists()
        onNodeWithContentDescription("Previous page")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Previous page").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        onNodeWithText("Ada").assertExists()
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"}}}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement(
        """{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Cyd"},{"name":"Dee"},{"name":"Eve"}]}"""
    )
    private val options = mapOf(
        "elementLabelProp" to JsonPrimitive("name"),
        "pagination" to Json.parseToJsonElement(
            """{"pageSize":2,"pageSizeOptions":[2,3]}"""
        ),
        "detail" to Json.parseToJsonElement(
            """{"type":"Control","scope":"#/properties/name"}"""
        ),
    )

    @Test fun materialPagesKeepAbsoluteDetailPath() = pageScenario(false)
    @Test fun cupertinoPagesKeepAbsoluteDetailPath() = pageScenario(true)
    @Test fun materialPagerUsesBuiltInBulgarianLocaleWithoutWriting() = builtInPager(false)
    @Test fun cupertinoPagerUsesBuiltInBulgarianLocaleWithoutWriting() = builtInPager(true)

    private fun builtInPager(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                i18n = JsonFormsI18nState(locale = locale),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/4/name", message = "server")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Next page").assertExists()
        onNodeWithText("Page 3: 1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Размер на страница").assertExists()
        onNodeWithText("Страница 3: 1 грешка").assertExists()
        onNodeWithContentDescription("Следваща страница").performClick()
        onNodeWithText("Cyd").assertExists()
        onNodeWithText("2 от 3").assertExists()
        runOnIdle { assertEquals(0, changes) }
    }

    private fun pageScenario(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Ada").assertExists()
        onNodeWithText("Bob").assertExists()
        onNodeWithText("Cyd").assertDoesNotExist()
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("Cyd").assertExists()
        onNodeWithText("Dee").performClick()
        onNodeWithContentDescription("Previous page").performClick()
        fun rowVisible(label: String): Boolean {
            val listRight = onNodeWithTag("list-detail-list-pane").fetchSemanticsNode().boundsInRoot.right
            return onAllNodes(hasClickAction() and hasText(label)).fetchSemanticsNodes()
                .any { it.boundsInRoot.left < listRight }
        }
        assertTrue(!rowVisible("Dee"))
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Daria")
        waitForIdle()
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"name":"Ada"},{"name":"Bob"},{"name":"Cyd"},{"name":"Daria"},{"name":"Eve"}]}"""
        ), data)
        onNodeWithContentDescription("Next page").performClick()
        assertTrue(rowVisible("Daria"))
        onNodeWithContentDescription("Page size").performClick()
        onAllNodes(hasClickAction() and hasText("3")).onFirst().performClick()
        onNodeWithText("Cyd").assertExists()
        assertTrue(!rowVisible("Daria"))
        assertEquals(1, changes)
    }

    @Test fun defaultIsOffAndLocalFalseOverridesScopedConfig() = runComposeUiTest {
        var enabled by mutableStateOf(false)
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                ) + if (enabled) emptyMap() else mapOf("pagination" to JsonPrimitive(false))),
                config = mapOf("jsonformsExtended" to mapOf("array" to mapOf(
                    "pagination" to mapOf("pageSize" to 2),
                ))),
                renderers = materialRenderers,
            )
        }
        onNodeWithText("Eve").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { enabled = true }
        onNodeWithText("Eve").assertDoesNotExist()
        onNodeWithContentDescription("Next page").assertExists()
    }

    @Test fun absentPaginationStaysOffAndLocaleUpdatesPagerLabels() = runComposeUiTest {
        var locale by mutableStateOf("en")
        var enabled by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = mapOf(
                    "elementLabelProp" to JsonPrimitive("name"),
                ) + if (enabled) mapOf("pagination" to Json.parseToJsonElement(
                    """{"pageSize":2}"""
                )) else emptyMap()),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (locale == "bg" && key == "collection.next") "Следваща страница" else fallback
                }),
                renderers = materialRenderers, onChange = { changes++ },
            )
        }
        onNodeWithText("Eve").assertExists()
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { enabled = true }
        onNodeWithContentDescription("Next page").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Следваща страница").assertExists()
        assertEquals(0, changes)
    }

    @Test fun errorsOnOtherPagesHaveNavigation() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/4/name", message = "host")),
                renderers = materialRenderers,
            )
        }
        onNodeWithText("Page 3: 1 error").performClick()
        onNodeWithText("Eve").assertExists()
        onNodeWithText("Ada").assertDoesNotExist()
    }

    @Test fun onePageKeepsSizeSelectorWhenSmallerSizeCanPage() = runComposeUiTest {
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options +
                    ("pagination" to Json.parseToJsonElement(
                        """{"pageSize":5,"pageSizeOptions":[2,5]}"""
                    ))),
                renderers = materialRenderers,
            )
        }
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        onNodeWithContentDescription("Page size").performClick()
        onAllNodes(hasClickAction() and hasText("2")).onFirst().performClick()
        onNodeWithContentDescription("Next page").assertExists()
        onNodeWithText("Eve").assertDoesNotExist()
    }

    @Test fun deletionClampsPageAndAddRevealsNewRow() = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ListWithDetailElement("#/properties/rows", options = options),
                renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithContentDescription("Next page").performClick()
        onNodeWithText("3 of 3").assertExists()
        onNodeWithText("Eve").assertExists()
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        onNodeWithText("2 of 2").assertExists()
        onNodeWithText("Dee").assertExists()
        assertEquals(1, changes)
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        onNodeWithText("3 of 3").assertExists()
        assertEquals(2, changes)
        assertEquals(5, (data.jsonObject["rows"] as kotlinx.serialization.json.JsonArray).size)
    }
}
