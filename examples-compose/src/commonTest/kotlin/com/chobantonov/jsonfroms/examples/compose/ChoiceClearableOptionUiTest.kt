package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class ChoiceClearableOptionUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{
        "status":{"type":"string","enum":["ready","done"]},
        "channel":{"oneOf":[{"const":"yes","title":"Yes"},{"const":"no","title":"No"}]},
        "priority":{"type":"integer","enum":[0,1]},
        "empty":{"type":"string","enum":["","x"]}
      }
    }""").jsonObject)
    private val initial = Json.parseToJsonElement("""{"status":"ready","channel":"yes","priority":0,"empty":""}""")

    @Test fun materialEnumClearableAndReadOnly() = enumChoice(false)
    @Test fun cupertinoEnumClearableAndReadOnly() = enumChoice(true)
    @Test fun materialOneOfEnumClearable() = oneOfChoice(false)
    @Test fun cupertinoOneOfEnumClearable() = oneOfChoice(true)
    @Test fun materialInvalidChoiceCanBeCleared() = invalidChoice(false)
    @Test fun cupertinoInvalidChoiceCanBeCleared() = invalidChoice(true)
    @Test fun materialChoiceMenusStillSelect() = menuSelection(false)
    @Test fun cupertinoChoiceMenusStillSelect() = menuSelection(true)
    @Test fun materialEnumSearchPreservesUnknownUntilSelection() = searchPreservesUnknown(false, false)
    @Test fun materialOneOfSearchPreservesUnknownUntilSelection() = searchPreservesUnknown(true, false)
    @Test fun cupertinoEnumSearchPreservesUnknownUntilSelection() = searchPreservesUnknown(false, true)
    @Test fun cupertinoOneOfSearchPreservesUnknownUntilSelection() = searchPreservesUnknown(true, true)
    @Test fun materialEnumSearchReportsPendingUntilSelection() = searchReportsPending(false, false)
    @Test fun materialOneOfSearchReportsPendingUntilSelection() = searchReportsPending(true, false)
    @Test fun cupertinoEnumSearchReportsPendingUntilSelection() = searchReportsPending(false, true)
    @Test fun cupertinoOneOfSearchReportsPendingUntilSelection() = searchReportsPending(true, true)
    @Test fun materialEnumHostReplacementDiscardsSearch() = hostReplacementDiscardsSearch(false, false)
    @Test fun materialOneOfHostReplacementDiscardsSearch() = hostReplacementDiscardsSearch(true, false)
    @Test fun cupertinoEnumHostReplacementDiscardsSearch() = hostReplacementDiscardsSearch(false, true)
    @Test fun cupertinoOneOfHostReplacementDiscardsSearch() = hostReplacementDiscardsSearch(true, true)
    @Test fun materialChoiceClearWorksWithKeyboard() = keyboardClear(false)
    @Test fun cupertinoChoiceClearWorksWithKeyboard() = keyboardClear(true)
    @Test fun materialZeroAndEmptyAreClearableValues() = zeroAndEmpty(false)
    @Test fun cupertinoZeroAndEmptyAreClearableValues() = zeroAndEmpty(true)

    private fun zeroAndEmpty(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var field by mutableStateOf("priority")
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle { assertFalse("priority" in data.jsonObject); field = "empty" }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("empty" in data.jsonObject)
            assertEquals(2, changes)
        }
    }

    private fun keyboardClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var field by mutableStateOf("status")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        repeat(2) { index ->
            onNodeWithText(if (index == 0) "ready" else "Yes")
                .performMouseInput { moveTo(center) }
            waitForIdle()
            onNodeWithContentDescription("Clear value")
                .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            onNodeWithContentDescription("Clear value").assertIsFocused()
            onNodeWithContentDescription("Clear value").performKeyInput {
                keyDown(Key.Enter); keyUp(Key.Enter)
            }
            runOnIdle {
                assertFalse((if (index == 0) "status" else "channel") in data.jsonObject,
                    "index=$index data=$data")
                if (index == 0) field = "channel"
            }
        }
        runOnIdle { assertEquals(2, changes) }
    }

    private fun menuSelection(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var field by mutableStateOf("status")
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("ready").performClick()
        onNodeWithText("done").performClick()
        runOnIdle { assertEquals(JsonPrimitive("done"), data.jsonObject.getValue("status")); field = "channel" }
        onNodeWithText("Yes").performClick()
        onNodeWithText("No").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("no"), data.jsonObject.getValue("channel"))
            assertEquals(2, changes)
        }
    }

    private fun searchPreservesUnknown(oneOf: Boolean, cupertino: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "status"
        val target = if (oneOf) "No" else "done"
        var data by mutableStateOf(Json.parseToJsonElement("""{"$field":"unknown"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field",
                    options = mapOf("autocomplete" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(target)
        runOnIdle {
            assertEquals(JsonPrimitive("unknown"), data.jsonObject[field])
            assertEquals(0, changes)
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(if (oneOf) "N" else "d")
        if (cupertino) onNodeWithText(target).performSemanticsAction(SemanticsActions.OnClick)
        else onNodeWithText(target).performClick()
        runOnIdle {
            assertEquals(JsonPrimitive(if (oneOf) "no" else "done"), data.jsonObject[field])
            assertEquals(1, changes)
        }
    }

    private fun searchReportsPending(oneOf: Boolean, cupertino: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "status"
        val target = if (oneOf) "No" else "done"
        var data by mutableStateOf(initial)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field",
                    options = mapOf("autocomplete" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(if (oneOf) "N" else "d")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            assertEquals(initial, data)
            assertEquals(0, changes)
        }
        if (cupertino) onNodeWithText(target).performSemanticsAction(SemanticsActions.OnClick)
        else onNodeWithText(target).performClick()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(JsonPrimitive(if (oneOf) "no" else "done"), data.jsonObject[field])
            assertEquals(1, changes)
        }
    }

    private fun hostReplacementDiscardsSearch(oneOf: Boolean, cupertino: Boolean) = runComposeUiTest {
        val field = if (oneOf) "channel" else "status"
        val replacement = if (oneOf) "no" else "done"
        val label = if (oneOf) "No" else "done"
        var data by mutableStateOf(initial)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field",
                    options = mapOf("autocomplete" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(if (oneOf) "Y" else "r")
        runOnIdle {
            assertEquals(CombinedValidityStatus.Pending, validity)
            data = Json.parseToJsonElement("""{"$field":"$replacement"}""")
        }
        assertEquals(label, onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        onNodeWithText(if (oneOf) "Yes" else "ready").assertDoesNotExist()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(JsonPrimitive(replacement), data.jsonObject[field])
            assertEquals(0, changes)
        }
    }

    private fun invalidChoice(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"status":"unknown","channel":"unknown"}"""))
        var field by mutableStateOf("status")
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle { assertFalse("status" in data.jsonObject); field = "channel" }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("channel" in data.jsonObject)
            assertEquals(2, changes)
        }
    }

    private fun enumChoice(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var clearable by mutableStateOf(false)
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/status",
                    options = mapOf("clearable" to JsonPrimitive(clearable))),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { readonly = false }
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("status" in data.jsonObject)
            assertEquals(1, changes)
        }
    }

    private fun oneOfChoice(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(initial)
        var clearable by mutableStateOf(false)
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/channel",
                    options = mapOf("clearable" to JsonPrimitive(clearable))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { clearable = true }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value").performClick()
        runOnIdle {
            assertFalse("channel" in data.jsonObject)
            assertEquals(1, changes)
        }
    }
}
