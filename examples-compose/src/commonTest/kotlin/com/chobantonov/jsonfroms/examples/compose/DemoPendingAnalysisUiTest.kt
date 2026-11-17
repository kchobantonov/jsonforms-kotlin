package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class DemoPendingAnalysisUiTest {
    @Test fun materialPendingDateDraftHasSeparateAnalysisStatus() = pendingDate(false)
    @Test fun cupertinoPendingDateDraftHasSeparateAnalysisStatus() = pendingDate(true)

    private fun pendingDate(cupertino: Boolean) = runComposeUiTest {
        val store = MapSettings()
        DemoPreferences(store).save(DemoSettings(
            uiStyle = if (cupertino) DemoUiStyle.Cupertino else DemoUiStyle.Material,
        ))
        val example = ExampleDescription(
            name = "pending-date", label = "Pending date",
            data = Json.parseToJsonElement("""{"date":"2024-03-15"}"""),
            schema = JsonSchema(Json.parseToJsonElement(
                """{"type":"object","properties":{"date":{"type":"string","format":"date"}}}"""
            ).jsonObject),
            uischema = ControlElement("#/properties/date"),
        )
        val other = example.copy(name = "other", label = "Other example")
        setContent { ExamplesComposeApp(examples = listOf(example, other),
            settingsStore = store) }
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/202")
        onNodeWithText(" · Analysis pending").assertExists()
        onNodeWithText("Demo").assertExists()
        onNodeWithText("Data").performClick()
        onNodeWithText(" · Analysis pending").assertDoesNotExist()
        onNodeWithText("Demo").performClick()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/202")
        onNodeWithText(" · Analysis pending").assertExists()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/2024")
        onNodeWithText(" · Analysis pending").assertDoesNotExist()
        onAllNodes(hasSetTextAction()).onLast().performTextReplacement("03/15/202")
        onNodeWithText(" · Analysis pending").assertExists()
        onNodeWithText("Other example").performClick()
        onNodeWithText(" · Analysis pending").assertDoesNotExist()
    }
}
