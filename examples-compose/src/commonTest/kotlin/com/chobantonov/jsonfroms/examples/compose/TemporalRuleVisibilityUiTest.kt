package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LeafCondition
import com.chobantonov.jsonfroms.core.models.Rule
import com.chobantonov.jsonfroms.core.models.RuleEffect
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalRuleVisibilityUiTest {
    @Test fun materialDatePickerClosesWhenRuleHidesControl() = hidesPicker(false, "date")
    @Test fun cupertinoDatePickerClosesWhenRuleHidesControl() = hidesPicker(true, "date")
    @Test fun materialTimePickerClosesWhenRuleHidesControl() = hidesPicker(false, "time")
    @Test fun cupertinoTimePickerClosesWhenRuleHidesControl() = hidesPicker(true, "time")
    @Test fun materialDateTimePickerClosesWhenRuleHidesControl() = hidesPicker(false, "date-time")
    @Test fun cupertinoDateTimePickerClosesWhenRuleHidesControl() = hidesPicker(true, "date-time")
    @Test fun materialInvalidDateDraftClearsWhenRuleHidesControl() = hidesPendingDraft(false)
    @Test fun cupertinoInvalidDateDraftClearsWhenRuleHidesControl() = hidesPendingDraft(true)

    private fun hidesPendingDraft(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"show":{"type":"boolean"},"value":{"type":"string","format":"date"}}
        }""").jsonObject)
        val committed = "2025-01-10"
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true,"value":"$committed"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/value",
                    options = mapOf("mask" to JsonPrimitive(false)),
                    rule = Rule(RuleEffect.SHOW, LeafCondition("#/show", JsonPrimitive(true)))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
                onValidityChange = { validity = it.status },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("not-a-date")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(0, changes)
        }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        runOnIdle { data = Json.parseToJsonElement("""{"show":false,"value":"$committed"}""") }
        onAllNodes(hasSetTextAction()).onFirst().assertDoesNotExist()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(0, changes)
        }
    }

    private fun hidesPicker(cupertino: Boolean, format: String) = runComposeUiTest {
        val value = when (format) {
            "date" -> "2025-01-10"
            "time" -> "09:30:00"
            else -> "2025-01-10T09:30:00Z"
        }
        val action = when (format) {
            "date" -> "Choose date"
            "time" -> "Choose time"
            else -> "Choose date and time"
        }
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"show":{"type":"boolean"},"value":{"type":"string","format":"$format"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"show":true,"value":"$value"}"""))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "showActions" to JsonPrimitive(true)), rule = Rule(
                    RuleEffect.SHOW, LeafCondition("#/show", JsonPrimitive(true)))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription(action).performClick()
        onNodeWithText("Cancel").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"show":false,"value":"$value"}""") }
        onNodeWithContentDescription(action).assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        assertEquals(0, changes)
    }
}
