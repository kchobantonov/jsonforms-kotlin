package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertContains
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
