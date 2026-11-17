package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MixedTypeConfirmationUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":["object","array"]}}}""",
    ).jsonObject)
    private val original = Json.parseToJsonElement("""{"value":{"child":1}}""")
    private val replacement = Json.parseToJsonElement("""{"value":{"other":2}}""")
    private val arrayValue = Json.parseToJsonElement("""{"value":[]}""")

    @Test fun materialChangeConfirmsAndCancelPreservesValue() = confirm(false)
    @Test fun cupertinoChangeConfirmsAndCancelPreservesValue() = confirm(true)
    @Test fun materialHostReplacementInvalidatesConfirmation() = replacement(false)
    @Test fun cupertinoHostReplacementInvalidatesConfirmation() = replacement(true)
    @Test fun materialReboundParentInvalidatesConfirmation() = rebound(false)
    @Test fun cupertinoReboundParentInvalidatesConfirmation() = rebound(true)
    @Test fun materialNeverPolicyChangesImmediately() = never(false)
    @Test fun cupertinoNeverPolicyChangesImmediately() = never(true)
    @Test fun materialConfirmationFollowsBulgarianLocale() = localizedConfirm(false)
    @Test fun cupertinoConfirmationFollowsBulgarianLocale() = localizedConfirm(true)

    private fun localizedConfirm(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = original, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale), onChange = { writes++ })
        }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Change this type?").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Промяна на типа?").assertExists()
        onNodeWithText("Текущата стойност ще бъде заменена.").assertExists()
        onNodeWithText("Не").performClick()
        runOnIdle { assertEquals(0, writes) }
    }

    private fun confirm(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Change this type?").assertExists()
        onNodeWithText("No").performClick()
        onNodeWithText("Object").assertExists()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(arrayValue, data); assertEquals(1, changes) }
    }

    private fun replacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Change this type?").assertExists()
        runOnIdle { data = replacement }
        onNodeWithText("Change this type?").assertDoesNotExist()
        runOnIdle { assertEquals(replacement, data); assertEquals(0, changes) }
    }

    private fun never(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"typeChange":"never"}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Change this type?").assertDoesNotExist()
        runOnIdle { assertEquals(arrayValue, data); assertEquals(1, changes) }
    }

    private fun rebound(cupertino: Boolean) = runComposeUiTest {
        val sameChild = original.jsonObject.getValue("value")
        val rebound = JsonObject(mapOf("value" to sameChild, "other" to JsonPrimitive(1)))
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Object").performClick()
        onNodeWithText("Array").performClick()
        onNodeWithText("Change this type?").assertExists()
        runOnIdle { data = rebound }
        onNodeWithText("Change this type?").assertDoesNotExist()
        runOnIdle { assertEquals(rebound, data); assertEquals(0, changes) }
    }
}
