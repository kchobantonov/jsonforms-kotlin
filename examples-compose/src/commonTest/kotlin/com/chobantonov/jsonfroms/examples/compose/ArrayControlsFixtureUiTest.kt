package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ArrayControlsFixtureUiTest {
    private fun fixture() = getBuiltInExamples().first { it.name == "spec-array-controls" }

    private fun element(example: ExampleDescription, category: String, scope: String): UiSchemaElement {
        val categories = (example.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        val selected = categories.single { it.label == category }
        return selected.elements.single {
            (it as? ControlElement)?.scope == scope || (it as? ListWithDetailElement)?.scope == scope
        }
    }

    @Test fun materialPortableTableEditAndDelete() = table(false)
    @Test fun cupertinoPortableTableEditAndDelete() = table(true)
    @Test fun materialExpandableAndListWithDetail() = arrayPresentations(false)
    @Test fun cupertinoExpandableAndListWithDetail() = arrayPresentations(true)
    @Test fun materialImportedActionBounds() = bounds(false)
    @Test fun cupertinoImportedActionBounds() = bounds(true)

    @Test fun materialImportedCompositeStaffCell() = staffCell(false)
    @Test fun cupertinoImportedCompositeStaffCell() = staffCell(true)
    @Test fun materialImportedRowDetailDialog() = rowDetail(false)
    @Test fun cupertinoImportedRowDetailDialog() = rowDetail(true)
    @Test fun materialImportedSidePanel() = sidePanel(false)
    @Test fun cupertinoImportedSidePanel() = sidePanel(true)
    @Test fun materialImportedBottomPanel() = bottomPanel(false)
    @Test fun cupertinoImportedBottomPanel() = bottomPanel(true)

    private fun staffCell(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = element(example, "Scalar and composite cells", "#/properties/staff")
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onAllNodes(hasContentDescription("Edit Address")).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("Apply").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private fun rowDetail(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = element(example, "Row and cell dialogs", "#/properties/sessions")
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onAllNodesWithText("Edit details").onFirst().performClick()
        onNodeWithTag("row-detail-dialog-frame").assertExists()
        onNodeWithText("Cancel").performClick()
    }

    private fun sidePanel(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = element(example, "Side panel", "#/properties/sessions")
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("No selection").assertExists()
        onNodeWithTag("table-row-scroll-0").performSemanticsAction(SemanticsActions.OnClick)
        onNodeWithText("No selection").assertDoesNotExist()
    }

    private fun bottomPanel(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = element(example, "Bottom panel", "#/properties/sessions")
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onNodeWithText("Show details").assertExists()
        onNodeWithText("Show details").performClick()
        onNodeWithTag("table-detail-bottom-splitter").assertExists()
    }

    private fun table(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        val control = element(example, "Table", "#/properties/sessions") as ControlElement
        assertEquals("true", control.options["table"].toString())
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = control, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasContentDescription("Edit Room")).onFirst().performClick()
        onNodeWithText("Room details").assertExists()
        onNodeWithText("Cancel").performClick()
        runOnIdle { assertEquals(example.data, data); assertEquals(0, changes) }
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(example.data.jsonObject.getValue("sessions").jsonArray.size - 1,
                data.jsonObject.getValue("sessions").jsonArray.size)
            assertEquals(1, changes)
        }
    }

    private fun arrayPresentations(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var view by mutableStateOf<UiSchemaElement>(element(example, "Expandable", "#/properties/speakers"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = view, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithText("Rosa Iqbal").onFirst().assertExists()
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(1, changes); assertEquals(1, data.jsonObject.getValue("speakers").jsonArray.size) }
        runOnIdle { view = element(example, "List with detail", "#/properties/sponsors") }
        onAllNodesWithText("Meridian Press").onFirst().assertExists()
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertEquals(2, changes); assertEquals(1, data.jsonObject.getValue("sponsors").jsonArray.size) }
    }

    private fun bounds(cupertino: Boolean) = runComposeUiTest {
        val example = fixture()
        var view by mutableStateOf<UiSchemaElement>(element(example, "Add, remove and bounds", "#/properties/rounds"))
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = view, config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers)
        }
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
        runOnIdle { view = element(example, "Add, remove and bounds", "#/properties/checklist") }
        onNodeWithContentDescription("Add item").assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete item")).onFirst().assertIsNotEnabled()
    }
}
