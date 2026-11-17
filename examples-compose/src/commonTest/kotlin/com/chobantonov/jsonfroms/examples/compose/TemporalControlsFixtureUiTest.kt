package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class TemporalControlsFixtureUiTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-temporal-controls" }

    private fun control(category: String, scope: String, occurrence: Int = 0): ControlElement {
        val categories = (fixture().uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        return categories.single { it.label == category }.elements.filterIsInstance<ControlElement>()
            .filter { it.scope == scope }[occurrence]
    }

    @Test fun materialSchemaSelectedDateMountsWithoutChangingData() = schemaDate(false)
    @Test fun cupertinoSchemaSelectedDateMountsWithoutChangingData() = schemaDate(true)
    @Test fun materialSchemaSelectedTimeAndDateTimeMountWithoutChangingData() = schemaOther(false)
    @Test fun cupertinoSchemaSelectedTimeAndDateTimeMountWithoutChangingData() = schemaOther(true)
    @Test fun materialAuthoredMonthKeepsSaveFormat() = authoredMonth(false)
    @Test fun cupertinoAuthoredMonthKeepsSaveFormat() = authoredMonth(true)
    @Test fun materialAuthoredTimeKeepsSaveFormat() = authoredSave(false, "time", "9:10 AM", "09:10")
    @Test fun cupertinoAuthoredTimeKeepsSaveFormat() = authoredSave(true, "time", "9:10 AM", "09:10")
    @Test fun materialAuthoredDateTimeKeepsSaveFormat() = authoredSave(false, "datetime", "03-11-26 04:15 pm", "2026/11/03 4:15 pm")
    @Test fun cupertinoAuthoredDateTimeKeepsSaveFormat() = authoredSave(true, "datetime", "03-11-26 04:15 pm", "2026/11/03 4:15 pm")
    @Test fun materialSchemaSecondsPickerStoresSeconds() = schemaSeconds(false)
    @Test fun cupertinoSchemaSecondsPickerStoresSeconds() = schemaSeconds(true)
    @Test fun materialAuthoredCoarseTimePickerKeepsStoredSeconds() = coarseTime(false)
    @Test fun cupertinoAuthoredCoarseTimePickerKeepsStoredSeconds() = coarseTime(true)
    @Test fun materialAuthoredTimeSecondsKeepsSaveFormat() = authoredSave(false, "timeSeconds", "08:01:31", "08:01:31")
    @Test fun cupertinoAuthoredTimeSecondsKeepsSaveFormat() = authoredSave(true, "timeSeconds", "08:01:31", "08:01:31")
    @Test fun materialAuthoredDateTimeSecondsPickerStoresSeconds() = dateTimeSeconds(false)
    @Test fun cupertinoAuthoredDateTimeSecondsPickerStoresSeconds() = dateTimeSeconds(true)
    @Test fun materialImportedInclusiveDateMaximumValidatesTypedInput() = boundValue(false,
        "bookingDate", "11/01/2026", "2026-11-01", "10/31/2026", "2026-10-31",
        "Value must be on or before 10/31/2026.")
    @Test fun cupertinoImportedInclusiveDateMaximumValidatesTypedInput() = boundValue(true,
        "bookingDate", "11/01/2026", "2026-11-01", "10/31/2026", "2026-10-31",
        "Value must be on or before 10/31/2026.")
    @Test fun materialImportedExclusiveDateMinimumValidatesTypedInput() = boundValue(false,
        "notBefore", "10/19/2026", "2026-10-19", "10/20/2026", "2026-10-20",
        "Value must be after 10/19/2026.")
    @Test fun cupertinoImportedExclusiveDateMinimumValidatesTypedInput() = boundValue(true,
        "notBefore", "10/19/2026", "2026-10-19", "10/20/2026", "2026-10-20",
        "Value must be after 10/19/2026.")
    @Test fun materialImportedTimeMinimumValidatesTypedInput() = boundValue(false,
        "officeHours", "8:00 AM", "08:00:00Z", "9:00 AM", "09:00:00Z",
        "Value must be on or after 9:00 AM.")
    @Test fun cupertinoImportedTimeMinimumValidatesTypedInput() = boundValue(true,
        "officeHours", "8:00 AM", "08:00:00Z", "9:00 AM", "09:00:00Z",
        "Value must be on or after 9:00 AM.")
    @Test fun materialImportedDateTimeMinimumValidatesTypedInput() = dateTimeBound(false)
    @Test fun cupertinoImportedDateTimeMinimumValidatesTypedInput() = dateTimeBound(true)
    @Test fun materialImportedReferencedPeriodsKeepSiblingMinimum() = referencedPeriods(false)
    @Test fun cupertinoImportedReferencedPeriodsKeepSiblingMinimum() = referencedPeriods(true)
    @Test fun materialImportedTupleDatesKeepTheirPositions() = tupleDates(false)
    @Test fun cupertinoImportedTupleDatesKeepTheirPositions() = tupleDates(true)
    @Test fun materialImportedMixedStringKeepsDatePicker() = mixedDate(false)
    @Test fun cupertinoImportedMixedStringKeepsDatePicker() = mixedDate(true)
    @Test fun materialImportedUnrestrictedPickerAllowsInvalidDate() = unrestrictedPicker(false)
    @Test fun cupertinoImportedUnrestrictedPickerAllowsInvalidDate() = unrestrictedPicker(true)
    @Test fun materialImportedRestrictedPickerRejectsExclusiveMinimum() = restrictedPicker(false)
    @Test fun cupertinoImportedRestrictedPickerRejectsExclusiveMinimum() = restrictedPicker(true)

    private fun schemaDate(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema,
                uischema = control("Selected by the schema", "#/properties/schemaBased/properties/date"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("10/14/2026")).onFirst().assertExists()
        onNodeWithContentDescription("Choose date").assertExists()
        assertEquals(0, changes)
    }

    private fun authoredMonth(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Selected by the UI schema", "#/properties/uiSchemaBased/properties/month"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("2027.01")).onFirst()
            .performTextReplacement("2027.02")
        waitForIdle()
        assertEquals(JsonPrimitive("2027-02"),
            data.jsonObject.getValue("uiSchemaBased").jsonObject["month"])
        assertEquals(1, changes)
    }

    private fun schemaOther(cupertino: Boolean) {
        for ((field, action) in listOf("time" to "Choose time", "datetime" to "Choose date and time")) {
            runComposeUiTest {
                val example = fixture()
                var changes = 0
                setContent {
                    JsonForms(data = example.data, schema = example.schema,
                        uischema = control("Selected by the schema", "#/properties/schemaBased/properties/$field"),
                        renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                        onChange = { changes++ })
                }
                onAllNodes(hasSetTextAction()).onFirst().assertExists()
                onNodeWithContentDescription(action).assertExists()
                assertEquals(0, changes)
            }
        }
    }

    private fun authoredSave(cupertino: Boolean, field: String, input: String, stored: String) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Selected by the UI schema", "#/properties/uiSchemaBased/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(input)
        waitForIdle()
        assertEquals(JsonPrimitive(stored), data.jsonObject.getValue("uiSchemaBased").jsonObject[field])
        assertEquals(1, changes)
    }

    private fun schemaSeconds(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Selected by the schema", "#/properties/schemaBased/properties/timeWithSeconds"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(0, changes)
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Seconds").performTextReplacement("46")
        waitForIdle()
        assertEquals(JsonPrimitive("09:30:46Z"), data.jsonObject.getValue("schemaBased").jsonObject["timeWithSeconds"])
        assertEquals(1, changes)
    }

    private fun coarseTime(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Selected by the UI schema", "#/properties/uiSchemaBased/properties/coarseTime"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose time").performClick()
        onNodeWithContentDescription("Seconds").assertDoesNotExist()
        onNodeWithContentDescription("Minutes").performTextReplacement("31")
        waitForIdle()
        assertEquals(JsonPrimitive("09:31:00"), data.jsonObject.getValue("uiSchemaBased").jsonObject["coarseTime"])
        assertEquals(1, changes)
    }

    private fun dateTimeSeconds(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Selected by the UI schema",
                    "#/properties/uiSchemaBased/properties/datetimeSeconds", occurrence = 1),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose date and time").performClick()
        onNodeWithText("Next").performClick()
        onNodeWithContentDescription("Seconds").performTextReplacement("31")
        waitForIdle()
        assertEquals(JsonPrimitive("2026/11/02 3:05:31 pm"),
            data.jsonObject.getValue("uiSchemaBased").jsonObject["datetimeSeconds"])
        assertEquals(1, changes)
    }

    private fun boundValue(cupertino: Boolean, field: String, invalidInput: String, invalidStored: String,
        validInput: String, validStored: String, error: String) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Restricting the range", "#/properties/bounded/properties/$field"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(invalidInput)
        waitForIdle()
        assertEquals(JsonPrimitive(invalidStored), data.jsonObject.getValue("bounded").jsonObject[field])
        onNodeWithText(error).assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(validInput)
        waitForIdle()
        assertEquals(JsonPrimitive(validStored), data.jsonObject.getValue("bounded").jsonObject[field])
        onNodeWithText(error).assertDoesNotExist()
        assertEquals(2, changes)
    }

    private fun dateTimeBound(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Restricting the range", "#/properties/bounded/properties/slot"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("10/04/2026 8:00 AM")
        waitForIdle()
        assertTrue(data.jsonObject.getValue("bounded").jsonObject.getValue("slot")
            .jsonPrimitive.content.startsWith("2026-10-04T"))
        onNodeWithText("Value must be on or after", substring = true).assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("10/06/2026 9:00 AM")
        waitForIdle()
        assertTrue(data.jsonObject.getValue("bounded").jsonObject.getValue("slot")
            .jsonPrimitive.content.startsWith("2026-10-06T"))
        onNodeWithText("Value must be on or after", substring = true).assertDoesNotExist()
        assertEquals(2, changes)
    }

    private fun referencedPeriods(cupertino: Boolean) {
        for ((field, from, initialTo, invalidTo, validTo) in listOf(
            listOf("bookingPeriod", "10/01/2026", "10/05/2026", "09/30/2026", "10/01/2026"),
            listOf("reportPeriod", "11/01/2026", "11/30/2026", "10/31/2026", "11/01/2026"),
        )) runComposeUiTest {
            val example = fixture()
            var data by mutableStateOf<JsonElement>(example.data)
            var changes = 0
            setContent {
                JsonForms(data = data, schema = example.schema,
                    uischema = control("In other structures", "#/properties/composed/properties/$field"),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
            onAllNodes(hasSetTextAction() and hasText(from)).onFirst().assertExists()
            onAllNodes(hasSetTextAction() and hasText(initialTo)).onFirst()
                .performTextReplacement(invalidTo)
            waitForIdle()
            assertEquals(JsonPrimitive(invalidTo.split('/').let { "${it[2]}-${it[0]}-${it[1]}" }),
                data.jsonObject.getValue("composed").jsonObject.getValue(field).jsonObject["to"])
            onNodeWithText("Value must be on or after $from.").assertExists()
            onAllNodes(hasSetTextAction() and hasText(invalidTo)).onFirst()
                .performTextReplacement(validTo)
            waitForIdle()
            onNodeWithText("Value must be on or after $from.").assertDoesNotExist()
            assertEquals(2, changes)
        }
    }

    private fun tupleDates(cupertino: Boolean) {
        for (field in listOf("tupleRange", "legacyTupleRange")) runComposeUiTest {
            val example = fixture()
            var data by mutableStateOf<JsonElement>(example.data)
            var changes = 0
            setContent {
                JsonForms(data = data, schema = example.schema,
                    uischema = control("In other structures", "#/properties/composed/properties/$field"),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { data = it.data; changes++ })
            }
            onAllNodes(hasSetTextAction() and hasText("10/01/2026")).onFirst().assertExists()
            onAllNodes(hasSetTextAction() and hasText("10/05/2026")).onFirst()
                .performTextReplacement("10/06/2026")
            waitForIdle()
            val items = data.jsonObject.getValue("composed").jsonObject.getValue(field).jsonArray
            assertEquals(JsonPrimitive("2026-10-01"), items[0])
            assertEquals(JsonPrimitive("2026-10-06"), items[1])
            assertEquals(1, changes)
        }
    }

    private fun mixedDate(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("In other structures", "#/properties/composed/properties/maybeDate"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose date").assertExists()
        onAllNodes(hasSetTextAction() and hasText("10/14/2026")).onFirst()
            .performTextReplacement("10/15/2026")
        waitForIdle()
        assertEquals(JsonPrimitive("2026-10-15"),
            data.jsonObject.getValue("composed").jsonObject["maybeDate"])
        assertEquals(1, changes)
        onNodeWithText("String").performClick()
        onNodeWithText("Null").performClick()
        waitForIdle()
        assertEquals(JsonNull, data.jsonObject.getValue("composed").jsonObject["maybeDate"])
        onNodeWithContentDescription("Choose date").assertDoesNotExist()
        assertEquals(2, changes)
    }

    private fun unrestrictedPicker(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Restricting the range", "#/properties/bounded/properties/unrestricted"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Value must be on or before 10/31/2026.").assertExists()
        onNodeWithContentDescription("Choose date").performClick()
        if (cupertino) onNodeWithText("25").performClick()
        else {
            onNodeWithContentDescription("Switch to text input mode").performClick()
            onNodeWithContentDescription("Date, MM/DD/YYYY").performTextReplacement("12252026")
        }
        waitForIdle()
        assertEquals(JsonPrimitive("2026-12-25"),
            data.jsonObject.getValue("bounded").jsonObject["unrestricted"])
        onNodeWithText("Value must be on or before 10/31/2026.").assertExists()
        assertEquals(1, changes)
    }

    private fun restrictedPicker(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Restricting the range", "#/properties/bounded/properties/notBefore"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose date").performClick()
        if (cupertino) onNodeWithText("19").performClick()
        else {
            onNodeWithContentDescription("Switch to text input mode").performClick()
            onNodeWithContentDescription("Date, MM/DD/YYYY").performTextReplacement("10192026")
        }
        waitForIdle()
        assertEquals(JsonPrimitive("2026-10-20"),
            data.jsonObject.getValue("bounded").jsonObject["notBefore"])
        assertEquals(0, changes)
        if (cupertino) onNodeWithText("21").performClick()
        else onNodeWithContentDescription("Date, MM/DD/YYYY").performTextReplacement("10212026")
        waitForIdle()
        assertEquals(JsonPrimitive("2026-10-21"),
            data.jsonObject.getValue("bounded").jsonObject["notBefore"])
        assertEquals(1, changes)
    }
}
