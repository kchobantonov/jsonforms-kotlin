package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FormatConflictUiTest {
    @Test fun materialTieKeepsDatePresentationAndTimeValidation() = verify(false)
    @Test fun cupertinoTieKeepsDatePresentationAndTimeValidation() = verify(true)

    private fun verify(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"time"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":"10:30:00Z"}"""))
        var errors: List<ErrorObject> = emptyList()
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "format" to JsonPrimitive("date"),
                    "dateFormat" to JsonPrimitive("YYYY-MM-DD"),
                )),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; errors = it.errors; writes++ })
        }
        onNodeWithContentDescription("Choose date").assertExists()
        runOnIdle { assertEquals(0, writes) }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("2024-06-15")
        runOnIdle {
            assertEquals(JsonPrimitive("2024-06-15"), data.jsonObject["value"])
            assertEquals(1, writes)
            assertTrue(errors.any { it.keyword == "format" }, errors.toString())
        }
    }
}
