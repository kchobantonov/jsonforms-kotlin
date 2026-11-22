package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ConfirmationUiTest {
    @Test fun materialDefaultRestrictHonorsArrayMinimum() = defaultRestrictArray(false)
    @Test fun cupertinoDefaultRestrictHonorsArrayMinimum() = defaultRestrictArray(true)
    @Test fun materialDefaultRestrictHonorsPropertyMinimum() = defaultRestrictProperty(false)
    @Test fun cupertinoDefaultRestrictHonorsPropertyMinimum() = defaultRestrictProperty(true)

    private fun defaultRestrictArray(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","minItems":1,"items":{"type":"string"}}"""
        ).jsonObject)
        var localRestrict by mutableStateOf(false)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""["Ada"]"""), schema = schema,
                uischema = ControlElement(scope = "#", options = if (localRestrict)
                    mapOf("restrict" to JsonPrimitive(false)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithContentDescription("Delete item").assertIsNotEnabled()
        runOnIdle { localRestrict = true }
        onNodeWithContentDescription("Delete item").assertIsEnabled()
    }

    private fun defaultRestrictProperty(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","minProperties":1,"additionalProperties":{"type":"string"}}"""
        ).jsonObject)
        var localRestrict by mutableStateOf(false)
        setContent {
            JsonForms(
                data = Json.parseToJsonElement("""{"item":"Ada"}"""), schema = schema,
                uischema = ControlElement(scope = "#", options = if (localRestrict)
                    mapOf("restrict" to JsonPrimitive(false)) else emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            )
        }
        onNodeWithContentDescription("Delete button").assertIsNotEnabled()
        runOnIdle { localRestrict = true }
        onNodeWithContentDescription("Delete button").assertIsEnabled()
    }

    @Test fun materialCancelThenConfirmDeletion() = verify(false)

    @Test fun cupertinoCancelThenConfirmDeletion() = verify(true)

    @Test fun materialHostChangeInvalidatesPendingRemoval() = verifyHostChange(false)

    @Test fun cupertinoHostChangeInvalidatesPendingRemoval() = verifyHostChange(true)

    @Test fun materialEqualPropertyReplacementInvalidatesConfirmation() = verifyEqualPropertyReplacement(false)

    @Test fun cupertinoEqualPropertyReplacementInvalidatesConfirmation() = verifyEqualPropertyReplacement(true)

    private fun verifyEqualPropertyReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"box":{"type":"object","additionalProperties":{"type":"object","properties":{"name":{"type":"string"}}}},"sibling":{"type":"string"}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""{"box":{"item":{"name":"Ada"}},"sibling":"one"}""")
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement("#/properties/box"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("Delete button").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle {
            data = Json.parseToJsonElement("""{"box":{"item":{"name":"Ada"}},"sibling":"two"}""")
        }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(0, changes)
    }

    @Test fun materialArrayCancelThenConfirmDeletion() = verifyArray(false)

    @Test fun cupertinoArrayCancelThenConfirmDeletion() = verifyArray(true)

    @Test fun materialArrayReplacementInvalidatesConfirmation() = verifyArrayReplacement(false)

    @Test fun cupertinoArrayReplacementInvalidatesConfirmation() = verifyArrayReplacement(true)

    @Test fun materialTableScalarUsesComplexFallback() = verifyTableScalar(false)

    @Test fun cupertinoTableScalarUsesComplexFallback() = verifyTableScalar(true)

    @Test fun materialTableElementAlwaysOverridesGlobalNever() = verifyTableAlways(false)

    @Test fun cupertinoTableElementAlwaysOverridesGlobalNever() = verifyTableAlways(true)

    @Test fun materialAlwaysConfirmsFalseAndZero() = verifyFalsyArrayValues(false, listOf("false", "0"))
    @Test fun cupertinoAlwaysConfirmsFalseAndZero() = verifyFalsyArrayValues(true, listOf("false", "0"))
    @Test fun materialAlwaysConfirmsEmptyStringAndNull() = verifyFalsyArrayValues(false, listOf("\"\"", "null"))
    @Test fun cupertinoAlwaysConfirmsEmptyStringAndNull() = verifyFalsyArrayValues(true, listOf("\"\"", "null"))
    @Test fun materialAlwaysConfirmsEmptyContainers() = verifyFalsyArrayValues(false, listOf("{}", "[]"))
    @Test fun cupertinoAlwaysConfirmsEmptyContainers() = verifyFalsyArrayValues(true, listOf("{}", "[]"))

    private fun verifyFalsyArrayValues(cupertino: Boolean, values: List<String>) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"array","items":{"type":"string"}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("[false]"))
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        for (old in values) {
            val original = Json.parseToJsonElement("[$old]")
            runOnIdle { data = original }
            onNodeWithContentDescription("Delete item").performClick()
            onNodeWithText("Delete this value?").assertExists()
            onNodeWithText("No").performClick()
            waitForIdle()
            assertEquals(original, data, old)
        }
        assertEquals(0, changes)
    }

    @Test fun materialTableRestrictPreventsMinimumDeletion() = verifyTableMinimum(false)

    @Test fun cupertinoTableRestrictPreventsMinimumDeletion() = verifyTableMinimum(true)

    @Test fun materialTableReplacementInvalidatesConfirmation() = verifyTableReplacement(false)

    @Test fun cupertinoTableReplacementInvalidatesConfirmation() = verifyTableReplacement(true)

    @Test fun materialEqualRowReplacementInvalidatesTableConfirmation() = verifyEqualTableReplacement(false)

    @Test fun cupertinoEqualRowReplacementInvalidatesTableConfirmation() = verifyEqualTableReplacement(true)

    @Test fun materialTableDialogUsesTranslatedCatalog() = verifyTableTranslation(false)

    @Test fun cupertinoTableDialogUsesTranslatedCatalog() = verifyTableTranslation(true)

    private fun verifyTableReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"array","items":{"type":"string"}}""").jsonObject)
        val original = Json.parseToJsonElement("""["Ada"]""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}""")
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""["Grace"]""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(original, data)
    }

    private fun verifyEqualTableReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"string"}},"sibling":{"type":"string"}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""{"rows":["Ada"],"sibling":"one"}""")
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement("#/properties/rows", options = mapOf(
                    "table" to JsonPrimitive(true),
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}"""),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"rows":["Ada"],"sibling":"two"}""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun verifyTableTranslation(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"array","items":{"type":"string"}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["Ada"]"""))
        val messages = mapOf(
            "confirm.delete.title" to "Да се изтрие ли стойността?",
            "confirm.delete.message" to "Стойността ще бъде премахната.",
            "confirm.accept" to "Да",
            "confirm.decline" to "Не",
        )
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}""")
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg", translate = { key, fallback, _ -> messages[key] ?: fallback }),
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Да се изтрие ли стойността?").assertExists()
        onNodeWithText("Стойността ще бъде премахната.").assertExists()
        onNodeWithText("Не").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""["Ada"]"""), data)
    }

    private fun verifyTableScalar(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"array","items":{"type":"string"}}""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["Ada"]"""))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(Json.parseToJsonElement("[]"), data)
    }

    private fun verifyTableAlways(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{"type":"array","items":{"type":"string"}}""").jsonObject)
        val original = Json.parseToJsonElement("""["Ada"]""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf(
                    "confirmation" to Json.parseToJsonElement("""{"delete":"always"}""")
                )),
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "never"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        waitForIdle()
        assertEquals(original, data)
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("[]"), data)
    }

    private fun verifyTableMinimum(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","minItems":1,"items":{"type":"string"}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""["Ada"]"""))
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                config = mapOf("restrict" to true),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").assertIsNotEnabled()
        assertEquals(Json.parseToJsonElement("""["Ada"]"""), data)
    }

    private fun verifyArrayReplacement(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"detail":{"type":"object","properties":{"code":{"type":"string"}}}}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""[{"name":"Ada","detail":{"code":"A"}}]""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data, schema = schema, uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""[{"name":"Grace","detail":{"code":"G"}}]""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals(original, data)
    }

    private fun verifyArray(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"detail":{"type":"object","properties":{"code":{"type":"string"}}}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """[{"name":"Ada","detail":{"code":"A"}}]"""
        ))
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data },
            )
        }
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        waitForIdle()
        assertEquals(1, (data as kotlinx.serialization.json.JsonArray).size)
        onNodeWithContentDescription("Delete item").performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(0, (data as kotlinx.serialization.json.JsonArray).size)
    }

    private fun verifyHostChange(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","additionalProperties":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""{"item":{"name":"Ada"}}""").jsonObject
        var data by mutableStateOf(original)
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement(scope = "#"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data.jsonObject },
            )
        }
        onNodeWithContentDescription("Delete button").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement("""{"item":{"name":"Grace"}}""").jsonObject }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { data = original }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        assertEquals("Ada", data["item"]?.jsonObject?.get("name")?.let { (it as JsonPrimitive).content })
    }

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","additionalProperties":{"type":"object","properties":{"name":{"type":"string"}}}}"""
        ).jsonObject)
        var data by mutableStateOf<JsonObject>(
            Json.parseToJsonElement("""{"item":{"name":"Ada"}}""").jsonObject
        )
        val renderers = if (cupertino) cupertinoRenderers else materialRenderers
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = ControlElement(scope = "#"),
                renderers = renderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data.jsonObject },
            )
        }
        onNodeWithContentDescription("Delete button").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        waitForIdle()
        assertEquals("Ada", data["item"]?.jsonObject?.get("name")?.let { (it as JsonPrimitive).content })
        onNodeWithText("Delete this value?").assertDoesNotExist()
        onNodeWithContentDescription("Delete button").performClick()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(false, data.containsKey("item"))
    }
}
