package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@OptIn(ExperimentalTestApi::class)
class AdditionalPropertiesFixtureUiTest {
    @Test fun materialImportedEmptyKeyEditsExactValue() = emptyKey(false)
    @Test fun cupertinoImportedEmptyKeyEditsExactValue() = emptyKey(true)
    @Test fun materialImportedDottedKeyEditsExactValue() = dottedKey(false)
    @Test fun cupertinoImportedDottedKeyEditsExactValue() = dottedKey(true)
    @Test fun materialDottedAddStaysOneKey() = dottedAdd(false)
    @Test fun cupertinoDottedAddStaysOneKey() = dottedAdd(true)
    @Test fun materialLocalFalseDisablesBlankAdd() = localFalse(false)
    @Test fun cupertinoLocalFalseDisablesBlankAdd() = localFalse(true)
    @Test fun materialWhitespaceNameIsStoredExactly() = whitespaceAdd(false)
    @Test fun cupertinoWhitespaceNameIsStoredExactly() = whitespaceAdd(true)
    @Test fun materialPropertyNamesAndPatternsGateAdds() = nameConstraints(false)
    @Test fun cupertinoPropertyNamesAndPatternsGateAdds() = nameConstraints(true)
    @Test fun materialPermittedBlankNameAddsWithoutDraftError() = blankAdd(false)
    @Test fun cupertinoPermittedBlankNameAddsWithoutDraftError() = blankAdd(true)
    @Test fun materialClearingIsolatedValueKeepsKey() = clearIsolated(false)
    @Test fun cupertinoClearingIsolatedValueKeepsKey() = clearIsolated(true)
    @Test fun materialImportedStaticAndDynamicFieldsStaySeparate() = mixedFields(false)
    @Test fun cupertinoImportedStaticAndDynamicFieldsStaySeparate() = mixedFields(true)
    @Test fun materialImportedDynamicActionsSwitchToBulgarian() = liveBulgarian(false)
    @Test fun cupertinoImportedDynamicActionsSwitchToBulgarian() = liveBulgarian(true)

    private fun liveBulgarian(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var locale by mutableStateOf("en")
        var changes = 0
        setContent {
            val messages = example.localizedTranslations[locale].orEmpty()
            JsonForms(data = example.data, schema = example.schema,
                uischema = ControlElement("#/properties/inventory"),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, values ->
                    messages[key]?.let { message -> values.entries.fold(message) { text, (name, value) ->
                        text.replace("{$name}", value.toString())
                    } } ?: fallback
                }),
                onChange = { changes++ })
        }
        onAllNodes(hasText("Property name", substring = true)).onFirst().assertExists()
        runOnIdle { locale = "bg" }
        onAllNodes(hasText("Име на свойство", substring = true)).onFirst().assertExists()
        onAllNodes(hasContentDescription("Добавяне на свойство към", substring = true))
            .onFirst().assertExists()
        onAllNodes(hasContentDescription("Преименуване на свойство")).assertCountEquals(2)
        runOnIdle { locale = "en" }
        onAllNodes(hasText("Property name", substring = true)).onFirst().assertExists()
        assertEquals(0, changes)
    }

    private fun mixedFields(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = ControlElement("#/properties/inventory"),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val reference = onAllNodes(hasSetTextAction() and hasText("INV-204", substring = true)).onFirst()
        val quantity = onAllNodes(hasSetTextAction() and hasText("12", substring = true)).onFirst()
        val warehouse = onAllNodes(hasSetTextAction() and hasText("Portland", substring = true)).onFirst()
        val handling = onAllNodes(hasSetTextAction() and hasText("Keep dry", substring = true)).onFirst()
        assertTrue(reference.fetchSemanticsNode().boundsInRoot.top < warehouse.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(quantity.fetchSemanticsNode().boundsInRoot.top < warehouse.fetchSemanticsNode().boundsInRoot.top)
        assertTrue(warehouse.fetchSemanticsNode().boundsInRoot.top < handling.fetchSemanticsNode().boundsInRoot.top)
        onAllNodes(hasContentDescription("Rename button")).assertCountEquals(2)
        onAllNodes(hasContentDescription("Delete button")).assertCountEquals(2)
        warehouse.performTextReplacement("Boston")
        waitForIdle()
        onAllNodes(hasContentDescription("Rename button")).onFirst().performClick()
        onAllNodes(hasSetTextAction() and hasText("warehouse")).onFirst().performTextReplacement("depot")
        onNodeWithContentDescription("Save button").performClick()
        waitForIdle()
        val inventory = data.jsonObject["inventory"]!!.jsonObject
        assertEquals(JsonPrimitive("INV-204"), inventory["reference"])
        assertEquals(JsonPrimitive(12), inventory["quantity"])
        assertEquals(null, inventory["warehouse"])
        assertEquals(JsonPrimitive("Boston"), inventory["depot"])
        assertEquals(JsonPrimitive("Keep dry"), inventory["handling"])
        assertEquals(2, changes)
    }

    private fun emptyKey(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("items[0]").assertExists()
        onNodeWithText("2024").assertExists()
        onNodeWithText("  spaced  ").assertExists()
        onAllNodes(hasSetTextAction() and hasText("unnamed label", substring = true)).onFirst()
            .performTextReplacement("edited empty")
        waitForIdle()
        val labels = data.jsonObject["labels"]!!.jsonObject
        assertEquals(JsonPrimitive("edited empty"), labels[""])
        assertEquals(JsonPrimitive("crate"), labels["items[0]"])
        assertEquals(JsonPrimitive("kept exactly"), labels["  spaced  "])
        assertEquals(1, changes)
    }

    private fun dottedKey(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("Imported keys").performClick()
        onNodeWithText("legacy.key").assertExists()
        onNodeWithText("notes/1").assertExists()
        onAllNodes(hasSetTextAction() and hasText("written by the old project system", substring = true))
            .onFirst().performTextReplacement("edited literal")
        waitForIdle()
        val annotations = data.jsonObject["annotations"]!!.jsonObject
        assertEquals(JsonPrimitive("edited literal"), annotations["legacy.key"])
        assertEquals(null, annotations["legacy"])
        assertEquals(JsonPrimitive("a slash is an ordinary character in a key"), annotations["notes/1"])
        assertEquals(1, changes)
    }

    private fun dottedAdd(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("sdf.sdf")
        onAllNodes(hasContentDescription("Add button")).onFirst().performClick()
        waitForIdle()
        val labels = data.jsonObject["labels"]!!.jsonObject
        assertEquals(JsonPrimitive(""), labels["sdf.sdf"])
        assertEquals(null, labels["sdf"])
        assertEquals(1, changes)
    }

    private fun localFalse(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { changes++ })
        }
        onNodeWithText("Option overrides").performClick()
        onNodeWithContentDescription("Add button").assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete button")).onFirst().assertIsNotEnabled()
        onAllNodes(hasContentDescription("Delete button")).onLast().assertIsNotEnabled()
        assertEquals(0, changes)
    }

    private fun whitespaceAdd(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("  two  ")
        onAllNodes(hasContentDescription("Add button")).onFirst().performClick()
        waitForIdle()
        val labels = data.jsonObject["labels"]!!.jsonObject
        assertEquals(JsonPrimitive(""), labels["  two  "])
        assertEquals(null, labels["two"])
        assertEquals(1, changes)
    }

    private fun nameConstraints(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = ControlElement("#/properties/telemetry"),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("")).onFirst().performTextReplacement("sensor-a")
        onNodeWithContentDescription("Add button").performClick()
        assertEquals(null, data.jsonObject["telemetry"]!!.jsonObject["sensor-a"])
        onAllNodes(hasSetTextAction() and hasText("sensor-a"))[0].performTextReplacement("sensor-deck-b")
        onNodeWithContentDescription("Add button").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(0), data.jsonObject["telemetry"]!!.jsonObject["sensor-deck-b"], "data=$data")
        assertEquals(1, changes)
    }

    private fun blankAdd(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = ControlElement("#/properties/annotations", options = mapOf(
                    "allowEmptyPropertyNames" to JsonPrimitive(true),
                )),
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Add button").assertIsEnabled()
        onNodeWithText("Property name is required").assertDoesNotExist()
        onNodeWithContentDescription("Add button").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(""), data.jsonObject["annotations"]!!.jsonObject[""])
        onNodeWithContentDescription("Add button").assertIsNotEnabled()
        onNodeWithText("Property name is required").assertDoesNotExist()
        assertEquals(1, changes)
    }

    private fun clearIsolated(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-additional-properties" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = example.uischema,
                config = example.config,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("unnamed label", substring = true)).onFirst()
            .performTextReplacement("")
        waitForIdle()
        val labels = data.jsonObject["labels"]!!.jsonObject
        assertEquals(JsonPrimitive(""), labels[""], "data=$data")
        assertEquals(1, changes)
    }
}
