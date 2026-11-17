package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class EnumArrayRestrictionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"choices":{"type":"array","uniqueItems":true,"minItems":1,"maxItems":1,"items":{"type":"string","enum":["red","blue"]}}}}"""
    ).jsonObject)
    private val checkboxes = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    @Test fun materialDefaultGuardsBothBoundsAndLocalFalse() = boundaries(false)
    @Test fun cupertinoDefaultGuardsBothBoundsAndLocalFalse() = boundaries(true)
    @Test fun materialInvalidDuplicatesCanRepairOneOccurrence() = duplicateRepair(false)
    @Test fun cupertinoInvalidDuplicatesCanRepairOneOccurrence() = duplicateRepair(true)
    @Test fun materialUnderMinimumCanAdd() = underMinimumRepair(false)
    @Test fun cupertinoUnderMinimumCanAdd() = underMinimumRepair(true)
    @Test fun materialArrayBoundsRemainRestrictedWithoutValidation() = modeIndependence(false)
    @Test fun cupertinoArrayBoundsRemainRestrictedWithoutValidation() = modeIndependence(true)
    @Test fun materialUnknownArrayOccurrencesStayVisibleUntilRemoval() = unknownOccurrences(false)
    @Test fun cupertinoUnknownArrayOccurrencesStayVisibleUntilRemoval() = unknownOccurrences(true)
    @Test fun materialInvalidScalarStaysVisibleUntilSelection() = invalidScalar(false)
    @Test fun cupertinoInvalidScalarStaysVisibleUntilSelection() = invalidScalar(true)
    @Test fun materialImportedNonArrayHasAccessibleState() = importedNonArrayAccessible(false)
    @Test fun cupertinoImportedNonArrayHasAccessibleState() = importedNonArrayAccessible(true)
    @Test fun materialMissingArrayIsNotShownAsImportedNull() = missingArray(false)
    @Test fun cupertinoMissingArrayIsNotShownAsImportedNull() = missingArray(true)
    @Test fun materialUnknownRemovalHonorsRestrictedMinimum() = unknownMinimum(false)
    @Test fun cupertinoUnknownRemovalHonorsRestrictedMinimum() = unknownMinimum(true)
    @Test fun materialCheckboxesDefaultHorizontalDespiteGlobalVertical() = checkboxOrientation(false, null, false)
    @Test fun cupertinoCheckboxesDefaultHorizontalDespiteGlobalVertical() = checkboxOrientation(true, null, false)
    @Test fun materialCheckboxesStackWithLocalVertical() = checkboxOrientation(false, true, false)
    @Test fun cupertinoCheckboxesStackWithLocalVertical() = checkboxOrientation(true, true, false)
    @Test fun materialCheckboxesWrapNarrowWidth() = checkboxOrientation(false, null, true)
    @Test fun cupertinoCheckboxesWrapNarrowWidth() = checkboxOrientation(true, null, true)
    @Test fun materialCheckboxLocalFalseOverridesGlobalVertical() = checkboxOrientation(false, false, false)
    @Test fun cupertinoCheckboxLocalFalseOverridesGlobalVertical() = checkboxOrientation(true, false, false)
    @Test fun materialContainsMinimumGuardsCheckboxAndUnknownRemoval() = containsMinimum(false)
    @Test fun cupertinoContainsMinimumGuardsCheckboxAndUnknownRemoval() = containsMinimum(true)
    @Test fun materialContainsMinimumGuardsMatchingUnknownValue() = unknownContainsMinimum(false)
    @Test fun cupertinoContainsMinimumGuardsMatchingUnknownValue() = unknownContainsMinimum(true)

    private fun importedNonArrayAccessible(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value outside choices: {\"raw\":true}").assertExists()
        assertTrue(onAllNodes(checkboxes).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Current value outside choices: {\"raw\":true}"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Current value outside choices: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"choices":null}""")
        }
        onNodeWithText("Текуща стойност извън опциите: null").assertExists()
        onAllNodes(checkboxes).onFirst().performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
            assertEquals(1, changes)
        }
    }

    private fun unknownContainsMinimum(cupertino: Boolean) = runComposeUiTest {
        val containsSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choices":{"type":"array","uniqueItems":true,
            "contains":{"const":42},"items":{"type":"string","enum":["red","blue"]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":[42,"blue"]}"""))
        var unrestricted by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = containsSchema,
                uischema = ControlElement("#/properties/choices", options =
                    mapOf("restrict" to JsonPrimitive(!unrestricted))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Remove choice: 42, 1").assertIsNotEnabled()
        onAllNodes(checkboxes).onLast().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":[42]}"""), data)
        runOnIdle { unrestricted = true }
        onNodeWithContentDescription("Remove choice: 42, 1").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":[]}"""), data)
        assertEquals(2, changes)
    }

    private fun containsMinimum(cupertino: Boolean) = runComposeUiTest {
        val containsSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choices":{"type":"array","uniqueItems":true,
            "contains":{"const":"red"},"items":{"type":"string","enum":["red","blue"]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":["red","blue"]}"""))
        var unrestricted by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = containsSchema,
                uischema = ControlElement("#/properties/choices", options =
                    mapOf("restrict" to JsonPrimitive(!unrestricted))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(checkboxes).onFirst().assertIsNotEnabled()
        onAllNodes(checkboxes).onLast().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        assertEquals(1, changes)
        runOnIdle { data = Json.parseToJsonElement("""{"choices":["red",42]}""") }
        onNodeWithContentDescription("Remove choice: 42, 2").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        assertEquals(2, changes)
        runOnIdle { unrestricted = true }
        onAllNodes(checkboxes).onFirst().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":[]}"""), data)
        assertEquals(3, changes)
    }

    private fun checkboxOrientation(cupertino: Boolean, vertical: Boolean?, narrow: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"choices":{"type":"array","uniqueItems":true,
            "items":{"type":"string","enum":["red","blue"]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":[]}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            Box(Modifier.width(if (narrow) 100.dp else 420.dp)) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/choices", options =
                        vertical?.let { mapOf("vertical" to JsonPrimitive(it)) } ?: emptyMap()),
                    config = mapOf("vertical" to true, "jsonformsExtended" to mapOf(
                        "radio" to mapOf("vertical" to true))),
                    i18n = JsonFormsI18nState(locale = locale),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
        }
        val bounds = onAllNodes(checkboxes).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(2, bounds.size)
        if (vertical == true || narrow) assertTrue(bounds[1].top > bounds[0].top)
        else {
            assertTrue(kotlin.math.abs(bounds[1].top - bounds[0].top) < 2f)
            assertTrue(bounds[1].left > bounds[0].left)
        }
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            if (vertical == true) "Vertical choices" else "Horizontal choices"))
            .fetchSemanticsNodes().also { assertEquals(1, it.size) }
        runOnIdle { locale = "bg" }
        onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,
            if (vertical == true) "Вертикални избори" else "Хоризонтални избори"))
            .fetchSemanticsNodes().also { assertEquals(1, it.size) }
        assertEquals(0, changes)
        onAllNodes(checkboxes)[1].performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["blue"]}"""), data)
        assertEquals(1, changes)
    }

    private fun missingArray(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value outside choices: null").assertDoesNotExist()
        assertEquals(0, changes)
        runOnIdle { data = Json.parseToJsonElement("""{"choices":null}""") }
        onNodeWithText("Current value outside choices: null").assertExists()
        assertEquals(0, changes)
    }

    private fun unknownMinimum(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":[42]}"""))
        var unrestricted by mutableStateOf(false)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choices", options =
                    mapOf("restrict" to JsonPrimitive(!unrestricted))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Remove choice: 42, 1").assertIsNotEnabled()
        assertEquals(Json.parseToJsonElement("""{"choices":[42]}"""), data)
        runOnIdle { unrestricted = true }
        onNodeWithContentDescription("Remove choice: 42, 1").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":[]}"""), data)
    }

    private fun unknownOccurrences(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":[42,"red",42]}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Remove choice: 42, 3").assertExists()
        onAllNodesWithText("Current value outside choices: 42").onFirst().assertExists()
        assertEquals(0, changes)
        runOnIdle { locale = "bg" }
        onAllNodesWithText("Текуща стойност извън опциите: 42").onFirst().assertExists()
        assertEquals(0, changes)
        onNodeWithContentDescription("Премахни избор: 42, 3").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":[42,"red"]}"""), data)
        onNodeWithContentDescription("Премахни избор: 42, 1").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        assertEquals(2, changes)
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
    }

    private fun invalidScalar(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":42}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value outside choices: 42").assertExists()
        assertEquals(0, changes)
        onAllNodes(checkboxes).onFirst().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        assertEquals(1, changes)
        onNodeWithText("Current value outside choices: 42").assertDoesNotExist()
    }

    private fun modeIndependence(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":["red"]}"""))
        var unrestricted by mutableStateOf(false)
        var mode by mutableStateOf(ValidationMode.NoValidation)
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/choices",
                    options = mapOf("restrict" to JsonPrimitive(!unrestricted))),
                validationMode = mode,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Unvalidated, validity)
        onAllNodes(checkboxes).onFirst().assertIsNotEnabled()
        onAllNodes(checkboxes).onLast().assertIsNotEnabled()
        assertEquals(0, changes)

        runOnIdle { unrestricted = true }
        onAllNodes(checkboxes).onLast().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red","blue"]}"""), data)
        assertEquals(CombinedValidityStatus.Unvalidated, validity)
        assertEquals(1, changes)

        runOnIdle { mode = ValidationMode.ValidateAndShow }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, changes)
    }

    private fun boundaries(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":["red"]}"""))
        var unrestricted by mutableStateOf(false)
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/choices", options = if (unrestricted)
                    mapOf("restrict" to JsonPrimitive(false)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(checkboxes).onFirst().assertIsNotEnabled()
        onAllNodes(checkboxes).onLast().assertIsNotEnabled()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        runOnIdle { unrestricted = true }
        onAllNodes(checkboxes).onFirst().assertIsEnabled()
        onAllNodes(checkboxes).onLast().assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"choices":["red","blue"]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
    }

    private fun duplicateRepair(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":["red","red"]}"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(checkboxes).onFirst().assertIsEnabled().performClick()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        onAllNodes(checkboxes).onFirst().assertIsNotEnabled()
    }

    private fun underMinimumRepair(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"choices":[]}"""))
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/choices"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onAllNodes(checkboxes).onFirst().assertIsEnabled().performClick()
        assertEquals(Json.parseToJsonElement("""{"choices":["red"]}"""), data)
        onAllNodes(checkboxes).onFirst().assertIsNotEnabled()
    }
}
