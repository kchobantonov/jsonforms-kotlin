package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performMouseInput
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ContainerValidationIndicatorFixtureUiTest {
    @Test fun materialCollapsedGroupAndCategoriesReflectImportedErrors() = initial(false)
    @Test fun cupertinoCollapsedGroupAndCategoriesReflectImportedErrors() = initial(true)
    @Test fun materialCompositeCellsIsolateInvalidRows() = compositeCells(false)
    @Test fun cupertinoCompositeCellsIsolateInvalidRows() = compositeCells(true)
    @Test fun materialUnboundLabelsUseBulgarianCatalog() = localizedLabels(false)
    @Test fun cupertinoUnboundLabelsUseBulgarianCatalog() = localizedLabels(true)
    @Test fun materialCertificationsSuppressMarkerButKeepOwnError() = certifications(false)
    @Test fun cupertinoCertificationsSuppressMarkerButKeepOwnError() = certifications(true)
    @Test fun materialCertificationSummaryExpandsWithoutWriting() = certificationSummary(false)
    @Test fun cupertinoCertificationSummaryExpandsWithoutWriting() = certificationSummary(true)
    @Test fun materialCountAndVisibilityFollowGlobalOptions() = countAndVisibility(false)
    @Test fun cupertinoCountAndVisibilityFollowGlobalOptions() = countAndVisibility(true)
    @Test fun materialInvalidCompositeDetailsShowFieldErrors() = compositeDetails(false)
    @Test fun cupertinoInvalidCompositeDetailsShowFieldErrors() = compositeDetails(true)
    @Test fun materialPreTouchPhoneKeepsGroupNavigationCue() = preTouchPhone(false)
    @Test fun cupertinoPreTouchPhoneKeepsGroupNavigationCue() = preTouchPhone(true)
    @Test fun materialCombinatorDetailsExposeImportedErrors() = combinatorDetails(false)
    @Test fun cupertinoCombinatorDetailsExposeImportedErrors() = combinatorDetails(true)
    @Test fun materialAllOfCellRepairPublishesOneWrite() = allOfRepair(false)
    @Test fun cupertinoAllOfCellRepairPublishesOneWrite() = allOfRepair(true)
    @Test fun materialAnyOfTabRepairPublishesOneWrite() = anyOfRepair(false)
    @Test fun cupertinoAnyOfTabRepairPublishesOneWrite() = anyOfRepair(true)
    @Test fun materialOneOfBranchRepairPublishesOneWrite() = oneOfRepair(false)
    @Test fun cupertinoOneOfBranchRepairPublishesOneWrite() = oneOfRepair(true)
    @Test fun materialCombinatorOwnErrorsAppearAfterTouch() = combinatorPreTouch(false)
    @Test fun cupertinoCombinatorOwnErrorsAppearAfterTouch() = combinatorPreTouch(true)
    @Test fun materialGroupDataTooltipOpensOnHover() = dataHover(false)
    @Test fun cupertinoGroupDataTooltipOpensOnHover() = dataHover(true)

    private fun dataHover(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Contains data").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Contains data").assertExists()
    }

    private fun combinatorDetails(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("Edit OneOf contact")).onFirst().performClick()
        onAllNodes(hasText("ERR:oneOf:/applicants/0/oneOfContact", substring = true))
            .onFirst().assertExists()
        onNodeWithText("Cancel").performClick()
        onAllNodes(hasContentDescription("Edit AnyOf contact")).onFirst().performClick()
        onAllNodes(hasText("ERR:required:/applicants/0/anyOfContact", substring = true))
            .onFirst().assertExists()
        onAllNodes(hasText("ERR:anyOf:/applicants/0/anyOfContact", substring = true))
            .onFirst().assertExists()
        onNodeWithText("Cancel").performClick()
        onAllNodes(hasContentDescription("Edit AllOf contact")).onFirst().performClick()
        onAllNodes(hasText("ERR:minLength:/applicants/0/allOfContact/phone", substring = true))
            .onFirst().assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(0, changes)
    }

    private fun allOfRepair(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("AllOf contact contains errors", substring = true))
            .assertCountEquals(1)
        onAllNodes(hasContentDescription("Edit AllOf contact")).onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("12", substring = true)).onFirst()
            .performTextReplacement("5550100")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        onAllNodes(hasContentDescription("AllOf contact contains errors", substring = true))
            .assertCountEquals(0)
        val applicants = data.jsonObject["applicants"] as JsonArray
        val allOf = applicants[0].jsonObject["allOfContact"]!!.jsonObject
        assertEquals(JsonPrimitive("5550100"), allOf["phone"])
        assertEquals(1, changes)
    }

    private fun anyOfRepair(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("AnyOf contact contains errors", substring = true))
            .assertCountEquals(1)
        onAllNodes(hasContentDescription("Edit AnyOf contact")).onFirst().performClick()
        onNodeWithText("Phone available").performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("5550100")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        onAllNodes(hasContentDescription("AnyOf contact contains errors", substring = true))
            .assertCountEquals(0)
        val applicants = data.jsonObject["applicants"] as JsonArray
        val anyOf = applicants[0].jsonObject["anyOfContact"]!!.jsonObject
        assertEquals(JsonPrimitive("5550100"), anyOf["phone"])
        assertEquals(1, changes)
    }

    private fun oneOfRepair(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("OneOf contact contains errors", substring = true))
            .assertCountEquals(1)
        onAllNodes(hasContentDescription("Edit OneOf contact")).onFirst().performClick()
        onAllNodes(hasText("Email contact")).onFirst().performClick()
        onNodeWithText("Phone contact").performClick()
        if (onAllNodes(hasText("Change this branch?")).fetchSemanticsNodes().isNotEmpty()) {
            onNodeWithText("Yes").performClick()
        }
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("5550100")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        onAllNodes(hasContentDescription("OneOf contact contains errors", substring = true))
            .assertCountEquals(0)
        val applicants = data.jsonObject["applicants"] as JsonArray
        val oneOf = applicants[0].jsonObject["oneOfContact"]!!.jsonObject
        assertEquals(JsonPrimitive("5550100"), oneOf["phone"])
        assertEquals(null, oneOf["email"])
        assertEquals(1, changes)
    }

    private fun combinatorPreTouch(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("oneOf", "anyOf"),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("OneOf contact contains errors", substring = true))
            .assertCountEquals(1)
        onAllNodes(hasContentDescription("Edit OneOf contact")).onFirst().performClick()
        onAllNodes(hasText("ERR:oneOf:/applicants/0/oneOfContact", substring = true))
            .assertCountEquals(0)
        onAllNodes(hasText("Email contact")).onFirst().performClick()
        onAllNodes(hasText("Email contact")).onLast().performClick()
        onAllNodes(hasText("ERR:oneOf:/applicants/0/oneOfContact", substring = true))
            .onFirst().assertExists()
        onNodeWithText("Cancel").performClick()

        onAllNodes(hasContentDescription("Edit AnyOf contact")).onFirst().performClick()
        onAllNodes(hasText("ERR:anyOf:/applicants/0/anyOfContact", substring = true))
            .assertCountEquals(0)
        onNodeWithText("Phone available").performClick()
        onAllNodes(hasText("ERR:anyOf:/applicants/0/anyOfContact", substring = true))
            .onFirst().assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(0, changes)
    }

    private fun preTouchPhone(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf(
                    "enableFilterErrorsBeforeTouch" to true,
                    "filterErrorKeywordsBeforeTouch" to listOf("minLength"),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Personal details, Contains errors").assertExists()
        onNodeWithContentDescription("Contains errors").assertExists()
        onNodeWithText("Emergency contact").performClick()
        onAllNodes(hasText("ERR:minLength:/emergencyContact/phone", substring = true))
            .assertCountEquals(0)
        onAllNodes(hasSetTextAction() and hasText("112", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasSetTextAction() and hasText("John Doe", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasText("ERR:minLength:/emergencyContact/phone", substring = true))
            .onFirst().assertExists()
        assertEquals(0, changes)
    }

    private fun compositeDetails(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ })
        }
        onNodeWithText("Composite cells").performClick()
        onAllNodes(hasContentDescription("Edit Contact")).onFirst().performClick()
        onAllNodes(hasText("City*", substring = true)).onFirst().assertExists()
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().count {
            it.config.contains(SemanticsProperties.Error) &&
                it.config[SemanticsProperties.Error] == "ERR:required:/applicants/0/contact"
        })
        onNodeWithText("Cancel").performClick()
        onAllNodes(hasContentDescription("Edit Experience")).onFirst().performClick()
        assertEquals(2, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().count {
            it.config.contains(SemanticsProperties.Error) &&
                it.config[SemanticsProperties.Error] == "ERR:required:/applicants/0/experience/0"
        })
        onNodeWithText("Cancel").performClick()
        assertEquals(0, changes)
    }

    private fun countAndVisibility(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var count by mutableStateOf(false)
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + ("jsonformsExtended" to mapOf(
                    "showValidationIndicator" to true,
                    "showValidationIndicatorCount" to count,
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                validationMode = mode,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Personal details, Contains errors").assertExists()
        runOnIdle { count = true }
        onNodeWithContentDescription("Personal details, 1 error").assertExists()
        onNodeWithContentDescription("Compliance, Contains errors").assertDoesNotExist()
        runOnIdle { mode = ValidationMode.ValidateAndHide }
        onNodeWithContentDescription("Personal details, 1 error").assertDoesNotExist()
        onNodeWithContentDescription("Compliance, Contains errors").assertDoesNotExist()
        runOnIdle { mode = ValidationMode.ValidateAndShow; count = false }
        onNodeWithContentDescription("Personal details, Contains errors").assertExists()
        assertEquals(0, changes)
    }

    private fun certifications(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { changes++ })
        }
        onNodeWithText("Compliance").performClick()
        onNodeWithContentDescription("Compliance, Contains errors").assertExists()
        onNodeWithText("Certifications").assertExists()
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        runOnIdle {
            data = JsonObject(data.jsonObject + ("certifications" to JsonArray(emptyList())))
        }
        onNodeWithText("ERR:minItems:/certifications").assertExists()
        assertEquals(0, changes)
    }

    private fun certificationSummary(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        val demoState = StateProps(data = example.data,
            localizedTranslations = example.localizedTranslations)
        var locale by mutableStateOf("en")
        var changes = 0
        // The source UI schema disables this marker; enable it here to exercise its documented summary.
        val ui = ControlElement("#/properties/certifications", options = mapOf(
            "showValidationIndicator" to JsonPrimitive(true),
            "showValidationIndicatorCount" to JsonPrimitive(true),
            "elementLabelProp" to JsonPrimitive("name"),
        ))
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = ui,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = exampleI18nForLocale(demoState, locale),
                onChange = { changes++ })
        }
        onNodeWithContentDescription("5 errors").performClick()
        onNodeWithText("Show 2 more").assertExists()
        onNodeWithText("Row 4 · Certificate: This value is too short.")
            .assertDoesNotExist()
        onNodeWithText("Show 2 more").performClick()
        onNodeWithText("Row 4 · Certificate: This value is too short.").assertExists()
        onNodeWithText("Row 4 · Expires on:", substring = true).assertExists()
        onNodeWithText("Show less").performClick()
        onNodeWithText("Row 4 · Certificate: This value is too short.")
            .assertDoesNotExist()
        onNodeWithText("Close").performClick()
        onNodeWithContentDescription("5 errors", substring = true).assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("5 грешки").performClick()
        onNodeWithText("Покажи още 2").performClick()
        onNodeWithText("Ред 4 · Сертификат: Стойността е твърде къса.").assertExists()
        onNodeWithText("Ред 4 · Валиден до:", substring = true).assertExists()
        onNodeWithText("Покажи по-малко").performClick()
        onNodeWithText("Затвори").performClick()
        assertEquals(0, changes)
    }

    private fun localizedLabels(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var locale by mutableStateOf("bg-BG")
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    example.localizedTranslations[locale.substringBefore('-')]?.get(key) ?: fallback
                }),
                onChange = { changes++ })
        }
        onNodeWithText("Лични данни").assertExists()
        onNodeWithText("Лице за контакт при спешност").assertExists()
        onNodeWithText("Съответствие").assertExists()
        onNodeWithContentDescription("Разделът съдържа данни")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Разделът съдържа данни").assertExists()
        runOnIdle { locale = "en" }
        onNodeWithText("Personal details").assertExists()
        onNodeWithText("Emergency contact").assertExists()
        onNodeWithText("Compliance").assertExists()
        onNodeWithContentDescription("Section contains data").assertExists()
        assertEquals(0, changes)
    }

    private fun compositeCells(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Composite cells").performClick()
        onNodeWithText("Composite cells").assertExists()
        assertEquals(1, onAllNodes(hasContentDescription("Contact contains errors", substring = true))
            .fetchSemanticsNodes().size)
        assertEquals(1, onAllNodes(hasContentDescription("Experience contains errors", substring = true))
            .fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(hasContentDescription("Edit Contact")).fetchSemanticsNodes().size)
        assertEquals(2, onAllNodes(hasContentDescription("Edit Experience")).fetchSemanticsNodes().size)
        for (name in listOf("OneOf contact", "AnyOf contact", "AllOf contact")) {
            assertEquals(1, onAllNodes(hasContentDescription("$name contains errors", substring = true))
                .fetchSemanticsNodes().size, name)
            assertEquals(2, onAllNodes(hasContentDescription("Edit $name"))
                .fetchSemanticsNodes().size, name)
        }
    }

    private fun initial(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-container-validation-indicator" }
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Personal details, Contains errors").assertExists()
        onNodeWithContentDescription("Compliance, Contains errors").assertExists()
        onNodeWithText("Emergency contact").assertExists()
        onNodeWithContentDescription("Contains errors").assertExists()
        onAllNodes(hasText("Phone number", substring = true, ignoreCase = true)).assertCountEquals(0)
        assertEquals(0, changes)

        onNodeWithText("Emergency contact").performClick()
        onAllNodes(hasText("Phone number", substring = true, ignoreCase = true)).onFirst().assertExists()
        runOnIdle {
            val contact = data.jsonObject["emergencyContact"]!!.jsonObject
            data = JsonObject(data.jsonObject + ("emergencyContact" to
                JsonObject(contact + ("phone" to JsonPrimitive("1234567")))))
        }
        onNodeWithContentDescription("Personal details, Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Compliance, Contains errors").assertExists()
        onNodeWithContentDescription("Contains errors").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
