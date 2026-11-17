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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NumericInvalidValueUiTest {
    @Test fun materialIntegerShowsImportedJson() = imported(false, true)
    @Test fun materialNumberShowsImportedJson() = imported(false, false)
    @Test fun cupertinoIntegerShowsImportedJson() = imported(true, true)
    @Test fun cupertinoNumberShowsImportedJson() = imported(true, false)

    private fun imported(cupertino: Boolean, integer: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"${if (integer) "integer" else "number"}"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid value: null").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"value":"7"}""")
        }
        onNodeWithText("Невалидна стойност: \"7\"").assertExists()
        onAllNodes(hasSetTextAction()).onFirst()
            .assert(hasStateDescription("Невалидна стойност: \"7\""))
        runOnIdle { data = Json.parseToJsonElement("""{"value":{"raw":true}}""") }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12")
        runOnIdle {
            assertEquals(12.0, data.jsonObject["value"]?.jsonPrimitive?.doubleOrNull)
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: {\"raw\":true}").assertDoesNotExist()
    }
}
