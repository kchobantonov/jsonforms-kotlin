package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalLiveConfigUiTest {
    @Test fun materialDateFormatChangeDiscardsStaleDraft() = formatChange(false, "date")
    @Test fun cupertinoDateFormatChangeDiscardsStaleDraft() = formatChange(true, "date")
    @Test fun materialTimeFormatChangeDiscardsStaleDraft() = formatChange(false, "time")
    @Test fun cupertinoTimeFormatChangeDiscardsStaleDraft() = formatChange(true, "time")
    @Test fun materialDateTimeFormatChangeDiscardsStaleDraft() = formatChange(false, "date-time")
    @Test fun cupertinoDateTimeFormatChangeDiscardsStaleDraft() = formatChange(true, "date-time")

    private fun formatChange(cupertino: Boolean, format: String) = runComposeUiTest {
        val (stored, oldDisplay, newDisplay, formatKey, newFormat) = when (format) {
            "date" -> listOf("2024-03-15", "03/15/2024", "15.03.2024", "dateFormat", "DD.MM.YYYY")
            "time" -> listOf("14:05:00Z", "2:05 PM", "14:05", "timeFormat", "HH:mm")
            else -> listOf("2024-03-15 14:05", "03/15/2024 2:05 PM", "15.03.2024 14:05",
                "dateTimeFormat", "DD.MM.YYYY HH:mm")
        }
        val schema = buildJsonSchema { put("type", "string") }
        val data = JsonPrimitive(stored)
        val initialConfig = if (format == "date-time") mapOf<String, Any?>(
            "dateTimeSaveFormat" to "YYYY-MM-DD HH:mm") else emptyMap()
        var config by mutableStateOf(initialConfig)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement(scope = "#", options = mapOf("format" to JsonPrimitive(format))),
                config = config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction() and hasText(oldDisplay, substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { config = initialConfig + (formatKey to newFormat) }
        onAllNodes(hasSetTextAction() and hasText(newDisplay, substring = true)).onFirst().assertExists()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(0, writes)
    }
}
