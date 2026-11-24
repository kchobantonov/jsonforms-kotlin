package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DurationControlUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"duration":{"type":"string","format":"duration"}}
    }""").jsonObject)

    @Test fun materialDurationStagesUnboundedComponents() = stages(false)
    @Test fun cupertinoDurationStagesUnboundedComponents() = stages(true)
    @Test fun materialQuotedShowActionsStillStages() = stages(false, JsonPrimitive("false"))
    @Test fun cupertinoQuotedShowActionsStillStages() = stages(true, JsonPrimitive("false"))
    @Test fun materialInvalidLocalShowActionsUsesGlobal() = globalShowActions(false)
    @Test fun cupertinoInvalidLocalShowActionsUsesGlobal() = globalShowActions(true)
    @Test fun materialDurationKeepsWeeksUntilEdited() = weeks(false)
    @Test fun cupertinoDurationKeepsWeeksUntilEdited() = weeks(true)
    @Test fun materialDurationTextKeepsInvalidDraftPending() = directText(false)
    @Test fun cupertinoDurationTextKeepsInvalidDraftPending() = directText(true)
    @Test fun materialCommittedDurationUsesSchemaValidation() = committedValidation(false)
    @Test fun cupertinoCommittedDurationUsesSchemaValidation() = committedValidation(true)
    @Test fun materialImmediateDurationEditsKeepFocus() = immediateEdit(false)
    @Test fun cupertinoImmediateDurationEditsKeepFocus() = immediateEdit(true)
    @Test fun materialPickerCorrectsInvalidTextDraft() = pickerCorrectsDraft(false)
    @Test fun cupertinoPickerCorrectsInvalidTextDraft() = pickerCorrectsDraft(true)
    @Test fun materialUiOnlyDurationDoesNotAddSchemaError() = uiOnlyFormat(false)
    @Test fun cupertinoUiOnlyDurationDoesNotAddSchemaError() = uiOnlyFormat(true)
    @Test fun materialEditedZeroWeeksUsesCanonicalZero() = editedZeroWeeks(false)
    @Test fun cupertinoEditedZeroWeeksUsesCanonicalZero() = editedZeroWeeks(true)
    @Test fun materialUntouchedPickerPreservesImportedWeeks() = untouchedPicker(false)
    @Test fun cupertinoUntouchedPickerPreservesImportedWeeks() = untouchedPicker(true)
    @Test fun materialPickerAddsAndRemovesVisibleUnits() = addAndRemoveUnits(false)
    @Test fun cupertinoPickerAddsAndRemovesVisibleUnits() = addAndRemoveUnits(true)
    @Test fun materialPickerSwitchesToWeeksAndCancels() = switchAndCancel(false)
    @Test fun cupertinoPickerSwitchesToWeeksAndCancels() = switchAndCancel(true)
    @Test fun materialPickerFitsNarrowHost() = narrowPicker(false)
    @Test fun cupertinoPickerFitsNarrowHost() = narrowPicker(true)
    @Test fun materialBulgarianPickerFitsNarrowHost() = narrowPicker(false, "bg")
    @Test fun cupertinoBulgarianPickerFitsNarrowHost() = narrowPicker(true, "bg")

    private fun globalShowActions(cupertino: Boolean) = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"duration":"PT1H30M"}""")
        var local by mutableStateOf(JsonPrimitive("false"))
        var global by mutableStateOf(false)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration",
                    options = mapOf("showActions" to local)),
                config = mapOf("showActions" to JsonPrimitive(global)),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers)
        }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Apply").assertCountEquals(0)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        runOnIdle { global = true }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Apply").onLast().assertExists()
        onAllNodesWithText("Cancel").onLast().performClick()
        runOnIdle { local = JsonPrimitive(false) }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Apply").assertCountEquals(0)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        runOnIdle { global = false; local = JsonPrimitive(true) }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Apply").onLast().assertExists()
    }

    private fun narrowPicker(cupertino: Boolean, locale: String = "en") = runComposeUiTest {
        val data = Json.parseToJsonElement("""{"duration":"PT1H30M"}""")
        var changes = 0
        setContent {
            Box(Modifier.width(240.dp).testTag("duration-narrow-host")) {
                JsonForms(data = data, schema = schema,
                    uischema = ControlElement("#/properties/duration"),
                    i18n = JsonFormsI18nState(locale = locale),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    onChange = { changes++ })
            }
        }
        onAllNodesWithContentDescription(if (locale == "bg") "Избери продължителност"
            else "Choose duration").onLast().performClick()
        val host = onNodeWithTag("duration-narrow-host").fetchSemanticsNode().boundsInRoot
        val panel = onNodeWithTag("duration-picker-panel", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(panel.width <= host.width + 2f,
            "Duration popup must fit the narrow host: panel=${panel.width} host=${host.width}")
        val components = onNodeWithText(if (locale == "bg") "От години до секунди"
            else "Years to seconds").fetchSemanticsNode().boundsInRoot
        val weeks = onAllNodesWithText(if (locale == "bg") "Седмици" else "Weeks")
            .onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue(weeks.top >= components.bottom,
            "Mode buttons should stack inside the narrow popup")
        assertTrue(components.left >= panel.left - 2f && components.right <= panel.right + 2f &&
            weeks.left >= panel.left - 2f && weeks.right <= panel.right + 2f,
            "Translated mode labels should stay inside the narrow popup")
        val hour = if (locale == "bg") "Часове" else "Hours"
        val minute = if (locale == "bg") "Минути" else "Minutes"
        val hourInput = onNodeWithContentDescription(hour).fetchSemanticsNode().boundsInRoot
        val minuteInput = onNodeWithContentDescription(minute).fetchSemanticsNode().boundsInRoot
        val hourLabel = onAllNodesWithText(hour).onLast().fetchSemanticsNode().boundsInRoot
        val minuteLabel = onAllNodesWithText(minute).onLast().fetchSemanticsNode().boundsInRoot
        val hourRemove = onNodeWithContentDescription(if (locale == "bg")
            "Премахване на „Часове“" else "Remove Hours").fetchSemanticsNode().boundsInRoot
        val minuteRemove = onNodeWithContentDescription(if (locale == "bg")
            "Премахване на „Минути“" else "Remove Minutes").fetchSemanticsNode().boundsInRoot
        assertTrue(hourInput.right <= hourLabel.left + 2f &&
            minuteInput.right <= minuteLabel.left + 2f,
            "Unit labels must follow their quantity fields")
        assertTrue(hourLabel.width >= 48f && minuteLabel.width >= 48f,
            "Narrow popup must leave readable space for unit labels")
        assertTrue(hourLabel.right <= hourRemove.left + 2f &&
            minuteLabel.right <= minuteRemove.left + 2f &&
            hourRemove.right <= panel.right + 2f && minuteRemove.right <= panel.right + 2f,
            "Remove actions must stay outside inputs and inside the popup")
        assertTrue(kotlin.math.abs(hourInput.right - minuteInput.right) <= 2f &&
            kotlin.math.abs(hourLabel.left - minuteLabel.left) <= 2f &&
            kotlin.math.abs(hourRemove.left - minuteRemove.left) <= 2f,
            "Duration rows must use aligned quantity, label and action columns")
        assertEquals(0, changes)
    }

    private fun addAndRemoveUnits(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"PT1H30M"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onNodeWithText("Hours").assertExists()
        onNodeWithText("Minutes").assertExists()
        onNodeWithText("Years").assertDoesNotExist()
        onNodeWithContentDescription("Remove Hours").assertExists()
        onNodeWithContentDescription("Remove Minutes").assertExists()
        onNodeWithText("Add unit").performClick()
        onNodeWithText("Years").performClick()
        onNodeWithText("Years").assertExists()
        assertEquals(0, changes)
        onNodeWithContentDescription("Remove Minutes").performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("PT1H"), data.jsonObject["duration"])
        assertEquals(1, changes)
    }

    private fun switchAndCancel(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"PT1H30M"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Weeks").onFirst().performClick()
        onNodeWithText("Hours").assertDoesNotExist()
        onNodeWithText("Add unit").assertDoesNotExist()
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(JsonPrimitive("PT1H30M"), data.jsonObject["duration"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onNodeWithText("Hours").assertExists()
        onNodeWithText("Minutes").assertExists()
    }

    private fun untouchedPicker(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"P000W"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Weeks").onLast().assertExists()
        onAllNodesWithText("Weeks").onFirst().performClick()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("P000W"), data.jsonObject["duration"])
        assertEquals(0, changes)
        onNodeWithText("Apply").assertDoesNotExist()
    }

    private fun editedZeroWeeks(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"P000W"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(JsonPrimitive("P000W"), data.jsonObject["duration"])
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("0").onLast().performTextReplacement("00")
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("P0D"), data.jsonObject["duration"])
        assertEquals(1, changes)
    }

    private fun uiOnlyFormat(cupertino: Boolean) = runComposeUiTest {
        val plainSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"duration":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"bad"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = plainSchema,
                uischema = ControlElement("#/properties/duration", options = mapOf(
                    "format" to JsonPrimitive("duration"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(0, changes)
        onAllNodesWithText("bad").onLast().performTextReplacement("P1")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        assertEquals(JsonPrimitive("bad"), data.jsonObject["duration"])
        onAllNodesWithText("P1").onLast().performTextReplacement("P1D")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(JsonPrimitive("P1D"), data.jsonObject["duration"])
        assertEquals(1, changes)
    }

    private fun immediateEdit(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"PT150M"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration", options = mapOf(
                    "showActions" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose duration").performClick()
        onAllNodesWithText("150").onLast().performTextReplacement("151")
        waitForIdle()
        assertEquals(JsonPrimitive("PT151M"), data.jsonObject["duration"])
        onAllNodesWithText("151").onLast().assertIsFocused()
        onAllNodesWithText("151").onLast().performTextReplacement("152")
        waitForIdle()
        assertEquals(JsonPrimitive("PT152M"), data.jsonObject["duration"])
        assertEquals(2, changes)
    }

    private fun pickerCorrectsDraft(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"P1D"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodesWithText("P1D").onLast().performTextReplacement("P1")
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("Weeks").onLast().performClick()
        onNodeWithText("Years").assertDoesNotExist()
        onAllNodesWithText("0").onLast().assertExists()
        onAllNodesWithText("0").onLast().performTextReplacement("00")
        onAllNodesWithText("Apply").onLast().assertIsEnabled()
        onAllNodesWithText("Apply").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("P0D"), data.jsonObject["duration"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, changes)
    }

    private fun committedValidation(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"bogus"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(0, changes)
        onAllNodesWithText("bogus").onLast().performTextReplacement("P2D")
        waitForIdle()
        assertEquals(JsonPrimitive("P2D"), data.jsonObject["duration"])
        assertEquals(CombinedValidityStatus.Valid, validity)
        assertEquals(1, changes)
    }

    private fun directText(cupertino: Boolean) = runComposeUiTest {
        val plainSchema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"duration":{"type":"string"}}
        }""").jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"P1D"}"""))
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = plainSchema,
                uischema = ControlElement("#/properties/duration", options = mapOf(
                    "format" to JsonPrimitive("duration"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodesWithText("P1D").onLast().performTextReplacement("P1")
        waitForIdle()
        assertEquals(JsonPrimitive("P1D"), data.jsonObject["duration"])
        assertEquals(0, changes)
        assertEquals(CombinedValidityStatus.Pending, validity)
        runOnIdle { data = Json.parseToJsonElement("""{"duration":"P2D"}""") }
        waitForIdle()
        onAllNodesWithText("P2D").onLast().assertExists()
        assertEquals(CombinedValidityStatus.Valid, validity)
        onAllNodesWithText("P2D").onLast().performTextReplacement("P3W")
        waitForIdle()
        assertEquals(JsonPrimitive("P3W"), data.jsonObject["duration"])
        assertEquals(1, changes)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun stages(cupertino: Boolean, showActionsOption: JsonPrimitive? = null) = runComposeUiTest {
        val original = "P23Y51M724DT92H150M3600S"
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"$original"}"""))
        var locale by mutableStateOf("en")
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration", options =
                    showActionsOption?.let { mapOf("showActions" to it) } ?: emptyMap()),
                i18n = JsonFormsI18nState(locale = locale),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onNodeWithText("23").performTextReplacement("24")
        assertEquals(JsonPrimitive(original), data.jsonObject["duration"])
        onAllNodesWithText("Cancel").onLast().performClick()
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onAllNodesWithText("23").onLast().performTextReplacement("oops")
        onAllNodesWithText("Apply").onLast().assertIsNotEnabled()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Pending, validity)
        assertEquals(JsonPrimitive(original), data.jsonObject["duration"])
        onAllNodesWithText("Cancel").onLast().performClick()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Valid, validity)
        runOnIdle { locale = "bg" }
        onAllNodesWithContentDescription("Избери продължителност").onLast().performClick()
        if (cupertino) {
            onAllNodesWithText("23").onLast().performTextReplacement("-1")
            onNodeWithText("Въведете неотрицателно цяло число.").assertExists()
            onAllNodesWithText("-1").onLast().performTextReplacement("23")
            onNodeWithText("Въведете неотрицателно цяло число.").assertDoesNotExist()
        }
        onAllNodesWithText("23").onLast().performTextReplacement("24")
        onAllNodesWithText("Прилагане").onLast().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("P24Y51M724DT92H150M3600S"), data.jsonObject["duration"])
        assertEquals(1, changes)
    }

    private fun weeks(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"P000W"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Apply").assertDoesNotExist()
        assertEquals(0, changes)
        onAllNodesWithContentDescription("Choose duration").onLast().performClick()
        onNodeWithText("0").performTextReplacement("90")
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("P90W"), data.jsonObject["duration"])
        assertEquals(1, changes)
    }
}
