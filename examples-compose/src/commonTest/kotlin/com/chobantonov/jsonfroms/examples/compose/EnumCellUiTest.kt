package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
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
class EnumCellUiTest {
    @Test fun materialEnumCellPreservesTypedValues() = verify(false)
    @Test fun cupertinoEnumCellPreservesTypedValues() = verify(true)
    @Test fun materialOneOfCellPreservesTitlesAndTypedValues() = verifyOneOf(false)
    @Test fun cupertinoOneOfCellPreservesTitlesAndTypedValues() = verifyOneOf(true)
    @Test fun materialStringConstCellRepairsInvalidImport() = verifyConst(false, false)
    @Test fun cupertinoStringConstCellRepairsInvalidImport() = verifyConst(true, false)
    @Test fun materialNumberConstCellRepairsInvalidImport() = verifyConst(false, true)
    @Test fun cupertinoNumberConstCellRepairsInvalidImport() = verifyConst(true, true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{"enum":[1,"1",true]}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":42}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        val tag = if (cupertino) "cupertino-enum-cell" else "material-enum-cell"
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        onNodeWithText("Current value outside choices: 42").assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Текуща стойност извън опциите: 42").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"choice":42}]""") }
        runOnIdle { assertEquals(0, writes) }
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription("1 (1)").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive(1), data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
        }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription("1 (\"1\")").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("1"), data.jsonArray[0].jsonObject["choice"])
            assertEquals(2, writes)
        }
    }

    private fun verifyOneOf(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{"oneOf":[
              {"const":1,"title":"Same"},
              {"const":"1","title":"Same"}
            ]}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":42}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        val tag = if (cupertino) "cupertino-enum-cell" else "material-enum-cell"
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        onNodeWithText("Current value outside choices: 42").assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Текуща стойност извън опциите: 42").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"choice":42}]""") }
        runOnIdle { assertEquals(0, writes) }
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription("Same (1)").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive(1), data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
        }
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription("Same (\"1\")").performClick()
        runOnIdle {
            assertEquals(JsonPrimitive("1"), data.jsonArray[0].jsonObject["choice"])
            assertEquals(2, writes)
        }
    }

    private fun verifyConst(cupertino: Boolean, numeric: Boolean) = runComposeUiTest {
        val choice = if (numeric) "5" else "\"ready\""
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "choice":{"const":$choice}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"choice":42}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf("table" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        val tag = if (cupertino) "cupertino-enum-cell" else "material-enum-cell"
        onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        onNodeWithText("Current value outside choices: 42").assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Текуща стойност извън опциите: 42").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Текуща стойност извън опциите: 42").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"choice":null}]""") }
        onNodeWithText("Текуща стойност извън опциите: null").assertExists()
        runOnIdle { assertEquals(0, writes) }
        onNodeWithTag(tag, useUnmergedTree = true).performClick()
        onNodeWithContentDescription(if (numeric) "5" else "ready").performClick()
        runOnIdle {
            assertEquals(if (numeric) JsonPrimitive(5) else JsonPrimitive("ready"),
                data.jsonArray[0].jsonObject["choice"])
            assertEquals(1, writes)
        }
        onNodeWithText("Текуща стойност извън опциите: null").assertDoesNotExist()
    }
}
