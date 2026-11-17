package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
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
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class NumericFixtureUiTest {
    @Test fun materialAuthoredSliderIsOptInAndStoresMultiples() = slider(false)
    @Test fun cupertinoAuthoredSliderIsOptInAndStoresMultiples() = slider(true)
    @Test fun materialAuthoredNumericEditorsKeepTypedValues() = editors(false)
    @Test fun cupertinoAuthoredNumericEditorsKeepTypedValues() = editors(true)

    private fun controls(root: UiSchemaElement?): List<ControlElement> = when (root) {
        is ControlElement -> listOf(root)
        is Layout -> root.elements.flatMap(::controls)
        is CategorizationElement -> root.elements.flatMap(::controls)
        else -> emptyList()
    }

    private fun slider(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-numeric-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("discountPercent"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        val progress = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
        onAllNodes(progress).onFirst()
        onNodeWithText("Current value: 0").assertExists()
        assertEquals(JsonPrimitive(0), data.jsonObject["discountPercent"])
        assertEquals(0, changes)
        onAllNodes(progress).onFirst().performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        waitForIdle()
        assertEquals(JsonPrimitive(5.0), data.jsonObject["discountPercent"])
        onAllNodes(progress).onFirst().performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        waitForIdle()
        assertEquals(JsonPrimitive(100.0), data.jsonObject["discountPercent"])
        onNodeWithContentDescription("Clear value").performClick()
        waitForIdle()
        assertFalse("discountPercent" in data.jsonObject)
        onNodeWithText("Not set").assertExists()
        assertEquals(3, changes)

        runOnIdle { selected = control("quantity") }
        onAllNodes(progress).assertCountEquals(0)
        assertEquals(JsonPrimitive(0), data.jsonObject["quantity"])
    }

    private fun editors(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-numeric-controls" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("quantity"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        fun show(name: String) { runOnIdle { selected = control(name) } }
        fun enter(value: String) {
            onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(value)
            waitForIdle()
        }
        assertEquals(JsonPrimitive(0), data.jsonObject["quantity"])
        assertEquals(0, changes)
        enter("1.9")
        assertEquals(JsonPrimitive(0), data.jsonObject["quantity"])
        assertEquals(0, changes)
        enter("2")
        assertEquals(JsonPrimitive(2), data.jsonObject["quantity"])

        show("batchCount")
        assertEquals(JsonPrimitive(3), data.jsonObject["batchCount"])
        assertEquals(1, changes)
        enter("4")
        assertEquals(JsonPrimitive(4), data.jsonObject["batchCount"])

        show("unitPrice")
        enter("12.75")
        assertEquals(JsonPrimitive(12.75), data.jsonObject["unitPrice"])

        show("weightKg")
        assertEquals(JsonPrimitive(0), data.jsonObject["weightKg"])
        enter("0.5")
        assertEquals(JsonPrimitive(0.5), data.jsonObject["weightKg"])

        show("toleranceMm")
        assertEquals(JsonPrimitive(-2.5), data.jsonObject["toleranceMm"])
        enter("-3.5")
        assertEquals(JsonPrimitive(-3.5), data.jsonObject["toleranceMm"])
        assertEquals(5, changes)
    }
}
