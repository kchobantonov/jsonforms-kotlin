package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.input.key.Key
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OneOfSelectorClearUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"choice":{"type":"object","title":"Choice",
        "properties":{"name":{"type":"string"}},
        "oneOf":[
          {"title":"Email contact","type":"object","properties":{"kind":{"const":"email","default":"email"},"email":{"type":"string"}},"required":["kind","email"]},
          {"title":"Phone contact","type":"object","properties":{"kind":{"const":"phone","default":"phone"},"phone":{"type":"string","default":""}},"required":["kind","phone"]}
        ]}}}""").jsonObject)
    private val original = Json.parseToJsonElement("""{"choice":{"name":"Alex","kind":"email","email":"alex@example.test"}}""")
    private val preserved = Json.parseToJsonElement("""{"choice":{"name":"Alex"}}""")
    private val phone = Json.parseToJsonElement("""{"choice":{"name":"Alex","kind":"phone","phone":""}}""")
    private val hostReplacement = Json.parseToJsonElement("""{"choice":{"name":"Host","kind":"email","email":"host@example.test"}}""")

    @Test fun materialClearPreservesEnclosingFieldAndNoSelection() = clear(false)
    @Test fun cupertinoClearPreservesEnclosingFieldAndNoSelection() = clear(true)
    @Test fun materialEmptySelectedBranchCanClear() = emptyBranch(false)
    @Test fun cupertinoEmptySelectedBranchCanClear() = emptyBranch(true)
    @Test fun materialSwitchPreservesEnclosingField() = switch(false)
    @Test fun cupertinoSwitchPreservesEnclosingField() = switch(true)
    @Test fun materialClearableFalseHidesAction() = optOut(false)
    @Test fun cupertinoClearableFalseHidesAction() = optOut(true)
    @Test fun materialReadOnlyRevokesPendingClear() = readonly(false)
    @Test fun cupertinoReadOnlyRevokesPendingClear() = readonly(true)
    @Test fun materialHostReplacementInvalidatesPendingClear() = stale(false)
    @Test fun cupertinoHostReplacementInvalidatesPendingClear() = stale(true)
    @Test fun materialKeyboardClearHasAccessibleName() = keyboardClear(false)
    @Test fun cupertinoKeyboardClearHasAccessibleName() = keyboardClear(true)

    private fun keyboardClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Email contact")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("oneof-selector-clear").assertContentDescriptionEquals("Clear value")
        onNodeWithTag("oneof-selector-clear")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("oneof-selector-clear").performKeyInput {
            keyDown(Key.Enter); keyUp(Key.Enter)
        }
        onNodeWithText("Change this branch?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(preserved, data); assertEquals(1, changes) }
    }

    private fun clear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithTag("oneof-selector-clear").performClick()
        onNodeWithText("Change this branch?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithTag("oneof-selector-clear").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(preserved, data); assertEquals(1, changes) }
        onNodeWithText("Email contact").assertDoesNotExist()
        onNodeWithTag("oneof-selector-clear").assertDoesNotExist()
        onNodeWithText("Choice").performClick()
        waitForIdle()
        onNodeWithText("Phone contact").performClick()
        runOnIdle { assertEquals(phone, data); assertEquals(2, changes) }
    }

    private fun emptyBranch(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(preserved)
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithTag("oneof-selector-clear").assertExists()
        onNodeWithTag("oneof-selector-clear").performClick()
        onNodeWithText("Change this branch?").assertDoesNotExist()
        onNodeWithTag("oneof-selector-clear").assertDoesNotExist()
        runOnIdle { assertEquals(preserved, data) }
    }

    private fun switch(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithText("Email contact").performClick()
        waitForIdle()
        onNodeWithText("Phone contact").performClick()
        onNodeWithText("Change this branch?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(phone, data) }
    }

    private fun optOut(cupertino: Boolean) = runComposeUiTest {
        setContent {
            JsonForms(data = original, schema = schema,
                uischema = ControlElement("#/properties/choice", options = mapOf("clearable" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithTag("oneof-selector-clear").assertDoesNotExist()
        onNodeWithText("Email contact").assertExists()
    }

    private fun readonly(cupertino: Boolean) = runComposeUiTest {
        var readonly by mutableStateOf(false)
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithTag("oneof-selector-clear").performClick()
        onNodeWithText("Change this branch?").assertExists()
        runOnIdle { readonly = true }
        onNodeWithText("Change this branch?").assertDoesNotExist()
        onNodeWithTag("oneof-selector-clear").assertDoesNotExist()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
    }

    private fun stale(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/choice"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithTag("oneof-selector-clear").performClick()
        onNodeWithText("Change this branch?").assertExists()
        runOnIdle { data = hostReplacement }
        onNodeWithText("Change this branch?").assertDoesNotExist()
        runOnIdle { assertEquals(hostReplacement, data); assertEquals(0, changes) }
    }
}
