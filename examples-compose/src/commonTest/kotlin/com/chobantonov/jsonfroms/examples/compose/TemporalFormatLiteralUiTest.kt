package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
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
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class TemporalFormatLiteralUiTest {
    @Test fun materialTimeCustomOffsetUsesUtcNormalizedValue() = customTimeOffset(false)
    @Test fun cupertinoTimeCustomOffsetUsesUtcNormalizedValue() = customTimeOffset(true)
    @Test fun materialDateTimeAcceptsCustomOffsetToken() = customDateTimeOffset(false)
    @Test fun cupertinoDateTimeAcceptsCustomOffsetToken() = customDateTimeOffset(true)

    private fun customTimeOffset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var writes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "format" to JsonPrimitive("time"),
                    "timeFormat" to JsonPrimitive("HH:mm:ss Z"),
                    "timeSaveFormat" to JsonPrimitive("HH:mm:ss Z"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("20:30:00 +02:00")
        waitForIdle()
        assertEquals(1, writes)
        assertEquals(Json.parseToJsonElement("""{"value":"18:30:00 +00:00"}"""), data)
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun customDateTimeOffset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var writes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "format" to JsonPrimitive("date-time"),
                    "dateTimeFormat" to JsonPrimitive("YYYY/MM/DD HH:mm:ss Z"),
                    "dateTimeSaveFormat" to JsonPrimitive("YYYY/MM/DD HH:mm:ss Z"),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst()
            .performTextReplacement("2026/08/16 20:31:00 +02:00")
        waitForIdle()
        assertEquals(1, writes)
        val stored = assertNotNull(data.jsonObject["value"] as? JsonPrimitive).content
        val match = assertNotNull(Regex("""(\d{4})/(\d{2})/(\d{2}) (\d{2}):(\d{2}):(\d{2}) ([+-]\d{2}:\d{2})""")
            .matchEntire(stored))
        val fields = match.groupValues
        val iso = "${fields[1]}-${fields[2]}-${fields[3]}T${fields[4]}:${fields[5]}:${fields[6]}${fields[7]}"
        assertEquals(Instant.parse("2026-08-16T18:31:00Z"), Instant.parse(iso))
    }

    @Test fun materialTimeAcceptsBracketQuotedDisplayLiteral() = bracketTime(false)
    @Test fun cupertinoTimeAcceptsBracketQuotedDisplayLiteral() = bracketTime(true)

    private fun bracketTime(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"time"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "timeFormat" to JsonPrimitive("HH:mm:ss[ UTC]"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("09:31:37 UTC")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:31:37Z"}"""), data)
    }
}
