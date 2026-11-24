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
    @Test fun materialAuthoredRgbStorage() = storageMode(false, "accentColor")
    @Test fun cupertinoAuthoredRgbStorage() = storageMode(true, "accentColor")
    @Test fun materialAuthoredHsbStorage() = storageMode(false, "chartSeries")
    @Test fun cupertinoAuthoredHsbStorage() = storageMode(true, "chartSeries")
    @Test fun materialAuthoredHslNormalization() = storageMode(false, "highlight")
    @Test fun cupertinoAuthoredHslNormalization() = storageMode(true, "highlight")
    @Test fun materialAuthoredAlphaClose() = storageMode(false, "watermark")
    @Test fun cupertinoAuthoredAlphaClose() = storageMode(true, "watermark")
    @Test fun materialAuthoredHex3Storage() = storageMode(false, "legacyBadge")
    @Test fun cupertinoAuthoredHex3Storage() = storageMode(true, "legacyBadge")
    @Test fun materialPickerOnlyStorage() = storageMode(false, "printSpot")
    @Test fun cupertinoPickerOnlyStorage() = storageMode(true, "printSpot")

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

    private fun storageMode(cupertino: Boolean, name: String) = runComposeUiTest {
        val example = getBuiltInExamples().first { it.name == "spec-color-control" }
        val authored = controls(example.uischema)
        val selected = authored.first { it.scope == "#/properties/$name" }
        var data by mutableStateOf(example.data)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = example.schema, uischema = selected,
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        val initial = when (name) {
            "accentColor" -> "rgb(237, 80, 80)"
            "chartSeries" -> "hsb(154, 76%, 71%)"
            "highlight" -> "hsl(48, 96%, 53%)"
            "watermark" -> "#3a7bd540"
            "legacyBadge" -> "#ed5050"
            else -> "#8c4a9e"
        }
        assertEquals(JsonPrimitive(initial), data.jsonObject[name])
        assertEquals(0, changes)
        if (name == "printSpot") onAllNodes(hasSetTextAction()).assertCountEquals(0)
        onNodeWithContentDescription("Choose color").performClick()
        if (name == "watermark") {
            onNodeWithText("Close").performClick()
            assertEquals(JsonPrimitive(initial), data.jsonObject[name])
            assertEquals(0, changes)
        } else {
            onNodeWithContentDescription("Color #ef4444").performClick()
            waitForIdle()
            when (name) {
                "accentColor" -> assertEquals(JsonPrimitive("rgb(239, 68, 68)"), data.jsonObject[name])
                "chartSeries" -> {
                    val hsb = (data.jsonObject[name] as JsonPrimitive).content
                    kotlin.test.assertTrue(hsb.startsWith("hsb(") && hsb.endsWith(")"))
                }
                "legacyBadge" -> assertEquals(JsonPrimitive("#e44"), data.jsonObject[name])
                else -> assertEquals(JsonPrimitive("#ef4444"), data.jsonObject[name])
            }
            assertEquals(1, changes)
        }
    }
}
