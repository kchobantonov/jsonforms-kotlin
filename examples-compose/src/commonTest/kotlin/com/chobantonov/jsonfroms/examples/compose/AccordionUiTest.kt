package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class AccordionUiTest {
    @Test fun materialUnboundAccordionRendersWithEmptyData() = unboundEmptyData(false)
    @Test fun cupertinoUnboundAccordionRendersWithEmptyData() = unboundEmptyData(true)
    @Test fun materialIdenticalDuplicateKeepsOpenIdentityThroughReorder() = identicalDuplicateReorder(false)
    @Test fun cupertinoIdenticalDuplicateKeepsOpenIdentityThroughReorder() = identicalDuplicateReorder(true)
    @Test fun materialSingleOpenAndExplicitCloseSurvivesUpdates() = scenario(false)
    @Test fun cupertinoSingleOpenAndExplicitCloseSurvivesUpdates() = scenario(true)
    @Test fun materialHiddenOpenFallsBack() = hidden(false)
    @Test fun cupertinoHiddenOpenFallsBack() = hidden(true)
    @Test fun materialCollapsedOptionsReactToConfig() = collapsed(false)
    @Test fun cupertinoCollapsedOptionsReactToConfig() = collapsed(true)
    @Test fun materialInitialOverridesCollapsedOnMount() = initialOverridesCollapse(false)
    @Test fun cupertinoInitialOverridesCollapsedOnMount() = initialOverridesCollapse(true)
    @Test fun materialSelectedIdentitySurvivesReorder() = reorder(false)
    @Test fun cupertinoSelectedIdentitySurvivesReorder() = reorder(true)
    @Test fun materialAllHiddenAndRecovery() = allHidden(false)
    @Test fun cupertinoAllHiddenAndRecovery() = allHidden(true)
    @Test fun materialMissingInitialReportsDiagnostic() = missingInitial(false)
    @Test fun cupertinoMissingInitialReportsDiagnostic() = missingInitial(true)
    @Test fun materialDuplicateNamesRemainIndependentlySelectable() = duplicateNames(false)
    @Test fun cupertinoDuplicateNamesRemainIndependentlySelectable() = duplicateNames(true)
    @Test fun materialRemovedOpenCategoryFallsBack() = removed(false)
    @Test fun cupertinoRemovedOpenCategoryFallsBack() = removed(true)
    @Test fun materialClosedPanelRetainsValidation() = validation(false)
    @Test fun cupertinoClosedPanelRetainsValidation() = validation(true)
    @Test fun materialStateDescriptionUsesLocaleBundle() = localizedState(false)
    @Test fun cupertinoStateDescriptionUsesLocaleBundle() = localizedState(true)
    @Test fun materialLocalFalseOverridesGlobalCollapse() = localOverride(false)
    @Test fun cupertinoLocalFalseOverridesGlobalCollapse() = localOverride(true)
    @Test fun materialIgnoresStepperAndOrientationOptions() = ignoresOtherNavigationOptions(false)
    @Test fun cupertinoIgnoresStepperAndOrientationOptions() = ignoresOtherNavigationOptions(true)
    @Test fun materialKeyboardOpensAndClosesPanel() = keyboardDisclosure(false)
    @Test fun cupertinoKeyboardOpensAndClosesPanel() = keyboardDisclosure(true)
    @Test fun materialNewCategorySetUsesItsInitialSelection() = newCategorySet(false)
    @Test fun cupertinoNewCategorySetUsesItsInitialSelection() = newCategorySet(true)
    @Test fun materialAddingCategoryKeepsOpenPanel() = addingCategoryKeepsSelection(false)
    @Test fun cupertinoAddingCategoryKeepsOpenPanel() = addingCategoryKeepsSelection(true)

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

    private fun unboundEmptyData(cupertino: Boolean) = runComposeUiTest {
        val layout = CategorizationElement(
            options = mapOf("variant" to JsonPrimitive("accordion")),
            elements = listOf(
                CategoryElement(label = "Introduction", elements = listOf(LabelElement("Welcome"))),
                CategoryElement(label = "Details", elements = listOf(LabelElement("More information"))),
            ),
        )
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""),
                schema = JsonSchema(Json.parseToJsonElement("""{}""").jsonObject),
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("Welcome").assertExists()
        onNodeWithText("More information").assertDoesNotExist()
        onNodeWithText("Details").performTouchInput { click() }
        onNodeWithText("Welcome").assertDoesNotExist()
        onNodeWithText("More information").assertExists()
        assertEquals(0, writes)
    }

    private fun identicalDuplicateReorder(cupertino: Boolean) = runComposeUiTest {
        val first = CategoryElement(label = "Same", name = "duplicate",
            elements = listOf(LabelElement("Content")))
        val second = first.copy()
        val original = CategorizationElement(
            options = mapOf("variant" to JsonPrimitive("accordion")),
            elements = listOf(first, second),
        )
        var layout by mutableStateOf(original)
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        val headers = onAllNodes(hasClickAction() and hasText("Same"))
        fun openCenter() = headers.fetchSemanticsNodes()
            .single { it.config.getOrNull(SemanticsProperties.StateDescription) == "Expanded" }
            .boundsInRoot.center
        val initialCenter = openCenter()
        if (headers.fetchSemanticsNodes().first().config
                .getOrNull(SemanticsProperties.StateDescription) == "Expanded") {
            headers.onLast().performClick()
        } else {
            headers.onFirst().performClick()
        }
        val beforeReorder = openCenter()
        assertNotEquals(initialCenter, beforeReorder)
        runOnIdle {
            layout = original.copy(
                options = original.options + ("vertical" to JsonPrimitive(false)),
                elements = listOf(second, first),
            )
        }
        assertNotEquals(beforeReorder, openCenter())
        assertEquals(0, writes)
    }

    private fun ui(initial: Boolean = true, rule: Boolean = false) = CategorizationElement(
        options = mapOf("variant" to JsonPrimitive("accordion")) +
            (if (initial) mapOf("initial" to JsonPrimitive("second")) else emptyMap()),
        elements = listOf(
            CategoryElement(label = "First", name = "first",
                elements = listOf(ControlElement(scope = "#/properties/a"))),
            CategoryElement(label = "Second", name = "second",
                rule = if (rule) secondRule else null,
                elements = listOf(ControlElement(scope = "#/properties/b"))),
        ),
    )

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("A").assertDoesNotExist()
        onNodeWithText("First").performTouchInput { click() }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun keyboardDisclosure(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"show":true}"""), schema = schema,
                uischema = ui(initial = false),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        val first = onAllNodes(hasClickAction() and hasText("First")).onFirst()
        onNodeWithText("A").assertExists()
        first.performSemanticsAction(SemanticsActions.RequestFocus)
        first.assertIsFocused()
        first.performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        onNodeWithText("A").assertDoesNotExist()
        first.performKeyInput { keyDown(Key.Spacebar); keyUp(Key.Spacebar) }
        onNodeWithText("A").assertExists()
    }

    private fun newCategorySet(cupertino: Boolean) = runComposeUiTest {
        val replacement = CategorizationElement(
            options = mapOf("variant" to JsonPrimitive("accordion"),
                "initial" to JsonPrimitive("fourth")),
            elements = listOf(
                CategoryElement(label = "Third", name = "third",
                    elements = listOf(ControlElement(scope = "#/properties/a"))),
                CategoryElement(label = "Fourth", name = "fourth",
                    elements = listOf(ControlElement(scope = "#/properties/b"))),
            ),
        )
        var current by mutableStateOf(ui())
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = current,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("Second").performClick()
        onNodeWithText("B").assertDoesNotExist()
        runOnIdle { current = replacement }
        onNodeWithText("Fourth").assertExists()
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
        assertEquals(0, writes)
    }

    private fun addingCategoryKeepsSelection(cupertino: Boolean) = runComposeUiTest {
        val original = ui()
        val extra = CategoryElement(label = "Third", name = "third",
            elements = listOf(ControlElement(scope = "#/properties/a")))
        var current by mutableStateOf(original)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = current,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("First").performClick()
        onNodeWithText("A").assertExists()
        runOnIdle { current = original.copy(elements = original.elements + extra) }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
    }

    private fun hidden(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(rule = true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("B").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("A").assertExists()
    }

    private fun initialOverridesCollapse(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("collapsed" to true)))
        }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
    }

    private fun reorder(cupertino: Boolean) = runComposeUiTest {
        var reversed by mutableStateOf(false)
        val original = ui(initial = false)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = if (reversed) original.copy(elements = original.elements.reversed()) else original,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("Second").performTouchInput { click() }
        onNodeWithText("B").assertExists()
        runOnIdle { reversed = true }
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
    }

    private fun allHidden(cupertino: Boolean) = runComposeUiTest {
        val original = ui(initial = false, rule = true)
        val first = original.elements.first() as CategoryElement
        val layout = original.copy(elements = listOf(first.copy(rule = secondRule), original.elements[1]))
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true}"""))
        setContent {
            JsonForms(data = data, schema = schema, uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("A").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false}""") }
        onNodeWithText("A").assertDoesNotExist()
        onNodeWithText("B").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        onNodeWithText("A").assertExists()
        onNodeWithText("B").assertDoesNotExist()
    }

    private fun missingInitial(cupertino: Boolean) = runComposeUiTest {
        val diagnostics = mutableListOf<UiDiagnostic>()
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        val layout = ui().copy(options = mapOf(
            "variant" to JsonPrimitive("accordion"), "initial" to JsonPrimitive("absent"),
        ))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf(UiDiagnosticReporterConfigKey to UiDiagnosticReporter { diagnostics += it }))
        }
        onNodeWithText("A").assertExists()
        waitForIdle()
        assertEquals(listOf("categorization.initialNotFound"), diagnostics.map { it.code })
        runOnIdle { data = Json.parseToJsonElement("""{"show":true}""") }
        waitForIdle()
        assertEquals(1, diagnostics.size)
    }

    private fun duplicateNames(cupertino: Boolean) = runComposeUiTest {
        val original = ui(initial = false)
        val layout = original.copy(elements = original.elements.map {
            (it as CategoryElement).copy(name = "duplicate")
        })
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("Second").performClick()
        onNodeWithText("B").assertExists()
        onNodeWithText("A").assertDoesNotExist()
    }

    private fun removed(cupertino: Boolean) = runComposeUiTest {
        val original = ui()
        var removeSecond by mutableStateOf(false)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = if (removeSecond) original.copy(elements = original.elements.take(1)) else original,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("B").assertExists()
        runOnIdle { removeSecond = true }
        onNodeWithText("Second").assertDoesNotExist()
        onNodeWithText("A").assertExists()
    }

    private fun validation(cupertino: Boolean) = runComposeUiTest {
        val original = ui()
        val second = original.elements[1] as CategoryElement
        val layout = original.copy(elements = listOf(
            original.elements[0],
            second.copy(options = mapOf(
                "showValidationIndicator" to JsonPrimitive(true),
                "showValidationIndicatorCount" to JsonPrimitive(true),
            )),
        ))
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                additionalErrors = listOf(ErrorObject(instancePath = "/b", message = "server")))
        }
        val header = onNodeWithContentDescription("Second, 1 error")
        header.assertExists()
        assertEquals("Expanded", header.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        header.performClick()
        onNodeWithText("server").assertDoesNotExist()
        header.assertExists()
        assertEquals("Collapsed", header.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        header.performClick()
        onNodeWithText("server").assertExists()
    }

    private fun localizedState(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(data = Json.parseToJsonElement("{}"), schema = schema,
                uischema = ui(initial = false),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale))
        }
        fun state() = onNodeWithText("First").fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        assertEquals("Expanded", state())
        onNodeWithText("First").performClick()
        assertEquals("Collapsed", state())
        runOnIdle { locale = "bg-BG" }
        assertEquals("Свита", state())
    }

    private fun localOverride(cupertino: Boolean) = runComposeUiTest {
        val layout = ui(initial = false).copy(options = mapOf(
            "variant" to JsonPrimitive("accordion"),
            "collapsed" to JsonPrimitive(false),
        ))
        var globalCollapsed by mutableStateOf(true)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("collapsed" to globalCollapsed)))
        }
        onNodeWithText("A").assertExists()
        onNodeWithText("First").performClick()
        onNodeWithText("A").assertDoesNotExist()
        runOnIdle { globalCollapsed = false }
        onNodeWithText("A").assertDoesNotExist()
    }

    private fun ignoresOtherNavigationOptions(cupertino: Boolean) = runComposeUiTest {
        val layout = ui(initial = false).copy(options = mapOf(
            "variant" to JsonPrimitive("accordion"),
            "vertical" to JsonPrimitive(true),
            "showNavButtons" to JsonPrimitive(true),
        ))
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = layout,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        val first = onNodeWithText("First").fetchSemanticsNode().boundsInRoot
        val second = onNodeWithText("Second").fetchSemanticsNode().boundsInRoot
        kotlin.test.assertTrue(second.top >= first.bottom - 2f)
        onNodeWithText("Next").assertDoesNotExist()
        onNodeWithText("Previous").assertDoesNotExist()
        onNodeWithText("A").assertExists()
    }

    private fun collapsed(cupertino: Boolean) = runComposeUiTest {
        var collapsed by mutableStateOf(false)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{}"""), schema = schema,
                uischema = ui(initial = false),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("collapsed" to collapsed)))
        }
        onNodeWithText("A").assertExists()
        runOnIdle { collapsed = true }
        onNodeWithText("A").assertDoesNotExist()
        runOnIdle { collapsed = false }
        onNodeWithText("A").assertExists()
    }
}
