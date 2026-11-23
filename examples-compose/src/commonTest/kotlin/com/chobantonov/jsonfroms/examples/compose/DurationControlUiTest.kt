package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
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

@OptIn(ExperimentalTestApi::class)
class DurationControlUiTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"duration":{"type":"string","format":"duration"}}
    }""").jsonObject)

    @Test fun materialDurationStagesUnboundedComponents() = stages(false)
    @Test fun cupertinoDurationStagesUnboundedComponents() = stages(true)
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

    private fun stages(cupertino: Boolean) = runComposeUiTest {
        val original = "P23Y51M724DT92H150M3600S"
        var data by mutableStateOf(Json.parseToJsonElement("""{"duration":"$original"}"""))
        var locale by mutableStateOf("en")
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/duration"),
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
