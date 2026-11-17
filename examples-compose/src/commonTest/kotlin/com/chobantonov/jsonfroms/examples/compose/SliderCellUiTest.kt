package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class SliderCellUiTest {
    @Test fun materialSliderCellRepairsInvalidImport() = verify(false)
    @Test fun cupertinoSliderCellRepairsInvalidImport() = verify(true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"array","items":{"type":"object","properties":{
            "value":{"type":"integer","minimum":1,"maximum":6,"default":2,"multipleOf":2}
          }}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""[{"value":"4"}]"""))
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "cells" to Json.parseToJsonElement("""{"value":{"slider":true}}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = if (cupertino) cupertinoCells else materialCells,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; writes++ })
        }
        val tag = if (cupertino) "cupertino-slider-cell" else "material-slider-cell"
        val slider = onNodeWithTag(tag, useUnmergedTree = true)
        slider.assertExists()
        assertEquals(1, onAllNodesWithText("Value").fetchSemanticsNodes().size)
        onNodeWithText("Invalid current value: \"4\"").assertExists()
        assertTrue(slider.fetchSemanticsNode().config[SemanticsProperties.Error]
            .contains("Invalid current value: \"4\""))
        runOnIdle { assertEquals(0, writes); locale = "bg-BG" }
        onNodeWithText("Невалидна текуща стойност: \"4\"").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{}]""") }
        onNodeWithText("Невалидна текуща стойност: \"4\"").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"value":null}]""") }
        onNodeWithText("Невалидна текуща стойност: null").assertExists()
        runOnIdle { assertEquals(0, writes) }
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        runOnIdle {
            assertEquals(JsonPrimitive(4), data.jsonArray[0].jsonObject["value"])
            assertEquals(1, writes)
        }
        onNodeWithText("Невалидна текуща стойност: null").assertDoesNotExist()
        runOnIdle { data = Json.parseToJsonElement("""[{"value":2}]""") }
        runOnIdle { assertEquals(1, writes) }
    }
}
