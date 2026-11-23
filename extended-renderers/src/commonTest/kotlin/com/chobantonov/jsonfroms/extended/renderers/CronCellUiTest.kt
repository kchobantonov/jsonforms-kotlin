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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
class CronCellUiTest {
    @Test fun materialTableSelectsCompactCronCells() = compactTable(false)
    @Test fun cupertinoTableSelectsCompactCronCells() = compactTable(true)
    @Test fun materialCompactCronPickerCommitsWithoutActions() = immediatePicker(false)
    @Test fun cupertinoCompactCronPickerCommitsWithoutActions() = immediatePicker(true)

    private fun immediatePicker(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{"schedule":{"type":"string","format":"cron"}}
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[{"schedule":"* * * * * *"}]
        }"""))
        var readonly by mutableStateOf(false)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"schedule"}]"""),
                    "cells" to Json.parseToJsonElement("""{"schedule":{"showActions":false}}"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data; writes++ })
        }
        onAllNodesWithContentDescription("Choose schedule").onFirst().performClick()
        onNodeWithContentDescription("Repeats").performClick()
        onNodeWithText("Hourly").performClick()
        onNodeWithContentDescription("Minutes").performClick()
        onNodeWithContentDescription("Minutes: 15").performScrollTo().performClick()
        assertEquals(Json.parseToJsonElement("""{
            "rows":[{"schedule":"0 15 * * * *"}]
        }"""), data)
        assertEquals(1, writes)
        onAllNodesWithText("Hourly").onLast().assertExists()
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Choose schedule").assertCountEquals(0)
        runOnIdle { readonly = false }
        onAllNodesWithContentDescription("Choose schedule").assertCountEquals(1)
        onAllNodesWithText("Hourly").assertCountEquals(0)
        assertEquals(1, writes)
    }

    private fun compactTable(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{
                    "id":{"type":"integer"},
                    "schedule":{"type":"string","format":"cron"}
                }
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"0 0 * * * *"},
                {"id":2,"schedule":"0 30 * * * *"}
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
        onAllNodesWithContentDescription("Choose schedule").assertCountEquals(2)
        onAllNodes(hasSetTextAction() and hasText("0 30 * * * *")).onLast()
            .performTextReplacement("0 45 * * * *")
        assertEquals(Json.parseToJsonElement("""{
            "rows":[
                {"id":1,"schedule":"0 0 * * * *"},
                {"id":2,"schedule":"0 45 * * * *"}
            ]
        }"""), data)
        assertEquals(1, writes)
        onAllNodesWithContentDescription("Choose schedule").onFirst().performClick()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, writes)
        runOnIdle { locale = "bg-BG" }
        onAllNodesWithContentDescription("Избери график").assertCountEquals(2)
        runOnIdle { readonly = true }
        onAllNodesWithContentDescription("Избери график").assertCountEquals(0)
        assertEquals(1, writes)
    }
}
