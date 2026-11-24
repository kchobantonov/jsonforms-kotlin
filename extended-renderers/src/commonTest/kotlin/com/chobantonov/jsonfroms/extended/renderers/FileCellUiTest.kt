package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class FileCellUiTest {
    private val tableSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"upload":{"type":"string","format":"binary"},"files":{"type":"array","items":{"type":"string","format":"binary"}}}}}}}"""
    ).jsonObject)

    @Test fun materialScalarTableCellSelectReplaceClear() = scalarCell(false)
    @Test fun cupertinoScalarTableCellSelectReplaceClear() = scalarCell(true)
    @Test fun materialSizeErrorFollowsMovedRow() = movedSizeError(false)
    @Test fun cupertinoSizeErrorFollowsMovedRow() = movedSizeError(true)
    @Test fun materialPendingPickerBlocksRowMoveUntilResolved() = pendingPickerBlocksRowMove(false)
    @Test fun cupertinoPendingPickerBlocksRowMoveUntilResolved() = pendingPickerBlocksRowMove(true)
    @Test fun materialHostReplacementRetiresPendingPicker() = hostReplacementRetiresPendingPicker(false)
    @Test fun cupertinoHostReplacementRetiresPendingPicker() = hostReplacementRetiresPendingPicker(true)
    @Test fun materialHostReplacementRejectsLegacyPickerCallback() = hostReplacementRejectsLegacyPickerCallback(false)
    @Test fun cupertinoHostReplacementRejectsLegacyPickerCallback() = hostReplacementRejectsLegacyPickerCallback(true)
    @Test fun materialHostReplacementRetiresMeasuredSize() = hostReplacementRetiresMeasuredSize(false)
    @Test fun cupertinoHostReplacementRetiresMeasuredSize() = hostReplacementRetiresMeasuredSize(true)

    private fun hostReplacementRetiresMeasuredSize(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"id":1,"files":[]}]}"""))
        val replacement = Json.parseToJsonElement("""{"rows":[{"id":2,"files":["large"]}]}""")
        var validity by mutableStateOf<CombinedValidity?>(null)
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"files"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf("restrict" to false, "formatMaximum" to JsonPrimitive(2),
                    "jsonformsExtended" to mapOf("propagateErrors" to true),
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                        onEvent(FilePickerEvent.Selected(JsonPrimitive("large"), 4))
                    }),
                onChange = { data = it.data },
                onValidityChange = { validity = it })
        }
        onNodeWithText("Select").performClick()
        waitForIdle()
        assertEquals(1, validity?.additionalErrorCount)
        runOnIdle { data = replacement }
        waitForIdle()
        assertEquals(replacement, data)
        assertEquals(0, validity?.additionalErrorCount)
        onAllNodesWithContentDescription("Selected file violates the size limit",
            substring = true).assertCountEquals(0)
    }

    private fun hostReplacementRejectsLegacyPickerCallback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"id":1}]}"""))
        val replacement = Json.parseToJsonElement("""{"rows":[{"id":2}]}""")
        var changes = 0
        var receive: ((kotlinx.serialization.json.JsonElement) -> Unit)? = null
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"upload"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerConfigKey to
                    ExtendedFilePicker { _, _, onSelected -> receive = onSelected }),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Select").performClick()
        val staleCallback = requireNotNull(receive)
        runOnIdle { data = replacement }
        waitForIdle()
        runOnIdle { staleCallback(JsonPrimitive("late")) }
        waitForIdle()
        assertEquals(replacement, data)
        assertEquals(0, changes)
    }

    private fun hostReplacementRetiresPendingPicker(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"id":1,"files":[]}]}"""))
        val replacement = Json.parseToJsonElement("""{"rows":[{"id":2,"files":[]}]}""")
        var validity by mutableStateOf<CombinedValidity?>(null)
        var changes = 0
        var receive: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"files"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to
                    ExtendedFilePickerEvents { _, _, onEvent -> receive = onEvent }),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it })
        }
        onNodeWithText("Select").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity?.status)
        val staleCallback = requireNotNull(receive)
        runOnIdle { data = replacement }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity?.status)
        onNodeWithText("Select").assertIsEnabled()
        runOnIdle { staleCallback(FilePickerEvent.Selected(JsonPrimitive("late"), 1)) }
        waitForIdle()
        assertEquals(replacement, data)
        assertEquals(0, changes)
    }

    private fun pendingPickerBlocksRowMove(cupertino: Boolean) = runComposeUiTest {
        val original = Json.parseToJsonElement("""{"rows":[{"id":1,"files":[]},{"id":2,"files":[]}]}""")
        val moved = Json.parseToJsonElement("""{"rows":[{"id":2,"files":[]},{"id":1,"files":[]}]}""")
        var data by mutableStateOf(original)
        var validity by mutableStateOf<CombinedValidity?>(null)
        var changes = 0
        var receive: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true), "showSortButtons" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"files"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to
                    ExtendedFilePickerEvents { _, _, onEvent -> receive = onEvent }),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it })
        }
        onAllNodesWithText("Select").onFirst().performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity?.status)
        assertTrue(validity?.pending == true)
        onAllNodesWithContentDescription("Move down").onFirst().performClick()
        waitForIdle()
        assertEquals(original, data)
        assertEquals(0, changes)
        assertEquals(CombinedValidityStatus.Pending, validity?.status)

        runOnIdle { requireNotNull(receive)(FilePickerEvent.ReadFailed()) }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity?.status)
        onAllNodesWithContentDescription("Move down").onFirst().performClick()
        waitForIdle()
        assertEquals(1, changes)
        assertEquals(moved, data)
        runOnIdle { requireNotNull(receive)(FilePickerEvent.Selected(JsonPrimitive("late"), 1)) }
        waitForIdle()
        assertEquals(1, changes)
        assertEquals(moved, data)
    }

    private fun movedSizeError(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"files":[]},{"files":[]}]}"""))
        var validity by mutableStateOf<CombinedValidity?>(null)
        val host = listOf(ErrorObject(instancePath = "/rows/0/files", keyword = "server",
            message = "Host error at first row"))
        val picker = ExtendedFilePickerEvents { _, _, onEvent ->
            onEvent(FilePickerEvent.Selected(JsonPrimitive("large"), 4))
        }
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true), "showSortButtons" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"files"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf("restrict" to false, "formatMaximum" to JsonPrimitive(2),
                    "jsonformsExtended" to mapOf("propagateErrors" to true),
                    ExtendedFilePickerEventsConfigKey to picker),
                additionalErrors = host,
                onChange = { data = it.data },
                onValidityChange = { validity = it })
        }
        onAllNodesWithText("Select").onFirst().performClick()
        waitForIdle()
        assertEquals(2, validity?.additionalErrorCount)
        assertTrue(generateSequence(onAllNodesWithContentDescription("Selected file violates the size limit",
            substring = true).onFirst()
            .fetchSemanticsNode()) { it.parent }.any {
            runCatching { it.config[SemanticsProperties.TestTag] }.getOrNull() == "table-row-scroll-0"
        })
        onAllNodesWithContentDescription("Move down").onFirst().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":[]},{"files":["large"]}]}"""), data)
        assertEquals(2, validity?.additionalErrorCount)
        assertTrue(generateSequence(onAllNodesWithContentDescription("Selected file violates the size limit",
            substring = true).onFirst()
            .fetchSemanticsNode()) { it.parent }.any {
            runCatching { it.config[SemanticsProperties.TestTag] }.getOrNull() == "table-row-scroll-1"
        })
        assertTrue(generateSequence(onAllNodesWithContentDescription("Host error at first row",
            substring = true).onFirst()
            .fetchSemanticsNode()) { it.parent }.any {
            runCatching { it.config[SemanticsProperties.TestTag] }.getOrNull() == "table-row-scroll-0"
        })
    }
    @Test fun materialInvalidArrayCellCanClear() = invalidCell(false, true)
    @Test fun cupertinoInvalidArrayCellCanClear() = invalidCell(true, true)
    @Test fun materialInvalidScalarCellCanClear() = invalidCell(false, false)
    @Test fun cupertinoInvalidScalarCellCanClear() = invalidCell(true, false)

    private fun invalidCell(cupertino: Boolean, multiple: Boolean) = runComposeUiTest {
        val key = if (multiple) "files" else "upload"
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"$key":{"raw":true}}]}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = tableSchema,
                uischema = ControlElement("#/properties/rows", options =
                    mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertExists()
        runOnIdle { assertEquals(0, changes) }
        if (!multiple) onNodeWithText("Замени")
            .performSemanticsAction(SemanticsActions.RequestFocus)
        onAllNodesWithText(if (multiple) "Премахни всички" else "Изчисти").onLast().performClick()
        runOnIdle {
            assertEquals(1, changes)
            val expected = if (multiple) """{"rows":[{"files":[]}]}""" else """{"rows":[{}]}"""
            assertEquals(Json.parseToJsonElement(expected), data)
        }
    }
    @Test fun materialArrayTableCellCommitsBatchAndShowsCompactFeedback() = arrayCell(false)
    @Test fun cupertinoArrayTableCellCommitsBatchAndShowsCompactFeedback() = arrayCell(true)
    @Test fun materialNarrowArrayCellKeepsClearReachable() = narrowArrayCell(false)
    @Test fun cupertinoNarrowArrayCellKeepsClearReachable() = narrowArrayCell(true)
    @Test fun materialCellFollowsBuiltinLocaleAndHostOverride() = localizedCell(false)
    @Test fun cupertinoCellFollowsBuiltinLocaleAndHostOverride() = localizedCell(true)
    @Test fun materialScalarCellWithoutPickerCanEditAndClear() = manualScalarCell(false)
    @Test fun cupertinoScalarCellWithoutPickerCanEditAndClear() = manualScalarCell(true)
    @Test fun materialArrayCellWithoutPickerCanAddAndClear() = manualArrayCell(false)
    @Test fun cupertinoArrayCellWithoutPickerCanAddAndClear() = manualArrayCell(true)
    @Test fun materialExplicitFileArrayDetailUsesDialog() = explicitFileArrayDetail(false)
    @Test fun cupertinoExplicitFileArrayDetailUsesDialog() = explicitFileArrayDetail(true)
    @Test fun materialFileArrayErrorStaysBesideScrollableSummary() = fileErrorGeometry(false)
    @Test fun cupertinoFileArrayErrorStaysBesideScrollableSummary() = fileErrorGeometry(true)
    @Test fun materialFileArrayMultipleErrorsShowTranslatedCount() = multipleErrors(false)
    @Test fun cupertinoFileArrayMultipleErrorsShowTranslatedCount() = multipleErrors(true)
    @Test fun materialPickerCellExposesErrorOnAction() = pickerErrorName(false)
    @Test fun cupertinoPickerCellExposesErrorOnAction() = pickerErrorName(true)
    @Test fun materialManualCellExposesErrorOnInput() = manualErrorName(false)
    @Test fun cupertinoManualCellExposesErrorOnInput() = manualErrorName(true)
    @Test fun materialPickerFailureStaysBesideClear() = pickerFailureGeometry(false)
    @Test fun cupertinoPickerFailureStaysBesideClear() = pickerFailureGeometry(true)

    private fun pickerFailureGeometry(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"rows":[{"files":[]}]}""")
        var changes = 0
        setContent {
            Box(Modifier.width(360.dp)) {
                JsonForms(
                    data = data,
                    schema = tableSchema,
                    uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                    config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                        onEvent(FilePickerEvent.ReadFailed())
                    }),
                    onChange = { changes++ },
                )
            }
        }
        onAllNodesWithText("Select").onLast().performClick()
        val warning = onNodeWithContentDescription("The selected file could not be read")
            .fetchSemanticsNode().boundsInRoot
        val clear = onNodeWithText("Clear all").fetchSemanticsNode().boundsInRoot
        assertTrue(warning.width > 0f && kotlin.math.abs(warning.center.y - clear.center.y) < 16f,
            "Warning=$warning, Clear=$clear")
        onNodeWithText("The selected file could not be read").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun pickerErrorName(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""),
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/files/0", message = "server file error")),
            )
        }
        val action = onNode(hasClickAction() and hasContentDescription("Select. server file error"))
        assertEquals("server file error", action.fetchSemanticsNode().config[SemanticsProperties.Error])
        onNodeWithContentDescription("server file error").assertExists()
        onNodeWithText("server file error").assertDoesNotExist()
    }

    private fun manualErrorName(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"upload":""}]}"""),
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                additionalErrors = listOf(ErrorObject(instancePath = "/rows/0/upload", message = "server upload error")),
            )
        }
        val input = onNode(hasSetTextAction() and
            hasContentDescription("File data or URL. server upload error"))
        assertEquals("server upload error", input.fetchSemanticsNode().config[SemanticsProperties.Error])
        onNodeWithText("server upload error").assertDoesNotExist()
    }

    private fun multipleErrors(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""),
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/rows/0/files/0", message = "first server error"),
                    ErrorObject(instancePath = "/rows/0/files/1", message = "second server error"),
                ),
                i18n = JsonFormsI18nState(translate = { key, fallback, _ ->
                    if (key.endsWith("file.errorCount")) "2 issues" else fallback
                }),
            )
        }
        onNodeWithContentDescription("2 issues").performClick()
        onNodeWithText("first server error", substring = true).assertExists()
        onNodeWithText("second server error", substring = true).assertExists()
    }

    private fun fileErrorGeometry(cupertino: Boolean) = runComposeUiTest {
        setContent {
            Box(Modifier.width(230.dp)) {
                JsonForms(
                    data = Json.parseToJsonElement(
                        """{"rows":[{"files":["data:text/plain;filename=long-long-long-name.txt;base64,YQ=="]}]}"""
                    ),
                    schema = tableSchema,
                    uischema = ControlElement(scope = "#/properties/rows", options = mapOf(
                        "table" to JsonPrimitive(true),
                        "columnDefs" to Json.parseToJsonElement("""[{"field":"files","width":160}]"""),
                    )),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                    additionalErrors = listOf(ErrorObject(
                        instancePath = "/rows/0/files/0", message = "server file error")),
                )
            }
        }
        val file = onNodeWithContentDescription("long-long-long-name.txt")
            .fetchSemanticsNode().boundsInRoot
        val error = onNodeWithContentDescription("server file error")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(file.center.y - error.center.y) < 16f)
        assertTrue(error.left >= file.left && error.width > 0f)
    }

    private fun explicitFileArrayDetail(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"rows":[{"files":[]}]}""")
        setContent {
            JsonForms(
                data = data,
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"files":{"detail":{"type":"Control","scope":"#"}}}"""),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
            )
        }
        onNodeWithContentDescription("Edit Files").assertExists()
        onNodeWithText("Select").assertDoesNotExist()
    }

    private fun manualScalarCell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{}]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data },
            )
        }
        onAllNodesWithText("File data or URL").onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("manual")
        assertEquals(Json.parseToJsonElement("""{"rows":[{"upload":"manual"}]}"""), data)
        onNodeWithText("Clear").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{}]}"""), data)
    }

    private fun manualArrayCell(cupertino: Boolean) = runComposeUiTest {
        val uniqueSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"upload":{"type":"string","format":"binary"},"files":{"type":"array","uniqueItems":true,"items":{"type":"string","format":"binary"}}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = uniqueSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data },
            )
        }
        if (cupertino) onAllNodesWithContentDescription("File data or URL").onLast().assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextInput("manual")
        onNodeWithText("Add attachment").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":["manual"]}]}"""), data)
        onAllNodes(hasSetTextAction()).onLast().performTextInput("manual")
        onNodeWithText("Add attachment").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":["manual"]}]}"""), data)
        val warning = onNodeWithContentDescription("This file is already attached")
            .fetchSemanticsNode().boundsInRoot
        val clear = onNodeWithText("Clear all").fetchSemanticsNode().boundsInRoot
        assertTrue(kotlin.math.abs(warning.center.y - clear.center.y) < 16f,
            "Warning center=${warning.center.y}, Clear center=${clear.center.y}")
        onNodeWithText("Clear all").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""), data)
    }

    private fun localizedCell(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        val data = Json.parseToJsonElement("""{"rows":[{"upload":"data:text/plain;filename=old.txt;base64,b2xk"}]}""")
        setContent {
            JsonForms(
                data = data,
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key.endsWith("file.clear") && locale.startsWith("bg")) "HOST CLEAR" else fallback
                }),
            )
        }
        onNodeWithText("Replace").assertExists()
        onNodeWithText("Clear").assertDoesNotExist()
        onNodeWithText("Replace").performSemanticsAction(SemanticsActions.RequestFocus)
        onNodeWithText("Clear").assertExists()
        runOnIdle { locale = "en-US" }
        onNodeWithText("Replace").assertExists()
        onNodeWithText("Clear").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Замени").assertExists()
        onNodeWithText("HOST CLEAR").assertExists()
        onNodeWithText("Изчисти").assertDoesNotExist()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"upload":"data:text/plain;filename=old.txt;base64,b2xk"}]}"""), data)
    }

    private fun narrowArrayCell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"files":["data:text/plain;filename=long-long-long-name.txt;base64,YQ=="]}]}"""
        ))
        setContent {
            Box(Modifier.width(360.dp)) {
                JsonForms(
                    data = data,
                    schema = tableSchema,
                    uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                    config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, _ -> }),
                    onChange = { data = it.data },
                )
            }
        }
        onNodeWithContentDescription("long-long-long-name.txt").assertExists()
        onNodeWithText("Clear all").performScrollTo().performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""), data)
    }

    private fun scalarCell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"upload":"data:text/plain;filename=old.txt;base64,b2xk"}]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    onEvent(FilePickerEvent.Selected(JsonPrimitive("data:text/plain;filename=new.txt;base64,bmV3"), 3))
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("old.txt").assertExists()
        onNodeWithText("Choose file").assertDoesNotExist()
        onNodeWithText("Replace").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"upload":"data:text/plain;filename=new.txt;base64,bmV3"}]}"""), data)
        onNodeWithText("new.txt").assertExists()
        onNodeWithText("Clear").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{}]}"""), data)
        onAllNodesWithText("No file").onFirst().assertExists()
    }

    private fun arrayCell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""))
        var changes = 0
        val picker = object : ExtendedFilePickerEvents {
            override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                error("Array cell used scalar picker")
            }

            override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                onEvent(FilePickerEvent.SelectedBatch(listOf(
                    FilePickerEvent.Selected(JsonPrimitive("data:text/plain;filename=short.txt;base64,YQ=="), 1),
                    FilePickerEvent.Selected(JsonPrimitive("data:text/plain;filename=very-long-filename-that-must-be-bounded.txt;base64,YWJj"), 3),
                )))
            }
        }
        setContent {
            JsonForms(
                data = data,
                schema = tableSchema,
                uischema = ControlElement(scope = "#/properties/rows", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf(
                    "restrict" to false,
                    ExtendedFilePickerEventsConfigKey to picker,
                    "formatMaximum" to JsonPrimitive(2),
                    "jsonformsExtended" to mapOf("propagateErrors" to true),
                ),
                onChange = { data = it.data; changes++ },
            )
        }
        onAllNodesWithText("Select").onLast().performClick()
        assertEquals(1, changes)
        assertEquals(Json.parseToJsonElement(
            """{"rows":[{"files":["data:text/plain;filename=short.txt;base64,YQ==","data:text/plain;filename=very-long-filename-that-must-be-bounded.txt;base64,YWJj"]}]}"""
        ), data)
        onNodeWithText("short.txt").assertExists()
        onNodeWithContentDescription("very-long-filename-that-must-be-bounded.txt").assertExists()
        onNodeWithContentDescription("Selected file violates the size limit").assertExists()
        onNodeWithText("Selected file violates the size limit").assertDoesNotExist()
        onNodeWithText("Clear all").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"files":[]}]}"""), data)
    }
}
