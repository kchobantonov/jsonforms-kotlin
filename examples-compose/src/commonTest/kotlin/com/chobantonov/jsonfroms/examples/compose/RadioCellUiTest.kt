package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
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
class RadioCellUiTest {
    @Test fun materialEnumRadioCellPreservesTypedValues() = verify(false, false)
    @Test fun cupertinoEnumRadioCellPreservesTypedValues() = verify(true, false)
    @Test fun materialOneOfRadioCellPreservesTitles() = verify(false, true)
    @Test fun cupertinoOneOfRadioCellPreservesTitles() = verify(true, true)
    @Test fun materialRadioCellPrefersEnumOverOneOf() = verifyEnumPriority(false)
    @Test fun cupertinoRadioCellPrefersEnumOverOneOf() = verifyEnumPriority(true)

    private fun verify(cupertino: Boolean, oneOf: Boolean) = runComposeUiTest {
        val choices = if (oneOf) """"oneOf":[
          {"const":1,"title":"Same"},{"const":"1","title":"Same"}
        ]""" else """"enum":[1,"1"]"""
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{$choices}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":42}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        val options = mapOf(
            "table" to JsonPrimitive(true),
            "cells" to Json.parseToJsonElement("""{"choice":{"format":"radio"}}"""),
        )
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        val tag = if (cupertino) "cupertino-radio-cell" else "material-radio-cell"
        val label = if (oneOf) "Same" else "1"
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        assertEquals(1, onAllNodesWithText("Choice").fetchSemanticsNodes().size)
        onNodeWithText("Current value outside choices: 42").assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Текуща стойност извън опциите: 42").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"choice":42}]""") }
        runOnIdle { assertEquals(0, writes) }
        onNodeWithContentDescription("$label (1)").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive(1), data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
        }
        onNodeWithContentDescription("$label (1)").assertIsSelected()
        onNodeWithContentDescription("$label (\"1\")").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("1"), data.jsonArray[0].jsonObject["choice"])
            assertEquals(2, writes)
        }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
    }

    private fun verifyEnumPriority(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{"enum":[1,"1"],"oneOf":[{"const":2},{"const":3}]}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":1}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"choice":{"format":"radio"}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-radio-cell" else "material-radio-cell",
            useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("1 (1)").assertExists()
        onNodeWithContentDescription("2").assertDoesNotExist()
        onNodeWithContentDescription("1 (\"1\")").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("1"), data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
        }
    }
}
