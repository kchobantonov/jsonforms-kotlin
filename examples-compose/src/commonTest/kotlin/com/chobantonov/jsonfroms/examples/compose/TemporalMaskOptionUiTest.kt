package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put

@OptIn(ExperimentalTestApi::class)
class TemporalMaskOptionUiTest {
    internal fun cupertinoDateTimeMask(maskOption: JsonPrimitive?, acceptsDraft: Boolean) =
        mask(true, "date-time", maskOption, acceptsDraft)

    @Test fun materialDefaultMaskRejectsLetters() = mask(false, "date", null, false)
    @Test fun cupertinoDefaultMaskRejectsLetters() = mask(true, "date", null, false)
    @Test fun materialBooleanFalseAllowsDraftLetters() = mask(false, "date", JsonPrimitive(false), true)
    @Test fun cupertinoBooleanFalseAllowsDraftLetters() = mask(true, "date", JsonPrimitive(false), true)
    @Test fun materialStringFalseDoesNotDisableMask() = mask(false, "date", JsonPrimitive("false"), false)
    @Test fun cupertinoStringFalseDoesNotDisableMask() = mask(true, "date", JsonPrimitive("false"), false)
    @Test fun materialTimeDefaultMaskRejectsLetters() = mask(false, "time", null, false)
    @Test fun cupertinoTimeDefaultMaskRejectsLetters() = mask(true, "time", null, false)
    @Test fun materialTimeBooleanFalseAllowsDraftLetters() = mask(false, "time", JsonPrimitive(false), true)
    @Test fun cupertinoTimeBooleanFalseAllowsDraftLetters() = mask(true, "time", JsonPrimitive(false), true)
    @Test fun materialTimeStringFalseDoesNotDisableMask() = mask(false, "time", JsonPrimitive("false"), false)
    @Test fun cupertinoTimeStringFalseDoesNotDisableMask() = mask(true, "time", JsonPrimitive("false"), false)
    @Test fun materialDateTimeDefaultMaskRejectsLetters() = mask(false, "date-time", null, false)
    @Test fun materialDateTimeBooleanFalseAllowsDraftLetters() = mask(false, "date-time", JsonPrimitive(false), true)
    @Test fun materialDateTimeStringFalseDoesNotDisableMask() = mask(false, "date-time", JsonPrimitive("false"), false)

    private fun mask(cupertino: Boolean, format: String, maskOption: JsonPrimitive?, acceptsDraft: Boolean) =
        runComposeUiTest {
        var changes = 0
        val formatOption = when (format) {
            "date" -> "dateFormat"
            "time" -> "timeFormat"
            else -> "dateTimeFormat"
        }
        val displayFormat = when (format) {
            "date" -> "DD.MM.YYYY"
            "time" -> "HH:mm"
            else -> "DD.MM.YYYY HH:mm"
        }
        setContent {
            JsonForms(
                data = JsonPrimitive(""),
                schema = buildJsonSchema { put("type", "string") },
                uischema = ControlElement("#", options = mapOf(
                    "format" to JsonPrimitive(format),
                    formatOption to JsonPrimitive(displayFormat),
                ) + (if (maskOption == null) emptyMap() else mapOf("mask" to maskOption))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ },
            )
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("bad")
        waitForIdle()
        assertEquals(if (acceptsDraft) 1 else 0,
            onAllNodes(hasSetTextAction() and hasText("bad")).fetchSemanticsNodes().size)
        assertEquals(0, changes)
    }
}
