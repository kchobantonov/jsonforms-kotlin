package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MixedNumericIntentUiTest {
    @Test fun materialNumericEditorSwitchPreservesValueAndIntent() = scenario(false)
    @Test fun cupertinoNumericEditorSwitchPreservesValueAndIntent() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":["number","integer"]}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":1.5}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"typeChange":"always"}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onNodeWithText("Number").performClick()
        onNodeWithText("Integer").performClick()
        onNodeWithText("Change this type?").assertDoesNotExist()
        onNodeWithText("Invalid integer.").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":1.5}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(0, changes)
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("2")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":2}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithText("Integer").performClick()
        onNodeWithText("Number").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":2}"""), data)
            assertEquals(1, changes)
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("2.5")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":2.5}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(2, changes)
        }
        onNodeWithText("Number").performClick()
        onNodeWithText("Integer").performClick()
        onNodeWithText("Invalid integer.").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"value":2.5}"""), data)
            assertEquals(CombinedValidityStatus.Valid, validity)
            assertEquals(2, changes)
        }
    }
}
