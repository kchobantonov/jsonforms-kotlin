package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ExtendedInvalidScalarUiTest {
    @Test fun materialDurationShowsImportedJson() = imported(false, "duration", false)
    @Test fun cupertinoDurationShowsImportedJson() = imported(true, "duration", false)
    @Test fun materialCronShowsImportedJson() = imported(false, "cron", false)
    @Test fun cupertinoCronShowsImportedJson() = imported(true, "cron", false)
    @Test fun materialColorTextShowsImportedJson() = imported(false, "color", false)
    @Test fun cupertinoColorTextShowsImportedJson() = imported(true, "color", false)
    @Test fun materialColorPickerShowsImportedJson() = imported(false, "color", true)
    @Test fun cupertinoColorPickerShowsImportedJson() = imported(true, "color", true)

    private fun imported(cupertino: Boolean, format: String, pickerOnly: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":{"raw":true}}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement("#/properties/value", options =
                    if (pickerOnly) mapOf("colorTextEntry" to JsonPrimitive(false)) else emptyMap()),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid value: {\"raw\":true}").assertExists()
        if (pickerOnly) {
            assertTrue(onNodeWithContentDescription("Choose color").fetchSemanticsNode()
                .config[SemanticsProperties.Error].contains("Invalid value: {\"raw\":true}"))
        } else {
            onAllNodes(hasSetTextAction()).onFirst()
                .assert(hasStateDescription("Invalid value: {\"raw\":true}"))
        }
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        onNodeWithText("Invalid value: {\"raw\":true}").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"value":null}""")
        }
        onNodeWithText("Невалидна стойност: null").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"value":{"raw":true}}""") }
        if (pickerOnly) {
            onNodeWithContentDescription("Избери цвят").performClick()
            onNodeWithContentDescription("Цвят #ef4444").performClick()
            runOnIdle { assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["value"]) }
        } else {
            val repaired = when (format) {
                "duration" -> "P2D"
                "cron" -> "0 * * * * *"
                else -> "#123456"
            }
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(repaired)
            runOnIdle { assertEquals(JsonPrimitive(repaired), data.jsonObject["value"]) }
        }
        runOnIdle { assertEquals(1, changes) }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertDoesNotExist()
    }
}
