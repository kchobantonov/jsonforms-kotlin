package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnostic
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnosticReporter
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.complex.materialTupleTester
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TupleUiTest {
    @Test fun materialTupleUsesBuiltInBulgarianValidationLabel() = builtInValidationLabel(false)
    @Test fun cupertinoTupleUsesBuiltInBulgarianValidationLabel() = builtInValidationLabel(true)

    private fun builtInValidationLabel(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","properties":{"name":{"type":"string"}}}],"additionalItems":false}"""
        ).jsonObject)
        var locale by mutableStateOf("en")
        var showCount by mutableStateOf(true)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""[{"name":"Ada"}]"""), schema = schema,
                uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
                    uischema = ControlElement(scope = "#", options = mapOf(
                        "showValidationIndicatorCount" to JsonPrimitive(showCount),
                    )),
                )),
                additionalErrors = listOf(ErrorObject(instancePath = "/0/name", message = "server")),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { writes++ })
        }
        onNodeWithContentDescription("1 error").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("1 грешка").assertExists()
        runOnIdle { showCount = false }
        onNodeWithContentDescription("Съдържа грешки").assertExists()
        assertEquals(0, writes)
    }

    @Test fun materialTypedTailDeletesWithoutTouchingFixedPosition() = typedTail(false)

    @Test fun cupertinoTypedTailDeletesWithoutTouchingFixedPosition() = typedTail(true)

    @Test fun materialForbiddenTailOffersCorrectiveRemoval() = forbiddenTail(false)

    @Test fun cupertinoForbiddenTailOffersCorrectiveRemoval() = forbiddenTail(true)

    @Test fun materialRendersImportedTupleFixture() = importedFixture(false)

    @Test fun cupertinoRendersImportedTupleFixture() = importedFixture(true)

    @Test fun materialMissingSecondPositionInitializesPrefix() = missingSecond(false)

    @Test fun cupertinoMissingSecondPositionInitializesPrefix() = missingSecond(true)

    @Test fun materialMissingFixedPositionCanBeEditedWithoutTailCreation() = missingFixed(false)

    @Test fun cupertinoMissingFixedPositionCanBeEditedWithoutTailCreation() = missingFixed(true)

    @Test fun materialHostReplacementInvalidatesTailDialog() = hostReplacement(false)

    @Test fun cupertinoHostReplacementInvalidatesTailDialog() = hostReplacement(true)

    @Test fun materialTupleActionsUseTranslatedCatalog() = translatedActions(false)

    @Test fun cupertinoTupleActionsUseTranslatedCatalog() = translatedActions(true)

    @Test fun materialFreeTailUsesMixedTypeEditor() = freeTail(false)

    @Test fun cupertinoFreeTailUsesMixedTypeEditor() = freeTail(true)

    @Test fun materialTupleRepairsImportedWrongShape() = importedWrongShape(false)

    @Test fun cupertinoTupleRepairsImportedWrongShape() = importedWrongShape(true)

    @Test fun materialTupleUsesBulgarianDefaultActions() = bulgarianDefaultActions(false)

    @Test fun cupertinoTupleUsesBulgarianDefaultActions() = bulgarianDefaultActions(true)

    @Test fun materialTupleTailDeleteKeepsContainsMatch() = containsTail(false)

    @Test fun cupertinoTupleTailDeleteKeepsContainsMatch() = containsTail(true)

    @Test fun materialTupleCancelsStaleContainsDelete() = staleContainsTail(false)

    @Test fun cupertinoTupleCancelsStaleContainsDelete() = staleContainsTail(true)

    private fun staleContainsTail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"},"contains":{"const":"yes"}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["yes","no"]"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("restrict" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Delete Item 2").assertIsEnabled()
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""["no","yes"]""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        onNodeWithText("Delete Item 2").assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun containsTail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"},"contains":{"const":"yes"}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["code","yes"]"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("restrict" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Delete Item 2").assertIsNotEnabled()
        assertEquals(0, changes)
        runOnIdle { data = Json.parseToJsonElement("""["yes","no"]""") }
        onNodeWithText("Delete Item 2").assertIsEnabled()
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["yes"]"""), data)
        assertEquals(1, changes)
    }

    @Test fun materialSchemaRebindInvalidatesTailDialog() = schemaRebind(false)

    @Test fun cupertinoSchemaRebindInvalidatesTailDialog() = schemaRebind(true)

    @Test fun ordinaryUniformArrayDoesNotSelectTupleRenderer() {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"number"},"minItems":3,"maxItems":3}"""
        ).jsonObject)
        assertEquals(-1, materialTupleTester(ControlElement(scope = "#"), schema, TesterContext()))
        assertEquals(1000, materialTupleTester(ControlElement(
            scope = "#", options = mapOf("variant" to JsonPrimitive("tuple")),
        ), schema, TesterContext()))
    }

    @Test fun materialNestedObjectSummaryResolvesScope() = nestedObjectSummary(false)

    @Test fun cupertinoNestedObjectSummaryResolvesScope() = nestedObjectSummary(true)

    @Test fun materialValidatorErrorAppearsOnClosedComplexSummary() = validatorSummaryError(false)

    @Test fun cupertinoValidatorErrorAppearsOnClosedComplexSummary() = validatorSummaryError(true)

    @Test fun materialComplexSummaryIndicatorOptionsAndOwnError() = complexSummaryIndicatorOptions(false)

    @Test fun cupertinoComplexSummaryIndicatorOptionsAndOwnError() = complexSummaryIndicatorOptions(true)

    @Test fun materialSeparatesTupleAndPositionValidation() = tupleValidationPlacement(false)

    @Test fun cupertinoSeparatesTupleAndPositionValidation() = tupleValidationPlacement(true)

    @Test fun materialShowsForbiddenTailErrorAtTuple() = forbiddenTailValidation(false)

    @Test fun cupertinoShowsForbiddenTailErrorAtTuple() = forbiddenTailValidation(true)

    @Test fun materialShowsModernForbiddenTailErrorAtTuple() = forbiddenTailValidation(false, modern = true)

    @Test fun cupertinoShowsModernForbiddenTailErrorAtTuple() = forbiddenTailValidation(true, modern = true)

    @Test fun materialShowsNestedForbiddenTailErrorAtTuple() = nestedForbiddenTailValidation(false)

    @Test fun cupertinoShowsNestedForbiddenTailErrorAtTuple() = nestedForbiddenTailValidation(true)

    @Test fun materialShowsNestedModernForbiddenTailErrorAtTuple() = nestedForbiddenTailValidation(false, modern = true)

    @Test fun cupertinoShowsNestedModernForbiddenTailErrorAtTuple() = nestedForbiddenTailValidation(true, modern = true)

    @Test fun materialArraySummaryResolvesItemScope() = arrayItemScopeSummary(false)

    @Test fun cupertinoArraySummaryResolvesItemScope() = arrayItemScopeSummary(true)

    @Test fun materialArraySummaryUsesLocalizedRemainingCount() = arraySummary(false)

    @Test fun cupertinoArraySummaryUsesLocalizedRemainingCount() = arraySummary(true)

    @Test fun materialComplexSummaryExposesDescendantError() = complexSummaryError(false)

    @Test fun cupertinoComplexSummaryExposesDescendantError() = complexSummaryError(true)

    @Test fun materialMissingLaterComplexApplyInitializesPrefix() = missingLaterComplexApply(false)

    @Test fun cupertinoMissingLaterComplexApplyInitializesPrefix() = missingLaterComplexApply(true)

    @Test fun materialOpeningMissingComplexPositionDoesNotCommit() = missingComplexOpen(false)

    @Test fun cupertinoOpeningMissingComplexPositionDoesNotCommit() = missingComplexOpen(true)

    @Test fun materialArrayPositionClearKeepsFixedSlot() = arrayPositionClear(false)

    @Test fun cupertinoArrayPositionClearKeepsFixedSlot() = arrayPositionClear(true)
    @Test fun materialTupleArrayContainsBlocksRestrictedClear() = tupleContainsClear(false)
    @Test fun cupertinoTupleArrayContainsBlocksRestrictedClear() = tupleContainsClear(true)
    @Test fun materialTupleArrayComposedMinimumBlocksRestrictedClear() = tupleContainsClear(false, composed = true)
    @Test fun cupertinoTupleArrayComposedMinimumBlocksRestrictedClear() = tupleContainsClear(true, composed = true)
    @Test fun materialTupleBooleanContainsBlocksRestrictedClear() = tupleContainsClear(false, booleanContains = true)
    @Test fun cupertinoTupleBooleanContainsBlocksRestrictedClear() = tupleContainsClear(true, booleanContains = true)

    @Test fun materialComplexClearIsDraftOnlyUntilApply() = complexClear(false)

    @Test fun cupertinoComplexClearIsDraftOnlyUntilApply() = complexClear(true)

    @Test fun materialComplexDraftClosesOnHostReplacement() = complexHostReplacement(false)

    @Test fun cupertinoComplexDraftClosesOnHostReplacement() = complexHostReplacement(true)

    @Test fun materialComplexPositionUsesRankedSummaryAndDetail() = rankedComplexDetail(false)

    @Test fun cupertinoComplexPositionUsesRankedSummaryAndDetail() = rankedComplexDetail(true)

    @Test fun materialComplexPositionDraftCancelsAndApplies() = complexDraft(false)

    @Test fun cupertinoComplexPositionDraftCancelsAndApplies() = complexDraft(true)

    @Test fun materialMalformedTupleLayoutReportsDiagnostic() = malformedLayout(false)

    @Test fun cupertinoMalformedTupleLayoutReportsDiagnostic() = malformedLayout(true)

    @Test fun materialNestedAuthoredScopeEditsPositionField() = nestedAuthoredScope(false)

    @Test fun cupertinoNestedAuthoredScopeEditsPositionField() = nestedAuthoredScope(true)

    @Test fun materialInvalidAuthoredPositionReportsDiagnostic() = invalidAuthoredPosition(false)

    @Test fun cupertinoInvalidAuthoredPositionReportsDiagnostic() = invalidAuthoredPosition(true)

    @Test fun materialAuthoredLayoutKeepsPositionIndex() = authoredLayout(false)

    @Test fun cupertinoAuthoredLayoutKeepsPositionIndex() = authoredLayout(true)

    @Test fun materialOmittedPositionErrorAppearsAtTuple() = omittedPositionError(false, omitted = true)

    @Test fun cupertinoOmittedPositionErrorAppearsAtTuple() = omittedPositionError(true, omitted = true)

    @Test fun materialShownPositionDoesNotDuplicateTupleError() = omittedPositionError(false, omitted = false)

    @Test fun cupertinoShownPositionDoesNotDuplicateTupleError() = omittedPositionError(true, omitted = false)

    @Test fun materialOmittedErrorsGroupByPosition() = omittedPositionValidationMode(false, hideSchema = false)

    @Test fun cupertinoOmittedErrorsGroupByPosition() = omittedPositionValidationMode(true, hideSchema = false)

    @Test fun materialOmittedErrorsRespectHiddenValidation() = omittedPositionValidationMode(false, hideSchema = true)

    @Test fun cupertinoOmittedErrorsRespectHiddenValidation() = omittedPositionValidationMode(true, hideSchema = true)

    @Test fun materialVerticalTupleStacksPositions() = tupleOrientation(false, true)

    @Test fun cupertinoVerticalTupleStacksPositions() = tupleOrientation(true, true)

    @Test fun materialDefaultTuplePlacesPositionsInRow() = tupleOrientation(false, false)

    @Test fun cupertinoDefaultTuplePlacesPositionsInRow() = tupleOrientation(true, false)

    @Test fun materialExplicitUniformTupleHasFixedPositions() = uniformTuple(false)

    @Test fun cupertinoExplicitUniformTupleHasFixedPositions() = uniformTuple(true)

    @Test fun materialInvalidTupleVariantReportsDiagnostic() = invalidVariant(false, false)

    @Test fun cupertinoInvalidTupleVariantReportsDiagnostic() = invalidVariant(true, false)

    @Test fun materialNonArrayTupleVariantReportsDiagnostic() = invalidVariant(false, true)

    @Test fun cupertinoNonArrayTupleVariantReportsDiagnostic() = invalidVariant(true, true)

    private fun nestedObjectSummary(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}]}"""
        ).jsonObject)
        val selected = ControlElement(scope = "#", options = mapOf(
            "summary" to Json.parseToJsonElement(
                """{"type":"Control","scope":"#/properties/contact/properties/city"}"""
            ),
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"contact":{"city":"Portland"}}]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
                    uischema = selected,
                )),
            )
        }
        onNodeWithText("Portland").assertExists()
    }

    private fun validatorSummaryError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","minLength":3}}}]}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"street":"x"}]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Cancel").performClick()
    }

    private fun complexSummaryIndicatorOptions(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        var parentEnabled by mutableStateOf<Boolean?>(null)
        var selectedEnabled by mutableStateOf<Boolean?>(null)
        var selectedCount by mutableStateOf(false)
        var globalEnabled by mutableStateOf<Boolean?>(null)
        var globalCount by mutableStateOf(false)
        var locale by mutableStateOf("en")
        var changes = 0
        fun parent() = ControlElement(
            scope = "#",
            options = parentEnabled?.let { mapOf("showValidationIndicator" to JsonPrimitive(it)) }
                ?: emptyMap(),
        )
        fun selected() = ControlElement(
            scope = "#",
            options = (selectedEnabled?.let {
                mapOf("showValidationIndicator" to JsonPrimitive(it))
            } ?: emptyMap()) + mapOf("showValidationIndicatorCount" to JsonPrimitive(selectedCount)),
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"street":"Old"}]"""),
                schema = schema,
                uischema = parent(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                uischemas = listOf(JsonFormsUISchemaRegistryEntry(
                    tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
                    uischema = selected(),
                )),
                config = mapOf("jsonformsExtended" to buildMap<String, Any?> {
                    globalEnabled?.let { put("showValidationIndicator", it) }
                    if (globalCount) put("showValidationIndicatorCount", true)
                }),
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/0", message = "Position is invalid"),
                    ErrorObject(instancePath = "/0/street", message = "Street is invalid"),
                    ErrorObject(instancePath = "", message = "Array is invalid"),
                ),
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key == "tuple.validationIndicator" && locale == "bg") "Грешки: $fallback"
                    else fallback
                }),
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithText("Position is invalid").assertExists()
        runOnIdle { globalEnabled = false }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithText("Position is invalid").assertExists()
        runOnIdle { parentEnabled = true }
        onNodeWithContentDescription("Contains errors").assertExists()
        runOnIdle { selectedEnabled = false }
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        onNodeWithText("Position is invalid").assertExists()
        runOnIdle { selectedEnabled = true; selectedCount = true }
        onNodeWithContentDescription("2 errors").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Грешки: 2 грешки").assertExists()
        runOnIdle { selectedEnabled = false }
        onNodeWithContentDescription("Грешки: 2 грешки").assertDoesNotExist()
        onNodeWithText("Position is invalid").assertExists()
        onNodeWithContentDescription("Редактирай Address").performClick()
        onNodeWithText("Street").assertExists()
        onNodeWithText("Отказ").performClick()
        runOnIdle { locale = "en"; selectedEnabled = true; selectedCount = false; globalCount = true }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("2 errors").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun tupleValidationPlacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","minLength":3},{"type":"integer","minimum":1}],"minItems":2,"additionalItems":false}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["x"]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
            )
        }
        onNodeWithText("ERR:minItems:").assertExists()
        onNodeWithText("ERR:minLength:/0").assertExists()
        onNodeWithText("ERR:type:/1").assertDoesNotExist()
        onNodeWithText("Item 2").assertExists()
    }

    private fun forbiddenTailValidation(cupertino: Boolean, modern: Boolean = false) = runComposeUiTest {
        val schemaJson = if (modern) {
            """{"type":"array","prefixItems":[{"type":"string"},{"type":"integer"}],"items":false}"""
        } else {
            """{"type":"array","items":[{"type":"string"},{"type":"integer"}],"additionalItems":false}"""
        }
        val schema = JsonSchema(Json.parseToJsonElement(schemaJson).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Code",3,true]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
            )
        }
        onNodeWithText(if (modern) "ERR:items:/2" else "ERR:additionalItems:/2").assertExists()
        onNodeWithText("Delete Item 3").assertExists()
    }

    private fun nestedForbiddenTailValidation(cupertino: Boolean, modern: Boolean = false) = runComposeUiTest {
        val tupleSchema = if (modern) {
            """{"type":"array","prefixItems":[{"type":"string"},{"type":"integer"}],"items":false}"""
        } else {
            """{"type":"array","items":[{"type":"string"},{"type":"integer"}],"additionalItems":false}"""
        }
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":$tupleSchema}}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"rows":["Code",3,true]}"""),
                schema = schema, uischema = ControlElement(scope = "#/properties/rows"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = ErrorTranslator { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
            )
        }
        onNodeWithText(if (modern) "ERR:items:/rows/2" else "ERR:additionalItems:/rows/2").assertExists()
        onNodeWithText("Delete Item 3").assertExists()
    }

    private fun arrayItemScopeSummary(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"array","title":"Contacts","items":{"type":"object","properties":{"name":{"type":"string"}}}}]}"""
        ).jsonObject)
        val selected = ControlElement(scope = "#", options = mapOf(
            "summary" to Json.parseToJsonElement("""{"type":"Control","scope":"#/properties/name"}"""),
        ))
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
            uischema = selected,
        ))
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[[{"name":"Alice"},{"name":"Bob"},{"name":"Cara"}]]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                uischemas = registry,
            )
        }
        onNodeWithText("Alice, Bob (+1 more)").assertExists()
    }

    private fun arraySummary(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"array","title":"Entries","items":{}}]}"""
        ).jsonObject)
        var hostOverride by mutableStateOf(false)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[[null,"A","B"]]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg", translate = if (hostOverride)
                    { key, fallback, values ->
                        if (key == "composite.summary.more") "и още ${values["count"]}" else fallback
                    } else null),
            )
        }
        onNodeWithText("A, B (+1 още)").assertExists()
        runOnIdle { hostOverride = true }
        onNodeWithText("A, B и още 1").assertExists()
    }

    private fun complexSummaryError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""[{"street":"Old"}]"""),
                schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/0/street", message = "Street is invalid"),
                    ErrorObject(instancePath = "", message = "Array is invalid"),
                ),
            )
        }
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithContentDescription("2 errors").assertDoesNotExist()
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Street").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private fun missingLaterComplexApply(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"},{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("[]"))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Edit Address").performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("Main")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["",{"street":"Main"}]"""), data)
    }

    private fun missingComplexOpen(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        val initial = Json.parseToJsonElement("[]")
        var data by mutableStateOf(initial)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
            )
        }
        onNodeWithText("Not set").assertExists()
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Street").assertExists()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals(initial, data)
        assertEquals(0, changes)
    }

    private fun tupleContainsClear(cupertino: Boolean, composed: Boolean = false,
        booleanContains: Boolean = false) = runComposeUiTest {
        val schemaText = if (booleanContains)
            """{"type":"array","items":[{"type":"array","title":"Tags","contains":true,"items":{"type":"string"}}],"additionalItems":false}"""
        else if (composed)
            """{"type":"array","items":[{"type":"array","title":"Tags","allOf":[{"minItems":1}],"items":{"type":"string"}}],"additionalItems":false}"""
        else """{"type":"array","items":[{"type":"array","title":"Tags","contains":{"const":"keep"},"items":{"type":"string"}}],"additionalItems":false}"""
        val schema = JsonSchema(Json.parseToJsonElement(schemaText).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""[["keep","other"]]"""))
        var restrict by mutableStateOf(true)
        var disableRemove by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "showEmptyButton" to JsonPrimitive(true),
                    "restrict" to JsonPrimitive(restrict))),
                config = mapOf("disableRemove" to disableRemove),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Edit Tags").performClick()
        onNodeWithText("Clear").assertIsNotEnabled()
        assertEquals(0, changes)
        runOnIdle { restrict = false; disableRemove = true }
        onNodeWithText("Clear").assertIsNotEnabled()
        runOnIdle { disableRemove = false }
        onNodeWithText("Clear").assertIsEnabled().performClick()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[[]]"""), data)
        assertEquals(1, changes)
    }

    private fun arrayPositionClear(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"array","title":"Phone numbers","items":{"type":"string"}}],"additionalItems":false}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""[["123","456"]]"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "showEmptyButton" to JsonPrimitive(true),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Edit Phone numbers").performClick()
        onNodeWithText("Clear").performClick()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("[[]]"), data)
        onNodeWithText("Delete Item 1").assertDoesNotExist()
    }

    private fun complexClear(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""[{"street":"Old"}]""")
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "showEmptyButton" to JsonPrimitive(true),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
            )
        }
        onNodeWithContentDescription("Edit Address").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Draft")
        onNodeWithText("Clear").performClick()
        onNodeWithText("Apply").assertExists()
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals(original, data)
        assertEquals(0, changes)
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Clear").performClick()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[{}]"""), data)
        assertEquals(1, changes)
    }

    private fun complexHostReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}]}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""[{"street":"Old"}]"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
            )
        }
        onNodeWithContentDescription("Edit Address").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Draft")
        runOnIdle { data = Json.parseToJsonElement("""[{"street":"Host"}]""") }
        waitForIdle()
        onNodeWithText("Apply").assertDoesNotExist()
        assertEquals(Json.parseToJsonElement("""[{"street":"Host"}]"""), data)
        assertEquals(0, changes)
    }

    private fun rankedComplexDetail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"},"city":{"type":"string","title":"City"}}}],"additionalItems":false}"""
        ).jsonObject)
        val selected = ControlElement(scope = "#", options = mapOf(
            "summary" to Json.parseToJsonElement("""{"type":"Control","scope":"#/properties/street"}"""),
            "detail" to Json.parseToJsonElement("""{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/properties/city"}]}"""),
            "showEmptyButton" to JsonPrimitive(false),
        ))
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, schemaPath, _ -> if (schemaPath == "#/items/0") 10 else -1 },
            uischema = selected,
        ))
        var data by mutableStateOf(Json.parseToJsonElement("""[{"street":"Old street","city":"Old city"}]"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "showEmptyButton" to JsonPrimitive(true),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                uischemas = registry,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Old street").assertExists()
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("City").assertExists()
        onNodeWithText("Street").assertDoesNotExist()
        onNodeWithText("Clear").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("New city")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[{"street":"Old street","city":"New city"}]"""), data)
    }

    private fun complexDraft(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","title":"Address","properties":{"street":{"type":"string","title":"Street"}}}],"additionalItems":false}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""[{"street":"Old"}]""")
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
            )
        }
        onNodeWithText("View details").assertExists()
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Street").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Cancelled")
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals(original, data)
        assertEquals(0, changes)
        onNodeWithContentDescription("Edit Address").performClick()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(0, changes)
        onNodeWithContentDescription("Edit Address").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("New")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[{"street":"New"}]"""), data)
        assertEquals(1, changes)
    }

    private fun missingSecond(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"},{"type":"integer","title":"Count"}],"additionalItems":false}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("[]"))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data },
            )
        }
        onNodeWithText("Count").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("30")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["",30]"""), data)
        assertEquals(1, changes)
    }

    private fun malformedLayout(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<TupleDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}]}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["A"]"""), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to JsonPrimitive("bad"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(TupleDiagnosticReporterConfigKey to TupleDiagnosticReporter { diagnostics.add(it) }),
            )
        }
        onNodeWithText("Unsupported tuple layout").assertExists()
        onNodeWithText("Code").assertExists()
        waitForIdle()
        assertEquals(listOf(TupleDiagnostic("tuple.detail.invalid", "", "#")), diagnostics)
    }

    private fun nestedAuthoredScope(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"object","properties":{"city":{"type":"string","title":"City"},"zip":{"type":"string","title":"ZIP"}}}],"additionalItems":false}"""
        ).jsonObject)
        val layout = Json.parseToJsonElement(
            """{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/items/0/properties/city"}]}"""
        )
        var data by mutableStateOf(Json.parseToJsonElement("""[{"city":"Old","zip":"123"}]"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to layout)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("City").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("New")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""[{"city":"New","zip":"123"}]"""), data)
    }

    private fun invalidAuthoredPosition(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<TupleDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string"}],"additionalItems":false}"""
        ).jsonObject)
        val layout = Json.parseToJsonElement(
            """{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/items/3"}]}"""
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["A"]"""), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to layout)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(TupleDiagnosticReporterConfigKey to TupleDiagnosticReporter { diagnostics.add(it) }),
            )
        }
        onNodeWithText("Unsupported position schema").assertExists()
        waitForIdle()
        assertEquals(listOf(TupleDiagnostic("tuple.position.invalid", "", "#/items/3")), diagnostics)
    }

    private fun authoredLayout(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"First"},{"type":"string","title":"Second"}],"additionalItems":false}"""
        ).jsonObject)
        val layout = Json.parseToJsonElement(
            """{"type":"VerticalLayout","elements":[{"type":"Group","label":"Ordered fields","elements":[{"type":"HorizontalLayout","elements":[{"type":"Control","scope":"#/items/1","label":"Reviewer second"},{"type":"Control","scope":"#/items/0"}]}]}]}"""
        )
        var data by mutableStateOf(Json.parseToJsonElement("""["A","B"]"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to layout)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Ordered fields").assertExists()
        val second = onNodeWithText("Reviewer second").fetchSemanticsNode().boundsInRoot
        val first = onNodeWithText("First").fetchSemanticsNode().boundsInRoot
        assertTrue(second.left < first.left, "Authored layout should reverse visual position order")
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("C")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["A","C"]"""), data)
    }

    private fun omittedPositionError(cupertino: Boolean, omitted: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"First"},{"type":"string","title":"Second","minLength":3}],"additionalItems":false}"""
        ).jsonObject)
        val controls = if (omitted) {
            """[{"type":"Control","scope":"#/items/0"}]"""
        } else {
            """[{"type":"Control","scope":"#/items/0"},{"type":"Control","scope":"#/items/1"}]"""
        }
        val layout = Json.parseToJsonElement("""{"type":"VerticalLayout","elements":$controls}""")
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Code","x"]"""), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to layout)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(
                    locale = "bg",
                    translate = { key, fallback, values ->
                        if (key == "tuple.omittedPositionError") "Скрита грешка: " + values["label"] + ": " + values["message"]
                        else fallback
                    },
                    translateError = ErrorTranslator { error, _, _ -> "ERR:" + error.keyword + ":" + error.instancePath },
                ),
            )
        }
        if (omitted) onNodeWithText("Скрита грешка: Second: ERR:minLength:/1").assertExists()
        else {
            onNodeWithText("Скрита грешка: Second: ERR:minLength:/1").assertDoesNotExist()
            onNodeWithText("ERR:minLength:/1").assertExists()
        }
    }

    private fun omittedPositionValidationMode(cupertino: Boolean, hideSchema: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"First"},{"type":"string","title":"Second","minLength":3},{"type":"integer","title":"Third","minimum":1}],"additionalItems":false}"""
        ).jsonObject)
        val layout = Json.parseToJsonElement(
            """{"type":"VerticalLayout","elements":[{"type":"Control","scope":"#/items/0"}]}"""
        )
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Code","x",0]"""), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("detail" to layout)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = if (hideSchema) ValidationMode.ValidateAndHide else ValidationMode.ValidateAndShow,
                additionalErrors = listOf(ErrorObject(instancePath = "/2", keyword = "host", message = "server bad")),
                i18n = JsonFormsI18nState(
                    translateError = ErrorTranslator { error, _, _ -> "ERR:" + error.keyword + ":" + error.instancePath },
                ),
            )
        }
        if (hideSchema) {
            onNodeWithText("Second: ERR:minLength:/1", substring = true).assertDoesNotExist()
            onNodeWithText("Third: ERR:host:/2", substring = true).assertExists()
        } else {
            onNodeWithText("Second: ERR:minLength:/1", substring = true).assertExists()
            onNodeWithText("Third: ERR:minimum:/2", substring = true).assertExists()
            onNodeWithText("ERR:host:/2", substring = true).assertExists()
        }
    }

    private fun tupleOrientation(cupertino: Boolean, vertical: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Alpha"},{"type":"string","title":"Beta"}],"additionalItems":false}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["A","B"]"""), schema = schema,
                uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("tuple" to mapOf("vertical" to vertical))),
            )
        }
        val first = onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        val second = onNodeWithText("Beta").fetchSemanticsNode().boundsInRoot
        if (vertical) assertTrue(second.top >= first.bottom, "Vertical tuple positions should stack")
        else assertTrue(second.left >= first.right, "Default tuple positions should form a row")
    }

    private fun uniformTuple(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"number"},"minItems":3,"maxItems":3}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("[1,2,3]"), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("variant" to JsonPrimitive("tuple"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithText("Item 1").assertExists()
        onNodeWithText("Item 2").assertExists()
        onNodeWithText("Item 3").assertExists()
        onNodeWithText("Add item").assertDoesNotExist()
        onNodeWithText("Delete Item 3").assertDoesNotExist()
    }

    private fun invalidVariant(cupertino: Boolean, nonArray: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<TupleDiagnostic>()
        val schema = JsonSchema(Json.parseToJsonElement(if (nonArray)
            """{"type":"string"}""" else """{"type":"array","items":{"type":"number"},"minItems":2}"""
        ).jsonObject)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement(if (nonArray) "\"text\"" else "[1,2]"), schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("variant" to JsonPrimitive("tuple"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(TupleDiagnosticReporterConfigKey to TupleDiagnosticReporter { diagnostics.add(it) }),
            )
        }
        onNodeWithText("Tuple configuration is unsupported").assertExists()
        waitForIdle()
        assertEquals(listOf(TupleDiagnostic(
            if (nonArray) "variant.inapplicable" else "variant.unsupported", "",
        )), diagnostics)
    }

    private fun schemaRebind(cupertino: Boolean) = runComposeUiTest {
        val first = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        val second = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"},{"type":"string","title":"Fixed"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["A","B"]""")
        var schema by mutableStateOf(first)
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { schema = second }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        onNodeWithText("Delete Item 2").assertDoesNotExist()
        runOnIdle { schema = first }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(original, data)
    }

    private fun freeTail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":true} """
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["A",42]"""))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Integer").assertExists()
        onNodeWithText("Add item").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["A",42,null]"""), data)
    }

    private fun importedWrongShape(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"},"maxItems":2}}}"""
        ).jsonObject)
        val initial = Json.parseToJsonElement("""{"rows":7}""")
        var data by mutableStateOf(initial)
        var locale by mutableStateOf("en")
        var changes = 0
        var validity: CombinedValidityStatus? = null
        var errors: List<ErrorObject> = emptyList()
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#/properties/rows"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; errors = it.errors; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Invalid current value: 7").assertExists()
        assertEquals(initial, data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onNodeWithText("Невалидна текуща стойност: 7").assertExists()
        onNodeWithText("Допълнителни елементи").assertExists()
        onNodeWithText("Добави елемент").assertIsNotEnabled()
        runOnIdle { locale = "de-DE" }
        waitForIdle()
        onNodeWithText("Invalid current value: 7").assertExists()
        onNodeWithText("Add item").assertIsNotEnabled()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("A")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":["A"]}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity, errors.toString())
        assertEquals(1, changes)
        onNodeWithText("Invalid current value: 7").assertDoesNotExist()
        onNodeWithText("Add item").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":["A",""]}"""), data)
        assertEquals(2, changes)
        onNodeWithText("Add item").assertIsNotEnabled()
    }

    private fun bulgarianDefaultActions(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        val initial = Json.parseToJsonElement("""["A","B"]""")
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                i18n = JsonFormsI18nState(locale = "bg"),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Допълнителни елементи").assertExists()
        onNodeWithText("Добави елемент").assertExists()
        onNodeWithText("Изтрий Елемент 2").performClick()
        onNodeWithText("Изтриване на стойността?").assertExists()
        onNodeWithText("Не").performClick()
        waitForIdle()
        assertEquals(initial, data)
    }

    private fun translatedActions(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["A","B"]""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                i18n = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
                    when (key) {
                        "tuple.position" -> "Позиция ${values["position"]}"
                        "tuple.delete" -> "Изтрий ${values["label"]}"
                        "tuple.additionalItems" -> "Допълнителни позиции"
                        "tuple.add" -> "Добави"
                        "confirm.delete.title" -> "Изтриване?"
                        "confirm.accept" -> "Да"
                        "confirm.decline" -> "Не"
                        else -> fallback
                    }
                }),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Допълнителни позиции").assertExists()
        onNodeWithText("Добави").assertExists()
        onNodeWithText("Изтрий Позиция 2").performClick()
        onNodeWithText("Изтриване?").assertExists()
        onNodeWithText("Не").performClick()
        waitForIdle()
        assertEquals(original, data)
    }

    private fun missingFixed(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("[]"))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("A")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["A"]"""), data)
        onNodeWithText("Delete Item 1").assertDoesNotExist()
    }

    private fun hostReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string"}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""["A","B"]""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""["A","C"]""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(original, data)
    }

    private fun importedFixture(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-tuple-control" }
        setContent {
            JsonForms(
                data = fixture.data, schema = fixture.schema, uischema = fixture.uischema,
                uischemas = fixture.uischemas,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = fixture.config,
            )
        }
        onNodeWithText("Drop-off coordinates").assertExists()
        onNodeWithText("X").assertExists()
        onNodeWithText("Y").assertExists()
        onNodeWithText("Ground position").assertExists()
        val longitude = onNodeWithText("Longitude").fetchSemanticsNode().boundsInRoot
        val latitude = onNodeWithText("Latitude").fetchSemanticsNode().boundsInRoot
        assertTrue(longitude.left < latitude.left)
    }

    private fun typedTail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":[{"type":"string","title":"Code"}],"additionalItems":{"type":"string","title":"Note"}}"""
        ).jsonObject)
        val initial = Json.parseToJsonElement("""["A","B"]""")
        var data by mutableStateOf(initial)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Code").assertExists()
        onNodeWithText("Delete Item 1").assertDoesNotExist()
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        waitForIdle()
        assertEquals(initial, data)
        onNodeWithText("Delete Item 2").performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["A"]"""), data)
        onNodeWithText("Add item").performClick()
        waitForIdle()
        assertEquals(2, (data as kotlinx.serialization.json.JsonArray).size)
    }

    private fun forbiddenTail(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","prefixItems":[{"type":"string","title":"Code"}],"items":false}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["A","legacy"]"""))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Add item").assertDoesNotExist()
        onNodeWithText("Delete Item 1").assertDoesNotExist()
        onNodeWithText("Delete Item 2").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["A"]"""), data)
    }
}
