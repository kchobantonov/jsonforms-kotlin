package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class TemporalCellUiTest {
    @Test fun materialDateCellRepairsInvalidImport() = verify(false, "date")
    @Test fun cupertinoDateCellRepairsInvalidImport() = verify(true, "date")
    @Test fun materialTimeCellRepairsInvalidImport() = verify(false, "time")
    @Test fun cupertinoTimeCellRepairsInvalidImport() = verify(true, "time")
    @Test fun materialDateTimeCellRepairsInvalidImport() = verify(false, "date-time")
    @Test fun cupertinoDateTimeCellRepairsInvalidImport() = verify(true, "date-time")
    @Test fun materialUiOnlyTimeCellRepairsInvalidImport() = verify(false, "time", uiOnly = true)
    @Test fun cupertinoUiOnlyTimeCellRepairsInvalidImport() = verify(true, "time", uiOnly = true)

    private fun verify(cupertino: Boolean, format: String, uiOnly: Boolean = false) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "value":{"type":"string"${if (uiOnly) "" else ",\"format\":\"$format\""}}
          }}
        }""").jsonObject)
        val (displayKey, saveKey, pattern, repaired) = when (format) {
            "date" -> listOf("dateFormat", "dateSaveFormat", "YYYY-MM-DD", "2024-06-15")
            "time" -> listOf("timeFormat", "timeSaveFormat", "HH:mm", "10:30")
            else -> listOf("dateTimeFormat", "dateTimeSaveFormat", "YYYY-MM-DD HH:mm", "2024-06-15 10:30")
        }
        val cellOptions = """{"value":{"mask":false,"$displayKey":"$pattern","$saveKey":"$pattern"${if (uiOnly) ",\"format\":\"$format\"" else ""}}}"""
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"value":null}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement(cellOptions),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-temporal-cell" else "material-temporal-cell")
            .assertExists()
        assertEquals(1, onAllNodesWithText("Value").fetchSemanticsNodes().size)
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription].contains("Invalid value: null"))
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Невалидна стойност: null"))
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"value":7}]""") }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(repaired)
        runOnIdle {
            val stored = when {
                uiOnly -> repaired
                format == "time" -> "10:30:00Z"
                format == "date-time" -> formatTemporal(TemporalValue(2024, 6, 15, 10, 30),
                    DEFAULT_DATE_TIME_SAVE_FORMAT)
                else -> repaired
            }
            assertEquals(JsonPrimitive(stored), data.jsonArray[0].jsonObject["value"])
            assertEquals(1, writes)
        }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
    }
}
