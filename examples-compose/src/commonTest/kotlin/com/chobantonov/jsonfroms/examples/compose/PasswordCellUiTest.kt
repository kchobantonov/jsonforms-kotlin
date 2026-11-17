package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PasswordCellUiTest {
    @Test fun materialSchemaPasswordCell() = verify(false, false)
    @Test fun cupertinoSchemaPasswordCell() = verify(true, false)
    @Test fun materialUiOtpPasswordCell() = verify(false, true)
    @Test fun cupertinoUiOtpPasswordCell() = verify(true, true)

    private fun verify(cupertino: Boolean, uiOtp: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "password":{"type":"string","maxLength":4${if (uiOtp) "" else ",\"format\":\"password\""} }
          }}
        }""").jsonObject)
        val cellOptions = if (uiOtp) """{"password":{"format":"password","variant":"otp","restrict":true}}"""
            else """{"password":{"restrict":true}}"""
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"password":null}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement(cellOptions))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-password-cell" else "material-password-cell")
            .assertExists()
        assertEquals(1, onAllNodesWithText("Password").fetchSemanticsNodes().size)
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription].contains("Invalid value: null"))
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Невалидна стойност: null"))
        runOnIdle { data = Json.parseToJsonElement("""[{"password":7}]""") }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("1234")
        runOnIdle {
            assertEquals(JsonPrimitive("1234"), data.jsonArray[0].jsonObject["password"])
            assertEquals(1, writes)
        }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12345")
        runOnIdle {
            assertEquals(JsonPrimitive("1234"), data.jsonArray[0].jsonObject["password"])
            assertEquals(1, writes)
        }
    }
}
