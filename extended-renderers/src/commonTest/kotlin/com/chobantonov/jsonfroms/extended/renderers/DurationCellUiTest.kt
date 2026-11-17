package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
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
class DurationCellUiTest {
    @Test fun materialTableSelectsCompactDurationCells() = compactTable(false)
    @Test fun cupertinoTableSelectsCompactDurationCells() = compactTable(true)
    @Test fun materialCompactDurationPickerCommitsWithoutActions() = immediatePicker(false)
    @Test fun cupertinoCompactDurationPickerCommitsWithoutActions() = immediatePicker(true)

    private fun immediatePicker(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{"duration":{"type":"string","format":"duration"}}
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[{"duration":"PT150M"}]
        }"""))
        var readonly by mutableStateOf(false)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"duration"}]"""),
                    "cells" to Json.parseToJsonElement("""{"duration":{"showActions":false}}"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data; writes++ })
        }
        onAllNodesWithContentDescription("Choose duration").onFirst().performClick()
        onAllNodesWithText("150").onLast().performTextReplacement("151")
        assertEquals(Json.parseToJsonElement("""{
            "rows":[{"duration":"PT151M"}]
        }"""), data)
        assertEquals(1, writes)
        onAllNodesWithText("151").onLast().assertExists()
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Choose duration").assertCountEquals(0)
        runOnIdle { readonly = false }
        onAllNodesWithContentDescription("Choose duration").assertCountEquals(1)
        onAllNodesWithText("151").assertCountEquals(0)
        assertEquals(1, writes)
    }

    private fun compactTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{
                    "id":{"type":"integer"},
                    "schedule":{"type":"string","format":"duration"}
                }
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"PT1H"},
                {"id":2,"schedule":"PT30M"}
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
        onAllNodesWithContentDescription("Choose duration").assertCountEquals(2)
        onAllNodes(hasSetTextAction() and hasText("PT30M")).onLast()
            .performTextReplacement("PT45M")
        assertEquals(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"PT1H"},
                {"id":2,"schedule":"PT45M"}
            ]
        }"""), data)
        assertEquals(1, writes)
        onAllNodesWithContentDescription("Choose duration").onFirst().performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, writes)
        runOnIdle { locale = "bg-BG" }
        onAllNodesWithContentDescription("Избери продължителност").assertCountEquals(2)
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Избери продължителност").assertCountEquals(0)
        assertEquals(1, writes)
    }
}
