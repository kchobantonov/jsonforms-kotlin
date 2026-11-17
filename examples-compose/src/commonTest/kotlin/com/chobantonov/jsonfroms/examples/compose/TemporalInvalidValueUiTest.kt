package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TemporalInvalidValueUiTest {
    @Test fun materialDateShowsImportedJson() = imported(false, "date")
    @Test fun materialTimeShowsImportedJson() = imported(false, "time")
    @Test fun materialDateTimeShowsImportedJson() = imported(false, "date-time")
    @Test fun cupertinoDateShowsImportedJson() = imported(true, "date")
    @Test fun cupertinoTimeShowsImportedJson() = imported(true, "time")
    @Test fun cupertinoDateTimeShowsImportedJson() = imported(true, "date-time")

    private fun imported(cupertino: Boolean, format: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format"}}
        }""").jsonObject)
        val (displayKey, saveKey, pattern, repaired) = when (format) {
            "date" -> listOf("dateFormat", "dateSaveFormat", "YYYY-MM-DD", "2024-06-15")
            "time" -> listOf("timeFormat", "timeSaveFormat", "HH:mm", "10:30")
            else -> listOf("dateTimeFormat", "dateTimeSaveFormat", "YYYY-MM-DD HH:mm", "2024-06-15 10:30")
        }
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "mask" to JsonPrimitive(false),
                    displayKey to JsonPrimitive(pattern),
                    saveKey to JsonPrimitive(pattern),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid value: null").assertExists()
        val initialState = onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue("Invalid value: null" in initialState)
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"value":7}""")
        }
        onNodeWithText("Невалидна стойност: 7").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(repaired)
        runOnIdle {
            val stored = when (format) {
                "time" -> "10:30:00Z"
                "date-time" -> formatTemporal(TemporalValue(2024, 6, 15, 10, 30),
                    DEFAULT_DATE_TIME_SAVE_FORMAT)
                else -> repaired
            }
            assertEquals(JsonPrimitive(stored), data.jsonObject["value"])
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: 7").assertDoesNotExist()
    }
}
