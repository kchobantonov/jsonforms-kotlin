package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CronControlUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"schedule":{"type":"string","format":"cron"}}
    }""").jsonObject)

    @Test fun materialPickerStagesAndPreservesAlias() = staged(false)
    @Test fun cupertinoPickerStagesAndPreservesAlias() = staged(true)
    @Test fun materialQuotedShowActionsStillStages() = staged(false, JsonPrimitive("false"))
    @Test fun cupertinoQuotedShowActionsStillStages() = staged(true, JsonPrimitive("false"))
    @Test fun materialInvalidLocalShowActionsUsesGlobal() = globalShowActions(false)
    @Test fun cupertinoInvalidLocalShowActionsUsesGlobal() = globalShowActions(true)
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
    @Test fun materialCronMultiSelectAndReset() = multiSelectAndReset(false)
    @Test fun cupertinoCronMultiSelectAndReset() = multiSelectAndReset(true)
    @Test fun materialCronScrollsToLastSecond() = scrollToLastSecond(false)
    @Test fun cupertinoCronScrollsToLastSecond() = scrollToLastSecond(true)
    @Test fun materialCronResetStaysSeparateFromDropdown() = resetWithoutOpeningMenu(false)
    @Test fun cupertinoCronResetStaysSeparateFromDropdown() = resetWithoutOpeningMenu(true)
    @Test fun materialCronResetKeepsKeyboardFocus() = keyboardReset(false)
    @Test fun cupertinoCronResetKeepsKeyboardFocus() = keyboardReset(true)
    @Test fun materialCronTextClearKeepsKeyboardFocus() = keyboardTextClear(false)
    @Test fun cupertinoCronTextClearKeepsKeyboardFocus() = keyboardTextClear(true)
    @Test fun materialCronAdvancedAndExpressionClearStayStaged() = clearPopupDraft(false)
    @Test fun cupertinoCronAdvancedAndExpressionClearStayStaged() = clearPopupDraft(true)
    @Test fun materialCronPopupFitsNarrowHost() = narrowPopup(false)
    @Test fun cupertinoCronPopupFitsNarrowHost() = narrowPopup(true)
    @Test fun materialBulgarianCronPopupFitsNarrowHost() = narrowPopup(false, "bg")
    @Test fun cupertinoBulgarianCronPopupFitsNarrowHost() = narrowPopup(true, "bg")
    @Test fun materialUnsetCronWaitsForFieldEdit() = unsetPicker(false)
    @Test fun cupertinoUnsetCronWaitsForFieldEdit() = unsetPicker(true)
    @Test fun materialBulgarianUnsetCronWaitsForFieldEdit() = unsetPicker(false, "bg")
    @Test fun cupertinoBulgarianUnsetCronWaitsForFieldEdit() = unsetPicker(true, "bg")
    @Test fun materialUnparseableCronWaitsForFieldEdit() = unsetPicker(false, initial = "0 0 9")
    @Test fun cupertinoUnparseableCronWaitsForFieldEdit() = unsetPicker(true, initial = "0 0 9")

    private fun globalShowActions(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}""")
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule",
                    options = mapOf("showActions" to local)),
                config = mapOf("showActions" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers)
        }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").assertCountEquals(0)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        runOnIdle { global = true }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").onLast().assertExists()
        onAllNodesWithText("Cancel").onLast().performClick()
        runOnIdle { local = JsonPrimitive(false) }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").assertCountEquals(0)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        runOnIdle { global = false; local = JsonPrimitive(true) }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").onLast().assertExists()
    }

    private fun unsetPicker(cupertino: Boolean, locale: String = "en",
        initial: String? = null) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement(if (initial == null) "{}"
            else """{"schedule":"$initial"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        val choose = if (locale == "bg") "Избери график" else "Choose schedule"
        val prompt = if (locale == "bg") "Избери честота" else "Choose how often"
        val repeats = if (locale == "bg") "Повтаря се" else "Repeats"
        val daily = if (locale == "bg") "Ежедневно" else "Daily"
        val hours = if (locale == "bg") "Часове" else "Hours"
        val apply = if (locale == "bg") "Приложи" else "Apply"
        onNodeWithContentDescription(choose).performClick()
        onNodeWithText(prompt).assertExists()
        if (initial != null)
            onNodeWithContentDescription(if (locale == "bg") "Израз" else "Expression")
                .assertTextContains(initial)
        assertEquals(prompt, onNodeWithContentDescription(repeats)
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithContentDescription(hours).assertDoesNotExist()
        onNodeWithContentDescription(repeats).performClick()
        onAllNodesWithText(daily).onLast().performClick()
        onNodeWithContentDescription(hours).assertExists()
        onNodeWithText(apply).performClick()
        waitForIdle()
        assertEquals(initial?.let(::JsonPrimitive), data.jsonObject["schedule"])
        assertEquals(0, changes)

        onNodeWithContentDescription(choose).performClick()
        onNodeWithContentDescription(repeats).performClick()
        onAllNodesWithText(daily).onLast().performClick()
        onNodeWithContentDescription(hours).performClick()
        onNodeWithContentDescription("$hours: 9").performScrollTo().performClick()
        onNodeWithContentDescription(hours).performClick()
        onNodeWithText(apply).performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 9 * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun narrowPopup(cupertino: Boolean, locale: String = "en") = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}""")
        var changes = 0
        setContent {
            Box(Modifier.width(240.dp).testTag("cron-narrow-host")) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/schedule"),
                    i18n = JsonFormsI18nState(locale = locale),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    onChange = { changes++ })
            }
        }
        onAllNodesWithContentDescription(if (locale == "bg") "Избери график"
            else "Choose schedule").onLast().performClick()
        val host = onNodeWithTag("cron-narrow-host").fetchSemanticsNode().boundsInRoot
        val panel = onNodeWithTag("cron-picker-panel", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(panel.width <= host.width + 2f,
            "Cron popup must fit the narrow host: panel=${panel.width} host=${host.width}")
        for (field in if (locale == "bg") listOf("Секунди", "Минути", "Часове")
            else listOf("Seconds", "Minutes", "Hours")) {
            val button = onNodeWithContentDescription(field).fetchSemanticsNode().boundsInRoot
            assertTrue(button.left >= panel.left - 2f && button.right <= panel.right + 2f,
                "$field selector must stay inside the popup")
        }
        val expression = onNodeWithContentDescription(if (locale == "bg") "Израз"
            else "Expression").fetchSemanticsNode().boundsInRoot
        assertTrue(expression.right <= panel.right + 2f,
            "Expression editor must stay inside the popup")
        assertEquals(0, changes)
    }

    private fun clearPopupDraft(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 L * ?"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithText("This selection uses advanced cron syntax.").assertExists()
        onNodeWithContentDescription("Days of month").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value: Days of month")
            .performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("This selection uses advanced cron syntax.").assertDoesNotExist()
        onNodeWithContentDescription("Expression").assertTextContains("0 0 9 * * ?")
        assertEquals(0, changes)
        onNodeWithContentDescription("Expression").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value: Expression")
            .performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Invalid cron expression.").assertDoesNotExist()
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(""), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun keyboardReset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Hours")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Every: Hours").assertExists()
        onNodeWithContentDescription("Hours").performKeyInput { keyDown(Key.Tab); keyUp(Key.Tab) }
        onNodeWithContentDescription("Every: Hours").assertIsFocused()
            .performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        onNodeWithContentDescription("Expression").assertTextContains("0 0 * * * *")
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 * * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun keyboardTextClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 L * ?"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Days of month")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value: Days of month").assertExists()
        onNodeWithContentDescription("Days of month").performKeyInput {
            keyDown(Key.Tab); keyUp(Key.Tab)
        }
        onNodeWithContentDescription("Clear value: Days of month").assertIsFocused()
            .performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        onNodeWithContentDescription("Clear value: Days of month").assertDoesNotExist()
        onNodeWithContentDescription("Expression").assertTextContains("0 0 9 * * ?")
        onNodeWithContentDescription("Expression")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value: Expression").assertExists()
        onNodeWithContentDescription("Expression").performKeyInput {
            keyDown(Key.Tab); keyUp(Key.Tab)
        }
        onNodeWithContentDescription("Clear value: Expression").assertIsFocused()
            .performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        onNodeWithContentDescription("Clear value: Expression").assertDoesNotExist()
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(""), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun resetWithoutOpeningMenu(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Every: Hours").assertDoesNotExist()
        onNodeWithContentDescription("Hours").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Every: Hours").assertExists()
        onNodeWithContentDescription("Every: Hours").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithContentDescription("Hours: 10").assertDoesNotExist()
        onNodeWithContentDescription("Expression").assertTextContains("0 0 * * * *")
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 * * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun multiSelectAndReset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Seconds").performClick()
        assertEquals(true, onNodeWithContentDescription("Seconds: 0")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(false, onNodeWithContentDescription("Seconds: 1")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithContentDescription("Seconds: 1").performClick()
        assertEquals(true, onNodeWithContentDescription("Seconds: 1")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithContentDescription("Seconds: 2").performClick()
        onNodeWithContentDescription("Seconds: 3").performClick()
        onNodeWithContentDescription("Seconds").performClick()
        onNodeWithContentDescription("Expression").assertTextContains("0-3 0 9 * * *")
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0-3 0 9 * * *"), data.jsonObject["schedule"])
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Seconds").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Every: Seconds").performSemanticsAction(SemanticsActions.OnClick)
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("* 0 9 * * *"), data.jsonObject["schedule"])
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Hours").performClick()
        onNodeWithContentDescription("Hours: 10").performScrollTo().performClick()
        onNodeWithContentDescription("Hours").performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 9,10 * * *"), data.jsonObject["schedule"])
        assertEquals(3, changes)
    }

    private fun scrollToLastSecond(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        onNodeWithContentDescription("Seconds").performClick()
        onNodeWithContentDescription("Seconds: 59").performScrollTo().performClick()
        onNodeWithContentDescription("Seconds").performClick()
        onNodeWithContentDescription("Expression").assertTextContains("0,59 0 9 * * *")
        assertEquals(0, changes)
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0,59 0 9 * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun accessiblePeriod(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        assertEquals("Selected period", onNodeWithContentDescription("Repeats")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithContentDescription("Repeats").performClick()
        assertEquals(true, onAllNodesWithText("Daily").onLast()
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithText("Hourly").performClick()
        onNodeWithContentDescription("Repeats").performClick()
        assertEquals(true, onAllNodesWithText("Hourly").onLast()
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(false, onAllNodesWithText("Daily").onLast()
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
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
        onNodeWithContentDescription("Expression").performTextReplacement("0 60 * * * *")
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
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
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
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
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
        assertEquals("Избран период", onNodeWithContentDescription("Повтаря се")
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        onNodeWithText("Отказ").performClick()
        onAllNodesWithText("@weekly").onLast().performClick()
        onNodeWithContentDescription("Изчисти стойността").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun staged(cupertino: Boolean, showActionsOption: JsonPrimitive? = null) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"@daily"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule", options =
                    showActionsOption?.let { mapOf("showActions" to it) } ?: emptyMap()),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
        onAllNodesWithText("Cancel").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("@daily"), data.jsonObject["schedule"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose schedule").onLast().performClick()
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("0 0 * * * *"), data.jsonObject["schedule"])
        assertEquals(1, changes)
    }

    private fun textValidation(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"schedule":"0 0 9 * * *"}"""))
        var validity: CombinedValidityStatus? = null
        var schemaErrors = -1
        var additionalErrors = -1
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/schedule"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data }, onValidityChange = {
                    validity = it.status
                    schemaErrors = it.schemaErrorCount
                    additionalErrors = it.additionalErrorCount
                })
        }
        onAllNodesWithText("0 0 9 * * *").onLast().performTextReplacement("* * * * *")
        waitForIdle()
        assertEquals(JsonPrimitive("* * * * *"), data.jsonObject["schedule"])
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(1, schemaErrors)
        assertEquals(0, additionalErrors)
        onAllNodesWithText("* * * * *").onLast().performTextReplacement("0 0 9 * * *")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(0, schemaErrors)
        assertEquals(0, additionalErrors)
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
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
        onNodeWithContentDescription("Minutes").performClick()
        onNodeWithContentDescription("Minutes: 15").performScrollTo().performClick()
        onNodeWithContentDescription("Expression").assertTextContains("0 15 * * * *")
        waitForIdle()
        assertEquals(JsonPrimitive("0 15 * * * *"), data.jsonObject["schedule"])
    }
}
