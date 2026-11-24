package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalPickerSecondsUiTest {
    @Test fun materialTimePickerKeepsUnshownSeconds() = preservesSeconds(false, false)
    @Test fun cupertinoTimePickerKeepsUnshownSeconds() = preservesSeconds(true, false)
    @Test fun materialDateTimePickerKeepsUnshownSeconds() = preservesSeconds(false, true)
    @Test fun cupertinoDateTimePickerKeepsUnshownSeconds() = preservesSeconds(true, true)
    @Test fun materialDateTimePickerAcceptsRetainedSecondWithinOneMinute() = boundedSecond(false)
    @Test fun cupertinoDateTimePickerAcceptsRetainedSecondWithinOneMinute() = boundedSecond(true)

    private fun boundedSecond(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date-time","formatMinimum":"2025-01-10T09:30:30Z","formatMaximum":"2025-01-10T09:30:40Z"}}}"""
        ).jsonObject)
        val original = Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}""")
        var data by mutableStateOf(original)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "showActions" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithText("Next").performClick()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(original, data)
    }

    private fun preservesSeconds(cupertino: Boolean, dateTime: Boolean) = runComposeUiTest {
        val format = if (dateTime) "date-time" else "time"
        val value = if (dateTime) "2025-01-10T09:30:37Z" else "09:30:37Z"
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"$format"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"$value"}"""))
        val original = data
        val views = if (dateTime) listOf("year", "month", "day", "hours", "minutes")
            else listOf("hours", "minutes")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "views" to JsonArray(views.map(::JsonPrimitive)),
                    "showActions" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription(if (dateTime) "Choose date and time" else "Choose time").performClick()
        if (dateTime) onNodeWithText("Next").performClick()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(original, data)
    }
}
