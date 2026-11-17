package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BooleanFixtureUiTest {
    @Test fun materialSwitchRequiresCanonicalBooleanToggleOption() = canonicalToggle(false)
    @Test fun cupertinoSwitchRequiresCanonicalBooleanToggleOption() = canonicalToggle(true)

    private fun canonicalToggle(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-boolean-controls" }
        val authored = controls(example.uischema)
            .first { it.scope == "#/properties/remoteWorker" }
        assertEquals(JsonPrimitive(true), authored.options["toggle"])
        var selected by mutableStateOf(authored.copy(options = mapOf("switch" to JsonPrimitive(true))))
        var changes = 0
        setContent {
            JsonForms(data = example.data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                config = mapOf("toggle" to true),
                onChange = { changes++ })
        }
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        onAllNodes(checkbox).assertCountEquals(1)
        onAllNodes(switch).assertCountEquals(0)
        runOnIdle { selected = authored.copy(options = mapOf("toggle" to JsonPrimitive("true"))) }
        onAllNodes(checkbox).assertCountEquals(1)
        onAllNodes(switch).assertCountEquals(0)
        runOnIdle { selected = authored }
        onAllNodes(checkbox).assertCountEquals(0)
        onAllNodes(switch).assertCountEquals(1)
        assertEquals(0, changes)
    }

    @Test fun materialAuthoredCheckboxAndSwitchKeepBooleanIdentity() = authored(false)
    @Test fun cupertinoAuthoredCheckboxAndSwitchKeepBooleanIdentity() = authored(true)
    @Test fun materialAuthoredCheckboxGroupsKeepMembershipAndLayout() = groups(false)
    @Test fun cupertinoAuthoredCheckboxGroupsKeepMembershipAndLayout() = groups(true)
    @Test fun materialAuthoredTableUsesSwitchCell() = table(false)
    @Test fun cupertinoAuthoredTableUsesSwitchCell() = table(true)

    private fun controls(root: UiSchemaElement?): List<ControlElement> = when (root) {
        is ControlElement -> listOf(root)
        is Layout -> root.elements.flatMap(::controls)
        is CategorizationElement -> root.elements.flatMap(::controls)
        else -> emptyList()
    }

    private fun authored(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-boolean-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("notifications"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        fun show(name: String) { runOnIdle { selected = control(name) } }

        onAllNodes(checkbox).onFirst().assertIsOff()
        onAllNodes(switch).assertCountEquals(0)
        assertEquals(JsonPrimitive(false), data.jsonObject["notifications"])
        assertEquals(0, changes)
        onAllNodes(checkbox).onFirst().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(true), data.jsonObject["notifications"])

        show("remoteWorker")
        onAllNodes(checkbox).assertCountEquals(0)
        onAllNodes(switch).onFirst().assertIsOn()
        onAllNodes(switch).onFirst().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(false), data.jsonObject["remoteWorker"])

        show("newsletter")
        onNode(hasStateDescription("Not set")).assertExists()
        assertFalse("newsletter" in data.jsonObject)
        onAllNodes(checkbox).onFirst().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(true), data.jsonObject["newsletter"])

        show("importedCount")
        onNode(hasStateDescription("Invalid value: 0")).assertExists()
        assertEquals(JsonPrimitive(0), data.jsonObject["importedCount"])
        onAllNodes(switch).onFirst().performClick()
        waitForIdle()
        assertEquals(JsonPrimitive(true), data.jsonObject["importedCount"])
        assertEquals(4, changes)
    }

    private fun groups(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-boolean-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("channels"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val row = onAllNodes(checkbox).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, row.size)
        assertTrue(kotlin.math.abs(row[1].top - row[0].top) < 2f)
        onAllNodes(checkbox)[1].assertIsOn()
        onAllNodes(checkbox)[2].performClick()
        waitForIdle()
        assertEquals(JsonArray(listOf(JsonPrimitive("SMS"), JsonPrimitive("Push"))), data.jsonObject["channels"])
        onAllNodes(checkbox)[1].performClick()
        waitForIdle()
        assertEquals(JsonArray(listOf(JsonPrimitive("Push"))), data.jsonObject["channels"])

        runOnIdle { selected = control("escalation") }
        val column = onAllNodes(checkbox).fetchSemanticsNodes().map { it.boundsInRoot }
        assertEquals(3, column.size)
        assertTrue(column[1].top > column[0].top)
        onAllNodes(checkbox)[0].assertIsOn()
        onAllNodes(checkbox)[2].assertIsOn()
        onAllNodes(checkbox)[1].performClick()
        waitForIdle()
        assertEquals(JsonArray(listOf(JsonPrimitive("Email"), JsonPrimitive("Push"), JsonPrimitive("SMS"))),
            data.jsonObject["escalation"])
        assertEquals(3, changes)
    }

    private fun table(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-boolean-controls" }
        val selected = controls(example.uischema).first { it.scope == "#/properties/team" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        onAllNodes(checkbox).assertCountEquals(2)
        onAllNodes(switch).assertCountEquals(2)
        assertEquals(0, changes)
        onAllNodes(switch)[0].assertIsOff()
        onAllNodes(switch)[0].performClick()
        waitForIdle()
        val team = data.jsonObject["team"] as JsonArray
        assertEquals(JsonPrimitive(true), team[0].jsonObject["onCall"])
        assertEquals(JsonPrimitive("false"), team[1].jsonObject["active"])
        onAllNodes(checkbox)[1].performClick()
        waitForIdle()
        val corrected = data.jsonObject["team"] as JsonArray
        assertEquals(JsonPrimitive(true), corrected[1].jsonObject["active"])
        assertEquals(2, changes)
    }
}
