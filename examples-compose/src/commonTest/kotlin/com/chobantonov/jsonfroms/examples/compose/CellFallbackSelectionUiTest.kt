package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoCells
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CellFallbackSelectionUiTest {
    @Test fun materialConflictingFormatUsesDateControlAtCellPath() = scenario(false)
    @Test fun cupertinoConflictingFormatUsesDateControlAtCellPath() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","properties":{
                "when":{"type":"string","format":"date"}}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """[{"when":"2024-06-15"}]"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"when":{"format":"password","mask":false}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithTag(if (cupertino) "cupertino-temporal-cell" else "material-temporal-cell")
            .assertDoesNotExist()
        onNodeWithContentDescription("Choose date").assertExists()
        runOnIdle { assertEquals(0, writes) }
        assertEquals(1, onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
        onAllNodes(hasSetTextAction())[0].performTextReplacement("2024-06-16")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"when":"2024-06-15"}]"""), data)
            assertEquals(0, writes)
        }
        onAllNodes(hasSetTextAction())[0].performTextReplacement("06/16/2024")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""[{"when":"2024-06-16"}]"""), data)
            assertEquals(1, writes)
        }
    }
}
