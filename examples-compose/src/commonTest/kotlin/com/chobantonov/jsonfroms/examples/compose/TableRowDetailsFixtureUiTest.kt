package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class TableRowDetailsFixtureUiTest {
    @Test fun materialContactCancel() = scenario(false, Phase.Cancel)
    @Test fun cupertinoContactCancel() = scenario(true, Phase.Cancel)
    @Test fun materialContactApply() = scenario(false, Phase.Apply)
    @Test fun cupertinoContactApply() = scenario(true, Phase.Apply)
    @Test fun materialOffPageErrorCorrection() = scenario(false, Phase.OffPageError)
    @Test fun cupertinoOffPageErrorCorrection() = scenario(true, Phase.OffPageError)
    @Test fun materialPanelSelection() = scenario(false, Phase.Panels)
    @Test fun cupertinoPanelSelection() = scenario(true, Phase.Panels)

    private enum class Phase { Cancel, Apply, OffPageError, Panels }

    private fun scenario(cupertino: Boolean, phase: Phase) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-table-row-details" }
        var data by mutableStateOf(fixture.data)
        var changes = 0
        setContent {
            JsonForms(
                data = data, schema = fixture.schema, uischema = fixture.uischema,
                uischemas = fixture.uischemas, config = fixture.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ },
            )
        }
        when (phase) {
            Phase.Cancel, Phase.Apply -> {
                onNodeWithText("Boston · 555-01000").assertExists()
                onNodeWithText("Applicant 01 — applicant1@example.com").assertExists()
                onAllNodesWithContentDescription("Edit Contact").onFirst()
                    .performSemanticsAction(SemanticsActions.OnClick)
                onNodeWithTag("row-detail-dialog-frame").assertExists()
                onNodeWithText("Contact details").assertExists()
                onAllNodes(hasSetTextAction() and hasText("Boston")).onFirst()
                    .performTextReplacement(if (phase == Phase.Cancel) "Discard" else "Cambridge")
                onNodeWithText(if (phase == Phase.Cancel) "Cancel" else "Apply").performClick()
                waitForIdle()
                if (phase == Phase.Cancel) {
                    assertEquals(0, changes)
                    assertEquals("Boston", data.jsonObject["applicants"]!!.jsonArray[0]
                        .jsonObject["contact"]!!.jsonObject["city"]!!.jsonPrimitive.content)
                } else {
                    assertEquals(1, changes)
                    assertEquals("Cambridge", data.jsonObject["applicants"]!!.jsonArray[0]
                        .jsonObject["contact"]!!.jsonObject["city"]!!.jsonPrimitive.content)
                    onNodeWithText("Cambridge · 555-01000").assertExists()
                }
            }
            Phase.OffPageError -> {
                onNodeWithText("Go to first error").performSemanticsAction(SemanticsActions.OnClick)
                onNodeWithText("Item 7 details").assertExists()
                onAllNodes(hasSetTextAction()).onLast().performTextReplacement("Available for interview")
                onNodeWithText("Apply").performClick()
                waitForIdle()
                assertEquals(1, changes)
                assertEquals("Available for interview", data.jsonObject["applicants"]!!.jsonArray[6]
                    .jsonObject["notes"]!!.jsonPrimitive.content)
                onNodeWithText("Go to first error").assertDoesNotExist()
            }
            Phase.Panels -> {
                onNodeWithText("Go to first error").performSemanticsAction(SemanticsActions.OnClick)
                onNodeWithText("Cancel").performClick()
                onNodeWithText("Side panel").performClick()
                onNodeWithText("No selection").assertExists()
                onNodeWithTag("table-row-scroll-6").performSemanticsAction(SemanticsActions.OnClick)
                onNodeWithText("No selection").assertDoesNotExist()
                onNodeWithText("Bottom panel").performClick()
                onNodeWithTag("table-detail-bottom-splitter").assertExists()
                onNodeWithTag("table-row-scroll-6").performSemanticsAction(SemanticsActions.OnClick)
                onNodeWithText("No selection").assertDoesNotExist()
                assertEquals(0, changes)
            }
        }
    }
}
