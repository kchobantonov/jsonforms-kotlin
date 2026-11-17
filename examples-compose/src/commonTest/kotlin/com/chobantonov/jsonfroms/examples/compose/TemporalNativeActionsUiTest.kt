package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalNativeActionsUiTest {
    @Test fun materialDayPickerCommitsWithoutActions() = day(false)
    @Test fun cupertinoDayPickerCommitsWithoutActions() = day(true)
    @Test fun materialTimePickerCommitsWithoutActions() = time(false)
    @Test fun cupertinoTimePickerCommitsWithoutActions() = time(true)
    @Test fun materialDayPickerDefaultsToImmediateCommit() = day(false, null)
    @Test fun cupertinoDayPickerDefaultsToImmediateCommit() = day(true, null)
    @Test fun materialTimePickerDefaultsToImmediateCommit() = time(false, null)
    @Test fun cupertinoTimePickerDefaultsToImmediateCommit() = time(true, null)

    private fun day(cupertino: Boolean, showActions: Boolean? = false) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10"}"""))
        var changes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = showActions?.let {
                    mapOf("showActions" to JsonPrimitive(it))
                } ?: emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        assertEquals(0, changes)
        if (cupertino) onNodeWithText("11").performClick()
        else {
            onNodeWithContentDescription("Switch to text input mode").performClick()
            onNodeWithContentDescription("Date, MM/DD/YYYY")
                .performTextReplacement("01112025")
        }
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-01-11"}"""), data)
        assertEquals(1, changes)
    }

    private fun time(cupertino: Boolean, showActions: Boolean? = false) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        var changes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"time"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = showActions?.let {
                    mapOf("showActions" to JsonPrimitive(it))
                } ?: emptyMap()),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++; data = it.data })
        }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        assertEquals(0, changes)
        if (cupertino) onNodeWithText("10").performClick()
        else onNodeWithContentDescription("10 hours").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"10:30:37Z"}"""), data)
        assertEquals(1, changes)
    }
}
