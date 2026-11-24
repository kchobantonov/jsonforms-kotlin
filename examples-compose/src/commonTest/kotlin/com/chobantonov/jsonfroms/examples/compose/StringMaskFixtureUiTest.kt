package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class StringMaskFixtureUiTest {
    @Test fun materialRawStorageAndInvalidImport() = rawAndImport(false)
    @Test fun cupertinoRawStorageAndInvalidImport() = rawAndImport(true)
    @Test fun materialFormattedStorage() = formatted(false)
    @Test fun cupertinoFormattedStorage() = formatted(true)
    @Test fun materialAlternativesAndCustomTokens() = alternativesAndTokens(false)
    @Test fun cupertinoAlternativesAndCustomTokens() = alternativesAndTokens(true)
    @Test fun materialPlainAndMultiline() = plainAndMultiline(false)
    @Test fun cupertinoPlainAndMultiline() = plainAndMultiline(true)
    @Test fun materialIncrementalMaskTyping() = incrementalMaskTyping(false)
    @Test fun cupertinoIncrementalMaskTyping() = incrementalMaskTyping(true)
    @Test fun materialFormattedMultiCharacterInsert() = formattedMultiCharacterInsert(false)
    @Test fun cupertinoFormattedMultiCharacterInsert() = formattedMultiCharacterInsert(true)
    @Test fun materialArrowCrossesMaskSeparator() = arrowCrossesMaskSeparator(false)
    @Test fun cupertinoArrowCrossesMaskSeparator() = arrowCrossesMaskSeparator(true)

    private fun control(root: UiSchemaElement?, property: String): ControlElement =
        root?.let { controlOrNull(it, property) } ?: error("Missing imported Control: $property")

    private fun controlOrNull(root: UiSchemaElement, property: String): ControlElement? = when (root) {
        is ControlElement -> root.takeIf { it.scope == "#/properties/$property" }
        is Layout -> root.elements.firstNotNullOfOrNull { controlOrNull(it, property) }
        is CategorizationElement -> root.elements.firstNotNullOfOrNull { controlOrNull(it, property) }
        else -> null
    }

    private fun rawAndImport(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        var changes = 0
        var selected by mutableStateOf(control(example.uischema, "bookingReference"))
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("482-913", substring = true)).onFirst().assertExists()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("731-204")
        assertEquals("731204", (data.jsonObject["bookingReference"] as JsonPrimitive).content)

        runOnIdle { selected = control(example.uischema, "legacyReference") }
        onAllNodes(hasSetTextAction() and hasText("PENDING-REVIEW", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("PENDING-REVIE")
        assertEquals("PENDING-REVIE", (data.jsonObject["legacyReference"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction() and hasText("PENDING-REVIE", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("111-222")
        assertEquals("111222", (data.jsonObject["legacyReference"] as JsonPrimitive).content)
        assertEquals(3, changes)
    }

    private fun formatted(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control(example.uischema, "contactPhone"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        onAllNodes(hasSetTextAction() and hasText("+1 (503) 555-0142", substring = true)).onFirst().assertExists()
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("4155550123")
        assertEquals("+1 (415) 555-0123", (data.jsonObject["contactPhone"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("")
        for (digit in "1235550142") {
            onAllNodes(hasSetTextAction()).onFirst().performTextInput(digit.toString())
        }
        assertEquals("+1 (123) 555-0142", (data.jsonObject["contactPhone"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performKeyInput {
            keyDown(Key.Backspace)
            keyUp(Key.Backspace)
        }
        assertEquals("+1 (123) 555-014", (data.jsonObject["contactPhone"] as JsonPrimitive).content)
        assertEquals(13, changes)
    }

    private fun alternativesAndTokens(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        var selected by mutableStateOf(control(example.uischema, "accountNumber"))
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction() and hasText("1234 5678 9012", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("8765 4321")
        assertEquals("87654321", (data.jsonObject["accountNumber"] as JsonPrimitive).content)

        runOnIdle {
            selected = control(example.uischema, "deckPosition")
        }
        onAllNodes(hasSetTextAction() and hasText("B-07", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("C-12")
        assertEquals("C12", (data.jsonObject["deckPosition"] as JsonPrimitive).content)

        runOnIdle {
            selected = control(example.uischema, "bayLabel")
        }
        onAllNodes(hasSetTextAction() and hasText("Süd", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Жаба")
        assertEquals("Жаба", (data.jsonObject["bayLabel"] as JsonPrimitive).content)

        runOnIdle {
            selected = control(example.uischema, "gateCode")
        }
        onAllNodes(hasSetTextAction() and hasText("81-4", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("81-42")
        assertEquals("8142", (data.jsonObject["gateCode"] as JsonPrimitive).content)
    }

    private fun plainAndMultiline(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        var selected by mutableStateOf(control(example.uischema, "contactName"))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(0, changes)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("")
        onAllNodes(hasText("Family name first", substring = true)).onFirst().assertExists()

        runOnIdle { selected = control(example.uischema, "officeCity") }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Boise")
        assertEquals("Boise", (data.jsonObject["officeCity"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Sea")
        waitForIdle()
        val suggestion = onAllNodes(hasText("Seattle") and hasClickAction() and !hasSetTextAction()).onFirst()
        if (cupertino) suggestion.performSemanticsAction(SemanticsActions.OnClick)
        else suggestion.performClick()
        waitForIdle()
        assertEquals("Seattle", (data.jsonObject["officeCity"] as JsonPrimitive).content,
            "Suggestion selection after $changes writes")
        onAllNodes(hasText("Seattle") and hasClickAction() and !hasSetTextAction()).assertCountEquals(0)

        runOnIdle { selected = control(example.uischema, "meetingNotes") }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("Arrive early.\nUse side entrance.")
        assertEquals("Arrive early.\nUse side entrance.",
            (data.jsonObject["meetingNotes"] as JsonPrimitive).content)
        assertEquals(5, changes)
    }

    private fun incrementalMaskTyping(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control(example.uischema, "bookingReference"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("")
        for ((index, digit) in "482913".withIndex()) {
            onAllNodes(hasSetTextAction()).onFirst().performTextInput(digit.toString())
            assertEquals("482913".take(index + 1),
                (data.jsonObject["bookingReference"] as JsonPrimitive).content)
        }
        onAllNodes(hasSetTextAction() and hasText("482-913", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performKeyInput {
            keyDown(Key.Backspace)
            keyUp(Key.Backspace)
        }
        assertEquals("48291", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction() and hasText("482-91", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst()
            .performSemanticsAction(SemanticsActions.SetSelection) { it(4, 4, false) }
        onAllNodes(hasSetTextAction()).onFirst().performTextInput("7")
        assertEquals("482791", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction() and hasText("482-791", substring = true)).onFirst().assertExists()
        onAllNodes(hasSetTextAction()).onFirst().performKeyInput {
            keyDown(Key.Backspace)
            keyUp(Key.Backspace)
        }
        assertEquals("48291", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
    }

    private fun formattedMultiCharacterInsert(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        var selected by mutableStateOf(control(example.uischema, "bookingReference"))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = selected,
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data; writes++ })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performTextReplacement("")
        field.performTextInput("731-204")
        assertEquals("731204", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
        onAllNodes(hasSetTextAction() and hasText("731-204", substring = true))
            .onFirst().assertExists()

        runOnIdle { selected = control(example.uischema, "contactPhone") }
        val phone = onAllNodes(hasSetTextAction()).onFirst()
        phone.performTextReplacement("")
        phone.performTextInput("+1 (415) 555-0123")
        assertEquals("+1 (415) 555-0123",
            (data.jsonObject["contactPhone"] as JsonPrimitive).content)
        assertEquals(4, writes)
    }

    private fun arrowCrossesMaskSeparator(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-string-controls" }
        var data by mutableStateOf(example.data)
        setContent {
            JsonForms(data = data, schema = example.schema,
                uischema = control(example.uischema, "bookingReference"),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { data = it.data })
        }
        val field = onAllNodes(hasSetTextAction()).onFirst()
        field.performClick()
        field.performSemanticsAction(SemanticsActions.SetSelection) { it(4, 4, false) }
        field.performKeyInput {
            keyDown(Key.DirectionLeft)
            keyUp(Key.DirectionLeft)
        }
        field.performTextInput("7")
        assertEquals("487291", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
        field.performTextReplacement("48291")
        field.performSemanticsAction(SemanticsActions.SetSelection) { it(2, 2, false) }
        field.performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        field.performTextInput("7")
        assertEquals("482791", (data.jsonObject["bookingReference"] as JsonPrimitive).content)
    }
}
