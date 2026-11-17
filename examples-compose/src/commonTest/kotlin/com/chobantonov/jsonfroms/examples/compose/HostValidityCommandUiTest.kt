package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.ValidationMode
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HostValidityCommandUiTest {
    @Test fun materialHostCommandUsesCombinedValidity() = guardedCommand(false)
    @Test fun cupertinoHostCommandUsesCombinedValidity() = guardedCommand(true)

    private fun guardedCommand(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"date":{"type":"string","format":"date",
            "formatMinimum":"2025-01-10"}},"required":["date"]
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"date":"2025-01-10"}"""))
        var hostErrors by mutableStateOf(emptyList<ErrorObject>())
        var mode by mutableStateOf(ValidationMode.ValidateAndShow)
        var validity by mutableStateOf<CombinedValidity?>(null)
        var submits = 0
        setContent {
            Column {
                JsonForms(
                    data = data, schema = schema,
                    uischema = ControlElement("#/properties/date",
                        options = mapOf("mask" to JsonPrimitive(false))),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    additionalErrors = hostErrors,
                    validationMode = mode,
                    onChange = { data = it.data },
                    onValidityChange = { validity = it },
                )
                Button(onClick = { submits++ }, enabled = validity?.status == CombinedValidityStatus.Valid) {
                    Text("Submit")
                }
            }
        }
        onNodeWithText("Submit").assertIsEnabled().performClick()
        assertEquals(1, submits)

        runOnIdle { hostErrors = listOf(ErrorObject(instancePath = "/date", message = "server")) }
        onNodeWithText("Submit").assertIsNotEnabled()
        assertEquals(CombinedValidityStatus.Invalid, validity?.status)
        assertEquals(1, validity?.additionalErrorCount)
        runOnIdle { hostErrors = emptyList() }
        onNodeWithText("Submit").assertIsEnabled()

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("not-a-date")
        onNodeWithText("Submit").assertIsNotEnabled()
        assertEquals(CombinedValidityStatus.Pending, validity?.status)
        assertEquals(true, validity?.pending)
        assertEquals(Json.parseToJsonElement("""{"date":"2025-01-10"}"""), data)

        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("01/09/2025")
        onNodeWithText("Submit").assertIsNotEnabled()
        assertEquals(CombinedValidityStatus.Invalid, validity?.status)
        assertEquals(1, validity?.schemaErrorCount)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("01/11/2025")
        onNodeWithText("Submit").assertIsEnabled().performClick()
        assertEquals(CombinedValidityStatus.Valid, validity?.status)
        assertEquals(2, submits)

        runOnIdle { mode = ValidationMode.NoValidation }
        onNodeWithText("Submit").assertIsNotEnabled()
        assertEquals(CombinedValidityStatus.Unvalidated, validity?.status)
        runOnIdle { mode = ValidationMode.ValidateAndShow }
        onNodeWithText("Submit").assertIsEnabled()
        assertEquals(2, submits)
    }
}
