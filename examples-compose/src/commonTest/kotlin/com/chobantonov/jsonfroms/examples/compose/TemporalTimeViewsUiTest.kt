package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TemporalTimeViewsUiTest {
    @Test fun materialSecondsOnlyPreservesHiddenHourAndMinute() = secondsOnly(false)
    @Test fun cupertinoSecondsOnlyPreservesHiddenHourAndMinute() = secondsOnly(true)
    @Test fun materialHourMinuteViewsPreserveHiddenSeconds() = hourMinute(false)
    @Test fun cupertinoHourMinuteViewsPreserveHiddenSeconds() = hourMinute(true)
    @Test fun materialDateOnlyViewsUseTimeDisplayFormat() = dateOnlyFallback(false)
    @Test fun cupertinoDateOnlyViewsUseTimeDisplayFormat() = dateOnlyFallback(true)
    @Test fun materialSecondsColumnHonorsBounds() = boundedSeconds(false)
    @Test fun cupertinoSecondsColumnHonorsBounds() = boundedSeconds(true)
    @Test fun materialTwelveHourColumnKeepsOtherComponents() = twelveHour(false)
    @Test fun cupertinoTwelveHourColumnKeepsOtherComponents() = twelveHour(true)
    @Test fun materialDateTimeSecondsColumnKeepsDateAndHiddenTime() = dateTimeSeconds(false)
    @Test fun cupertinoDateTimeSecondsColumnKeepsDateAndHiddenTime() = dateTimeSeconds(true)
    @Test fun materialDateTimeDateOnlyViewsFallBackToDisplayTime() = dateTimeDateOnlyFallback(false)
    @Test fun cupertinoDateTimeDateOnlyViewsFallBackToDisplayTime() = dateTimeDateOnlyFallback(true)
    @Test fun materialSecondsCommitImmediatelyWithoutActions() = immediateSeconds(false)
    @Test fun cupertinoSecondsCommitImmediatelyWithoutActions() = immediateSeconds(true)
    @Test fun materialImmediateSecondsRespectBounds() = immediateSecondsBound(false)
    @Test fun cupertinoImmediateSecondsRespectBounds() = immediateSecondsBound(true)
    @Test fun materialTimeColumnsDefaultToImmediateCommit() = immediateSeconds(false, null)
    @Test fun cupertinoTimeColumnsDefaultToImmediateCommit() = immediateSeconds(true, null)
    @Test fun materialAbsentAmpmUsesTwentyFourHourColumn() = defaultTwentyFourHour(false)
    @Test fun cupertinoAbsentAmpmUsesTwentyFourHourColumn() = defaultTwentyFourHour(true)

    private val schema = JsonSchema(Json.parseToJsonElement(
        """{"type":"object","properties":{"value":{"type":"string","format":"time"}}}"""
    ).jsonObject)

    @Composable
    private fun Form(cupertino: Boolean, views: List<String>,
        onChange: (JsonElement) -> Unit,
        data: JsonElement,
        locale: String = "en",
        ampm: Boolean? = false,
        formSchema: JsonSchema = schema,
        showActions: Boolean? = true,
    ) {
        JsonForms(data = data, schema = formSchema,
            uischema = ControlElement("#/properties/value", options = mapOf(
                "views" to JsonArray(views.map(::JsonPrimitive)),
                "timeFormat" to JsonPrimitive("HH:mm:ss")) + (ampm?.let {
                    mapOf("ampm" to JsonPrimitive(it))
                } ?: emptyMap()) + (showActions?.let {
                    mapOf("showActions" to JsonPrimitive(it))
                } ?: emptyMap())),
            i18n = JsonFormsI18nState(locale = locale),
            renderers = if (cupertino) cupertinoRenderers else materialRenderers,
            onChange = { onChange(it.data) })
    }

    private fun secondsOnly(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("seconds"), { data = it }, data) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Hours").assertDoesNotExist()
        onNodeWithContentDescription("Minutes").assertDoesNotExist()
        onNodeWithContentDescription("Seconds").performTextReplacement("42")
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:42Z"}"""), data)
    }

    private fun hourMinute(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("hours", "minutes"), { data = it }, data) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Seconds").assertDoesNotExist()
        onNodeWithContentDescription("Minutes").performTextReplacement("31")
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:31:37Z"}"""), data)
    }

    private fun dateOnlyFallback(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        var locale by mutableStateOf("en")
        setContent { Form(cupertino, listOf("year", "month", "day"), { data = it }, data, locale) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Hours").assertExists()
        onNodeWithContentDescription("Minutes").assertExists()
        onNodeWithContentDescription("Seconds").assertExists()
        runOnIdle { locale = "bg" }
        onNodeWithContentDescription("Секунди").assertExists()
        onNodeWithText("Готово").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""), data)
    }

    private fun boundedSeconds(cupertino: Boolean) = runComposeUiTest {
        val bounded = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"time","formatMaximum":"09:30:40Z"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("seconds"), { data = it }, data, formSchema = bounded) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Seconds").performTextReplacement("45")
        onNodeWithText("OK").assertIsNotEnabled()
        onNodeWithContentDescription("Seconds").performTextReplacement("39")
        onNodeWithText("OK").assertIsEnabled().performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:39Z"}"""), data)
    }

    private fun twelveHour(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("hours"), { data = it }, data, ampm = true) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Minutes").assertDoesNotExist()
        onNodeWithText("AM").performClick()
        onNodeWithText("PM").assertExists()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"21:30:37Z"}"""), data)
    }

    private fun dateTimeSeconds(cupertino: Boolean) = runComposeUiTest {
        val dateTimeSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date-time"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}"""))
        setContent {
            JsonForms(data = data, schema = dateTimeSchema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "views" to JsonArray(listOf("year", "month", "day", "seconds").map(::JsonPrimitive)),
                    "showActions" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithText("Next").performClick()
        onNodeWithContentDescription("Hours").assertDoesNotExist()
        onNodeWithContentDescription("Minutes").assertDoesNotExist()
        onNodeWithContentDescription("Seconds").performTextReplacement("42")
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:42Z"}"""), data)
    }

    private fun dateTimeDateOnlyFallback(cupertino: Boolean) = runComposeUiTest {
        val dateTimeSchema = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"date-time"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"2025-01-10T09:30:37Z"}"""))
        setContent {
            JsonForms(data = data, schema = dateTimeSchema,
                uischema = ControlElement("#/properties/value", options = mapOf(
                    "views" to JsonArray(listOf("year", "month", "day").map(::JsonPrimitive)),
                    "showActions" to JsonPrimitive(true))),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithText("Next").performClick()
        onNodeWithContentDescription("Hours").assertExists()
        onNodeWithContentDescription("Minutes").performTextReplacement("31")
        onNodeWithContentDescription("Seconds").assertDoesNotExist()
        onNodeWithText("OK").performClick()
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"2025-01-10T09:31:37Z"}"""), data)
    }

    private fun immediateSeconds(cupertino: Boolean, showActions: Boolean? = false) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("seconds"), { data = it }, data,
            showActions = showActions) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithText("OK").assertDoesNotExist()
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithContentDescription("Seconds").performTextReplacement("42")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:42Z"}"""), data)
        onNodeWithContentDescription("Seconds").performTextReplacement("44")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:44Z"}"""), data)
    }

    private fun immediateSecondsBound(cupertino: Boolean) = runComposeUiTest {
        val bounded = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"value":{"type":"string","format":"time","formatMaximum":"09:30:40Z"}}}"""
        ).jsonObject)
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("seconds"), { data = it }, data,
            formSchema = bounded, showActions = false) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Seconds").performTextReplacement("45")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""), data)
        onNodeWithText("Select a time within the allowed range.").assertExists()
        onNodeWithContentDescription("Seconds").performTextReplacement("39")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"09:30:39Z"}"""), data)
    }

    private fun defaultTwentyFourHour(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"value":"09:30:37Z"}"""))
        setContent { Form(cupertino, listOf("hours"), { data = it }, data,
            ampm = null, showActions = null) }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithText("AM").assertDoesNotExist()
        onNodeWithText("PM").assertDoesNotExist()
        onNodeWithContentDescription("Hours").performTextReplacement("21")
        waitForIdle()
        assertEquals(Json.parseToJsonElement("""{"value":"21:30:37Z"}"""), data)
    }
}
