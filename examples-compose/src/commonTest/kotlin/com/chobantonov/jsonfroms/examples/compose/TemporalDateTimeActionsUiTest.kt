package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.controls.DEFAULT_DATE_TIME_SAVE_FORMAT
import com.chobantonov.jsonfroms.material.renderers.controls.TemporalValue
import com.chobantonov.jsonfroms.material.renderers.controls.formatTemporal
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TemporalDateTimeActionsUiTest {
    @Test fun materialDateTimeColumnsCommitEachStepByDefault() = columns(false)
    @Test fun cupertinoDateTimeColumnsCommitEachStepByDefault() = columns(true)
    @Test fun materialNativeDateThenTimeDialCommitsByDefault() = materialNative()
    @Test fun cupertinoNativeDateTimeCommitsByDefault() = cupertinoNative()

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":"string","format":"date-time"}}}"""
    ).jsonObject)

    private fun columns(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "views" to JsonArray(listOf("year", "month", "hours", "minutes")
                        .map(::JsonPrimitive)))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        assertEquals(0, changes)
        onNodeWithContentDescription("Month 02").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-10T09:30:37Z"}"""), data)
        assertEquals(1, changes)
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Next").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithContentDescription("Minutes").performTextReplacement("31")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-10T09:31:37Z"}"""), data)
        assertEquals(2, changes)
    }

    private fun materialNative() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { changes++; data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        assertEquals(0, changes)
        onNodeWithContentDescription("Switch to text input mode").performClick()
        onNodeWithContentDescription("Date, MM/DD/YYYY").performTextReplacement("01112025")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-01-11T09:30:37Z"}"""), data)
        assertEquals(1, changes)
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Next").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        waitForIdle()
        onNodeWithContentDescription("Hours").assertDoesNotExist()
        onNodeWithContentDescription("12 hours").assertExists()
        onNodeWithContentDescription("10 hours").performClick()
        waitForIdle()
        val selectedInstant = formatTemporal(TemporalValue(2025, 1, 11, 10, 30, 37),
            DEFAULT_DATE_TIME_SAVE_FORMAT)
        assertEquals(Json.parseToJsonElement("""{"value":"$selectedInstant"}"""), data)
        assertEquals(2, changes)
    }

    private fun cupertinoNative() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = cupertinoRenderers,
                onChange = { changes++; data = it.data })
        }
        waitForIdle()
        onNodeWithContentDescription("Choose date and time").performClick()
        waitForIdle()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        assertEquals(0, changes)
        // Native date-time exposes date, hour, and minute wheels in that order.
        // Visible hour labels depend on the host time zone, so select the wheel by position.
        val wheels = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(wheels).fetchSemanticsNodes().size >= 3 }
        onAllNodes(wheels)[1]
            .performSemanticsAction(SemanticsActions.ScrollToIndex) { it(20) }
        waitForIdle()
        val committed = data.jsonObject.getValue("value").toString().trim('"')
        assertNotEquals("2025-01-10T09:30:37Z", committed)
        assertTrue(committed.startsWith("2025-01-10T"))
        assertTrue(committed.endsWith(":30:37Z"))
        assertEquals(1, changes)
    }
}
