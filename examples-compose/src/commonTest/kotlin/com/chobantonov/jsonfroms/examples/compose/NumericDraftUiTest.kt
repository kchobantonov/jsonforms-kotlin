package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsState
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.JsonFormsStateProvider
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.reducers.coreReducer
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsCoreState
import com.chobantonov.jsonfroms.core.store.JsonFormsSubStates
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumericDraftUiTest {
    private fun assertNumericValue(expected: Double, data: JsonElement) {
        assertEquals(expected, data.jsonObject["value"]?.jsonPrimitive?.doubleOrNull)
    }
    @Test fun materialIntegerDraftDoesNotPublishInvalidText() = scenario(false, true)
    @Test fun materialNumberDraftDoesNotPublishInvalidText() = scenario(false, false)
    @Test fun cupertinoIntegerDraftDoesNotPublishInvalidText() = scenario(true, true)
    @Test fun cupertinoNumberDraftDoesNotPublishInvalidText() = scenario(true, false)
    @Test fun materialIntegerReadOnlyDiscardsPendingDraft() = readonly(false, true)
    @Test fun materialNumberReadOnlyDiscardsPendingDraft() = readonly(false, false)
    @Test fun cupertinoIntegerReadOnlyDiscardsPendingDraft() = readonly(true, true)
    @Test fun cupertinoNumberReadOnlyDiscardsPendingDraft() = readonly(true, false)
    @Test fun materialIntegerDisableRuleDiscardsPendingDraft() = disableRule(false, true)
    @Test fun materialNumberDisableRuleDiscardsPendingDraft() = disableRule(false, false)
    @Test fun cupertinoIntegerDisableRuleDiscardsPendingDraft() = disableRule(true, true)
    @Test fun cupertinoNumberDisableRuleDiscardsPendingDraft() = disableRule(true, false)
    @Test fun materialIntegerEditorRemovalReleasesPending() = editorRemoval(false, true)
    @Test fun materialNumberEditorRemovalReleasesPending() = editorRemoval(false, false)
    @Test fun cupertinoIntegerEditorRemovalReleasesPending() = editorRemoval(true, true)
    @Test fun cupertinoNumberEditorRemovalReleasesPending() = editorRemoval(true, false)
    @Test fun materialIntegerRestrictBoundsAndRepair() = restrictedBounds(false, true)
    @Test fun materialNumberRestrictBoundsAndRepair() = restrictedBounds(false, false)
    @Test fun cupertinoIntegerRestrictBoundsAndRepair() = restrictedBounds(true, true)
    @Test fun cupertinoNumberRestrictBoundsAndRepair() = restrictedBounds(true, false)
    @Test fun materialNumberRestrictOffLeavesValidationActive() = unrestrictedBounds(false)
    @Test fun cupertinoNumberRestrictOffLeavesValidationActive() = unrestrictedBounds(true)
    @Test fun materialNumberBoundErrorIsBulgarian() = bulgarianBoundError(false)
    @Test fun cupertinoNumberBoundErrorIsBulgarian() = bulgarianBoundError(true)
    @Test fun materialIntegerStepsUseMultipleOfAndBounds() = stepActions(false, true)
    @Test fun cupertinoIntegerStepsUseMultipleOfAndBounds() = stepActions(true, true)
    @Test fun materialNumberExplicitStepKeepsSchemaValidation() = stepActions(false, false)
    @Test fun cupertinoNumberExplicitStepKeepsSchemaValidation() = stepActions(true, false)
    @Test fun materialFractionalIntegerStepReportsConfigurationError() = invalidIntegerStep(false)
    @Test fun cupertinoFractionalIntegerStepReportsConfigurationError() = invalidIntegerStep(true)

    private fun stepActions(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(if (integer) """{
          "type":"object","properties":{"value":{"type":"integer","minimum":0,"maximum":9,"multipleOf":3}}
        }""" else """{
          "type":"object","properties":{"value":{"type":"number","minimum":0,"maximum":1,"multipleOf":0.25}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            if (integer) """{"value":6}""" else """{"value":0.5}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options =
                    if (integer) emptyMap() else mapOf("step" to JsonPrimitive(0.1))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        val increase = onNodeWithContentDescription("Increase value")
        val decrease = onNodeWithContentDescription("Decrease value")
        increase.performClick()
        runOnIdle {
            assertNumericValue(if (integer) 9.0 else 0.6, data)
            assertEquals(if (integer) CombinedValidityStatus.Valid else CombinedValidityStatus.Invalid,
                validity)
            assertEquals(1, changes)
        }
        if (integer) increase.assertIsNotEnabled()
        decrease.performClick()
        runOnIdle {
            assertNumericValue(if (integer) 6.0 else 0.5, data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(2, changes)
        }
    }

    private fun invalidIntegerStep(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"integer","multipleOf":3}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":6}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value",
                    options = mapOf("step" to JsonPrimitive(0.5))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Invalid numeric step.").assertExists()
        onNodeWithContentDescription("Increase value").assertIsNotEnabled()
        onNodeWithContentDescription("Decrease value").assertIsNotEnabled()
        runOnIdle { assertNumericValue(6.0, data) }
    }

    private fun unrestrictedBounds(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"number","maximum":10}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value",
                    options = mapOf("restrict" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12")
        runOnIdle {
            assertNumericValue(12.0, data)
            assertEquals(CombinedValidityStatus.Invalid, validity)
            assertEquals(1, changes)
        }
        onNodeWithText("Outside allowed range.").assertDoesNotExist()
    }

    private fun bulgarianBoundError(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"number","maximum":10}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg"),
                onChange = { data = it.data },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12")
        onNodeWithText("Извън позволения диапазон.").assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(Json.parseToJsonElement("""{"value":7}"""), data)
        }
    }

    private fun restrictedBounds(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}","minimum":0,"maximum":10}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextReplacement("12")
        onNodeWithText("Outside allowed range.").assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(Json.parseToJsonElement("""{"value":7}"""), data)
            assertEquals(0, changes)
        }
        field.performTextReplacement("9")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertNumericValue(9.0, data)
            assertEquals(1, changes)
            data = Json.parseToJsonElement("""{"value":-5}""")
        }
        field.performTextReplacement("-3")
        runOnIdle {
            assertNumericValue(-3.0, data)
            assertEquals(2, changes)
        }
        field.performTextReplacement("0")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertNumericValue(0.0, data)
            assertEquals(3, changes)
        }
    }

    private fun editorRemoval(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}"}}
        }""").jsonObject)
        val initialData = Json.parseToJsonElement("""{"value":7}""")
        val core = defaultMiddleware(
            state = JsonFormsCoreState(),
            action = Actions.init(initialData, schema, ControlElement("#/properties/value")),
            defaultReducer = coreReducer,
        )
        val initialState = ComposeJsonFormsState(
            jsonforms = JsonFormsSubStates(
                core = core,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            ),
        )
        var editorVisible by mutableStateOf(true)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonFormsStateProvider(
                initState = initialState,
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            ) {
                if (editorVisible) JsonFormsDispatchRenderer()
            }
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("-")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            editorVisible = false
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(0, changes)
            assertEquals(initialData, initialState.jsonforms.core.data)
        }
        assertEquals(0, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
    }

    private fun disableRule(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}"},"lock":{"type":"boolean"}}
        }""").jsonObject)
        val control = ControlElement(
            scope = "#/properties/value",
            rule = Rule(
                effect = RuleEffect.DISABLE,
                condition = SchemaBasedCondition(
                    scope = "#/properties/lock",
                    schema = buildJsonSchema { put("const", true) },
                ),
            ),
        )
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7,"lock":false}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("-")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            data = Json.parseToJsonElement("""{"value":7,"lock":true}""")
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(Json.parseToJsonElement("""{"value":7,"lock":true}"""), data)
            assertEquals(0, changes)
        }
        onNodeWithText("7").assertExists()
        onNodeWithText("Invalid integer.").assertDoesNotExist()
        onNodeWithText("Invalid number.").assertDoesNotExist()
    }

    private fun readonly(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7}"""))
        var readonly by mutableStateOf(false)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("-")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            readonly = true
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(Json.parseToJsonElement("""{"value":7}"""), data)
            assertEquals(0, changes)
        }
        onNodeWithText("7").assertExists()
        onNodeWithText(if (integer) "Invalid integer." else "Invalid number.").assertDoesNotExist()
    }

    private fun scenario(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":7}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity) }
        field.performTextReplacement(if (integer) "-" else "1e309")
        onNodeWithText(if (integer) "Invalid integer." else "Invalid number.").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":7}"""), data)
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(0, changes)
        }
        field.performTextReplacement(if (integer) "9007199254740992" else "9007199254740993")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":7}"""), data)
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(0, changes)
        }
        field.performTextReplacement(if (integer) "9" else "9.5")
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                if (integer) """{"value":9}""" else """{"value":9.5}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(1, changes)
        }
        field.performTextReplacement(if (integer) "1.9" else "NaN")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            data = Json.parseToJsonElement("""{"value":4}""")
        }
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(1, changes)
        }
    }
}
