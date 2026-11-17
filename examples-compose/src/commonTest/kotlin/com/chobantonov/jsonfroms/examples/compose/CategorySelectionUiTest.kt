package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class CategorySelectionUiTest {
    @Test fun materialTabsKeepIdentityThroughRuleChanges() = scenario(false, false)
    @Test fun cupertinoTabsKeepIdentityThroughRuleChanges() = scenario(true, false)
    @Test fun materialStepsKeepIdentityThroughRuleChanges() = scenario(false, true)
    @Test fun cupertinoStepsKeepIdentityThroughRuleChanges() = scenario(true, true)
    @Test fun materialMissingInitialReportsAuthorDiagnostic() = missingInitial(false)
    @Test fun cupertinoMissingInitialReportsAuthorDiagnostic() = missingInitial(true)
    @Test fun materialDuplicateInitialReportsWithoutResolvingByPosition() = duplicateInitial(false)
    @Test fun cupertinoDuplicateInitialReportsWithoutResolvingByPosition() = duplicateInitial(true)
    @Test fun materialNamedSelectionSurvivesTranslatedLabels() = translatedLabelsKeepNames(false)
    @Test fun cupertinoNamedSelectionSurvivesTranslatedLabels() = translatedLabelsKeepNames(true)
    @Test fun materialSameNameInSeparateCategorizationsIsValid() = sameNameInSeparateCategorizations(false)
    @Test fun cupertinoSameNameInSeparateCategorizationsIsValid() = sameNameInSeparateCategorizations(true)
    @Test fun materialStepButtonsFollowVisibleBounds() = stepButtons(false)
    @Test fun cupertinoStepButtonsFollowVisibleBounds() = stepButtons(true)
    @Test fun materialAllHiddenHasNoStalePanel() = allHidden(false)
    @Test fun cupertinoAllHiddenHasNoStalePanel() = allHidden(true)
    @Test fun materialTabsRetainSelectionThroughReorderAndOrientation() = reorderAndOrient(false, false)
    @Test fun cupertinoTabsRetainSelectionThroughReorderAndOrientation() = reorderAndOrient(true, false)
    @Test fun materialStepsRetainSelectionThroughReorderAndOrientation() = reorderAndOrient(false, true)
    @Test fun cupertinoStepsRetainSelectionThroughReorderAndOrientation() = reorderAndOrient(true, true)
    @Test fun materialTabsFallBackAfterRemoval() = removed(false, false)
    @Test fun cupertinoTabsFallBackAfterRemoval() = removed(true, false)
    @Test fun materialStepsFallBackAfterRemoval() = removed(false, true)
    @Test fun cupertinoStepsFallBackAfterRemoval() = removed(true, true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"a":{"type":"string"},"b":{"type":"string"},"show":{"type":"boolean"}}}"""
    ).jsonObject)
    private val secondRule = Rule(
        effect = RuleEffect.SHOW,
        condition = SchemaBasedCondition(
            scope = "#/properties/show",
            schema = buildJsonSchema { put("const", true) },
            failWhenUndefined = true,
        ),
    )

    private fun ui(stepper: Boolean, initial: String, showNavButtons: Boolean = false) = CategorizationElement(
        options = mapOf("initial" to JsonPrimitive(initial)) +
            (if (stepper) mapOf("variant" to JsonPrimitive("stepper")) else emptyMap()) +
            (if (showNavButtons) mapOf("showNavButtons" to JsonPrimitive(true)) else emptyMap()),
        elements = listOf(
            CategoryElement(label = "First", name = "first",
                elements = listOf(ControlElement(scope = "#/properties/a"))),
            CategoryElement(label = "Second", name = "second", rule = secondRule,
                elements = listOf(ControlElement(scope = "#/properties/b"))),
        ),
    )

    private fun scenario(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(stepper, "second"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("B").assertExists()
        if (stepper) onNodeWithText("Next").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("A").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        assertEquals(0, changes)
    }

    private fun stepButtons(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ui(stepper = true, initial = "first", showNavButtons = true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/b", message = "server")),
                onChange = { changes++ })
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("Previous").assertIsNotEnabled()
        onNodeWithText("Next").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        onNodeWithText("server").assertExists()
        onNodeWithText("Next").assertIsNotEnabled()
        onNodeWithText("Previous").performTouchInput { click() }
        onNodeWithText("A").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("Next").assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun allHidden(cupertino: Boolean) = runComposeUiTest {
        val original = ui(stepper = false, initial = "first")
        val first = original.elements.first() as CategoryElement
        val layout = original.copy(elements = listOf(first.copy(rule = secondRule), original.elements[1]))
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("A").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("First").assertDoesNotExist()
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("A").assertDoesNotExist()
        onNodeWithText("B").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithText("A").assertExists()
    }

    private fun reorderAndOrient(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        val original = ui(stepper, initial = "first")
        var layout by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"show":true}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        runOnIdle { layout = original.copy(elements = original.elements.reversed()) }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        runOnIdle {
            layout = layout.copy(options = layout.options + ("vertical" to JsonPrimitive(true)))
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("First").performTouchInput { click() }
        onNodeWithText("A").assertExists()
        assertEquals(0, changes)
    }

    private fun removed(cupertino: Boolean, stepper: Boolean) = runComposeUiTest {
        val original = ui(stepper, initial = "second")
        var layout by mutableStateOf(original)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"show":true}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("B").assertExists()
        runOnIdle { layout = original.copy(elements = original.elements.take(1)) }
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("A").assertExists()
        runOnIdle { layout = original }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
    }

    private fun missingInitial(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ui(stepper = false, initial = "absent"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }))
        }
        onNodeWithText("A").assertExists()
        waitForIdle()
        assertEquals(listOf("categorization.initialNotFound"), diagnostics.map { it.code })
        assertEquals("absent", diagnostics.single().details["initial"])
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        waitForIdle()
        assertEquals(1, diagnostics.size)
    }

    private fun duplicateInitial(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        var changes = 0
        val categorization = CategorizationElement(
            options = mapOf("initial" to JsonPrimitive("same")),
            elements = listOf(
                CategoryElement(label = "First", name = "same",
                    elements = listOf(ControlElement(scope = "#/properties/a"))),
                CategoryElement(label = "Second", name = "same",
                    elements = listOf(ControlElement(scope = "#/properties/b"))),
            ),
        )
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = categorization,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                onChange = { changes++ })
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
        waitForIdle()
        assertEquals(listOf("categorization.duplicateName"), diagnostics.map { it.code })
        assertEquals("same", diagnostics.single().details["name"])
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        assertEquals(0, changes)
    }

    private fun translatedLabelsKeepNames(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var changes = 0
        val layout = CategorizationElement(
            options = mapOf("initial" to JsonPrimitive("review")),
            elements = listOf(
                CategoryElement(label = "Draft", i18n = "draft", name = "draft",
                    elements = listOf(ControlElement(scope = "#/properties/a"))),
                CategoryElement(label = "Review", i18n = "review", name = "review",
                    elements = listOf(ControlElement(scope = "#/properties/b"))),
            ),
        )
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale,
                    translate = { key, fallback, _ -> if (locale == "bg") when (key) {
                        "draft.label" -> "Чернова"
                        "review.label" -> "Преглед"
                        else -> fallback
                    } else fallback }),
                onChange = { changes++ })
        }
        onNodeWithText("B").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithText("Преглед").assertExists()
        onNodeWithText("Чернова").performTouchInput { click() }
        onNodeWithText("A").assertExists()
        runOnIdle { locale = "en" }
        onNodeWithText("Draft").assertExists()
        onNodeWithText("Review").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        assertEquals(listOf("draft", "review"), layout.elements.map { it.name })
        assertEquals(0, changes)
    }

    private fun sameNameInSeparateCategorizations(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        var changes = 0
        val layout = VerticalLayout(elements = listOf(
            CategorizationElement(options = mapOf("initial" to JsonPrimitive("summary")),
                elements = listOf(CategoryElement(label = "First section", name = "summary",
                    elements = listOf(ControlElement(scope = "#/properties/a"))))),
            CategorizationElement(options = mapOf("initial" to JsonPrimitive("summary")),
                elements = listOf(CategoryElement(label = "Second section", name = "summary",
                    elements = listOf(ControlElement(scope = "#/properties/b"))))),
        ))
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }),
                onChange = { changes++ })
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertExists()
        waitForIdle()
        assertEquals(emptyList(), diagnostics.map { it.code })
        assertEquals(0, changes)
    }
}
