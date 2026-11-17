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
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalFormatLiteralUiTest {
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
