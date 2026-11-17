package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalPrefixItemsUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
        "${'$'}schema":"https://json-schema.org/draft/2020-12/schema",
        "type":"object",
        "properties":{"range":{"type":"array","title":"Cover",
            "prefixItems":[
                {"type":"string","format":"date","title":"From"},
                {"type":"string","format":"date","title":"To"}
            ],"items":false}}
    }""").jsonObject)

    @Test fun materialPrefixItemsSelectsDateEditorsAtBothPositions() = scenario(false)
    @Test fun cupertinoPrefixItemsSelectsDateEditorsAtBothPositions() = scenario(true)

    private fun scenario(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"range":["2026-10-01","2026-10-05"]}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/range"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasContentDescription("Choose date")).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("10/01/2026")).onFirst().assertExists()
        onAllNodes(hasSetTextAction() and hasText("10/05/2026")).onFirst()
            .performTextReplacement("10/06/2026")
        waitForIdle()
        val values = data.jsonObject.getValue("range").jsonArray
        assertEquals(JsonPrimitive("2026-10-01"), values[0])
        assertEquals(JsonPrimitive("2026-10-06"), values[1])
        assertEquals(1, changes)
    }
}
