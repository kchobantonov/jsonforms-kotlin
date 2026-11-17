package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ColorCellUiTest {
    @Test fun materialTableSelectsCompactColorCells() = compactTable(false)
    @Test fun cupertinoTableSelectsCompactColorCells() = compactTable(true)
    @Test fun materialPickerOnlyColorCell() = pickerOnlyTable(false)
    @Test fun cupertinoPickerOnlyColorCell() = pickerOnlyTable(true)
    @Test fun materialHostReorderDiscardsStaleColorDraft() = hostReorder(false)
    @Test fun cupertinoHostReorderDiscardsStaleColorDraft() = hostReorder(true)

    private fun hostReorder(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{
                    "id":{"type":"integer"},"shade":{"type":"string","format":"color"}
                }
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[{"id":1,"shade":"#ff0000"},{"id":2,"shade":"#0000ff"}]
        }"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"shade"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data; writes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("#0000ff")).onLast()
            .performTextReplacement("rgb(0, 255")
        assertEquals(0, writes)
        runOnIdle { data = Json.parseToJsonElement("""{
            "rows":[{"id":2,"shade":"#0000ff"},{"id":1,"shade":"#ff0000"}]
        }""") }
        onAllNodes(hasSetTextAction() and hasText("#0000ff")).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("#ff0000")).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("rgb(0, 255")).assertCountEquals(0)
        assertEquals(0, writes)
    }

    private fun pickerOnlyTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{"shade":{"type":"string","format":"color"}}
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"shade":"#ff0000"}]}"""))
        var readonly by mutableStateOf(false)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"shade"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                config = mapOf("jsonformsExtended" to mapOf("colorTextEntry" to false)),
                onChange = { data = it.data; writes++ })
        }
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        onAllNodesWithContentDescription("Choose color").assertCountEquals(1)
        onAllNodesWithContentDescription("Choose color").onFirst().performClick()
        onNodeWithContentDescription("Color #22c55e").performClick()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"shade":"#22c55e"}]}"""), data)
        assertEquals(1, writes)
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Choose color").assertCountEquals(0)
        assertEquals(1, writes)
    }

    private fun compactTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{
                    "id":{"type":"integer"},
                    "schedule":{"type":"string","format":"color"}
                }
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"#ff0000"},
                {"id":2,"schedule":"#0000ff"}
            ]
        }"""))
        var readonly by mutableStateOf(false)
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"schedule"}]"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        onAllNodesWithContentDescription("Choose color").assertCountEquals(2)
        onAllNodes(hasSetTextAction() and hasText("#0000ff")).onLast()
            .performTextReplacement("rgb(0, 255")
        assertEquals(0, writes)
        onAllNodes(hasSetTextAction() and hasText("rgb(0, 255")).onLast()
            .performTextReplacement("#00ff00")
        assertEquals(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"#ff0000"},
                {"id":2,"schedule":"#00ff00"}
            ]
        }"""), data)
        assertEquals(1, writes)
        onAllNodesWithContentDescription("Choose color").onFirst().performClick()
        onAllNodesWithContentDescription("Close").onFirst().performClick()
        assertEquals(1, writes)
        runOnIdle { locale = "bg-BG" }
        onAllNodesWithContentDescription("Избери цвят").assertCountEquals(2)
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Избери цвят").assertCountEquals(0)
        runOnIdle { data = Json.parseToJsonElement("""{
            "rows":[{"id":1,"schedule":"#ff0000"},{"id":2,"schedule":"#123456"}]
        }""") }
        onAllNodes(hasText("#123456")).assertCountEquals(1)
        assertEquals(1, writes)
    }
}
