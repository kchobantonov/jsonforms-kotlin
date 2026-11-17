package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CompositeCellDeleteUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}}}}""",
    ).jsonObject)
    private val fixedCountSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","minItems":1,"items":{"type":"object","properties":{"contact":{"type":"object","properties":{"city":{"type":"string"}}}}}}}}""",
    ).jsonObject)
    private val arrayCellSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":"array","items":{"type":"string"}}}}}}}""",
    ).jsonObject)
    private val original = Json.parseToJsonElement("""{"rows":[{"contact":{"city":"Boston"}}]}""")
    private val emptyData = Json.parseToJsonElement("""{"rows":[{"contact":{}}]}""")
    private val cleared = Json.parseToJsonElement("""{"rows":[{}]}""")
    private val replacement = Json.parseToJsonElement("""{"rows":[{"contact":{"city":"Host"}}]}""")
    private val nullableSchema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"contact":{"type":["object","null"],"properties":{"city":{"type":"string"}}}}}}}}""",
    ).jsonObject)

    private fun ui(policy: String? = null) = ControlElement("#/properties/rows", options = buildMap {
        put("table", JsonPrimitive(true))
        put("columnDefs", Json.parseToJsonElement("""[{"field":"contact","headerName":"Contact"}]"""))
        if (policy != null) put("confirmation", Json.parseToJsonElement("""{"delete":"$policy"}"""))
    })

    @Test fun materialNonemptyCellConfirmsAndCancels() = confirm(false)
    @Test fun cupertinoNonemptyCellConfirmsAndCancels() = confirm(true)
    @Test fun materialReplacementInvalidatesPendingClear() = replaced(false)
    @Test fun cupertinoReplacementInvalidatesPendingClear() = replaced(true)
    @Test fun materialEmptyCellClearsWithoutPrompt() = emptyCell(false)
    @Test fun cupertinoEmptyCellClearsWithoutPrompt() = emptyCell(true)
    @Test fun materialAlwaysPromptsForEmptyCell() = always(false)
    @Test fun cupertinoAlwaysPromptsForEmptyCell() = always(true)
    @Test fun materialDraftClearUsesCompositePolicy() = draftClear(false)
    @Test fun cupertinoDraftClearUsesCompositePolicy() = draftClear(true)
    @Test fun materialNeverPolicyAndBulgarianAction() = never(false)
    @Test fun cupertinoNeverPolicyAndBulgarianAction() = never(true)
    @Test fun materialReadOnlyRevokesPendingCellClear() = readonlyRevocation(false)
    @Test fun cupertinoReadOnlyRevokesPendingCellClear() = readonlyRevocation(true)
    @Test fun materialArrayCellClearConfirms() = arrayCell(false)
    @Test fun cupertinoArrayCellClearConfirms() = arrayCell(true)
    @Test fun materialDeleteConfirmationFollowsBulgarianLocale() = localizedConfirm(false)
    @Test fun cupertinoDeleteConfirmationFollowsBulgarianLocale() = localizedConfirm(true)

    private fun localizedConfirm(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var writes = 0
        setContent {
            JsonForms(data = original, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale), onChange = { writes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Изтриване на стойността?").assertExists()
        onNodeWithText("Стойността ще бъде премахната. Това действие не може да се отмени тук.").assertExists()
        onNodeWithText("Не").performClick()
        runOnIdle { assertEquals(0, writes) }
    }

    private fun confirm(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(cleared, data); assertEquals(1, changes) }
    }

    private fun replaced(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf<JsonElement>(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = replacement }
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { assertEquals(replacement, data); assertEquals(0, changes) }
    }

    private fun emptyCell(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(emptyData)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { assertEquals(cleared, data); assertEquals(1, changes) }
    }

    private fun always(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(emptyData)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui("always"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(cleared, data); assertEquals(1, changes) }
    }

    private fun draftClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        val control = ui().copy(options = ui().options + ("cells" to Json.parseToJsonElement(
            """{"contact":{"detail":{"type":"Control","scope":"#"}}}""",
        )))
        setContent {
            JsonForms(data = data, schema = nullableSchema, uischema = control,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Edit Contact").performClick()
        onAllNodesWithContentDescription("Clear value").onFirst().performClick()
        onNodeWithText("Change this type?").assertExists()
        onNodeWithText("Yes").performClick()
        onNodeWithText("Apply").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        onNodeWithText("Contact details").assertExists()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithText("Apply").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"rows":[{"contact":null}]}"""), data)
            assertEquals(1, changes)
        }
    }

    private fun never(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui("never"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Изчисти Contact").performClick()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { assertEquals(cleared, data); assertEquals(1, changes) }
    }

    private fun readonlyRevocation(cupertino: Boolean) = runComposeUiTest {
        var readonly by mutableStateOf(false)
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixedCountSchema, uischema = ui(), readonly = readonly,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { readonly = true }
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { readonly = false }
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
    }

    private fun arrayCell(cupertino: Boolean) = runComposeUiTest {
        val originalArray = Json.parseToJsonElement("""{"rows":[{"contact":["a"]}]}""")
        var data by mutableStateOf(originalArray)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = arrayCellSchema, uischema = ui(),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Clear Contact").performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(cleared, data); assertEquals(1, changes) }
    }
}
