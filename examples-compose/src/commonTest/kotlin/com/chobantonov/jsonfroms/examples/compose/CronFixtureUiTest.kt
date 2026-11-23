package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CronFixtureUiTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-cron-control" }

    private fun control(category: String, scope: String): ControlElement {
        val categories = (fixture().uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        return categories.single { it.label == category }.elements.filterIsInstance<GroupElement>()
            .single().elements.filterIsInstance<ControlElement>().single { it.scope == scope }
    }

    @Test fun materialAuthoredSchemaScheduleEditsAsCron() = schemaSchedule(false)
    @Test fun cupertinoAuthoredSchemaScheduleEditsAsCron() = schemaSchedule(true)
    @Test fun materialAuthoredUiScheduleAndUnsetStaySeparate() = uiSchedules(false)
    @Test fun cupertinoAuthoredUiScheduleAndUnsetStaySeparate() = uiSchedules(true)
    @Test fun materialRejectedDialectsRemainEditableWithError() = rejectedDialects(false)
    @Test fun cupertinoRejectedDialectsRemainEditableWithError() = rejectedDialects(true)
    @Test fun materialAuthoredWeekdayListPickerPreservesAndEditsRawText() = authoredPicker(false,
        "supplierFeed", "MON-FRI", "MON,WED,FRI", "0 30 6 ? * MON,WED,FRI")
    @Test fun cupertinoAuthoredWeekdayListPickerPreservesAndEditsRawText() = authoredPicker(true,
        "supplierFeed", "MON-FRI", "MON,WED,FRI", "0 30 6 ? * MON,WED,FRI")
    @Test fun materialAuthoredLastDayPickerPreservesAndEditsRawText() = authoredPicker(false,
        "monthEndReport", "L", "28", "0 0 23 28 * ?")
    @Test fun cupertinoAuthoredLastDayPickerPreservesAndEditsRawText() = authoredPicker(true,
        "monthEndReport", "L", "28", "0 0 23 28 * ?")

    private fun schemaSchedule(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Schema selection", "#/properties/selectedBySchema/properties/stockSync"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").assertExists()
        onAllNodes(hasSetTextAction() and hasText("0 0/15 * * * *")).onFirst()
            .performTextReplacement("0 0/20 * * * *")
        waitForIdle()
        assertEquals(JsonPrimitive("0 0/20 * * * *"),
            data.jsonObject.getValue("selectedBySchema").jsonObject["stockSync"])
        assertEquals(1, changes)
    }

    private fun uiSchedules(cupertino: Boolean) {
        runComposeUiTest {
            val example = fixture()
            var data by mutableStateOf<JsonElement>(example.data)
            var changes = 0
            setContent {
                JsonForms(data = data, schema = example.schema,
                    uischema = control("UI schema selection", "#/properties/selectedByUiSchema/properties/cacheWarm"),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    onChange = { data = it.data; changes++ })
            }
            onNodeWithContentDescription("Choose schedule").assertExists()
            onAllNodes(hasSetTextAction() and hasText("10 * * * * *")).onFirst()
                .performTextReplacement("20 * * * * *")
            waitForIdle()
            assertEquals(JsonPrimitive("20 * * * * *"),
                data.jsonObject.getValue("selectedByUiSchema").jsonObject["cacheWarm"])
            assertEquals(1, changes)
        }
        runComposeUiTest {
            val example = fixture()
            var changes = 0
            setContent {
                JsonForms(data = example.data, schema = example.schema,
                    uischema = control("UI schema selection", "#/properties/selectedByUiSchema/properties/unset"),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    onChange = { changes++ })
            }
            onNodeWithContentDescription("Choose schedule").performClick()
            onNodeWithText("Cancel").performClick()
            assertEquals(0, changes)
        }
    }

    private fun rejectedDialects(cupertino: Boolean) {
        for ((field, initial, corrected) in listOf(
            Triple("unixFiveField", "*/15 * * * *", "0 */15 * * * *"),
            Triple("quartzSevenField", "0 0 12 ? * MON 2027", "0 0 12 ? * MON"),
        )) runComposeUiTest {
            val example = fixture()
            var data by mutableStateOf<JsonElement>(example.data)
            var changes = 0
            setContent {
                JsonForms(data = data, schema = example.schema,
                    uischema = control("Unsupported dialect", "#/properties/rejected/properties/$field"),
                    renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                    onChange = { data = it.data; changes++ })
            }
            onNodeWithText("Invalid cron expression.").assertExists()
            onAllNodes(hasSetTextAction() and hasText(initial)).onFirst()
                .performTextReplacement(corrected)
            waitForIdle()
            assertEquals(JsonPrimitive(corrected), data.jsonObject.getValue("rejected").jsonObject[field])
            onNodeWithText("Invalid cron expression.").assertDoesNotExist()
            assertEquals(1, changes)
        }
    }

    private fun authoredPicker(cupertino: Boolean, field: String, raw: String,
        replacement: String, stored: String) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf<JsonElement>(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control("Schema selection", "#/properties/selectedBySchema/properties/$field"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose schedule").performClick()
        if (raw == "L") onAllNodes(hasSetTextAction() and hasText(raw)).onFirst().assertExists()
        else onNodeWithContentDescription("Days of week").assertExists()
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(0, changes)
        onNodeWithContentDescription("Choose schedule").performClick()
        if (raw == "L") onAllNodes(hasSetTextAction() and hasText(raw)).onFirst()
            .performTextReplacement(replacement)
        else {
            onNodeWithContentDescription("Days of week").performClick()
            onNodeWithContentDescription("Days of week: TUE").performClick()
            onNodeWithContentDescription("Days of week: THU").performClick()
            onNodeWithContentDescription("Days of week").performClick()
        }
        onNodeWithText("Apply").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(stored),
            data.jsonObject.getValue("selectedBySchema").jsonObject[field])
        assertEquals(1, changes)
    }
}
