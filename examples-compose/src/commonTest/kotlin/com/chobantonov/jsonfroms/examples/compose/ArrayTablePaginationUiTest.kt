package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ArrayTablePaginationUiTest {
    @Test fun materialFocusedRowSurvivesHostUpdate() = focusedRowSurvivesHostUpdate(false)
    @Test fun cupertinoFocusedRowSurvivesHostUpdate() = focusedRowSurvivesHostUpdate(true)

    private fun focusedRowSurvivesHostUpdate(cupertino: Boolean) = runComposeUiTest {
        val rowSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["Ada","Grace"]""") as JsonArray
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = rowSchema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 1, false) }
        runOnIdle { data = JsonArray(listOf(JsonPrimitive("Adele"), original[1])) }
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        assertEquals(1, onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].start)
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("X")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["AXdele","Grace"]"""), data)
        assertEquals(1, changes)
    }

    @Test fun materialFocusedRowFollowsTableMove() = focusedRowFollowsTableMove(false)
    @Test fun cupertinoFocusedRowFollowsTableMove() = focusedRowFollowsTableMove(true)
    @Test fun materialDuplicateRowOccurrenceFollowsTableMove() = focusedRowFollowsTableMove(false, duplicate = true)
    @Test fun cupertinoDuplicateRowOccurrenceFollowsTableMove() = focusedRowFollowsTableMove(true, duplicate = true)

    private fun focusedRowFollowsTableMove(cupertino: Boolean, duplicate: Boolean = false) = runComposeUiTest {
        val rowSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string"}}"""
        ).jsonObject)
        val initial = if (duplicate) Json.parseToJsonElement("""["Ada","Ada"]""")
            else Json.parseToJsonElement("""["Ada","Grace"]""")
        var data by mutableStateOf<JsonElement>(initial)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = rowSchema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true), "showSortButtons" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 1, false) }
        onAllNodes(hasContentDescription("Move down")).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        assertEquals(if (duplicate) initial else Json.parseToJsonElement("""["Grace","Ada"]"""), data)
        if (duplicate) assertEquals(listOf(false, true), onAllNodes(hasSetTextAction())
            .fetchSemanticsNodes().map { it.config[SemanticsProperties.Focused] })
        onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        assertEquals(1, onAllNodes(hasSetTextAction()).onLast()
            .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].start)
        onAllNodes(hasSetTextAction()).onLast().performTextInput("X")
        waitForIdle()
        assertEquals(if (duplicate) Json.parseToJsonElement("""["Ada","AXda"]""")
            else Json.parseToJsonElement("""["Grace","AXda"]"""), data)
        assertEquals(2, changes)
    }

    @Test fun materialFocusedRowFollowsHostReorder() = focusedRowFollowsHostReorder(false)
    @Test fun cupertinoFocusedRowFollowsHostReorder() = focusedRowFollowsHostReorder(true)

    private fun focusedRowFollowsHostReorder(cupertino: Boolean) = runComposeUiTest {
        val rowSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["Ada","Grace"]""") as JsonArray
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = rowSchema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performClick()
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.SetSelection) { it(1, 1, false) }
        runOnIdle { data = JsonArray(listOf(original[1], original[0])) }
        onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
        assertEquals(1, onAllNodes(hasSetTextAction()).onLast()
            .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange].start)
        onAllNodes(hasSetTextAction()).onLast().performTextInput("X")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["Grace","AXda"]"""), data)
        assertEquals(1, changes)
    }

    @Test fun materialOffPageSchemaErrorAppearsInPager() = offPageSchemaError(false)
    @Test fun cupertinoHostOffPageSchemaErrorAppearsInPager() = offPageSchemaError(true)

    private fun offPageSchemaError(cupertino: Boolean) = runComposeUiTest {
        val constrained = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"string","minLength":2}}}}"""
        ).jsonObject)
        val values = Json.parseToJsonElement("""{"rows":["aa","bb","cc","dd","ee","x"]}""")
        var writes = 0
        setContent {
            JsonForms(data = values, schema = constrained, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        assertEquals(5, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithText("Page 2: 1 error").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, writes)
    }

    @Test fun materialEmptyPageChoicesUseDocumentedDefaults() = emptyChoicesUseDefaults(false)
    @Test fun cupertinoHostEmptyPageChoicesUseDocumentedDefaults() = emptyChoicesUseDefaults(true)

    private fun emptyChoicesUseDefaults(cupertino: Boolean) = runComposeUiTest {
        var choices by mutableStateOf("[2]")
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ui(mapOf("pagination" to Json.parseToJsonElement(
                    """{"pageSize":2,"pageSizeOptions":$choices}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Page size").assertDoesNotExist()
        runOnIdle { choices = "[]" }
        onNodeWithContentDescription("Page size").assertExists()
        runOnIdle { choices = "[0,-1,\"10\"]" }
        onNodeWithContentDescription("Page size").assertExists()
        assertEquals(0, writes)
    }

    @Test fun materialQuotedBooleanDoesNotDisablePagination() = strictPaginationShape(false)
    @Test fun cupertinoHostQuotedBooleanDoesNotDisablePagination() = strictPaginationShape(true)

    private fun strictPaginationShape(cupertino: Boolean) = runComposeUiTest {
        var local by mutableStateOf<kotlinx.serialization.json.JsonElement>(JsonPrimitive("false"))
        var scoped by mutableStateOf(true)
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ui(mapOf("pagination" to local)),
                config = mapOf("jsonformsExtended" to mapOf(
                    "array" to mapOf("pagination" to scoped))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Next page").assertExists()
        runOnIdle { local = JsonPrimitive(false) }
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { local = JsonPrimitive(1); scoped = false }
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { scoped = true }
        onNodeWithContentDescription("Next page").assertExists()
        assertEquals(0, writes)
    }

    @Test fun materialPagerNavigatesWithKeyboard() = keyboardPager(false)
    @Test fun cupertinoHostPagerNavigatesWithKeyboard() = keyboardPager(true)

    private fun keyboardPager(cupertino: Boolean) = runComposeUiTest {
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ui(mapOf("pagination" to Json.parseToJsonElement(
                    """{"pageSize":3}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Next page")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Next page").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        onNodeWithText("2 of 3").assertExists()
        onNodeWithContentDescription("Previous page")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Previous page").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        onNodeWithText("1 of 3").assertExists()
        assertEquals(0, writes)
    }

    @Test fun materialPagerUsesBuiltInBulgarianLabels() = builtInPager(false)
    @Test fun cupertinoHostPagerUsesBuiltInBulgarianLabels() = builtInPager(true)

    private fun builtInPager(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = initial, schema = schema,
                uischema = ui(mapOf("pagination" to Json.parseToJsonElement(
                    """{"pageSize":3}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/6", message = "server")),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("Next page").assertExists()
        onNodeWithText("1 of 3").assertExists()
        onNodeWithText("Page 3: 1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Следваща страница").assertExists()
        onNodeWithText("1 от 3").assertExists()
        onNodeWithText("Страница 3: 1 грешка").performClick()
        onNodeWithText("3 от 3").assertExists()
        assertEquals(0, writes)
    }

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"string"}}}}"""
    ).jsonObject)
    private val initial = Json.parseToJsonElement("""{"rows":["a","b","c","d","e","f","g"]}""")
    private fun ui(options: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()) =
        ControlElement("#/properties/rows", options = mapOf("table" to JsonPrimitive(true)) + options)

    @Test fun materialDefaultPagesAndEditsAbsoluteRow() = scenario(false)
    @Test fun cupertinoDefaultPagesAndEditsAbsoluteRow() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        assertEquals(5, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").performClick()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("F")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":["a","b","c","d","e","F","g"]}"""), data)
        onNodeWithContentDescription("Previous page").performClick()
        assertEquals(5, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(1, changes)
    }

    @Test fun localFalseOverridesScopedPaginationAndLocalObjectCanReenable() = runComposeUiTest {
        var local by mutableStateOf<kotlinx.serialization.json.JsonElement>(JsonPrimitive(false))
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ui(mapOf("pagination" to local)),
                config = mapOf("jsonformsExtended" to mapOf("array" to mapOf("pagination" to true))),
                renderers = materialRenderers,
            )
        }
        assertEquals(7, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").assertDoesNotExist()
        runOnIdle { local = Json.parseToJsonElement("""{"pageSize":2,"pageSizeOptions":[2,4]}""") }
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").assertExists()
    }

    @Test fun errorPageLinkAndDeleteClampUseAbsoluteIndices() = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ui(mapOf(
                    "pagination" to Json.parseToJsonElement("""{"pageSize":3}"""),
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}"""),
                )),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/6", message = "server")),
                renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Page 3: 1 error").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(androidx.compose.ui.test.hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        onNodeWithText("2 of 2").assertExists()
        assertEquals(Json.parseToJsonElement("""{"rows":["a","b","c","d","e","f"]}"""), data)
        assertEquals(1, changes)
        onNodeWithContentDescription("Add item").performClick()
        waitForIdle()
        onNodeWithText("3 of 3").assertExists()
        assertEquals(2, changes)
    }

    @Test fun moveAcrossPageBoundaryChangesStoredOrder() = runComposeUiTest {
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ui(mapOf("showSortButtons" to JsonPrimitive(true))),
                renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithContentDescription("Next page").performClick()
        onAllNodes(hasContentDescription("Move up")).onFirst().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":["a","b","c","d","f","e","g"]}"""), data)
        assertEquals(1, changes)
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    @Test fun agGridFallbackUsesPortablePagination() = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(
                data = initial, schema = schema,
                uischema = ui(mapOf("variant" to JsonPrimitive("ag-grid"))),
                renderers = materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithText("AG Grid is unavailable here; showing an array table.").assertExists()
        assertEquals(5, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onNodeWithContentDescription("Next page").performClick()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }

    @Test fun pendingDateDraftKeepsItsPageMounted() = runComposeUiTest {
        val dateSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"string","format":"date"}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["2024-03-15","2024-04-10"]"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = dateSchema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "pagination" to Json.parseToJsonElement("""{"pageSize":1}"""),
                )),
                renderers = materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/15/202")
        waitForIdle()
        onNodeWithContentDescription("Next page").assertIsNotEnabled()
        onNodeWithContentDescription("Add item").assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/15/2024")
        waitForIdle()
        onNodeWithContentDescription("Next page").assertIsEnabled()
        onNodeWithContentDescription("Next page").performClick()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }
}
