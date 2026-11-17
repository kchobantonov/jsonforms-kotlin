package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class KitchenSinkFixtureUiTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-kitchen-sink" }

    private fun section(example: ExampleDescription, step: String, tab: String): CategoryElement {
        val steps = (example.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single()
        val selected = steps.elements.filterIsInstance<CategoryElement>().single { it.label == step }
        val tabs = selected.elements.filterIsInstance<CategorizationElement>().single()
        return tabs.elements.filterIsInstance<CategoryElement>().single { it.label == tab }
    }

    @Test fun materialNestedStepsAndTabs() = navigation(false)
    @Test fun cupertinoNestedStepsAndTabs() = navigation(true)
    @Test fun materialApplicantEdit() = applicantEdit(false)
    @Test fun cupertinoApplicantEdit() = applicantEdit(true)
    @Test fun materialNativeReferenceDialog() = referenceDialog(false)
    @Test fun cupertinoNativeReferenceDialog() = referenceDialog(true)
    @Test fun materialInvalidDataCategorySummary() = invalidDataSummary(false)
    @Test fun cupertinoInvalidDataCategorySummary() = invalidDataSummary(true)
    @Test fun materialInvalidReferenceOnSecondPage() = invalidReferencePage(false)
    @Test fun cupertinoInvalidReferenceOnSecondPage() = invalidReferencePage(true)

    private fun invalidReferencePage(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val table = section(example, "References", "References: table").elements
            .filterIsInstance<ControlElement>().single()
        var data by mutableStateOf(example.invalidData!!)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = table,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Page 2: 1 error").assertExists()
        onNodeWithContentDescription("Next page").performClick()
        onAllNodes(hasSetTextAction() and hasText("broken")).onFirst()
            .performTextReplacement("colleague5@example.com")
        onNodeWithText("Page 2: 1 error").assertDoesNotExist()
        runOnIdle {
            assertEquals("colleague5@example.com", data.jsonObject.getValue("team").jsonArray[4]
                .jsonObject.getValue("email").jsonPrimitive.content)
            assertEquals(1, changes)
        }
    }

    private fun invalidDataSummary(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var data by mutableStateOf(example.invalidData!!)
        val valid = example.data.jsonObject
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithContentDescription("Profile, Contains errors").assertExists()
        onNodeWithContentDescription("Job, Contains errors").assertExists()
        onNodeWithContentDescription("References, Contains errors").assertExists()
        runOnIdle {
            data = JsonObject(data.jsonObject + valid.filterKeys {
                it in setOf("email", "phone", "otp", "address")
            })
        }
        onNodeWithContentDescription("Profile, Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("Job, Contains errors").assertExists()
        runOnIdle {
            data = JsonObject(data.jsonObject + valid.filterKeys {
                it in setOf("age", "budget", "startDate", "experience")
            })
        }
        onNodeWithContentDescription("Job, Contains errors").assertDoesNotExist()
        onNodeWithContentDescription("References, Contains errors").assertExists()
        runOnIdle { data = JsonObject(data.jsonObject + ("team" to valid.getValue("team"))) }
        onNodeWithContentDescription("References, Contains errors").assertDoesNotExist()
    }

    private fun navigation(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Profile").assertExists()
        onNodeWithText("Applicant and account").assertExists()
        onNodeWithText("Job").performClick()
        onNodeWithText("Job preferences").assertExists()
        onNodeWithText("References").performClick()
        onNodeWithText("References: table").assertExists()
        onNodeWithText("References: AG Grid").assertDoesNotExist()
        onNodeWithText("Finish").performClick()
        onNodeWithText("Résumé and documents").assertExists()
        onNodeWithText("Technical work sample").assertDoesNotExist()
    }

    private fun applicantEdit(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val applicant = section(example, "Profile", "Applicant and account").elements.single()
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = applicant,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("Ada")).onFirst().performTextReplacement("Grace")
        runOnIdle {
            assertEquals("Grace", data.jsonObject.getValue("firstName").jsonPrimitive.content)
            assertEquals(1, changes)
        }
    }

    private fun referenceDialog(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val table = section(example, "References", "References: table").elements
            .filterIsInstance<ControlElement>().single()
        assertEquals("#/properties/team", table.scope)
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = table,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onAllNodesWithText("Colleague 1").onFirst().assertExists()
        onAllNodesWithText("Edit details").onFirst().performClick()
        onNodeWithTag("row-detail-dialog-frame").assertExists()
        onNodeWithText("Cancel").performClick()
    }
}
