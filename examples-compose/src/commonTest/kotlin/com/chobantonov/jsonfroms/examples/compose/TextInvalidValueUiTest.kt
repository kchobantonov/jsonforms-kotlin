package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TextInvalidValueUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"text":{"type":"string"}}
    }""").jsonObject)

    @Test fun materialSingleLineShowsImportedJson() = imported(false, false)
    @Test fun materialMultilineShowsImportedJson() = imported(false, true)
    @Test fun cupertinoSingleLineShowsImportedJson() = imported(true, false)
    @Test fun cupertinoMultilineShowsImportedJson() = imported(true, true)

    private fun imported(cupertino: Boolean, multi: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"text":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement("#/properties/text", options = mapOf("multi" to JsonPrimitive(multi))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid value: null").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().assert(hasStateDescription("Invalid value: null"))
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        waitForIdle()
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"text":42}""")
        }
        onNodeWithText("Невалидна стойност: 42").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"text":{"raw":true}}""") }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("fixed")
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"text":"fixed"}"""), data)
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertDoesNotExist()
    }
}
