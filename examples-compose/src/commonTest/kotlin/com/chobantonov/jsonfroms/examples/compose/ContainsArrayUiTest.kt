package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class ContainsArrayUiTest {
    @Test fun materialExpandableShowsEmptyContainsError() = scenario(false, "expandable")
    @Test fun cupertinoExpandableShowsEmptyContainsError() = scenario(true, "expandable")
    @Test fun materialTableShowsEmptyContainsError() = scenario(false, "table")
    @Test fun cupertinoTableShowsEmptyContainsError() = scenario(true, "table")
    @Test fun materialListShowsEmptyContainsError() = scenario(false, "list")
    @Test fun cupertinoListShowsEmptyContainsError() = scenario(true, "list")
    @Test fun materialExpandableKeepsLastMatchOnDelete() = deletion(false, "expandable")
    @Test fun cupertinoExpandableKeepsLastMatchOnDelete() = deletion(true, "expandable")
    @Test fun materialTableKeepsLastMatchOnDelete() = deletion(false, "table")
    @Test fun cupertinoTableKeepsLastMatchOnDelete() = deletion(true, "table")
    @Test fun materialListKeepsLastMatchOnDelete() = deletion(false, "list")
    @Test fun cupertinoListKeepsLastMatchOnDelete() = deletion(true, "list")
    @Test fun materialExpandableCancelsStaleContainsDelete() = staleConfirmation(false, "expandable")
    @Test fun cupertinoExpandableCancelsStaleContainsDelete() = staleConfirmation(true, "expandable")
    @Test fun materialTableCancelsStaleContainsDelete() = staleConfirmation(false, "table")
    @Test fun cupertinoTableCancelsStaleContainsDelete() = staleConfirmation(true, "table")
    @Test fun materialListCancelsStaleContainsDelete() = staleConfirmation(false, "list")
    @Test fun cupertinoListCancelsStaleContainsDelete() = staleConfirmation(true, "list")

    private fun scenario(cupertino: Boolean, mode: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"status":{"type":"string"}}},"contains":{"properties":{"status":{"const":"yes"}},"required":["status"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[]}"""))
        var changes = 0
        setContent {
            val options = mapOf("hideArraySummaryValidation" to JsonPrimitive(true),
                "showValidationIndicator" to JsonPrimitive(false))
            val ui = if (mode == "list") ListWithDetailElement("#/properties/rows", options = options)
                else ControlElement("#/properties/rows", options = options +
                    if (mode == "table") mapOf("table" to JsonPrimitive(true)) else emptyMap())
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(translateError = { error, _, _ ->
                    "ERR:${error.keyword}:${error.instancePath}"
                }),
                onChange = { data = it.data; changes++ })
        }
        val error = onNodeWithText("ERR:contains:/rows")
        error.assertExists()
        assertTrue(error.fetchSemanticsNode().config[SemanticsProperties.Error].isNotBlank())
        assertEquals(0, changes)
        runOnIdle { data = Json.parseToJsonElement("""{"rows":[{"status":"yes"}]}""") }
        onNodeWithText("ERR:contains:/rows").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun deletion(cupertino: Boolean, mode: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"status":{"type":"string"}}},"contains":{"properties":{"status":{"const":"yes"}},"required":["status"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"status":"yes"},{"status":"no"}]}"""))
        var changes = 0
        setContent {
            val options = mapOf("restrict" to JsonPrimitive(true))
            val ui = if (mode == "list") ListWithDetailElement("#/properties/rows", options = options)
                else ControlElement("#/properties/rows", options = options +
                    if (mode == "table") mapOf("table" to JsonPrimitive(true)) else emptyMap())
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val deleteButtons = onAllNodes(hasContentDescription("Delete item"))
        assertEquals(2, deleteButtons.fetchSemanticsNodes().size)
        deleteButtons[0].assertIsNotEnabled()
        deleteButtons[1].assertIsEnabled()
        deleteButtons[1].performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"rows":[{"status":"yes"}]}"""), data)
        assertEquals(1, changes)
        onAllNodes(hasContentDescription("Delete item"))[0].assertIsNotEnabled()
    }

    private fun staleConfirmation(cupertino: Boolean, mode: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"status":{"type":"string"}}},"contains":{"properties":{"status":{"const":"yes"}},"required":["status"]}}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement(
            """{"rows":[{"status":"yes"},{"status":"no"}]}"""))
        var changes = 0
        setContent {
            val options = mapOf("restrict" to JsonPrimitive(true))
            val ui = if (mode == "list") ListWithDetailElement("#/properties/rows", options = options)
                else ControlElement("#/properties/rows", options = options +
                    if (mode == "table") mapOf("table" to JsonPrimitive(true)) else emptyMap())
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("jsonformsExtended" to mapOf("confirmation" to mapOf("default" to "always"))),
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasContentDescription("Delete item"))[1].performClick()
        onNodeWithText("Delete this value?").assertExists()
        runOnIdle { data = Json.parseToJsonElement(
            """{"rows":[{"status":"no"},{"status":"yes"}]}""") }
        waitForIdle()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        val deleteButtons = onAllNodes(hasContentDescription("Delete item"))
        deleteButtons[0].assertIsEnabled()
        deleteButtons[1].assertIsNotEnabled()
        assertEquals(0, changes)
    }
}
