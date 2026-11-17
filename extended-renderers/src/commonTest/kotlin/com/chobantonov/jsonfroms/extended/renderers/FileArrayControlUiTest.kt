package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class FileArrayControlUiTest {
    @Test fun materialShowsMissingRequiredError() = required(false, false)
    @Test fun cupertinoShowsMissingRequiredError() = required(true, false)
    @Test fun materialInvalidImportedArrayCanClear() = invalidImported(false)
    @Test fun cupertinoInvalidImportedArrayCanClear() = invalidImported(true)

    private fun invalidImported(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"a":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = arraySchema(),
                uischema = ControlElement("#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: {\"raw\":true}"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Invalid value: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"a":null}""")
        }
        onNodeWithText("Невалидна стойност: null").assertExists()
        onNodeWithText("Премахни всички").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
            assertEquals(1, changes)
        }
    }
    @Test fun materialFiltersRequiredButKeepsHostError() = required(false, true)
    @Test fun cupertinoFiltersRequiredButKeepsHostError() = required(true, true)
    @Test fun materialShowsMinItemsErrorForStoredEmptyArray() = minItems(false)
    @Test fun cupertinoShowsMinItemsErrorForStoredEmptyArray() = minItems(true)
    @Test fun materialRestrictsCountActions() = restrictedCount(false)
    @Test fun cupertinoRestrictsCountActions() = restrictedCount(true)
    @Test fun materialAllowsCountActionsWhenUnrestricted() = unrestrictedCount(false)
    @Test fun cupertinoAllowsCountActionsWhenUnrestricted() = unrestrictedCount(true)
    @Test fun materialRejectsDuplicateEncodedValue() = duplicate(false)
    @Test fun cupertinoRejectsDuplicateEncodedValue() = duplicate(true)
    @Test fun materialRejectsStalePickerResult() = stalePicker(false)
    @Test fun cupertinoRejectsStalePickerResult() = stalePicker(true)
    @Test fun materialHonorsClearableFalse() = notClearable(false)
    @Test fun cupertinoHonorsClearableFalse() = notClearable(true)
    @Test fun materialFileActionsRetranslateOnLocaleChange() = localizedActions(false)
    @Test fun cupertinoFileActionsRetranslateOnLocaleChange() = localizedActions(true)
    @Test fun materialFileBundleWorksWithoutFormCatalog() = noCatalogLocaleActions(false)
    @Test fun cupertinoFileBundleWorksWithoutFormCatalog() = noCatalogLocaleActions(true)
    @Test fun materialClearAllWritesEmptyArray() = clearAll(false)
    @Test fun cupertinoClearAllWritesEmptyArray() = clearAll(true)
    @Test fun materialPickerAppendsAcceptedValue() = pickerAppends(false)
    @Test fun cupertinoPickerAppendsAcceptedValue() = pickerAppends(true)
    @Test fun materialUnrestrictedMaxItemsShowsSchemaError() = maxItemsFeedback(false)
    @Test fun cupertinoUnrestrictedMaxItemsShowsSchemaError() = maxItemsFeedback(true)
    @Test fun materialLocalRestrictFalseOverridesGlobalTrue() = localRestrict(false)
    @Test fun cupertinoLocalRestrictFalseOverridesGlobalTrue() = localRestrict(true)
    @Test fun materialLocalClearableTrueOverridesGlobalFalse() = localClearable(false)
    @Test fun cupertinoLocalClearableTrueOverridesGlobalFalse() = localClearable(true)
    @Test fun materialReadonlyDisablesFileActions() = readonly(false)
    @Test fun cupertinoReadonlyDisablesFileActions() = readonly(true)
    @Test fun materialRejectedSizePreservesValue() = eventOutcome(false, FilePickerEvent.RejectedSize(FileSizeViolation("formatMaximum", 3.0, 4)), false, "The selected file does not meet the size limit")
    @Test fun cupertinoRejectedSizePreservesValue() = eventOutcome(true, FilePickerEvent.RejectedSize(FileSizeViolation("formatMaximum", 3.0, 4)), false, "The selected file does not meet the size limit")
    @Test fun materialReadFailurePreservesValue() = eventOutcome(false, FilePickerEvent.ReadFailed(), false, "The selected file could not be read")
    @Test fun cupertinoReadFailurePreservesValue() = eventOutcome(true, FilePickerEvent.ReadFailed(), false, "The selected file could not be read")
    @Test fun materialCancelClearsSelection() = eventOutcome(false, FilePickerEvent.Cancelled, true, null)
    @Test fun cupertinoCancelClearsSelection() = eventOutcome(true, FilePickerEvent.Cancelled, true, null)
    @Test fun materialRestrictedSizePreventsCommit() = eventOutcome(false, FilePickerEvent.Selected(JsonPrimitive("too large"), 4), false, "The selected file does not meet the size limit")
    @Test fun cupertinoRestrictedSizePreventsCommit() = eventOutcome(true, FilePickerEvent.Selected(JsonPrimitive("too large"), 4), false, "The selected file does not meet the size limit")
    @Test fun materialDefaultRestrictPreventsOversizedFile() = eventOutcome(false, FilePickerEvent.Selected(JsonPrimitive("too large"), 4), false, "The selected file does not meet the size limit", explicitRestrict = false)
    @Test fun cupertinoDefaultRestrictPreventsOversizedFile() = eventOutcome(true, FilePickerEvent.Selected(JsonPrimitive("too large"), 4), false, "The selected file does not meet the size limit", explicitRestrict = false)
    @Test fun materialUnrestrictedSizeOwnsValidity() = ownedSizeValidity(false)
    @Test fun cupertinoUnrestrictedSizeOwnsValidity() = ownedSizeValidity(true)
    @Test fun materialSizeOwnerPreservesHostError() = preservesHostError(false)
    @Test fun cupertinoSizeOwnerPreservesHostError() = preservesHostError(true)
    @Test fun materialSizeSummaryTracksRemovalAndLocale() = sizeSummary(false)
    @Test fun cupertinoSizeSummaryTracksRemovalAndLocale() = sizeSummary(true)
    @Test fun materialLateSelectionAfterCancelIsIgnored() = lateAfterCancel(false)
    @Test fun cupertinoLateSelectionAfterCancelIsIgnored() = lateAfterCancel(true)
    @Test fun materialExternalReplacementCancelsPendingSelection() = externalReplacement(false)
    @Test fun cupertinoExternalReplacementCancelsPendingSelection() = externalReplacement(true)
    @Test fun materialDisposedPickerCannotCommit() = disposedPicker(false)
    @Test fun cupertinoDisposedPickerCannotCommit() = disposedPicker(true)
    @Test fun materialScalarFileSizeAndCancel() = scalarFile(false)
    @Test fun cupertinoScalarFileSizeAndCancel() = scalarFile(true)
    @Test fun materialBatchFiltersAndCommitsOnce() = batchFiltersAndCommitsOnce(false)
    @Test fun cupertinoBatchFiltersAndCommitsOnce() = batchFiltersAndCommitsOnce(true)
    @Test fun materialBatchReadFailureKeepsPreviousAttachments() = batchReadFailure(false)
    @Test fun cupertinoBatchReadFailureKeepsPreviousAttachments() = batchReadFailure(true)
    @Test fun materialExternalReplacementRejectsLateBatch() = staleBatchAfterReplacement(false)
    @Test fun cupertinoExternalReplacementRejectsLateBatch() = staleBatchAfterReplacement(true)
    @Test fun materialFileArrayHonorsSharedActionFlags() = actionFlags(false)
    @Test fun cupertinoFileArrayHonorsSharedActionFlags() = actionFlags(true)
    @Test fun materialFileArrayRejectsPickerAfterAddRevocation() = revokedPicker(false)
    @Test fun cupertinoFileArrayRejectsPickerAfterAddRevocation() = revokedPicker(true)
    @Test fun materialContainsMinimumGuardsFileRemovalAndClearAll() = containsMinimum(false)
    @Test fun cupertinoContainsMinimumGuardsFileRemovalAndClearAll() = containsMinimum(true)

    private fun containsMinimum(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["one","two"]}"""))
        var unrestricted by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data,
                schema = arraySchema("\"contains\":{\"const\":\"one\"},"),
                uischema = ControlElement(scope = "#/properties/a", options =
                    mapOf("restrict" to JsonPrimitive(!unrestricted))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onAllNodesWithText("Remove").onLast().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"a":["one"]}"""), data)
        onNodeWithText("Clear all").assertIsNotEnabled()
        assertEquals(1, changes)
        runOnIdle { unrestricted = true }
        onNodeWithText("Clear all").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
        assertEquals(2, changes)
    }

    private fun arraySchema(extra: String = ""): JsonSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"a":{"type":"array",$extra"items":{"type":"string","format":"binary"}}}}"""
    ).jsonObject)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","required":["a"],"properties":{"a":{"type":"array","minItems":1,"items":{"type":"string","format":"binary"}}}}"""
    ).jsonObject)

    private fun required(cupertino: Boolean, filterRequired: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("{}"),
                schema = schema,
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = if (filterRequired) mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("required"),
                ) else emptyMap(),
                additionalErrors = if (filterRequired) listOf(
                    ErrorObject(instancePath = "/a", keyword = "server", message = "server error")
                ) else emptyList(),
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "server") "server error" else "ERR:${error.keyword}"
                }),
            )
        }
        onNodeWithText("File data or URL").assertExists()
        if (filterRequired) {
            onNodeWithText("ERR:required").assertDoesNotExist()
            onNodeWithText("server error").assertExists()
        } else {
            onNodeWithText("ERR:required").assertExists()
        }
    }

    private fun minItems(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":[]}"""),
                schema = schema,
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("required"),
                ),
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> "ERR:${error.keyword}" }),
            )
        }
        onNodeWithText("ERR:minItems").assertExists()
    }

    private fun restrictedCount(cupertino: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"a":["one"]}""")
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"minItems\":1,\"maxItems\":1,"),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to true),
                onChange = { data = it.data },
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onNodeWithText("Clear all").assertIsNotEnabled()
        onNodeWithText("Add attachment").assertIsNotEnabled()
        assertEquals(initial, data)
    }

    private fun unrestrictedCount(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["one"]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"minItems\":1,\"maxItems\":1,"),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false),
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> "ERR:${error.keyword}" }),
                onChange = { data = it.data },
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsEnabled()
        onNodeWithText("Clear all").assertIsEnabled()
        onNodeWithText("Add attachment").assertIsNotEnabled()
        onNodeWithText("Remove").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
        onNodeWithText("ERR:minItems").assertExists()
    }

    private fun duplicate(cupertino: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"a":["same"]}""")
        var data by mutableStateOf(initial)
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"uniqueItems\":true,"),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (locale == "bg" && key.endsWith("file.duplicate")) "Файлът вече е прикачен" else fallback
                    },
                ),
                onChange = { data = it.data },
            )
        }
        onNode(hasSetTextAction()).performTextInput("same")
        onNodeWithText("Add attachment").performClick()
        assertEquals(initial, data)
        onNodeWithText("This file is already attached").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Файлът вече е прикачен").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"a":["external"]}""") }
        onNodeWithText("Файлът вече е прикачен").assertDoesNotExist()
    }

    private fun notClearable(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("clearable" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onNodeWithText("Clear all").assertIsNotEnabled()
    }

    private fun localizedActions(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (locale == "bg" && key.endsWith("file.clearAll")) "Clear attachments" else fallback
                    },
                ),
            )
        }
        onNodeWithText("Clear all").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Clear attachments").assertExists()
        onNodeWithText("Clear all").assertDoesNotExist()
    }

    private fun noCatalogLocaleActions(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
            )
        }
        onNodeWithText("Clear all").assertExists()
        onNodeWithText("Remove").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Премахни всички").assertExists()
        onNodeWithText("Премахни").assertExists()
    }

    private fun clearAll(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["one","two"]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Clear all").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
    }

    private fun pickerAppends(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["one"]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerConfigKey to ExtendedFilePicker { _, _, onSelected ->
                    onSelected(JsonPrimitive("two"))
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Choose file").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":["one","two"]}"""), data)
    }

    private fun maxItemsFeedback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["one"]}"""))
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"maxItems\":1,"),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false),
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> "ERR:${error.keyword}" }),
                onChange = { data = it.data },
            )
        }
        onNode(hasSetTextAction()).performTextInput("two")
        onNodeWithText("Add attachment").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":["one","two"]}"""), data)
        onNodeWithText("ERR:maxItems").assertExists()
    }

    private fun localRestrict(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema("\"minItems\":1,\"maxItems\":1,"),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("restrict" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to true),
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsEnabled()
        onNodeWithText("Clear all").assertIsEnabled()
    }

    private fun localClearable(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("clearable" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("clearable" to false),
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsEnabled()
        onNodeWithText("Clear all").assertIsEnabled()
    }

    private fun readonly(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"a":["one"]}"""),
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("readonly" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
            )
        }
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onNodeWithText("Clear all").assertIsNotEnabled()
        onNodeWithText("Add attachment").assertIsNotEnabled()
    }

    private fun eventOutcome(
        cupertino: Boolean,
        event: FilePickerEvent,
        cleared: Boolean,
        feedback: String?,
        explicitRestrict: Boolean = true,
    ) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"a":["old"]}""")
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"formatMaximum\":3,"),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("formatMaximum" to JsonPrimitive(3))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = (if (explicitRestrict) mapOf("restrict" to true) else emptyMap()) + mapOf(
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent -> onEvent(event) },
                ),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Choose file").performClick()
        assertEquals(if (cleared) Json.parseToJsonElement("""{"a":[]}""") else initial, data)
        if (feedback != null) onNodeWithText(feedback).assertExists()
    }

    private fun ownedSizeValidity(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":[]}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var pending: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf(
                    "formatMaximum" to JsonPrimitive(3), "propagateErrors" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    pending = onEvent
                }),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { pending?.invoke(FilePickerEvent.Selected(JsonPrimitive("oversized"), 4)) }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"a":["oversized"]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Selected file violates the size limit").assertExists()
        onNodeWithText("Clear all").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("Selected file violates the size limit").assertDoesNotExist()
    }

    private fun preservesHostError(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":[]}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf(
                    "formatMaximum" to JsonPrimitive(3), "propagateErrors" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    onEvent(FilePickerEvent.Selected(JsonPrimitive("oversized"), 4))
                }),
                additionalErrors = listOf(ErrorObject(instancePath = "/a", keyword = "server", message = "host error")),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("host error", substring = true).assertExists()
        onNodeWithText("Selected file violates the size limit", substring = true).assertExists()
        onNodeWithText("Clear all").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("host error", substring = true).assertExists()
    }

    private fun sizeSummary(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":[]}"""))
        var locale by mutableStateOf("en")
        var sequence = 0
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf(
                    "formatMaximum" to JsonPrimitive(3), "propagateErrors" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    onEvent(FilePickerEvent.Selected(JsonPrimitive("bad${++sequence}"), 4))
                }),
                i18n = JsonFormsI18nState(
                    locale = locale,
                    translate = { key, fallback, _ ->
                        if (locale == "bg" && key.endsWith("file.sizeInvalid")) "Невалиден размер на файла" else fallback
                    },
                ),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Choose file").performClick()
        onNodeWithText("Choose file").performClick()
        onNodeWithText("2 selected files violate size limits").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Невалиден размер на файла").assertExists()
        onAllNodesWithText("Премахни").onFirst().performClick()
        assertEquals(Json.parseToJsonElement("""{"a":["bad2"]}"""), data)
        onNodeWithText("Невалиден размер на файла").assertExists()
    }

    private fun lateAfterCancel(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":[]}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var callback: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    callback = onEvent
                }),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { callback?.invoke(FilePickerEvent.Cancelled) }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        runOnIdle { callback?.invoke(FilePickerEvent.Selected(JsonPrimitive("late"), 1)) }
        assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
    }

    private fun externalReplacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["old"]}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var callback: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    callback = onEvent
                }),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { data = Json.parseToJsonElement("""{"a":["external"]}""") }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        runOnIdle { callback?.invoke(FilePickerEvent.Selected(JsonPrimitive("late"), 1)) }
        assertEquals(Json.parseToJsonElement("""{"a":["external"]}"""), data)
    }

    private fun disposedPicker(cupertino: Boolean) = runComposeUiTest {
        var visible by mutableStateOf(true)
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":[]}"""))
        var callback: ((FilePickerEvent) -> Unit)? = null
        setContent {
            if (visible) JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    callback = onEvent
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Choose file").performClick()
        runOnIdle { visible = false }
        waitForIdle()
        runOnIdle { callback?.invoke(FilePickerEvent.Selected(JsonPrimitive("late"), 1)) }
        assertEquals(Json.parseToJsonElement("""{"a":[]}"""), data)
    }

    private fun scalarFile(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":"old"}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var callback: ((FilePickerEvent) -> Unit)? = null
        val scalarSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","required":["a"],"properties":{"a":{"type":"string","format":"binary"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = data,
                schema = scalarSchema,
                uischema = ControlElement(scope = "#/properties/a", options = mapOf(
                    "formatMaximum" to JsonPrimitive(3), "propagateErrors" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    callback = onEvent
                }),
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.keyword == "required") "required now" else error.message
                }),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        runOnIdle { callback?.invoke(FilePickerEvent.Selected(JsonPrimitive("oversized"), 4)) }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Selected file violates the size limit").assertExists()
        onNodeWithText("Choose file").performClick()
        runOnIdle { callback?.invoke(FilePickerEvent.Cancelled) }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("{}"), data)
        onNodeWithText("Selected file violates the size limit").assertDoesNotExist()
        onNodeWithText("required now").assertExists()
    }

    private fun batchFiltersAndCommitsOnce(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["old"]}"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema("\"uniqueItems\":true,\"maxItems\":3,"),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("formatMaximum" to JsonPrimitive(2))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(
                    "restrict" to true,
                    ExtendedFilePickerEventsConfigKey to object : ExtendedFilePickerEvents {
                        override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                            error("Array control used scalar picker")
                        }

                        override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                            onEvent(FilePickerEvent.SelectedBatch(listOf(
                                FilePickerEvent.Selected(JsonPrimitive("a"), 1),
                                FilePickerEvent.Selected(JsonPrimitive("old"), 1),
                                FilePickerEvent.Selected(JsonPrimitive("large"), 4),
                                FilePickerEvent.Selected(JsonPrimitive("b"), 1),
                                FilePickerEvent.Selected(JsonPrimitive("c"), 1),
                            )))
                        }
                    },
                ),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Choose file").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":["old","a","b"]}"""), data)
        assertEquals(1, changes)
        onNodeWithText("The selected file does not meet the size limit").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"a":["external"]}""") }
        onNodeWithText("The selected file does not meet the size limit").assertDoesNotExist()
    }

    private fun batchReadFailure(cupertino: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"a":["old"]}""")
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent ->
                    onEvent(FilePickerEvent.ReadFailed("second file failed"))
                }),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Choose file").performClick()
        assertEquals(initial, data)
        assertEquals(0, changes)
        onNodeWithText("The selected file could not be read").assertExists()
    }

    private fun staleBatchAfterReplacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["old"]}"""))
        var changes = 0
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var callback: ((FilePickerEvent) -> Unit)? = null
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = mapOf("formatMaximum" to JsonPrimitive(2))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerEventsConfigKey to object : ExtendedFilePickerEvents {
                    override fun pick(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                        error("Array control used scalar picker")
                    }
                    override fun pickMultiple(schema: JsonSchema, options: JsonOptions, onEvent: (FilePickerEvent) -> Unit) {
                        callback = onEvent
                    }
                }),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { data = Json.parseToJsonElement("""{"a":["external"]}""") }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        runOnIdle {
            callback?.invoke(FilePickerEvent.SelectedBatch(listOf(
                FilePickerEvent.Selected(JsonPrimitive("late"), 4),
            )))
            callback?.invoke(FilePickerEvent.ReadFailed("late failure"))
        }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"a":["external"]}"""), data)
        assertEquals(0, changes)
        onNodeWithText("The selected file could not be read").assertDoesNotExist()
        onNodeWithText("Selected file violates the size limit").assertDoesNotExist()
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun stalePicker(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["old"]}"""))
        var selection: ((JsonElement) -> Unit)? = null
        setContent {
            JsonForms(
                data = data,
                schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf(ExtendedFilePickerConfigKey to ExtendedFilePicker { _, _, onSelected ->
                    selection = onSelected
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Choose file").performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"a":["external"]}""") }
        waitForIdle()
        runOnIdle { selection?.invoke(JsonPrimitive("late")) }
        assertEquals(Json.parseToJsonElement("""{"a":["external"]}"""), data)
    }

    private fun actionFlags(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"a":["old"]}"""))
        var localDisable by mutableStateOf<Boolean?>(null)
        var globalDisable by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a", options = localDisable?.let {
                    mapOf("disableAdd" to JsonPrimitive(it), "disableRemove" to JsonPrimitive(it))
                } ?: emptyMap()),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("disableAdd" to globalDisable, "disableRemove" to globalDisable),
                onChange = { data = it.data; changes++ },
            )
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onNodeWithText("Clear all").assertIsNotEnabled()
        runOnIdle { localDisable = false }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodesWithText("Remove").onFirst().assertIsEnabled()
        onNodeWithText("Clear all").assertIsEnabled()
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("new")
        onNodeWithText("Add attachment").performClick()
        assertEquals(Json.parseToJsonElement("""{"a":["old","new"]}"""), data)
        assertEquals(1, changes)
        runOnIdle { globalDisable = false; localDisable = true }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodesWithText("Remove").onFirst().assertIsNotEnabled()
        onNodeWithText("Clear all").assertIsNotEnabled()
        assertEquals(Json.parseToJsonElement("""{"a":["old","new"]}"""), data)
    }

    private fun revokedPicker(cupertino: Boolean) = runComposeUiTest {
        val initial = Json.parseToJsonElement("""{"a":["old"]}""")
        var data by mutableStateOf(initial)
        var disableAdd by mutableStateOf(false)
        var callback: ((FilePickerEvent) -> Unit)? = null
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = arraySchema(),
                uischema = ControlElement(scope = "#/properties/a"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("disableAdd" to disableAdd,
                    ExtendedFilePickerEventsConfigKey to ExtendedFilePickerEvents { _, _, onEvent -> callback = onEvent }),
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Choose file").performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { disableAdd = true }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        runOnIdle { callback?.invoke(FilePickerEvent.Selected(JsonPrimitive("late"), 4)) }
        assertEquals(initial, data)
        assertEquals(0, changes)
    }
}
