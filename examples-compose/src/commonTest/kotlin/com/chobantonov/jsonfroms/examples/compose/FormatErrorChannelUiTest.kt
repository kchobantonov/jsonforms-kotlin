package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FormatErrorChannelUiTest {
    @Test fun materialDurationUsesSchemaErrorChannel() = verify(false, false)
    @Test fun cupertinoDurationUsesSchemaErrorChannel() = verify(true, false)
    @Test fun materialCronUsesSchemaErrorChannel() = verify(false, true)
    @Test fun cupertinoCronUsesSchemaErrorChannel() = verify(true, true)
    @Test fun materialDateBoundUsesSchemaErrorChannel() = temporal(false, "date", "2025-01-09", "2025-01-10")
    @Test fun cupertinoDateBoundUsesSchemaErrorChannel() = temporal(true, "date", "2025-01-09", "2025-01-10")
    @Test fun materialTimeBoundUsesSchemaErrorChannel() = temporal(false, "time", "09:00:00Z", "10:00:00Z")
    @Test fun cupertinoTimeBoundUsesSchemaErrorChannel() = temporal(true, "time", "09:00:00Z", "10:00:00Z")
    @Test fun materialDateTimeBoundUsesSchemaErrorChannel() = temporal(false, "date-time",
        "2025-01-09T09:00:00Z", "2025-01-10T10:00:00Z")
    @Test fun cupertinoDateTimeBoundUsesSchemaErrorChannel() = temporal(true, "date-time",
        "2025-01-09T09:00:00Z", "2025-01-10T10:00:00Z")

    private fun temporal(cupertino: Boolean, format: String, invalid: String, valid: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"value":{"type":"string","format":"$format",
                "formatMinimum":"$valid"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":"$invalid"}"""))
        var hostErrors by mutableStateOf(listOf(ErrorObject(
            instancePath = "/value", keyword = "server", message = "Server rejected value")))
        var validity: CombinedValidity? = null
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                additionalErrors = hostErrors,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it })
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(1, validity?.additionalErrorCount)
            assertEquals(0, writes)
            hostErrors = emptyList()
        }
        waitForIdle()
        runOnIdle {
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(0, validity?.additionalErrorCount)
            data = Json.parseToJsonElement("""{"value":"$valid"}""")
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity?.status)
            assertEquals(0, validity?.schemaErrorCount)
            assertEquals(0, validity?.additionalErrorCount)
            assertEquals(0, writes)
        }
    }

    private fun verify(cupertino: Boolean, cron: Boolean) = runComposeUiTest {
        val field = if (cron) "schedule" else "duration"
        val format = if (cron) "cron" else "duration"
        val invalid = if (cron) "* * * * *" else "bogus"
        val valid = if (cron) "0 0 9 * * *" else "P2D"
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"object","properties":{"$field":{"type":"string","format":"$format"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"$field":"$invalid"}"""))
        var hostErrors by mutableStateOf(listOf(ErrorObject(
            instancePath = "/server", message = "Server rejected request")))
        var validity: CombinedValidity? = null
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/$field"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                additionalErrors = hostErrors,
                onChange = { data = it.data; writes++ },
                onValidityChange = { validity = it })
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(1, validity?.additionalErrorCount)
            assertEquals(0, writes)
        }
        onAllNodesWithText(invalid).onLast().performTextReplacement(valid)
        waitForIdle()
        runOnIdle {
            assertEquals(Json.parseToJsonElement("""{"$field":"$valid"}"""), data)
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(0, validity?.schemaErrorCount)
            assertEquals(1, validity?.additionalErrorCount)
            assertEquals(1, writes)
            hostErrors = emptyList()
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Valid, validity?.status)
            assertEquals(0, validity?.schemaErrorCount)
            assertEquals(0, validity?.additionalErrorCount)
            assertEquals(1, writes)
        }
    }
}
