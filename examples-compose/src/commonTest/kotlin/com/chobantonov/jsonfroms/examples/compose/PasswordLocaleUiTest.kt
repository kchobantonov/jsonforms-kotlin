package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PasswordLocaleUiTest {
    @Test fun materialRevealActionFollowsLocaleAndScopedOverride() = password(false)
    @Test fun cupertinoRevealActionFollowsLocaleAndScopedOverride() = password(true)
    @Test fun materialRevealAndClearStayIndependent() = revealAndClear(false)
    @Test fun cupertinoRevealAndClearStayIndependent() = revealAndClear(true)
    @Test fun materialPasswordRespectsRestrictedMaxLength() = restrictedLength(false)
    @Test fun cupertinoPasswordRespectsRestrictedMaxLength() = restrictedLength(true)

    private fun restrictedLength(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"password":{"type":"string","maxLength":4}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"password":"12345"}"""))
        var restrict by mutableStateOf(true)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/password", options = mapOf(
                    "format" to JsonPrimitive("password"), "restrict" to JsonPrimitive(restrict))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("123456")
        assertEquals(JsonPrimitive("12345"), data.jsonObject["password"])
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("1234")
        assertEquals(JsonPrimitive("1234"), data.jsonObject["password"])
        assertEquals(1, changes)
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("12345")
        assertEquals(JsonPrimitive("1234"), data.jsonObject["password"])
        assertEquals(1, changes)
        runOnIdle { restrict = false }
        waitForIdle()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("123456")
        assertEquals(JsonPrimitive("123456"), data.jsonObject["password"])
        assertEquals(2, changes)
    }

    private fun revealAndClear(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"password":{"type":"string","format":"password"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"password":"secret"}"""))
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            TestTouchInputMode()
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/password"),
                readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Show password").assertExists()
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
        assertEquals(JsonPrimitive("secret"), data.jsonObject["password"])
        assertEquals(0, changes)
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithContentDescription("Hide password").performClick()
        onNodeWithContentDescription("Show password").assertExists()
        assertEquals(0, changes)
        runOnIdle { readonly = false }
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["password"])
        assertEquals(1, changes)
    }

    private fun password(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en-US")
        var scopedOverride by mutableStateOf(false)
        var data by mutableStateOf(JsonPrimitive("secret value"))
        var changes = 0
        setContent {
            JsonForms(data = data,
                schema = buildJsonSchema { put("type", "string") },
                uischema = ControlElement(scope = "#", i18n = "secret",
                    options = mapOf("format" to JsonPrimitive("password"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (scopedOverride && key == "secret.showPasswordAriaLabel") "Reveal secret" else fallback
                }),
                onChange = { data = it.data as JsonPrimitive; changes++ })
        }
        onNodeWithContentDescription("Show password").performClick()
        onNodeWithContentDescription("Hide password").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithContentDescription("Скрий паролата").performClick()
        onNodeWithContentDescription("Покажи паролата").assertExists()
        runOnIdle { scopedOverride = true }
        onNodeWithContentDescription("Reveal secret").assertExists()
        assertEquals(JsonPrimitive("secret value"), data)
        assertEquals(0, changes)
    }
}
