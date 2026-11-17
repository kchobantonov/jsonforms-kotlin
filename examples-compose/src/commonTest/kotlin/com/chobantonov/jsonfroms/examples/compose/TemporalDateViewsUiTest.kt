package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalDateViewsUiTest {
    @Test fun materialYearOnlyDatePreservesMonthAndDay() = yearOnlyDate(false)
    @Test fun cupertinoYearOnlyDatePreservesMonthAndDay() = yearOnlyDate(true)
    @Test fun materialMonthDateClampsDay() = monthDate(false)
    @Test fun cupertinoMonthDateClampsDay() = monthDate(true)
    @Test fun materialTimeOnlyViewsUseDateDisplayGranularity() = timeOnlyDateFallback(false)
    @Test fun cupertinoTimeOnlyViewsUseDateDisplayGranularity() = timeOnlyDateFallback(true)
    @Test fun materialDateTimeYearViewKeepsFinerDateAndTime() = dateTimeYear(false)
    @Test fun cupertinoDateTimeYearViewKeepsFinerDateAndTime() = dateTimeYear(true)
    @Test fun materialDateTimeTimeOnlyViewsUseDateDisplayGranularity() = dateTimeDateFallback(false)
    @Test fun cupertinoDateTimeTimeOnlyViewsUseDateDisplayGranularity() = dateTimeDateFallback(true)
    @Test fun materialYearViewHonorsDateBounds() = boundedYear(false)
    @Test fun cupertinoYearViewHonorsDateBounds() = boundedYear(true)
    @Test fun materialYearPanelSelectsFromNextPage() = yearPanel(false)
    @Test fun cupertinoYearPanelSelectsFromNextPage() = yearPanel(true)
    @Test fun materialMonthPanelSelectsMonth() = monthPanel(false)
    @Test fun cupertinoMonthPanelSelectsMonth() = monthPanel(true)
    @Test fun materialCoarseDateCommitsImmediatelyWithoutActions() = immediateMonth(false)
    @Test fun cupertinoCoarseDateCommitsImmediatelyWithoutActions() = immediateMonth(true)
    @Test fun materialImmediateDateRejectsOutOfRangeChoice() = immediateBound(false)
    @Test fun cupertinoImmediateDateRejectsOutOfRangeChoice() = immediateBound(true)
    @Test fun materialCoarseDateDefaultsToImmediateCommit() = immediateMonth(false, null)
    @Test fun cupertinoCoarseDateDefaultsToImmediateCommit() = immediateMonth(true, null)

    private fun yearOnlyDate(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-10-14"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Year").performTextReplacement("2027")
        onNodeWithContentDescription("Month").assertDoesNotExist()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2027-10-14"}"""), data)
    }

    private fun monthDate(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-31"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year", "month")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Year").assertExists()
        onNodeWithContentDescription("Month").performTextReplacement("02")
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-28"}"""), data)
    }

    private fun timeOnlyDateFallback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-10-14"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("hours"), "dateFormat" to JsonPrimitive("YYYY-MM")),
                i18n = JsonFormsI18nState(locale = "bg"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Избери дата").performClick()
        onNodeWithContentDescription("Година").assertExists()
        onNodeWithContentDescription("Месец").assertExists()
        onNodeWithContentDescription("Ден").assertDoesNotExist()
        onNodeWithText("Отказ").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-10-14"}"""), data)
    }

    private fun dateTimeYear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-10-14T09:30:37Z"}"""))
        val schema = schema("date-time")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year", "hours")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithContentDescription("Year").performTextReplacement("2027")
        onNodeWithContentDescription("Month").assertDoesNotExist()
        onNodeWithText("Next").performClick()
        onNodeWithContentDescription("Hours").assertExists()
        onNodeWithContentDescription("Minutes").assertDoesNotExist()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2027-10-14T09:30:37Z"}"""), data)
    }

    private fun dateTimeDateFallback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-14T09:30:37Z"}"""))
        val schema = schema("date-time")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("hours"),
                    "dateTimeFormat" to JsonPrimitive("YYYY-MM HH:mm")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithContentDescription("Month").assertExists()
        onNodeWithContentDescription("Year").assertExists()
        onNodeWithContentDescription("Day").assertDoesNotExist()
        onNodeWithContentDescription("Month").performTextReplacement("02")
        onNodeWithText("Next").performClick()
        onNodeWithContentDescription("Hours").assertExists()
        onNodeWithContentDescription("Minutes").assertDoesNotExist()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-14T09:30:37Z"}"""), data)
    }

    private fun boundedYear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-10-14"}"""))
        val schema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date","formatMaximum":"2026-12-31"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Year").performTextReplacement("2027")
        onNodeWithText("OK").assertIsNotEnabled()
        onNodeWithContentDescription("Year").performTextReplacement("2026")
        onNodeWithText("OK").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2026-10-14"}"""), data)
    }

    private fun yearPanel(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-10-14"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Year 2025").assertIsSelected()
        onNodeWithContentDescription("Next 12 years").performClick()
        onNodeWithContentDescription("Year 2029").performClick()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2029-10-14"}"""), data)
    }

    private fun monthPanel(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-31"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = control(listOf("year", "month")),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Month 01").assertIsSelected()
        onNodeWithContentDescription("Month 02").performClick()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-28"}"""), data)
    }

    private fun immediateMonth(cupertino: Boolean, showActions: Boolean? = false) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-14"}"""))
        val schema = schema("date")
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = if (showActions == null) ControlElement("#/properties/value", options = mapOf(
                    "views" to JsonArray(listOf("year", "month").map(::JsonPrimitive))))
                    else control(listOf("year", "month"), "showActions" to JsonPrimitive(showActions)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithContentDescription("Month 02").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-02-14"}"""), data)
        onNodeWithContentDescription("Month 03").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-03-14"}"""), data)
    }

    private fun immediateBound(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-03-14"}"""))
        val bounded = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date","formatMinimum":"2025-03-01"}}}"""
        ).jsonObject)
        setContent {
            JsonForms(data = data, schema = bounded,
                uischema = control(listOf("year", "month"), "showActions" to JsonPrimitive(false)),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date").performClick()
        onNodeWithContentDescription("Month 02").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-03-14"}"""), data)
        onNodeWithText("Select a date within the allowed range.").assertExists()
        onNodeWithContentDescription("Month 04").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-04-14"}"""), data)
    }

    private fun schema(format: String) = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":"string","format":"$format"}}}"""
    ).jsonObject)

    private fun control(views: List<String>, vararg other: Pair<String, JsonPrimitive>): ControlElement =
        ControlElement("#/properties/value", options = mapOf(
            "showActions" to JsonPrimitive(true),
            "views" to JsonArray(views.map(::JsonPrimitive)), *other))
}
