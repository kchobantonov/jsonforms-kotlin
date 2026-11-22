package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.models.SchemaBasedCondition
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CronControlUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"schedule":{"type":"string","format":"cron"}}
    }""").jsonObject)

    @Test fun materialPickerStagesAndPreservesAlias() = staged(false)
    @Test fun cupertinoPickerStagesAndPreservesAlias() = staged(true)
    @Test fun materialTextUsesSchemaValidation() = textValidation(false)
    @Test fun cupertinoTextUsesSchemaValidation() = textValidation(true)
    @Test fun materialImmediatePickerCommits() = immediate(false)
    @Test fun cupertinoImmediatePickerCommits() = immediate(true)
    @Test fun materialCronClearAndReadonlyLocale() = clearAndReadonly(false)
    @Test fun cupertinoCronClearAndReadonlyLocale() = clearAndReadonly(true)
    @Test fun materialCronDiscardsPickerOnHostReplacement() = hostReplacement(false)
    @Test fun cupertinoCronDiscardsPickerOnHostReplacement() = hostReplacement(true)
    @Test fun materialCronDiscardsPickerWhenDisabled() = disableRule(false)
    @Test fun cupertinoCronDiscardsPickerWhenDisabled() = disableRule(true)
    @Test fun materialCronInvalidPickerStaysStaged() = invalidPicker(false)
    @Test fun cupertinoCronInvalidPickerStaysStaged() = invalidPicker(true)
    @Test fun materialCronPeriodSelectionIsAccessible() = accessiblePeriod(false)
    @Test fun cupertinoCronPeriodSelectionIsAccessible() = accessiblePeriod(true)

    private fun accessiblePeriod(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        assertEquals(true, onAllNodesWithText("Day").onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals("Selected period", onAllNodesWithText("Day").onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onAllNodesWithText("Hour").onFirst().performClick()
        assertEquals(true, onAllNodesWithText("Hour").onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(false, onAllNodesWithText("Day").onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
    }

    private fun invalidPicker(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onAllNodesWithText("Hour").onFirst().performClick()
        onAllNodesWithText("0").onFirst().performTextReplacement("60")
        onNodeWithText("Invalid cron expression.").assertExists()
        onNodeWithText("Apply").assertIsNotEnabled()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        onNodeWithText("Cancel").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
    }

    private fun hostReplacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onAllNodesWithText("Hour").onFirst().performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"schedule":"@weekly"}""") }
        waitForIdle()
        onNodeWithText("Apply").assertDoesNotExist()
        onAllNodesWithText("@weekly").onLast().assertExists()
        assertEquals(0, changes)
    }

    private fun disableRule(cupertino: Boolean) = runComposeUiTest {
        val lockedSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"schedule":{"type":"string","format":"cron"},
          "lock":{"type":"boolean"}}
        }""").jsonObject)
        val control = ControlElement("#/properties/schedule", rule = Rule(
            effect = RuleEffect.DISABLE,
            condition = SchemaBasedCondition("#/properties/lock", buildJsonSchema {
                put("const", JsonPrimitive(true))
            }),
        ))
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"schedule":"@daily","lock":false}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = lockedSchema, uischema = control,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onAllNodesWithText("Hour").onFirst().performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"schedule":"@daily","lock":true}""") }
        waitForIdle()
        onNodeWithText("Apply").assertDoesNotExist()
        onNodeWithContentDescription("Choose schedule").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""{"schedule":"@daily","lock":false}""") }
        waitForIdle()
        onNodeWithText("Apply").assertDoesNotExist()
        onNodeWithContentDescription("Choose schedule").assertExists()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
    }

    private fun clearAndReadonly(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@weekly"}"""))
        var readonly by mutableStateOf(false)
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"), readonly = readonly,
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onAllNodesWithContentDescription("Choose schedule").onLast().assertExists()
        runOnIdle { readonly = true; locale = "bg" }
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
        onNodeWithContentDescription("Избери график").assertDoesNotExist()
        assertEquals(JsonPrimitive("@weekly"), data.jsonObject["schedule"])
        runOnIdle { readonly = false }
        onNodeWithContentDescription("Избери график").assertExists()
        onNodeWithContentDescription("Избери график").performClick()
        assertEquals("Избран период", onAllNodesWithText("Седмица").onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Отказ").performClick()
        onAllNodesWithText("@weekly").onLast().performClick()
        onNodeWithContentDescription("Изчисти стойността").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun staged(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Hour").onFirst().performClick()
        onAllNodesWithText("Cancel").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Hour").onFirst().performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 * * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun textValidation(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data }, onValidityChange = { validity = it.status })
        }
        onAllNodesWithText("0 0 9 * * *").onLast().performTextReplacement("* * * * *")
        waitForIdle()
        assertEquals(JsonPrimitive("* * * * *"), data.jsonObject["schedule"])
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodesWithText("* * * * *").onLast().performTextReplacement("0 0 9 * * *")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun immediate(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"* * * * * *"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule", options = mapOf(
                    "showActions" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onAllNodesWithText("Hour").onLast().performClick()
        onAllNodesWithText("*").onFirst().performTextReplacement("15")
        waitForIdle()
        assertEquals(JsonPrimitive("0 15 * * * *"), data.jsonObject["schedule"])
    }
}
