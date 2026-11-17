package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoNativeControl
import com.chobantonov.jsonfroms.cupertino.renderers.controls.cupertinoNativeControlTester
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CupertinoNativeFallbackUiTest {
    @Test fun dateFallbackShowsInvalidImportAndRepairsWithoutMountWrite() =
        fallback("date", "2024-01-02", uiFormat = false)
    @Test fun timeFallbackShowsInvalidImportAndRepairsWithoutMountWrite() =
        fallback("time", "10:30:00Z", uiFormat = true)

    private fun fallback(format: String, replacement: String, uiFormat: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":null}"""))
        var locale by mutableStateOf("en")
        var changes = 0
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string"${if (uiFormat) "" else ",\"format\":\"$format\""}}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options =
                    if (uiFormat) mapOf("format" to JsonPrimitive(format)) else emptyMap()),
                renderers = listOf(JsonFormsRendererRegistryEntry(
                    cupertinoNativeControlTester, CupertinoNativeControl)),
                i18n = JsonFormsI18nState(locale = locale),
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Invalid value: null").assertExists()
        assertTrue(onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.Error].contains("Invalid value: null"))
        runOnIdle {
            assertEquals(0, changes)
            locale = "bg-BG"
        }
        onNodeWithText("Невалидна стойност: null").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(replacement)
        runOnIdle {
            assertEquals(JsonPrimitive(replacement), data.jsonObject["value"])
            assertEquals(1, changes)
            data = Json.parseToJsonElement("""{"value":"external"}""")
        }
        onNodeWithText("Невалидна стойност: null").assertDoesNotExist()
        assertEquals("external", onAllNodes(hasSetTextAction()).onFirst()
            .fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        runOnIdle { assertEquals(1, changes) }
    }
}
