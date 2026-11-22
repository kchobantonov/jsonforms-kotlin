package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ChoiceCellReadonlyUiTest {
    @Test fun materialSelectorObeysReadonly() = verify(false, false)
    @Test fun cupertinoSelectorObeysReadonly() = verify(true, false)
    @Test fun materialRadioObeysReadonly() = verify(false, true)
    @Test fun cupertinoRadioObeysReadonly() = verify(true, true)

    private fun verify(cupertino: Boolean, radio: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{"enum":[1,"1"]}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":1}]"""))
        var readonly by mutableStateOf(true)
        var writes = 0
        val options = if (radio) mapOf(
            "table" to JsonPrimitive(true),
            "cells" to Json.parseToJsonElement("""{"choice":{"format":"radio"}}"""),
        ) else mapOf("table" to JsonPrimitive(true))
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                readonly = readonly,
                onChange = { data = it.data; writes++ })
        }
        val tag = when {
            cupertino && radio -> "cupertino-radio-cell"
            cupertino -> "cupertino-enum-cell"
            radio -> "material-radio-cell"
            else -> "material-enum-cell"
        }
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        if (radio) onNodeWithContentDescription("1 (\"1\")").assertIsNotEnabled()
        else onNodeWithTag(tag, useUnmergedTree = true).assertIsNotEnabled()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes); readonly = false }
        onNodeWithContentDescription("Clear value").assertExists()
        if (radio) {
            onNodeWithContentDescription("1 (\"1\")").assertIsEnabled().performClick()
        } else {
            onNodeWithTag(tag, useUnmergedTree = true).assertIsEnabled().performClick()
            onNodeWithContentDescription("1 (\"1\")").performClick()
        }
        runOnIdle {
            assertEquals(JsonPrimitive("1"), data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
            readonly = true
        }
        if (radio) onNodeWithContentDescription("1 (\"1\")").assertIsSelected().assertIsNotEnabled()
        else onNodeWithTag(tag, useUnmergedTree = true).assertIsNotEnabled()
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"choice":1}]""") }
        runOnIdle { assertEquals(1, writes) }
    }
}
