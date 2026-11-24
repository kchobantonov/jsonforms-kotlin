package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class TemporalFormatHintUiTest {
    @Test fun materialDateHint() = hint(false, "date", "dateFormat", "DD.MM.YYYY")
    @Test fun cupertinoDateHint() = hint(true, "date", "dateFormat", "DD.MM.YYYY")
    @Test fun materialTimeHint() = hint(false, "time", "timeFormat", "HH:mm")
    @Test fun cupertinoTimeHint() = hint(true, "time", "timeFormat", "HH:mm")
    @Test fun materialDateTimeHint() = hint(false, "date-time", "dateTimeFormat", "DD.MM.YYYY HH:mm")
    @Test fun cupertinoDateTimeHint() = hint(true, "date-time", "dateTimeFormat", "DD.MM.YYYY HH:mm")
    @Test fun materialAuthoredPlaceholderOverridesFormatHint() =
        hint(false, "date", "dateFormat", "DD.MM.YYYY", "Enter a date")
    @Test fun cupertinoAuthoredPlaceholderOverridesFormatHint() =
        hint(true, "date", "dateFormat", "DD.MM.YYYY", "Enter a date")

    @Test fun materialDateHintUpdateKeepsDraft() = hintUpdateKeepsDraft(false, "date", "2024-03-15")
    @Test fun cupertinoDateHintUpdateKeepsDraft() = hintUpdateKeepsDraft(true, "date", "2024-03-15")
    @Test fun materialTimeHintUpdateKeepsDraft() = hintUpdateKeepsDraft(false, "time", "09:30:00")
    @Test fun cupertinoTimeHintUpdateKeepsDraft() = hintUpdateKeepsDraft(true, "time", "09:30:00")
    @Test fun materialDateTimeHintUpdateKeepsDraft() = hintUpdateKeepsDraft(false, "date-time", "2024-03-15T09:30:00Z")
    @Test fun cupertinoDateTimeHintUpdateKeepsDraft() = hintUpdateKeepsDraft(true, "date-time", "2024-03-15T09:30:00Z")

    private fun hintUpdateKeepsDraft(cupertino: Boolean, format: String, stored: String) = runComposeUiTest {
        var control by mutableStateOf(ControlElement("#", options = mapOf(
            "format" to JsonPrimitive(format), "mask" to JsonPrimitive(false),
            "placeholder" to JsonPrimitive("First hint"),
        )))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        var changes = 0
        setContent {
            JsonForms(data = JsonPrimitive(stored), schema = buildJsonSchema { put("type", "string") },
                uischema = control, renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ }, onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        runOnIdle { control = control.copy(options = control.options + ("placeholder" to JsonPrimitive("Updated hint"))) }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        onAllNodes(hasSetTextAction()).onFirst().assertIsFocused()
        assertEquals("bad", onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
        assertEquals(0, changes)
    }

    private fun hint(cupertino: Boolean, format: String, formatOption: String, displayFormat: String,
                     authoredPlaceholder: String? = null) =
        runComposeUiTest {
            val hint = authoredPlaceholder ?: displayFormat
            setContent {
                JsonForms(
                    data = JsonPrimitive(""),
                    schema = buildJsonSchema { put("type", "string") },
                    uischema = ControlElement("#", options = mapOf(
                        "format" to JsonPrimitive(format),
                        formatOption to JsonPrimitive(displayFormat),
                    ) + (if (authoredPlaceholder != null) mapOf(
                        "placeholder" to JsonPrimitive(authoredPlaceholder)) else emptyMap())),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                )
            }
            if (cupertino) onNodeWithText(hint, useUnmergedTree = true).assertExists()
            assertContains(
                onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode()
                    .config[SemanticsProperties.StateDescription],
                hint,
            )
        }
}
