package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorFixtureUiTest {
    @Test fun materialAuthoredUiAndSchemaFormatsSelectColor() = fixture(false)
    @Test fun cupertinoAuthoredUiAndSchemaFormatsSelectColor() = fixture(true)
    @Test fun materialAuthoredStorageAndPickerModes() = storageModes(false)
    @Test fun cupertinoAuthoredStorageAndPickerModes() = storageModes(true)

    private fun controls(root: UiSchemaElement?): List<ControlElement> = when (root) {
        is ControlElement -> listOf(root)
        is Layout -> root.elements.flatMap(::controls)
        is CategorizationElement -> root.elements.flatMap(::controls)
        else -> emptyList()
    }

    private fun fixture(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-color-control" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("statusDot"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        assertEquals(JsonPrimitive("color"), selected.options["format"])
        onNodeWithContentDescription("Choose color").assertExists()
        assertEquals(JsonPrimitive("rgb(82, 196, 26)"), data.jsonObject["statusDot"])
        assertEquals(0, changes)
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Color #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("rgb(239, 68, 68)"), data.jsonObject["statusDot"])

        runOnIdle { selected = control("brandPrimary") }
        onNodeWithContentDescription("Choose color").assertExists()
        assertEquals(JsonPrimitive("#3a7bd5"), data.jsonObject["brandPrimary"])
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Color #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["brandPrimary"])

        runOnIdle { selected = control("importedTint") }
        onNodeWithText("Invalid color.").assertExists()
        assertEquals(JsonPrimitive("color(display-p3 0.4 0.2 0.6)"), data.jsonObject["importedTint"])
        assertEquals(2, changes)
    }

    private fun storageModes(cupertino: Boolean) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-color-control" }
        val authored = controls(example.uischema)
        fun control(name: String) = authored.first { it.scope == "#/properties/$name" }
        var selected by mutableStateOf(control("accentColor"))
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        fun show(name: String) { runOnIdle { selected = control(name) } }
        fun choosePreset() {
            onNodeWithContentDescription("Choose color").performClick()
            onNodeWithContentDescription("Color #ef4444").performClick()
            waitForIdle()
        }
        assertEquals(JsonPrimitive("rgb(237, 80, 80)"), data.jsonObject["accentColor"])
        assertEquals(0, changes)
        choosePreset()
        assertEquals(JsonPrimitive("rgb(239, 68, 68)"), data.jsonObject["accentColor"])

        show("chartSeries")
        assertEquals(JsonPrimitive("hsb(154, 76%, 71%)"), data.jsonObject["chartSeries"])
        choosePreset()
        val hsb = (data.jsonObject["chartSeries"] as JsonPrimitive).content
        kotlin.test.assertTrue(hsb.startsWith("hsb(") && hsb.endsWith(")"))

        show("highlight")
        assertEquals(JsonPrimitive("hsl(48, 96%, 53%)"), data.jsonObject["highlight"])
        choosePreset()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["highlight"])

        show("watermark")
        assertEquals(JsonPrimitive("#3a7bd540"), data.jsonObject["watermark"])
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithText("Close").performClick()
        assertEquals(JsonPrimitive("#3a7bd540"), data.jsonObject["watermark"])

        show("legacyBadge")
        assertEquals(JsonPrimitive("#ed5050"), data.jsonObject["legacyBadge"])
        choosePreset()
        assertEquals(JsonPrimitive("#e44"), data.jsonObject["legacyBadge"])

        show("printSpot")
        onAllNodes(hasSetTextAction()).assertCountEquals(0)
        assertEquals(JsonPrimitive("#8c4a9e"), data.jsonObject["printSpot"])
        choosePreset()
        assertEquals(JsonPrimitive("#ef4444"), data.jsonObject["printSpot"])
        assertEquals(5, changes)
    }
}
