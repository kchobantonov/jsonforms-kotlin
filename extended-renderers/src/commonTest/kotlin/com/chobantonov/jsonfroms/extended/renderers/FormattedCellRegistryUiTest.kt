package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class FormattedCellRegistryUiTest {
    @Test fun materialColorUiFormatBeatsSchemaPassword() = conflict(false, "color", "#ff0000", "#00ff00", "Choose color")
    @Test fun cupertinoColorUiFormatBeatsSchemaPassword() = conflict(true, "color", "#ff0000", "#00ff00", "Choose color")
    @Test fun materialCronUiFormatBeatsSchemaPassword() = conflict(false, "cron", "0 0 * * * *", "0 15 * * * *", "Choose schedule")
    @Test fun cupertinoCronUiFormatBeatsSchemaPassword() = conflict(true, "cron", "0 0 * * * *", "0 15 * * * *", "Choose schedule")
    @Test fun materialDurationUiFormatBeatsSchemaPassword() = conflict(false, "duration", "PT1H", "PT2H", "Choose duration")
    @Test fun cupertinoDurationUiFormatBeatsSchemaPassword() = conflict(true, "duration", "PT1H", "PT2H", "Choose duration")

    private fun conflict(cupertino: Boolean, format: String, initial: String, next: String,
        action: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"rows":{"type":"array","items":{
                "type":"object","properties":{
                    "id":{"type":"integer"},
                    "value":{"type":"string","format":"password"},
                    "other":{"type":"string"}
                }
            }}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{
            "rows":[{"id":1,"value":"$initial","other":"kept"}]
        }"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "columnDefs" to Json.parseToJsonElement("""[{"field":"value"}]"""),
                    "cells" to Json.parseToJsonElement("""{"value":{"format":"$format"}}"""))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                cells = if (cupertino) cupertinoExtendedCells else materialExtendedCells,
                onChange = { data = it.data; writes++ })
        }
        onAllNodesWithContentDescription(action).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText(initial)).onLast()
            .performTextReplacement(next)
        assertEquals(Json.parseToJsonElement("""{
            "rows":[{"id":1,"value":"$next","other":"kept"}]
        }"""), data)
        assertEquals(1, writes)
        runOnIdle { data = Json.parseToJsonElement("""{
            "rows":[{"id":1,"other":"host"}]
        }""") }
        onAllNodesWithContentDescription(action).assertCountEquals(1)
        assertEquals(1, writes)
        assertEquals(Json.parseToJsonElement("""{
            "rows":[{"id":1,"other":"host"}]
        }"""), data)
        runOnIdle { data = Json.parseToJsonElement("""{
            "rows":[{"id":1,"value":42,"other":"host"}]
        }""") }
        onAllNodesWithText("Invalid value: 42").assertCountEquals(0)
        val inputSemantics = onAllNodes(hasSetTextAction() and hasText("42")).onLast()
            .fetchSemanticsNode().config
        assertTrue(inputSemantics[SemanticsProperties.Error].contains("Invalid value: 42"))
        assertTrue(inputSemantics[SemanticsProperties.ContentDescription]
            .any { it.contains("Invalid value: 42") })
        onAllNodes(hasSetTextAction() and hasText("42")).onLast()
            .performMouseInput { moveTo(center) }
        waitForIdle()
        onAllNodesWithText("Invalid value: 42", substring = true).assertCountEquals(1)
        assertEquals(1, writes)
    }
}
