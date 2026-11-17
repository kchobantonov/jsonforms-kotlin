package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class PreTouchErrorsFixtureUiTest {
    @Test fun importedFixtureKeepsSevenStructuredErrors() {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        val errors = OptimumCodeJsonSchemaValidator().compile(requireNotNull(example.schema)).validate(example.data)
        assertEquals(7, errors.size)
        assertEquals(4, errors.count { it.keyword == "required" })
        assertEquals(1, errors.count { it.keyword == "pattern" })
        assertEquals(1, errors.count { it.keyword == "minLength" })
        assertEquals(1, errors.count { it.keyword == "minItems" })
    }
    @Test fun materialFiltersOnlyConfiguredKeywords() = selectiveFilter(false)
    @Test fun cupertinoFiltersOnlyConfiguredKeywords() = selectiveFilter(true)
    @Test fun materialBlurRevealsOnlyVisitedField() = blur(false)
    @Test fun cupertinoBlurRevealsOnlyVisitedField() = blur(true)
    @Test fun materialFilterNeverOverridesHiddenValidation() = hiddenValidation(false)
    @Test fun cupertinoFilterNeverOverridesHiddenValidation() = hiddenValidation(true)
    @Test fun materialHostErrorFilteringKeepsOtherKeyword() = additionalErrors(false)
    @Test fun cupertinoHostErrorFilteringKeepsOtherKeyword() = additionalErrors(true)
    @Test fun materialArrayItemCityFiltersUntilBlur() = arrayChild(false)
    @Test fun cupertinoArrayItemCityFiltersUntilBlur() = arrayChild(true)
    @Test fun materialLiveBulgarianMessagesRemainFiltered() = localized(false)
    @Test fun cupertinoLiveBulgarianMessagesRemainFiltered() = localized(true)
    @Test fun materialNoValidationKeepsSchemaFeedbackHidden() = noValidation(false)
    @Test fun cupertinoNoValidationKeepsSchemaFeedbackHidden() = noValidation(true)
    @Test fun materialTouchedFieldSurvivesHostReplacement() = touchLifecycle(false)
    @Test fun cupertinoTouchedFieldSurvivesHostReplacement() = touchLifecycle(true)

    private fun errorLabel(error: com.chobantonov.jsonfroms.core.validation.ErrorObject): String =
        "ERR:${error.keyword}:${error.params["missingProperty"] ?: error.instancePath}"

    private fun selectiveFilter(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var filtering by mutableStateOf(false)
        var keywords by mutableStateOf(listOf("required"))
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf(
                    "enableFilterErrorsBeforeTouch" to filtering,
                    "filterErrorKeywordsBeforeTouch" to keywords,
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onNodeWithText("ERR:required:legalName").assertExists()
        onNodeWithText("ERR:required:contactEmail").assertExists()
        onNodeWithText("ERR:pattern:/memberNumber").assertExists()
        runOnIdle { filtering = true }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasText("ERR:required:contactEmail")).assertCountEquals(0)
        onNodeWithText("ERR:pattern:/memberNumber").assertExists()

        onNodeWithText("Field override").performClick()
        onNodeWithText("ERR:required:internalCode").assertExists()
        onNodeWithText("Other validation").performClick()
        onNodeWithText("ERR:minLength:/notes").assertExists()
        onNodeWithText("Collection summary").performClick()
        onNodeWithText("ERR:minItems:/offices").assertExists()
        runOnIdle { keywords = emptyList() }
        onNodeWithText("ERR:minItems:/offices").assertExists()
        onNodeWithText("Other validation").performClick()
        onAllNodes(hasText("ERR:minLength:/notes")).assertCountEquals(0)
        onNodeWithText("Required fields").performClick()
        onAllNodes(hasText("ERR:pattern:/memberNumber")).assertCountEquals(0)
        assertEquals(0, changes)
    }

    private fun blur(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasSetTextAction() and hasText("12ab", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("ERR:required:legalName").assertExists()
        onAllNodes(hasText("ERR:required:contactEmail")).assertCountEquals(0)
        assertEquals(0, changes)
    }

    private fun hiddenValidation(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var filtering by mutableStateOf(false)
        var validationMode by mutableStateOf(ValidationMode.ValidateAndShow)
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to filtering),
                validationMode = validationMode,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onNodeWithText("ERR:required:legalName").assertExists()
        runOnIdle { validationMode = ValidationMode.ValidateAndHide }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasText("ERR:pattern:/memberNumber")).assertCountEquals(0)
        runOnIdle { filtering = true }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasText("ERR:pattern:/memberNumber")).assertCountEquals(0)
        runOnIdle { validationMode = ValidationMode.ValidateAndShow }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onNodeWithText("ERR:pattern:/memberNumber").assertExists()
        assertEquals(0, changes)
    }

    private fun additionalErrors(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to true),
                additionalErrors = listOf(
                    ErrorObject(instancePath = "/memberNumber", keyword = "required", message = "Host required"),
                ),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    if (error.message == "Host required") "HOST:required:memberNumber"
                    else errorLabel(error)
                }),
                onChange = { changes++ },
            )
        }
        onAllNodes(hasText("HOST:required:memberNumber", substring = true)).assertCountEquals(0)
        onNodeWithText("ERR:pattern:/memberNumber").assertExists()
        assertEquals(0, changes)
    }

    private fun arrayChild(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onNodeWithText("Collection summary").performClick()
        onNodeWithText("ERR:minItems:/offices").assertExists()
        onAllNodes(hasText("ERR:required:city")).assertCountEquals(0)
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasText("ERR:required:city")).assertCountEquals(0)
        onAllNodes(hasSetTextAction() and hasText("PDX", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("ERR:required:city").assertExists()
        assertEquals(0, changes)
    }

    private fun localized(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        val demoState = StateProps(data = example.data, localizedTranslations = example.localizedTranslations)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = exampleI18nForLocale(demoState, locale),
                onChange = { changes++ },
            )
        }
        onNodeWithText("A member number is exactly seven digits.").assertExists()
        onAllNodes(hasText("This field is required.")).assertCountEquals(0)
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Задължителни полета").assertExists()
        onNodeWithText("Членският номер е точно седем цифри.").assertExists()
        onAllNodes(hasText("Полето е задължително.")).assertCountEquals(0)
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasSetTextAction() and hasText("12ab", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("Полето е задължително.").assertExists()
        assertEquals(0, changes)
    }

    private fun noValidation(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var filtering by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(
                data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to filtering),
                validationMode = ValidationMode.NoValidation,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasText("ERR:pattern:/memberNumber")).assertCountEquals(0)
        runOnIdle { filtering = true }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        onAllNodes(hasText("ERR:pattern:/memberNumber")).assertCountEquals(0)
        assertEquals(0, changes)
    }

    private fun touchLifecycle(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-pre-touch-errors" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = example.schema, uischema = example.uischema,
                config = example.config + mapOf("enableFilterErrorsBeforeTouch" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ -> errorLabel(error) }),
                onChange = { changes++ },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onAllNodes(hasSetTextAction() and hasText("12ab", substring = true)).onFirst()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithText("ERR:required:legalName").assertExists()
        runOnIdle { data = JsonObject(example.data.jsonObject + ("legalName" to JsonPrimitive("Ada"))) }
        onAllNodes(hasText("ERR:required:legalName")).assertCountEquals(0)
        runOnIdle { data = example.data }
        onNodeWithText("ERR:required:legalName").assertExists()
        assertEquals(0, changes)
    }
}
