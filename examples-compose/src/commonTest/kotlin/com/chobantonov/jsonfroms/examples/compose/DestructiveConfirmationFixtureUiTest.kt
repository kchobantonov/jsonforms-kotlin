package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class DestructiveConfirmationFixtureUiTest {
    @Test fun materialDefaultTableDeleteConfirms() = scenario(false, "lines", false)
    @Test fun cupertinoDefaultTableDeleteConfirms() = scenario(true, "lines", false)
    @Test fun materialElementOverrideSkipsConfirmation() = scenario(false, "quietLines", false)
    @Test fun cupertinoElementOverrideSkipsConfirmation() = scenario(true, "quietLines", false)
    @Test fun materialAdaptedChargesUseNativeTable() = scenario(false, "charges", false)
    @Test fun cupertinoAdaptedChargesUseNativeTable() = scenario(true, "charges", false)
    @Test fun materialBulgarianConfirmation() = scenario(false, "lines", true)
    @Test fun cupertinoBulgarianConfirmation() = scenario(true, "lines", true)

    @Test fun materialImportedBranchChangeConfirms() = branch(false)
    @Test fun cupertinoImportedBranchChangeConfirms() = branch(true)
    @Test fun materialImportedMixedTypeUsesComplexPolicy() = mixed(false)
    @Test fun cupertinoImportedMixedTypeUsesComplexPolicy() = mixed(true)
    @Test fun materialImportedScalarTypeSkipsComplexPrompt() = scalar(false)
    @Test fun cupertinoImportedScalarTypeSkipsComplexPrompt() = scalar(true)

    @Test fun materialImportedDynamicPropertyDelete() = delete(false, "notes")
    @Test fun cupertinoImportedDynamicPropertyDelete() = delete(true, "notes")
    @Test fun materialImportedExpandableItemDelete() = delete(false, "milestones")
    @Test fun cupertinoImportedExpandableItemDelete() = delete(true, "milestones")
    @Test fun materialImportedListDetailDelete() = delete(false, "coordinators")
    @Test fun cupertinoImportedListDetailDelete() = delete(true, "coordinators")
    @Test fun materialImportedMixedTreeDelete() = delete(false, "payload")
    @Test fun cupertinoImportedMixedTreeDelete() = delete(true, "payload")
    @Test fun materialImportedCompositeCellDelete() = delete(false, "compositeRows")
    @Test fun cupertinoImportedCompositeCellDelete() = delete(true, "compositeRows")

    private fun delete(cupertino: Boolean, field: String) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        val original = fixture.data
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureElement(fixture, field),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val action = when (field) {
            "notes" -> "Delete button"
            "payload" -> "Delete"
            "compositeRows" -> "Clear Object"
            else -> "Delete item"
        }
        onAllNodes(hasContentDescription(action)).onFirst().performClick()
        onNodeWithText("Delete this value?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onAllNodes(hasContentDescription(action)).onFirst().performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(1, changes)
            when (field) {
                "notes" -> assertEquals(JsonObject(emptyMap()), data.jsonObject.getValue(field))
                "payload" -> {
                    assertEquals(false, "assignment" in data.jsonObject.getValue(field).jsonObject)
                    assertEquals(original.jsonObject.getValue(field).jsonObject.getValue("supplier"),
                        data.jsonObject.getValue(field).jsonObject.getValue("supplier"))
                }
                "compositeRows" -> assertEquals(false,
                    "object" in data.jsonObject.getValue(field).jsonArray[0].jsonObject)
                else -> assertEquals(original.jsonObject.getValue(field).jsonArray.size - 1,
                    data.jsonObject.getValue(field).jsonArray.size)
            }
        }
    }

    @Test fun materialImportedEmptyCompositeCellSkipsPrompt() = emptyComposite(false)
    @Test fun cupertinoImportedEmptyCompositeCellSkipsPrompt() = emptyComposite(true)

    private fun emptyComposite(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        var data by mutableStateOf(fixture.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureElement(fixture, "compositeRows"),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasContentDescription("Clear Object"))[1].performClick()
        onNodeWithText("Delete this value?").assertDoesNotExist()
        runOnIdle {
            assertEquals(false, "object" in data.jsonObject.getValue("compositeRows").jsonArray[1].jsonObject)
            assertEquals(1, changes)
        }
    }

    @Test fun materialImportedOneOfSelectorClearConfirms() = clearContact(false)
    @Test fun cupertinoImportedOneOfSelectorClearConfirms() = clearContact(true)

    private fun clearContact(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        var data by mutableStateOf(fixture.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureControl(fixture, "contact"),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("By email")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("oneof-selector-clear", useUnmergedTree = true).performClick()
        onNodeWithText("Change the selection?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(fixture.data, data); assertEquals(0, changes) }
        onNodeWithText("By email")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithTag("oneof-selector-clear", useUnmergedTree = true).performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle {
            assertEquals(false, "contact" in data.jsonObject)
            assertEquals(1, changes)
        }
        onNodeWithTag("oneof-selector-clear", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("By email").assertDoesNotExist()
    }

    private fun fixtureElement(
        fixture: com.chobantonov.jsonfroms.examples.ExampleDescription, field: String,
    ): UiSchemaElement {
        val categories = (fixture.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        return categories.flatMap { it.elements }.single {
            (it as? ControlElement)?.scope == "#/properties/$field" ||
                (it as? ListWithDetailElement)?.scope == "#/properties/$field"
        }
    }

    private fun branch(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        val original = fixture.data
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureControl(fixture, "contact"),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("By email").performClick()
        waitForIdle()
        onNodeWithText("By phone").performClick()
        onNodeWithText("Change the selection?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onNodeWithText("By email").performClick()
        waitForIdle()
        onNodeWithText("By phone").performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertNotEquals(original, data); assertEquals(1, changes) }
    }

    private fun mixed(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        val original = fixture.data
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureControl(fixture, "payload"),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodesWithText("Object").onFirst().performClick()
        onAllNodesWithText("Array").onLast().performClick()
        onNodeWithText("Change the type?").assertExists()
        onNodeWithText("No").performClick()
        runOnIdle { assertEquals(original, data); assertEquals(0, changes) }
        onAllNodesWithText("Object").onFirst().performClick()
        onAllNodesWithText("Array").onLast().performClick()
        onNodeWithText("Yes").performClick()
        runOnIdle { assertNotEquals(original, data); assertEquals(1, changes) }
    }

    private fun scalar(cupertino: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        val original = fixture.data
        var data by mutableStateOf(original)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = fixtureControl(fixture, "scalar"),
                config = fixture.config, i18n = fixtureI18n(fixture),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("String").performClick()
        onNodeWithText("Number").performClick()
        onNodeWithText("Change the type?").assertDoesNotExist()
        runOnIdle { assertNotEquals(original, data); assertEquals(1, changes) }
    }

    private fun fixtureControl(
        fixture: com.chobantonov.jsonfroms.examples.ExampleDescription, field: String,
    ): ControlElement {
        val categories = (fixture.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        return categories.flatMap { it.elements }.filterIsInstance<ControlElement>()
            .single { it.scope == "#/properties/$field" }
    }

    private fun fixtureI18n(fixture: com.chobantonov.jsonfroms.examples.ExampleDescription) =
        JsonFormsI18nState(locale = "en", translate = { key, fallback, _ ->
            fixture.localizedTranslations.getValue("en")[key] ?: fallback
        })

    private fun scenario(cupertino: Boolean, field: String, bulgarian: Boolean) = runComposeUiTest {
        val fixture = getBuiltInExamples().first { it.name == "spec-destructive-confirmation" }
        val categories = (fixture.uischema as VerticalLayout).elements
            .filterIsInstance<CategorizationElement>().single().elements.filterIsInstance<CategoryElement>()
        val control = categories.flatMap { it.elements }.filterIsInstance<ControlElement>()
            .single { it.scope == "#/properties/$field" }
        if (field == "charges") assertEquals("true", control.options["table"].toString())
        val locale = if (bulgarian) "bg" else "en"
        val entries = fixture.localizedTranslations.getValue(locale)
        val originalCount = fixture.data.jsonObject.getValue(field).jsonArray.size
        var data by mutableStateOf(fixture.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = fixture.schema, uischema = control,
                config = fixture.config,
                i18n = JsonFormsI18nState(locale = locale,
                    translate = { key, fallback, _ -> entries[key] ?: fallback }),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val title = entries.getValue("confirm.delete.title")
        val decline = entries.getValue("confirm.decline")
        val accept = entries.getValue("confirm.accept")
        onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
        if (field == "quietLines") {
            onNodeWithText(title).assertDoesNotExist()
        } else {
            onNodeWithText(title).assertExists()
            onNodeWithText(decline).performClick()
            runOnIdle {
                assertEquals(fixture.data, data)
                assertEquals(0, changes)
            }
            onAllNodes(hasContentDescription("Delete item")).onFirst().performClick()
            onNodeWithText(accept).performClick()
        }
        runOnIdle {
            assertEquals(originalCount - 1, data.jsonObject.getValue(field).jsonArray.size)
            assertEquals(1, changes)
        }
    }
}
