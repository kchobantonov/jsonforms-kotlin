package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.input.key.Key
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
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OtpPasswordUiTest {
    private val boundedSchema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"code":{
        "type":"string","format":"password","minLength":2,"maxLength":4
      }}
    }""").jsonObject)

    @Test fun materialOtpCommitsStringAndPartialCodes() = otp(false)
    @Test fun cupertinoOtpCommitsStringAndPartialCodes() = otp(true)
    @Test fun materialOtpWithoutBothBoundsFallsBack() = fallback(false)
    @Test fun cupertinoOtpWithoutBothBoundsFallsBack() = fallback(true)
    @Test fun materialOtpKeepsKeyboardCaretAndLeadingZero() = keyboard(false)
    @Test fun cupertinoOtpKeepsKeyboardCaretAndLeadingZero() = keyboard(true)
    @Test fun materialOtpKeepsActionsInNarrowReadonlyField() = narrowReadonly(false)
    @Test fun cupertinoOtpKeepsActionsInNarrowReadonlyField() = narrowReadonly(true)
    @Test fun materialOtpShowsImportedOverflowUntilRecovery() = importedOverflow(false)
    @Test fun cupertinoOtpShowsImportedOverflowUntilRecovery() = importedOverflow(true)
    @Test fun materialTablePasswordCellKeepsOrdinaryMask() = tableCell(false)
    @Test fun cupertinoTablePasswordCellKeepsOrdinaryMask() = tableCell(true)

    private fun tableCell(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"rows":{"type":"array","items":{
            "type":"object","properties":{"code":{
              "type":"string","format":"password","minLength":2,"maxLength":4
            }}
          }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"code":"01"}]}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"code":{"variant":"otp"}}"""))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithTag("otp-password-field").assertDoesNotExist()
        onNodeWithContentDescription("Show password").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("0007")
        assertEquals(Json.parseToJsonElement("""{"rows":[{"code":"0007"}]}"""), data)
    }

    private fun importedOverflow(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"code":"01234"}"""))
        var changes = 0
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(data = data, schema = boundedSchema,
                    uischema = ControlElement("#/properties/code", options = mapOf(
                        "variant" to JsonPrimitive("otp"))),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
        }
        onNodeWithText("+1").assertExists()
        assertEquals(JsonPrimitive("01234"), data.jsonObject["code"])
        assertEquals(0, changes)
        onNodeWithTag("otp-password-field").performTextReplacement("0123")
        waitForIdle()
        onNodeWithText("+1").assertDoesNotExist()
        assertEquals(JsonPrimitive("0123"), data.jsonObject["code"])
        assertEquals(1, changes)
    }

    private fun narrowReadonly(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"code":"01"}"""))
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            Box(Modifier.width(220.dp)) {
                JsonForms(data = data, schema = boundedSchema,
                    uischema = ControlElement("#/properties/code", options = mapOf(
                        "variant" to JsonPrimitive("otp"))),
                    readonly = readonly,
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
        }
        val fieldRight = onNodeWithTag("otp-password-field").fetchSemanticsNode().boundsInRoot.right
        val revealRight = onNodeWithContentDescription("Show password")
            .fetchSemanticsNode().boundsInRoot.right
        assertTrue(revealRight <= fieldRight + 1f)
        onNodeWithContentDescription("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
        assertEquals(JsonPrimitive("01"), data.jsonObject["code"])
        assertEquals(0, changes)
    }

    private fun keyboard(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"code":"01"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = boundedSchema,
                uischema = ControlElement("#/properties/code", options = mapOf(
                    "variant" to JsonPrimitive("otp"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val field = onNodeWithTag("otp-password-field")
        field.performClick()
        field.performTextReplacement("012")
        field.performKeyInput { keyDown(Key.DirectionLeft); keyUp(Key.DirectionLeft) }
        field.performTextInput("9")
        waitForIdle()
        assertEquals(JsonPrimitive("0192"), data.jsonObject["code"])
        assertEquals(2, changes)
        field.performTextReplacement("0007")
        assertEquals(JsonPrimitive("0007"), data.jsonObject["code"])
        assertEquals(3, changes)
    }

    private fun otp(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"code":"01"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = boundedSchema,
                uischema = ControlElement("#/properties/code", options = mapOf(
                    "variant" to JsonPrimitive("otp"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        for (index in 0..3) onNodeWithTag("otp-password-box-$index").assertExists()
        onNodeWithTag("otp-password-field").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("0")
        waitForIdle()
        assertEquals(JsonPrimitive("0"), data.jsonObject["code"])
        assertEquals(CombinedValidityStatus.Invalid, validity)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("0012")
        waitForIdle()
        assertEquals(JsonPrimitive("0012"), data.jsonObject["code"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        val beforeReveal = changes
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
        assertEquals(beforeReveal, changes)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("00123")
        assertEquals(JsonPrimitive("0012"), data.jsonObject["code"])
        assertEquals(beforeReveal, changes)
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["code"])
        assertEquals(beforeReveal + 1, changes)
    }

    private fun fallback(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"code":{"type":"string","maxLength":4}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"code":"00"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/code", options = mapOf(
                    "format" to JsonPrimitive("password"), "variant" to JsonPrimitive("otp"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithTag("otp-password-field").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("000")
        assertEquals(JsonPrimitive("000"), data.jsonObject["code"])
    }
}
