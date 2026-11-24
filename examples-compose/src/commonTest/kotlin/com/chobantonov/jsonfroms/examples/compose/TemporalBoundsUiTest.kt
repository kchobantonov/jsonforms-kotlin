package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.core.store.CombinedValidity
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalBoundsUiTest {
    @Test fun materialOffsetTimeBoundRestrictsPicker() = offsetTimeBound(false)
    @Test fun cupertinoOffsetTimeBoundRestrictsPicker() = offsetTimeBound(true)
    @Test fun materialHiddenTemporalBoundParticipatesInValidity() = hiddenBound(false)
    @Test fun cupertinoHiddenTemporalBoundParticipatesInValidity() = hiddenBound(true)
    @Test fun materialHiddenTemporalSchemaPrerequisiteInvalidatesForm() = hiddenBoundSchemaPrerequisite(false)
    @Test fun cupertinoHiddenTemporalSchemaPrerequisiteInvalidatesForm() = hiddenBoundSchemaPrerequisite(true)
    @Test fun materialNonStringDataBoundInvalidatesAndRecovers() = invalidDataBound(false)
    @Test fun cupertinoBulgarianNonStringDataBoundInvalidatesAndRecovers() = invalidDataBound(true)
    @Test fun materialMalformedTemporalReferenceDisablesPicker() = malformedDataReference(false)
    @Test fun cupertinoBulgarianMalformedTemporalReferenceDisablesPicker() = malformedDataReference(true)
    @Test fun materialRelativeDataDateBoundFollowsSibling() = dynamicSiblingBound(false)
    @Test fun cupertinoRelativeDataDateBoundFollowsSibling() = dynamicSiblingBound(true)
    @Test fun materialRelativeDataTimeBoundFollowsSibling() = dynamicSiblingBound(false, "time")
    @Test fun cupertinoRelativeDataTimeBoundFollowsSibling() = dynamicSiblingBound(true, "time")
    @Test fun materialRelativeDataDateTimeBoundFollowsSibling() = dynamicSiblingBound(false, "date-time")
    @Test fun cupertinoRelativeDataDateTimeBoundFollowsSibling() = dynamicSiblingBound(true, "date-time")
    @Test fun materialOpenDatePickerUpdatesRangeLocale() = pickerLocale(false)
    @Test fun cupertinoOpenDatePickerUpdatesRangeLocale() = pickerLocale(true)
    @Test fun materialOpenDatePickerUpdatesSchemaBound() = pickerBoundUpdate(false)
    @Test fun cupertinoOpenDatePickerUpdatesSchemaBound() = pickerBoundUpdate(true)
    @Test fun materialOpenTimePickerUpdatesSchemaBound() = pickerBoundUpdate(false, "time")
    @Test fun cupertinoOpenTimePickerUpdatesSchemaBound() = pickerBoundUpdate(true, "time")
    @Test fun materialOpenDateTimePickerUpdatesSchemaBound() = pickerBoundUpdate(false, "date-time")
    @Test fun cupertinoOpenDateTimePickerUpdatesSchemaBound() = pickerBoundUpdate(true, "date-time")
    @Test fun materialDateBoundMessageUsesDisplayFormat() = displayedBound(false)
    @Test fun cupertinoDateBoundMessageUsesDisplayFormat() = displayedBound(true)
    @Test fun materialTypedDateOutsideBoundCommitsWithError() = temporal(false, "date", "dateFormat",
        "YYYY-MM-DD", "2025-01-10", "2025-01-09", "2025-01-11")
    @Test fun cupertinoTypedDateOutsideBoundCommitsWithError() = temporal(true, "date", "dateFormat",
        "YYYY-MM-DD", "2025-01-10", "2025-01-09", "2025-01-11")
    @Test fun materialTypedTimeOutsideBoundCommitsWithError() = temporal(false, "time", "timeFormat",
        "HH:mm:ss", "09:30:30Z", "09:30:00", "09:31:00")
    @Test fun cupertinoTypedTimeOutsideBoundCommitsWithError() = temporal(true, "time", "timeFormat",
        "HH:mm:ss", "09:30:30Z", "09:30:00", "09:31:00")
    @Test fun materialTypedDateTimeOutsideBoundCommitsWithError() = temporal(false, "date-time", "dateTimeFormat",
        "YYYY-MM-DDTHH:mm:ssZ", "2025-01-10T09:30:30Z", "2025-01-10T09:30:00Z", "2025-01-10T09:31:00Z")
    @Test fun cupertinoTypedDateTimeOutsideBoundCommitsWithError() = temporal(true, "date-time", "dateTimeFormat",
        "YYYY-MM-DDTHH:mm:ssZ", "2025-01-10T09:30:30Z", "2025-01-10T09:30:00Z", "2025-01-10T09:31:00Z")

    @Test fun materialDatePickerRejectsOutOfRangeInitialDate() = picker(false, "date",
        "2025-01-10", "2025-01-09", "Choose date", "OK")
    @Test fun cupertinoDatePickerRejectsOutOfRangeInitialDate() = picker(true, "date",
        "2025-01-10", "2025-01-09", "Choose date", "OK")
    @Test fun materialDatePickerHonorsRestrictFalse() = picker(false, "date",
        "2025-01-10", "2025-01-09", "Choose date", "OK", restrict = false)
    @Test fun cupertinoDatePickerHonorsRestrictFalse() = picker(true, "date",
        "2025-01-10", "2025-01-09", "Choose date", "OK", restrict = false)
    @Test fun materialTimePickerRejectsOutOfRangeMinute() = picker(false, "time",
        "09:30:30", "09:30:00", "Choose time", "OK")
    @Test fun cupertinoTimePickerRejectsOutOfRangeMinute() = picker(true, "time",
        "09:30:30", "09:30:00", "Choose time", "OK")
    @Test fun materialDateTimePickerRejectsOutOfRangeInitialDate() = picker(false, "date-time",
        "2025-01-10T09:30:30Z", "2025-01-09T09:30:00Z", "Choose date and time", "Next")
    @Test fun cupertinoDateTimePickerRejectsOutOfRangeMinute() = picker(true, "date-time",
        "2025-01-10T09:30:30Z", "2025-01-10T09:30:00Z", "Choose date and time", "OK")
    @Test fun materialDateTimePickerRejectsBoundaryMinuteOnTimeStep() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"date-time",
            "formatMinimum":"2025-01-10T09:30:30Z"}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"2025-01-10T09:30:00Z"}"""),
                schema = schema, uischema = stagedControl("#/properties/value"),
                renderers = materialRenderers)
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithText("Next").performClick()
        onNodeWithText("OK").assertIsNotEnabled()
    }
    @Test fun bulgarianDateRangeFeedbackUsesBulgarianCatalog() = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"date",
            "formatMinimum":"2025-01-10"}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"2025-01-09"}"""), schema = schema,
                uischema = stagedControl("#/properties/value", options = mapOf(
                    "dateFormat" to JsonPrimitive("DD.MM.YYYY"))), renderers = materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg-BG"))
        }
        onNodeWithText("Стойността трябва да е на или след 10.01.2025.").assertExists()
        onNodeWithContentDescription("Избери дата").performClick()
        onNodeWithText("Изберете дата в позволения диапазон.").assertExists()
        onNodeWithText("Отказ").performClick()
        onNodeWithText("Изберете дата в позволения диапазон.").assertDoesNotExist()
    }

    private fun temporal(cupertino: Boolean, format: String, formatOption: String,
        displayFormat: String, bound: String, invalid: String, valid: String) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format",
            "formatMinimum":"$bound"}}
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement("""{"value":"$bound"}"""))
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = stagedControl("#/properties/value", options = mapOf(
                    formatOption to JsonPrimitive(displayFormat),
                    "mask" to JsonPrimitive(false),
                )),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data }, onValidityChange = { validity = it.status })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(invalid)
        val displayedBound = if (format == "time") bound.removeSuffix("Z") else bound
        onNodeWithText("Value must be on or after $displayedBound.").assertExists()
        onAllNodes(hasSetTextAction()).onFirst().assert(SemanticsMatcher.expectValue(
            SemanticsProperties.Error, "Value must be on or after $displayedBound."))
        runOnIdle {
            val stored = if (format == "time") "${invalid}Z" else invalid
            assertEquals(Json.parseToJsonElement("""{"value":"$stored"}"""), data)
            assertEquals(CombinedValidityStatus.Invalid, validity)
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(valid)
        onAllNodes(hasSetTextAction()).onFirst().assert(!SemanticsMatcher.keyIsDefined(
            SemanticsProperties.Error))
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity) }
    }

    private fun picker(cupertino: Boolean, format: String, bound: String, initial: String,
        action: String, confirm: String, restrict: Boolean = true) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format",
            "formatMinimum":"$bound"}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"$initial"}"""), schema = schema,
                uischema = stagedControl("#/properties/value", options = mapOf(
                    "restrict" to JsonPrimitive(restrict))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription(action).performClick()
        if (format == "date-time" && cupertino) onNodeWithText("Next").performClick()
        val feedback = when (format) {
            "date" -> "Select a date within the allowed range."
            "time" -> "Select a time within the allowed range."
            else -> "Select a date and time within the allowed range."
        }
        if (restrict) {
            onNodeWithText(confirm).assertIsNotEnabled()
            onNodeWithText(confirm).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, feedback))
            onNodeWithText(feedback).assertExists()
        } else {
            onNodeWithText(confirm).assertIsEnabled()
            onNodeWithText(confirm).assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            onNodeWithText(feedback).assertDoesNotExist()
        }
        onNodeWithText("Cancel").performClick()
        onNodeWithText(feedback).assertDoesNotExist()
    }

    private fun displayedBound(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"date",
            "formatMinimum":"2025-01-10"}}
        }""").jsonObject)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"2025-01-09"}"""), schema = schema,
                uischema = stagedControl("#/properties/value", options = mapOf(
                    "dateFormat" to JsonPrimitive("DD.MM.YYYY"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Value must be on or after 10.01.2025.").assertExists()
    }

    private fun pickerLocale(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"date",
            "formatMinimum":"2025-01-10"}}
        }""").jsonObject)
        var locale by mutableStateOf("en-US")
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"2025-01-09"}"""), schema = schema,
                uischema = stagedControl("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale))
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithText("Select a date within the allowed range.").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Изберете дата в позволения диапазон.").assertExists()
        onNodeWithText("Select a date within the allowed range.").assertDoesNotExist()
        onNodeWithText("Отказ").performClick()
        onNodeWithText("Изберете дата в позволения диапазон.").assertDoesNotExist()
    }

    private fun pickerBoundUpdate(cupertino: Boolean, format: String = "date") = runComposeUiTest {
        val initial = when (format) {
            "date" -> "2025-01-09"
            "time" -> "09:30:00"
            else -> "2025-01-10T09:30:00Z"
        }
        val minimum = when (format) {
            "date" -> "2025-01-10"
            "time" -> "09:30:30"
            else -> "2025-01-10T09:30:30Z"
        }
        val relaxed = when (format) {
            "date" -> "2025-01-08"
            "time" -> "09:29:00"
            else -> "2025-01-10T09:29:00Z"
        }
        val action = when (format) {
            "date" -> "Choose date"
            "time" -> "Choose time"
            else -> "Choose date and time"
        }
        val feedback = when (format) {
            "date" -> "Select a date within the allowed range."
            "time" -> "Select a time within the allowed range."
            else -> "Select a date and time within the allowed range."
        }
        fun schema(minimum: String) = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"$format",
            "formatMinimum":"$minimum"}}
        }""").jsonObject)
        var currentSchema by mutableStateOf(schema(minimum))
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"$initial"}"""), schema = currentSchema,
                uischema = stagedControl("#/properties/value"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription(action).performClick()
        if (format == "date-time") onNodeWithText("Next").performClick()
        onNodeWithText("OK").assertIsNotEnabled()
        onNodeWithText("OK").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, feedback))
        onNodeWithText(feedback).assertExists()
        runOnIdle { currentSchema = schema(relaxed) }
        onNodeWithText("OK").assertIsEnabled()
        onNodeWithText("OK").assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        onNodeWithText(feedback).assertDoesNotExist()
        onNodeWithText("Cancel").performClick()
    }

    private fun dynamicSiblingBound(cupertino: Boolean, format: String = "date") = runComposeUiTest {
        val start = when (format) {
            "date" -> "2025-01-08"
            "time" -> "09:30:00Z"
            else -> "2025-01-10T09:30:00Z"
        }
        val initial = when (format) {
            "date" -> "2025-01-09"
            "time" -> "09:40:00Z"
            else -> "2025-01-10T09:40:00Z"
        }
        val entered = when (format) {
            "date" -> "2025-01-07"
            "time" -> "09:20:00Z"
            else -> "2025-01-10T09:20:00Z"
        }
        val relaxed = when (format) {
            "date" -> "2025-01-06"
            "time" -> "09:10:00Z"
            else -> "2025-01-10T09:10:00Z"
        }
        val formatOption = when (format) {
            "date" -> "dateFormat"
            "time" -> "timeFormat"
            else -> "dateTimeFormat"
        }
        val displayFormat = when (format) {
            "date" -> "YYYY-MM-DD"
            "time" -> "HH:mm:ssZ"
            else -> "YYYY-MM-DDTHH:mm:ssZ"
        }
        val action = when (format) {
            "date" -> "Choose date"
            "time" -> "Choose time"
            else -> "Choose date and time"
        }
        val feedback = when (format) {
            "date" -> "Select a date within the allowed range."
            "time" -> "Select a time within the allowed range."
            else -> "Select a date and time within the allowed range."
        }
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "start":{"type":"string","format":"$format"},
            "end":{"type":"string","format":"$format",
              "formatMinimum":{"${'$'}data":"1/start"}}
          }
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"start":"$start","end":"$initial"}"""))
        var validity: CombinedValidity? by mutableStateOf(null)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = stagedControl("#/properties/end", options = mapOf(
                    formatOption to JsonPrimitive(displayFormat),
                    "timeSaveFormat" to JsonPrimitive("HH:mm:ssZ"),
                    "mask" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data }, onValidityChange = { validity = it })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(entered)
        onNodeWithText("Value must be on or after $start.").assertExists()
        runOnIdle {
            assertEquals(Json.parseToJsonElement(
                """{"start":"$start","end":"$entered"}"""), data)
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
        }
        onNodeWithContentDescription(action).performClick()
        if (format == "date-time") onNodeWithText("Next").performClick()
        onNodeWithText("OK").assertIsNotEnabled()
        runOnIdle { data = Json.parseToJsonElement(
            """{"start":"$relaxed","end":"$entered"}""") }
        onNodeWithText("OK").assertIsEnabled()
        onNodeWithText(feedback).assertDoesNotExist()
        waitForIdle()
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity?.status,
            "validity=$validity data=$data expectedStart=$relaxed") }
        onNodeWithText("Cancel").performClick()
    }

    private fun invalidDataBound(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "start":{},
            "end":{"type":"string","format":"date",
              "formatMinimum":{"${'$'}data":"1/start"}}
          }
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"start":42,"end":"2025-01-07"}"""))
        var validity: CombinedValidity? by mutableStateOf(null)
        val locale = if (cupertino) "bg-BG" else "en-US"
        val errorText = if (cupertino) "Свързаната граница трябва да е низ."
            else "Referenced temporal bound must be a string."
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = stagedControl("#/properties/end", options = mapOf(
                    "dateFormat" to JsonPrimitive("YYYY-MM-DD"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale),
                onValidityChange = { validity = it })
        }
        onNodeWithText(errorText).assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(0, validity?.additionalErrorCount)
        }
        onNodeWithContentDescription(if (cupertino) "Избери дата" else "Choose date").performClick()
        onNodeWithText(if (cupertino) "Готово" else "OK").assertIsNotEnabled()
        runOnIdle { data = Json.parseToJsonElement(
            """{"start":"2025-01-06","end":"2025-01-07"}""") }
        onNodeWithText(if (cupertino) "Готово" else "OK").assertIsEnabled()
        waitForIdle()
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity?.status) }
        onNodeWithText(if (cupertino) "Отказ" else "Cancel").performClick()
        onNodeWithText(errorText).assertDoesNotExist()
    }

    private fun malformedDataReference(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"end":{"type":"string","format":"date",
            "formatMinimum":{"${'$'}data":"/sta~2rt"}}}
        }""").jsonObject)
        var validity: CombinedValidity? by mutableStateOf(null)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"end":"2025-01-10"}"""),
                schema = schema, uischema = stagedControl("#/properties/end"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = if (cupertino) "bg-BG" else "en-US"),
                onValidityChange = { validity = it })
        }
        val feedback = if (cupertino) "Невалидна препратка към границата за дата или час."
            else "Invalid temporal bound data reference."
        onNodeWithText(feedback).assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
        }
        onNodeWithContentDescription(if (cupertino) "Избери дата" else "Choose date").performClick()
        onNodeWithText(if (cupertino) "Готово" else "OK").assertIsNotEnabled()
    }

    private fun hiddenBound(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "visible":{"type":"string"},
            "start":{"type":"string","format":"date"},
            "end":{"type":"string","format":"date",
              "formatMinimum":{"${'$'}data":"1/start"}}
          }
        }""").jsonObject)
        var data by mutableStateOf<JsonElement>(Json.parseToJsonElement(
            """{"visible":"Ready","start":"2025-01-10","end":"2025-01-09"}"""))
        var validity: CombinedValidity? by mutableStateOf(null)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = stagedControl("#/properties/visible"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = { validity = it })
        }
        onNodeWithText("Ready").assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            assertEquals(0, validity?.additionalErrorCount)
        }
        runOnIdle { data = Json.parseToJsonElement(
            """{"visible":"Ready","start":"2025-01-08","end":"2025-01-09"}""") }
        waitForIdle()
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity?.status) }
    }

    private fun hiddenBoundSchemaPrerequisite(cupertino: Boolean) = runComposeUiTest {
        fun schema(format: String?, boundLiteral: String = "\"2025-01-10\"") =
            JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{
            "visible":{"type":"string"},
            "hidden":{"type":"string"${format?.let { ",\"format\":\"$it\"" }.orEmpty()},
              "formatMinimum":$boundLiteral}
          }
        }""").jsonObject)
        var activeSchema by mutableStateOf(schema(null))
        var validity: CombinedValidity? by mutableStateOf(null)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"visible":"Ready","hidden":"2025-01-10"}"""),
                schema = activeSchema, uischema = stagedControl("#/properties/visible"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = { validity = it })
        }
        onNodeWithText("Ready").assertExists()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            activeSchema = schema("email")
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            activeSchema = schema("date", "42")
        }
        waitForIdle()
        runOnIdle {
            assertEquals(CombinedValidityStatus.Invalid, validity?.status)
            assertEquals(1, validity?.schemaErrorCount)
            activeSchema = schema("date")
        }
        waitForIdle()
        runOnIdle { assertEquals(CombinedValidityStatus.Valid, validity?.status) }
    }

    private fun offsetTimeBound(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string","format":"time",
            "formatExclusiveMinimum":"10:30:00+01:00"}}
        }""").jsonObject)
        var validity: CombinedValidityStatus? by mutableStateOf(null)
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"10:30:00+01:00"}"""),
                schema = schema, uischema = stagedControl("#/properties/value", options = mapOf(
                    "timeFormat" to JsonPrimitive("HH:mm:ss"))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onValidityChange = { validity = it.status })
        }
        onNodeWithText("Value must be after 09:30:00.").assertExists()
        runOnIdle { assertEquals(CombinedValidityStatus.Invalid, validity) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithText("OK").assertIsNotEnabled()
        onNodeWithText("Select a time within the allowed range.").assertExists()
        onNodeWithText("Cancel").performClick()
    }
    private fun stagedControl(scope: String, options: JsonOptions = emptyMap()): ControlElement =
        ControlElement(scope, options = options + ("showActions" to JsonPrimitive(true)))
}
