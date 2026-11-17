package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.input.key.Key
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ChoiceSearchFixtureUiTest {
    @Test fun materialSearchIsOptInAndStoresChoiceValue() = searchable(false)
    @Test fun cupertinoSearchIsOptInAndStoresChoiceValue() = searchable(true)
    @Test fun materialAuthoredRadioChoicesKeepValueIdentity() = authoredRadio(false)
    @Test fun cupertinoAuthoredRadioChoicesKeepValueIdentity() = authoredRadio(true)
    @Test fun materialAuthoredRadioSupportsArrowAndSpace() = authoredRadioKeyboard(false)
    @Test fun cupertinoAuthoredRadioSupportsArrowAndSpace() = authoredRadioKeyboard(true)

    private fun controls(root: UiSchemaElement?): List<ControlElement> = when (root) {
        is ControlElement -> listOf(root)
        is Layout -> root.elements.flatMap(::controls)
        is CategorizationElement -> root.elements.flatMap(::controls)
        else -> emptyList()
    }

    private fun searchable(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-choice-controls" }
        val authored = controls(example.uischema)
        val originSearch = authored.first { it.scope == "#/properties/origin" && it.options["autocomplete"] == JsonPrimitive(true) }
        val originDefault = authored.first { it.scope == "#/properties/origin" && it.options["autocomplete"] == null }
        val handlerSearch = authored.first { it.scope == "#/properties/handler" }
        var selected by mutableStateOf(originSearch)
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Sea")
        assertEquals("Salem", (data.jsonObject["origin"] as JsonPrimitive).content)
        onAllNodes(hasText("Seattle") and hasClickAction() and !hasSetTextAction())
            .onFirst().performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        assertEquals("Seattle", (data.jsonObject["origin"] as JsonPrimitive).content)
        assertEquals(1, changes)

        runOnIdle { selected = handlerSearch }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Eng")
        assertEquals("ops", (data.jsonObject["handler"] as JsonPrimitive).content)
        onAllNodes(hasText("Engineering") and hasClickAction() and !hasSetTextAction())
            .onFirst().performSemanticsAction(SemanticsActions.OnClick)
        waitForIdle()
        assertEquals("eng", (data.jsonObject["handler"] as JsonPrimitive).content)
        assertEquals(2, changes)

        runOnIdle { selected = originDefault }
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals("Seattle", (data.jsonObject["origin"] as JsonPrimitive).content)
    }

    private fun authoredRadio(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-choice-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first {
            it.scope == "#/properties/$name" && it.options["format"] == JsonPrimitive("radio")
        }
        var selected by mutableStateOf(control("processing"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val radios = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        fun show(name: String) { runOnIdle { selected = control(name) } }
        fun choose(index: Int, name: String, expected: String) {
            onAllNodes(radios)[index].performClick()
            waitForIdle()
            assertEquals(Json.parseToJsonElement(expected), data.jsonObject[name])
            onAllNodes(radios)[index].assertIsSelected()
        }
        onAllNodes(radios)[1].assertIsSelected()
        val horizontal = onAllNodes(radios).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue(kotlin.math.abs(horizontal[1].top - horizontal[0].top) < 2f)
        choose(0, "processing", "\"Standard\"")

        show("workspace")
        onAllNodes(radios)[2].assertIsSelected()
        val vertical = onAllNodes(radios).fetchSemanticsNodes().map { it.boundsInRoot }
        assertTrue(vertical[1].top > vertical[0].top)
        choose(0, "workspace", "\"Private office\"")

        show("department")
        onAllNodes(radios)[1].assertIsSelected()
        choose(0, "department", "\"eng\"")

        show("priority")
        onAllNodes(radios)[1].assertIsSelected()
        choose(0, "priority", "1")

        show("region")
        onAllNodes(radios)[0].assertIsNotSelected()
        onAllNodes(radios)[1].assertIsNotSelected()
        choose(1, "region", "\"Manchester\"")

        show("supplier")
        onNodeWithText("Current value outside choices: \"Legacy\"").assertExists()
        assertEquals(JsonPrimitive("Legacy"), data.jsonObject["supplier"])
        choose(0, "supplier", "\"Internal\"")

        show("objectRadio")
        onAllNodes(radios)[1].assertIsSelected()
        choose(0, "objectRadio", """{"mode":"email","settings":{"priority":1}}""")

        show("arrayRadio")
        onAllNodes(radios)[1].assertIsSelected()
        choose(0, "arrayRadio", """["email",{"priority":1}]""")
        assertEquals(8, changes)
    }

    private fun authoredRadioKeyboard(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-choice-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("processing"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Express")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Express").assertIsFocused()
        onNodeWithContentDescription("Express").performKeyInput {
            keyDown(Key.DirectionRight); keyUp(Key.DirectionRight)
        }
        onNodeWithContentDescription("Overnight").assertIsFocused()
        onNodeWithContentDescription("Overnight").performKeyInput {
            keyDown(Key.Spacebar); keyUp(Key.Spacebar)
        }
        waitForIdle()
        assertEquals(JsonPrimitive("Overnight"), data.jsonObject["processing"])

        runOnIdle { selected = control("department") }
        onNodeWithContentDescription("Finance")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Finance").assertIsFocused()
        onNodeWithContentDescription("Finance").performKeyInput {
            keyDown(Key.DirectionUp); keyUp(Key.DirectionUp)
        }
        onNodeWithContentDescription("Engineering").assertIsFocused()
        onNodeWithContentDescription("Engineering").performKeyInput {
            keyDown(Key.Spacebar); keyUp(Key.Spacebar)
        }
        waitForIdle()
        assertEquals(JsonPrimitive("eng"), data.jsonObject["department"])
        assertEquals(2, changes)
    }
}
