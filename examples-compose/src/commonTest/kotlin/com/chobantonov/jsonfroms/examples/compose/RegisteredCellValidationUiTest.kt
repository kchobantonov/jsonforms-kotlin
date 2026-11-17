package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellRenderer
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsCellRendererRegistryEntry
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class RegisteredCellValidationUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"rows":{"type":"array","items":{"type":"object","properties":{"code":{"type":"string","minLength":2}}}}}}"""
    ).jsonObject)
    private val ui = ControlElement("#/properties/rows", options = mapOf(
        "table" to JsonPrimitive(true),
        "showValidationIndicator" to JsonPrimitive(false),
    ))

    @Test fun materialRegisteredCellKeepsItsErrorMarker() = verify(false, false)
    @Test fun cupertinoRegisteredCellKeepsItsErrorMarker() = verify(true, false)
    @Test fun materialCellWithOwnFeedbackAvoidsDuplicateMarker() = verify(false, true)
    @Test fun cupertinoCellWithOwnFeedbackAvoidsDuplicateMarker() = verify(true, true)

    private fun verify(cupertino: Boolean, ownsFeedback: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"rows":[{"code":"X"}]}"""))
        var hostErrors by mutableStateOf(listOf(
            ErrorObject(instancePath = "/rows/0/code", message = "Server rejected code")))
        var writes = 0
        val cell = JsonFormsCellRendererRegistryEntry(
            tester = { _, _, _ -> 100 },
            cell = ComposeJsonFormsCellRenderer { Text("Registered code cell") },
            ownsValidationFeedback = ownsFeedback,
        )
        setContent {
            JsonForms(data = data, schema = schema, uischema = ui,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                cells = listOf(cell), additionalErrors = hostErrors,
                onChange = { writes++ })
        }
        onNodeWithText("Registered code cell").assertExists()
        if (ownsFeedback) {
            onNodeWithContentDescription("Code contains errors", substring = true).assertDoesNotExist()
        } else {
            onNodeWithContentDescription("Code contains errors", substring = true).assertExists()
            onNodeWithContentDescription("Server rejected code", substring = true).assertExists()
        }
        runOnIdle {
            assertEquals(0, writes)
            data = Json.parseToJsonElement("""{"rows":[{"code":"OK"}]}""")
            hostErrors = emptyList()
        }
        onNodeWithContentDescription("Code contains errors", substring = true).assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
    }
}
