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
class PasswordInvalidValueUiTest {
    @Test fun materialPasswordShowsImportedJson() = imported(false, false)
    @Test fun materialOtpShowsImportedJson() = imported(false, true)
    @Test fun cupertinoPasswordShowsImportedJson() = imported(true, false)
    @Test fun cupertinoOtpShowsImportedJson() = imported(true, true)

    private fun imported(cupertino: Boolean, otp: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"secret":{"type":"string","format":"password"${if (otp) ",\"minLength\":4,\"maxLength\":4" else ""}}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"secret":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement("#/properties/secret", options =
                    if (otp) mapOf("variant" to JsonPrimitive("otp")) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ },
            )
        }
        onNodeWithText("Invalid value: null").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().assert(hasStateDescription("Invalid value: null"))
        runOnIdle { data = Json.parseToJsonElement("""{}""") }
        onNodeWithText("Invalid value: null").assertDoesNotExist()
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
            data = Json.parseToJsonElement("""{"secret":42}""")
        }
        onNodeWithText("Невалидна стойност: 42").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("1234")
        runOnIdle {
            assertEquals(JsonPrimitive("1234"), data.jsonObject["secret"])
            assertEquals(1, changes)
        }
        onNodeWithText("Невалидна стойност: 42").assertDoesNotExist()
    }
}
