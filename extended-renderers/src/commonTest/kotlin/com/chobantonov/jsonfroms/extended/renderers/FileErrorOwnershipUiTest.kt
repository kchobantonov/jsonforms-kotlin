package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FileErrorOwnershipUiTest {
    @Test fun materialDuplicateFileControlsOwnErrorsIndependently() = ownership(false)
    @Test fun cupertinoDuplicateFileControlsOwnErrorsIndependently() = ownership(true)

    private fun ownership(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"file":{"type":"string","format":"binary"}}}"""
        ).jsonObject)
        val control = ControlElement("#/properties/file", options = mapOf(
            "formatMaximum" to JsonPrimitive(3), "propagateErrors" to JsonPrimitive(true)))
        val withSecond = VerticalLayout(elements = listOf(control, control))
        val withoutSecond = VerticalLayout(elements = listOf(control))
        val relaxed = VerticalLayout(elements = listOf(ControlElement("#/properties/file",
            options = mapOf("formatMaximum" to JsonPrimitive(6),
                "propagateErrors" to JsonPrimitive(true)))))
        var data by mutableStateOf(Json.parseToJsonElement("""{"file":"old"}"""))
        var showSecond by mutableStateOf(true)
        var useRelaxedLimit by mutableStateOf(false)
        var hostErrors by mutableStateOf(listOf(
            ErrorObject(instancePath = "/file", keyword = "host", message = "First host error")))
        var validity by mutableStateOf<CombinedValidity?>(null)
        var picks = 0
        val picker = ExtendedFilePickerEvents { _, _, onEvent ->
            picks++
            onEvent(FilePickerEvent.Selected(JsonPrimitive(if (picks == 3) "larger" else "large"),
                if (picks == 3) 8 else 3L + picks))
        }
        var changes = 0
        setContent {
            JsonForms(
                data = data,
                schema = schema,
                uischema = if (showSecond) withSecond else if (useRelaxedLimit) relaxed else withoutSecond,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                config = mapOf("restrict" to false, ExtendedFilePickerEventsConfigKey to picker),
                additionalErrors = hostErrors,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it },
            )
        }

        fun assertErrors(count: Int, stage: String) = runOnIdle {
            assertEquals(0, validity?.schemaErrorCount)
            assertEquals(count, validity?.additionalErrorCount, stage)
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
        }
        assertErrors(1, "initial")
        onAllNodesWithText("Choose file").onFirst().performClick()
        waitForIdle()
        assertEquals(1, picks, "first pick invoked")
        assertEquals(1, changes, "first pick dispatched")
        runOnIdle { assertEquals(Json.parseToJsonElement("""{"file":"large"}"""), data, "first value") }
        assertErrors(2, "first picker")

        onAllNodesWithText("Choose file").onLast().performClick()
        waitForIdle()
        assertErrors(3, "second picker")
        assertEquals(1, changes)

        runOnIdle {
            hostErrors = listOf(ErrorObject(instancePath = "/file", keyword = "host",
                message = "Replacement host error"))
        }
        assertErrors(3, "host replacement")
        runOnIdle { showSecond = false }
        waitForIdle()
        assertErrors(2, "second disposed")
        runOnIdle { useRelaxedLimit = true }
        waitForIdle()
        assertErrors(1, "size option changed")
        onAllNodesWithText("Choose file").onFirst().performClick()
        waitForIdle()
        assertErrors(2, "new size option used")
        runOnIdle { data = Json.parseToJsonElement("""{"file":"replacement"}""") }
        waitForIdle()
        assertErrors(1, "data replacement")
        assertEquals(2, changes)
    }
}
