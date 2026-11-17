package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class NumericCellUiTest {
    @Test fun materialIntegerCellKeepsInvalidValueUntilRepair() = verify(false, true)
    @Test fun cupertinoIntegerCellKeepsInvalidValueUntilRepair() = verify(true, true)
    @Test fun materialNumberCellKeepsInvalidValueUntilRepair() = verify(false, false)
    @Test fun cupertinoNumberCellKeepsInvalidValueUntilRepair() = verify(true, false)

    private fun verify(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "value":{"type":"${if (integer) "integer" else "number"}","minimum":0,"maximum":10}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"value":"7"}]"""))
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
        onNodeWithTag(if (cupertino) "cupertino-numeric-cell" else "material-numeric-cell").assertExists()
        onNodeWithText("Invalid value: \"7\"").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: \"7\""))
        onNodeWithContentDescription("Value contains errors", substring = true).assertExists()
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Невалидна стойност: \"7\"").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Невалидна стойност: \"7\"").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"value":null}]""") }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"value":"7"}]""") }
        onNodeWithText("Невалидна стойност: \"7\"").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("5")
        runOnIdle {
            assertEquals(5.0, data.jsonArray[0].jsonObject.getValue("value").jsonPrimitive.doubleOrNull)
            assertEquals(1, writes)
        }
        onNodeWithText("Невалидна стойност: \"7\"").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12")
        runOnIdle {
            assertEquals(5.0, data.jsonArray[0].jsonObject.getValue("value").jsonPrimitive.doubleOrNull)
            assertEquals(1, writes)
        }
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Извън позволения диапазон."))
        runOnIdle { data = Json.parseToJsonElement("""[{"value":8}]""") }
        assertEquals("8", onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
        runOnIdle { assertEquals(1, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(if (integer) "9" else "9.5")
        runOnIdle {
            assertEquals(if (integer) 9.0 else 9.5,
                data.jsonArray[0].jsonObject.getValue("value").jsonPrimitive.doubleOrNull)
            assertEquals(2, writes)
        }
    }
}
