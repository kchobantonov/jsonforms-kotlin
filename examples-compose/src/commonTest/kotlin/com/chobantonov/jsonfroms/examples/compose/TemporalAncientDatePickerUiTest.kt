package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.isSelected
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class TemporalAncientDatePickerUiTest {
    @Test fun materialYearOneDatePickerSelectsItsBoundaryDay() = materialBoundaryYear(
        "01/02/0001", "0001-01-02", "January 1", "Tuesday, January 2, 1")
    @Test fun materialYear9999DatePickerSelectsItsBoundaryDay() = materialBoundaryYear(
        "01/02/9999", "9999-01-02", "January 9999", "Saturday, January 2, 9999")

    private fun materialBoundaryYear(input: String, stored: String, month: String, selectedDay: String) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(input)
        assertEquals(stored, (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText(month)).onFirst().assertExists()
        onAllNodes(hasText(selectedDay) and isSelected()).onFirst().assertExists()
    }

    @Test fun cupertinoYearOnePickerExcludesEarlierYears() = cupertinoBoundaryYear("01/02/0001", "0001-01-02", "0")
    @Test fun cupertinoYear9999PickerExcludesLaterYears() = cupertinoBoundaryYear("01/02/9999", "9999-01-02", "10000")

    private fun cupertinoBoundaryYear(input: String, stored: String, excludedYear: String) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = cupertinoRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(input)
        assertEquals(stored, (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText(excludedYear), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun cupertinoTypedAncientDateOpensAtItsSelection() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = cupertinoRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220")
        assertEquals("1220-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        val y: (String) -> Float = {
            onAllNodes(hasText(it), useUnmergedTree = true).onFirst()
                .fetchSemanticsNode().boundsInRoot.center.y
        }
        assertTrue(abs(y("March") - (y("December") + y("June")) / 2f) < 1f)
        assertTrue(abs(y("12") - (y("9") + y("15")) / 2f) < 1f)
        assertTrue(abs(y("1220") - (y("1217") + y("1223")) / 2f) < 1f)
    }

    @Test fun cupertinoTypedAncientDateTimeOpensAtItsSelection() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date-time"),
                uischema = ControlElement("#/properties/value"),
                renderers = cupertinoRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220 10:05 AM")
        assertTrue((data.jsonObject["value"] as JsonPrimitive).content.startsWith("1220-03-12T"))
        onNodeWithContentDescription("Choose date and time").performClick()
        val y: (String) -> Float = {
            onAllNodes(hasText(it), useUnmergedTree = true).onFirst()
                .fetchSemanticsNode().boundsInRoot.center.y
        }
        assertTrue(abs(y("March") - (y("December") + y("June")) / 2f) < 1f)
        assertTrue(abs(y("12") - (y("9") + y("15")) / 2f) < 1f)
        assertTrue(abs(y("1220") - (y("1217") + y("1223")) / 2f) < 1f)
    }

    @Test fun ancientDatePickerUsesBulgarianCalendarLabels() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220")
        assertEquals("1220-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Избери дата").performClick()
        onAllNodes(hasText("март 1220", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("четвъртък, 12 март 1220 г.") and isSelected()).onFirst().assertExists()
    }

    @Test fun typedModernDateKeepsItsSelectedDayInLocalBrowserTime() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/2026")
        assertEquals("2026-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText("March 2026", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Thursday, March 12, 2026") and isSelected()).onFirst().assertExists()
    }

    @Test fun modernDatePickerUsesBulgarianCalendarLabels() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                i18n = JsonFormsI18nState(locale = "bg-BG"),
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/2026")
        assertEquals("2026-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Избери дата").performClick()
        onAllNodes(hasText("Избери дата")).onFirst().assertExists()
        onNodeWithContentDescription("Избрана дата: четвъртък, 12 март 2026 г.").assertExists()
        onAllNodes(hasText("март 2026", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("четвъртък, 12 март 2026 г.") and isSelected()).onFirst().assertExists()
        val modeToggle = onNodeWithContentDescription("Превключи към въвеждане на дата")
        modeToggle.performClick()
        onAllNodes(hasText("Въведи дата")).onFirst().assertExists()
        onNodeWithContentDescription("Въведена дата: четвъртък, 12 март 2026 г.").assertExists()
        onNodeWithContentDescription("Превключи към календар").assertExists()
    }

    @Test fun typedAncientDateOpensAtItsSelectedMonth() = runComposeUiTest {
        val schema = schema("date")
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220")
        assertEquals("1220-03-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText("March 1220", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Thursday, March 12, 1220") and isSelected()).onFirst().assertExists()
    }

    @Test fun typedThirteenthCenturyDateOpensAtItsSelectedMonth() = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema("date"),
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("11/12/1231")
        assertEquals("1231-11-12", (data.jsonObject["value"] as JsonPrimitive).content)
        onNodeWithContentDescription("Choose date").performClick()
        onAllNodes(hasText("November 1231", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Wednesday, November 12, 1231") and isSelected()).onFirst().assertExists()
    }

    @Test fun typedAncientDateTimeOpensAtItsSelectedMonth() = runComposeUiTest {
        val schema = schema("date-time")
        var data by mutableStateOf(Json.parseToJsonElement("{}"))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/value"),
                renderers = materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("03/12/1220 10:05 AM")
        assertTrue((data.jsonObject["value"] as JsonPrimitive).content.startsWith("1220-03-12T"))
        onNodeWithContentDescription("Choose date and time").performClick()
        onAllNodes(hasText("March 1220", substring = true)).onFirst().assertExists()
        onAllNodes(hasText("Thursday, March 12, 1220") and isSelected()).onFirst().assertExists()
    }

    private fun schema(format: String) = JsonSchema(Json.parseToJsonElement("""{
        "type":"object","properties":{"value":{"type":"string","format":"$format"}}
    }""").jsonObject)
}
