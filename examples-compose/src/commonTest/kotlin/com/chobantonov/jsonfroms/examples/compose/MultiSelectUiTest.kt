package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MultiSelectUiTest {
    private val enumSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"minItems":1,"maxItems":2,"items":{"type":"string","enum":["ops","fin","qa"]}}}}"""
    ).jsonObject)
    private fun ui(restrict: Boolean? = null) = ControlElement("#/properties/teams", options =
        mapOf("variant" to JsonPrimitive("multi-select")) +
            (restrict?.let { mapOf("restrict" to JsonPrimitive(it)) } ?: emptyMap()))

    @Test fun materialAddsAndRemovesFiniteChoicesWithinBounds() = selection(false)
    @Test fun cupertinoAddsAndRemovesFiniteChoicesWithinBounds() = selection(true)
    @Test fun materialPreservesInvalidImportedValueAndTitledChoice() = imported(false)
    @Test fun cupertinoPreservesInvalidImportedValueAndTitledChoice() = imported(true)
    @Test fun materialMultiSelectShowsImportedNull() = importedNull(false, false)
    @Test fun cupertinoMultiSelectShowsImportedNull() = importedNull(true, false)
    @Test fun materialChipsShowsImportedNull() = importedNull(false, true)
    @Test fun cupertinoChipsShowsImportedNull() = importedNull(true, true)

    private fun importedNull(cupertino: Boolean, chips: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = enumSchema,
                uischema = ControlElement("#/properties/teams", options =
                    mapOf("variant" to JsonPrimitive(if (chips) "chips" else "multi-select"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Current value: null").assertExists()
        assertTrue(onNodeWithText("Add choice").fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Current value: null"))
        runOnIdle { data = Json.parseToJsonElement("{}") }
        onNodeWithText("Current value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"teams":null}""")
        }
        onNodeWithText("Текуща стойност: null").assertExists()
        onNodeWithText("Добави избор").performClick()
        onNodeWithText("ops").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
            assertEquals(1, changes)
        }
    }
    @Test fun materialRepairsInvalidImportedShapeOnlyAfterSelection() = repairShape(false)
    @Test fun cupertinoRepairsInvalidImportedShapeOnlyAfterSelection() = repairShape(true)
    @Test fun materialLocalFalseAllowsOverMaximum() = unrestricted(false)
    @Test fun cupertinoLocalFalseAllowsOverMaximum() = unrestricted(true)
    @Test fun materialEqualLabelsKeepDistinctValues() = equalLabels(false)
    @Test fun cupertinoEqualLabelsKeepDistinctValues() = equalLabels(true)
    @Test fun materialSearchUsesTitlesAndLocalPrecedence() = search(false)
    @Test fun cupertinoSearchUsesTitlesAndLocalPrecedence() = search(true)
    @Test fun materialRestrictedArrayRepairsImportedOverMaximum() = repairOverMaximum(false)
    @Test fun cupertinoRestrictedArrayRepairsImportedOverMaximum() = repairOverMaximum(true)
    @Test fun materialRestrictedArrayRepairsImportedUnderMinimum() = repairUnderMinimum(false)
    @Test fun cupertinoRestrictedArrayRepairsImportedUnderMinimum() = repairUnderMinimum(true)
    @Test fun materialFixedCountChoiceCanBeReplaced() = fixedCountReplacement(false)
    @Test fun cupertinoFixedCountChoiceCanBeReplaced() = fixedCountReplacement(true)
    @Test fun materialChoicesCanBeReplacedAtEitherCountBoundary() = replaceAtBoundaries(false)
    @Test fun cupertinoChoicesCanBeReplacedAtEitherCountBoundary() = replaceAtBoundaries(true)
    @Test fun materialPendingAddClosesWhenEditingIsDisabled() = pendingAddPermission(false)
    @Test fun cupertinoPendingAddClosesWhenEditingIsDisabled() = pendingAddPermission(true)
    @Test fun materialContainsMinimumGuardsMultiSelectRemoval() = containsMinimum(false)
    @Test fun cupertinoContainsMinimumGuardsMultiSelectRemoval() = containsMinimum(true)
    @Test fun materialContainsMinimumGuardsChipsRemoval() = containsMinimum(false, chips = true)
    @Test fun cupertinoContainsMinimumGuardsChipsRemoval() = containsMinimum(true, chips = true)

    private fun containsMinimum(cupertino: Boolean, chips: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"teams":{"type":"array","uniqueItems":true,
            "contains":{"const":"ops"},"items":{"type":"string","enum":["ops","fin"]}}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""))
        var unrestricted by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/teams",
                options = mapOf("variant" to JsonPrimitive(if (chips) "chips" else "multi-select"),
                    "restrict" to JsonPrimitive(!unrestricted))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Remove choice: ops").assertIsNotEnabled()
        onNodeWithText("Remove choice: fin").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
        assertEquals(1, changes)
        runOnIdle { unrestricted = true }
        onNodeWithText("Remove choice: ops").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":[]}"""), data)
        assertEquals(2, changes)
    }

    private fun pendingAddPermission(cupertino: Boolean) = runComposeUiTest {
        val control = ui(restrict = true).copy(rule = Rule(
            effect = RuleEffect.DISABLE,
            condition = SchemaBasedCondition(
                scope = "#/properties/lock",
                schema = buildJsonSchema { put("const", JsonPrimitive(true)) },
            ),
        ))
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"],"lock":false}"""))
        var readonly by mutableStateOf(false)
        setContent {
            JsonForms(
                data = data, schema = enumSchema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                readonly = readonly,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").assertExists()
        runOnIdle { readonly = true }
        waitForIdle()
        onNodeWithText("fin").assertDoesNotExist()
        runOnIdle { readonly = false }
        waitForIdle()
        onNodeWithText("fin").assertDoesNotExist()
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"],"lock":true}""") }
        waitForIdle()
        onNodeWithText("fin").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"],"lock":false}""") }
        waitForIdle()
        onNodeWithText("fin").assertDoesNotExist()
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin"],"lock":false}"""), data)
    }

    private fun replaceAtBoundaries(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""))
        setContent {
            Box(Modifier.width(320.dp)) {
                JsonForms(
                    data = data, schema = enumSchema, uischema = ui(restrict = true),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data },
                )
            }
        }
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Replace choice: ops").performClick()
        onNodeWithText("qa").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["qa","fin"]}"""), data)
        onNodeWithText("Remove choice: fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["qa"]}"""), data)
        onNodeWithText("Remove choice: qa").assertIsNotEnabled()
        onNodeWithText("Replace choice: qa").performClick()
        onNodeWithText("ops").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
    }

    private fun fixedCountReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,
              "minItems":1,"maxItems":1,"items":{"type":"string","enum":["ops","fin"]}},
              "lock":{"type":"boolean"}}}"""
        ).jsonObject)
        val control = ui(restrict = true).copy(rule = Rule(
            effect = RuleEffect.DISABLE,
            condition = SchemaBasedCondition(
                scope = "#/properties/lock",
                schema = buildJsonSchema { put("const", JsonPrimitive(true)) },
            ),
        ))
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"],"lock":false}"""))
        var validity: CombinedValidityStatus? = null
        var locale by mutableStateOf("en")
        var readonly by mutableStateOf(false)
        setContent {
            Box(Modifier.width(320.dp)) {
                JsonForms(
                    data = data, schema = schema, uischema = control,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    i18n = JsonFormsI18nState(locale = locale),
                    readonly = readonly,
                    onChange = { data = it.data },
                    onValidityChange = { validity = it.status },
                )
            }
        }
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Remove choice: ops").assertIsNotEnabled()
        onNodeWithText("Replace choice: ops").performClick()
        onNodeWithText("fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["fin"],"lock":false}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("Replace choice: fin").assertExists()
        onNodeWithText("Replace choice: fin").performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"],"lock":false}""") }
        waitForIdle()
        onNodeWithText("fin").assertDoesNotExist()
        runOnIdle { locale = "bg-BG" }
        waitForIdle()
        onNodeWithText("Смени избор: ops").assertExists()
        runOnIdle { locale = "de-DE" }
        waitForIdle()
        onNodeWithText("Replace choice: ops").assertExists()
        onNodeWithText("Replace choice: ops").performClick()
        runOnIdle { readonly = true }
        waitForIdle()
        onNodeWithText("Replace choice: ops").assertDoesNotExist()
        onNodeWithText("fin").assertDoesNotExist()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"],"lock":false}"""), data)
        runOnIdle { readonly = false }
        waitForIdle()
        onNodeWithText("Replace choice: ops").assertExists()
        onNodeWithText("fin").assertDoesNotExist()
        onNodeWithText("Replace choice: ops").performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"],"lock":true}""") }
        waitForIdle()
        onNodeWithText("Replace choice: ops").assertDoesNotExist()
        onNodeWithText("fin").assertDoesNotExist()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"],"lock":true}"""), data)
        runOnIdle { data = Json.parseToJsonElement("""{"teams":["ops"],"lock":false}""") }
        waitForIdle()
        onNodeWithText("Replace choice: ops").assertExists()
        onNodeWithText("fin").assertDoesNotExist()
    }

    private fun repairUnderMinimum(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,
              "minItems":2,"maxItems":3,"items":{"type":"string","enum":["ops","fin","qa"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":[]}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ui(restrict = true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(Json.parseToJsonElement("""{"teams":[]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Add choice").performClick()
        onNodeWithText("ops").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Remove choice: ops").assertIsNotEnabled()
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun repairOverMaximum(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops","fin","qa"]}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(
                data = data, schema = enumSchema, uischema = ui(restrict = true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin","qa"]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Remove choice: qa").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""), data)
        assertEquals(CombinedValidityStatus.Valid, validity)
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Remove choice: fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
        onNodeWithText("Remove choice: ops").assertIsNotEnabled()
    }

    private fun selection(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"]}"""))
        setContent {
            JsonForms(
                data = data, schema = enumSchema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Remove choice: ops").assertIsNotEnabled()
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""), data)
        onNodeWithText("Add choice").assertIsNotEnabled()
        onNodeWithText("Remove choice: ops").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["fin"]}"""), data)
        onNodeWithText("Remove choice: fin").assertIsNotEnabled()
    }

    private fun imported(cupertino: Boolean) = runComposeUiTest {
        val titledSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"maxItems":2,"items":{"oneOf":[{"const":"ops","title":"Operations"},{"const":"fin","title":"Finance"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["legacy"]}"""))
        var locale by mutableStateOf("en-US")
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = titledSchema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("legacy").assertExists()
        onNodeWithText("Current value outside choices: \"legacy\"").assertExists()
        assertEquals(0, changes)
        runOnIdle { locale = "bg" }
        onNodeWithText("Текуща стойност извън опциите: \"legacy\"").assertExists()
        assertEquals(0, changes)
        runOnIdle { locale = "en" }
        onNodeWithText("Add choice").performClick()
        onNodeWithText("Operations").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["legacy","ops"]}"""), data)
        onNodeWithText("Remove choice: legacy").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops"]}"""), data)
        onNodeWithText("Operations").assertExists()
        onNodeWithText("Current value outside choices: \"legacy\"").assertDoesNotExist()
    }

    private fun unrestricted(cupertino: Boolean) = runComposeUiTest {
        val oneItemSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"maxItems":1,"items":{"type":"string","enum":["ops","fin"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":["ops"]}"""))
        var validity by mutableStateOf<CombinedValidityStatus?>(null)
        setContent {
            JsonForms(
                data = data, schema = oneItemSchema, uischema = ui(restrict = false),
                config = mapOf("restrict" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
                onValidityChange = { validity = it.status },
            )
        }
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"teams":["ops","fin"]}"""), data)
        assertEquals(CombinedValidityStatus.Invalid, validity)
    }

    private fun repairShape(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":"legacy"}"""))
        setContent {
            JsonForms(
                data = data, schema = enumSchema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Current value: \"legacy\"").assertExists()
        assertEquals(Json.parseToJsonElement("""{"teams":"legacy"}"""), data)
        onNodeWithText("Add choice").performClick()
        onNodeWithText("fin").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["fin"]}"""), data)
    }

    private fun equalLabels(cupertino: Boolean) = runComposeUiTest {
        val titledSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"ops","title":"Team"},{"const":"fin","title":"Team"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":[]}"""))
        setContent {
            JsonForms(
                data = data, schema = titledSchema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Add choice").performClick()
        onNodeWithContentDescription("Team (fin)").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["fin"]}"""), data)
        onNodeWithText("Add choice").performClick()
        onNodeWithContentDescription("Team (ops)").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["fin","ops"]}"""), data)
    }

    private fun search(cupertino: Boolean) = runComposeUiTest {
        val titledSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"teams":{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"ops","title":"Operations"},{"const":"fin","title":"Finance"},{"const":"qa","title":"Quality"}]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"teams":[]}"""))
        var localSearch by mutableStateOf(false)
        setContent {
            JsonForms(
                data = data, schema = titledSchema,
                uischema = ControlElement("#/properties/teams", options = mapOf(
                    "variant" to JsonPrimitive("multi-select"),
                    "autocomplete" to JsonPrimitive(localSearch),
                )),
                config = mapOf("autocomplete" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithText("Add choice").performClick()
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        onNodeWithText("Quality").performClick()
        runOnIdle { localSearch = true }
        onNodeWithText("Add choice").performClick()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("ops")
        onNodeWithText("No matches").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("oper")
        onNodeWithText("Operations").performClick()
        assertEquals(Json.parseToJsonElement("""{"teams":["qa","ops"]}"""), data)
    }
}
