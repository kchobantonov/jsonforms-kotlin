package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.inputAdornmentGlyph
import com.chobantonov.jsonfroms.compose.InputAdornmentIconResolver
import com.chobantonov.jsonfroms.compose.LocalInputAdornmentIconResolver
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.cupertino.renderers.cupertinoRenderers
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InputAdornmentIconUiTest {
    @Test fun portableSemanticGlyphsAndUnknownFallback() {
        assertEquals("ⓘ", inputAdornmentGlyph("information"))
        assertEquals("◉", inputAdornmentGlyph("account"))
        assertEquals("◷", inputAdornmentGlyph("schedule"))
        assertEquals("🔒", inputAdornmentGlyph("lock"))
        assertEquals(null, inputAdornmentGlyph("unmapped"))
    }

    @Test fun materialIconNamesAndPointerTooltip() = iconTooltip(false)
    @Test fun cupertinoIconNamesAndPointerTooltip() = iconTooltip(true)
    @Test fun materialHostIconsOverrideGlyphsAndPreserveFallback() = hostIcons(false)
    @Test fun cupertinoHostIconsOverrideGlyphsAndPreserveFallback() = hostIcons(true)

    private fun hostIcons(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        val options = Json.parseToJsonElement("""{
          "startAdornment":[
            {"kind":"icon","icon":"information","label":"Information"},
            {"kind":"icon","icon":"custom","label":"Custom","tooltip":"Custom help"}],
          "endAdornment":{"kind":"icon","icon":"unmapped","label":"Unmapped"}
        }""").jsonObject
        val resolver: InputAdornmentIconResolver = { name ->
            if (name == "information" || name == "custom") ColorPainter(Color.Magenta) else null
        }
        var writes = 0
        setContent {
            CompositionLocalProvider(LocalInputAdornmentIconResolver provides resolver) {
                JsonForms(data = Json.parseToJsonElement("""{"value":"Ada"}"""), schema = schema,
                    uischema = ControlElement("#/properties/value", options = options),
                    renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                    onChange = { writes++ })
            }
        }
        onNodeWithContentDescription("Information").assertExists()
        onNodeWithContentDescription("Custom").assertExists()
        onNodeWithText("ⓘ", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Custom", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("Unmapped").assertExists()
        onNodeWithContentDescription("Custom")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("Custom help").assertExists()
        assertEquals(0, writes)
    }

    private fun iconTooltip(cupertino: Boolean) = runComposeUiTest {
        val schema = JsonSchema(Json.parseToJsonElement("""{
          "type":"object","properties":{"value":{"type":"string"}}
        }""").jsonObject)
        val options = Json.parseToJsonElement("""{
          "startAdornment":{"kind":"icon","icon":"information","label":"Information",
            "tooltip":"More information"},
          "endAdornment":[{"kind":"icon","icon":"unmapped","label":"Unmapped"},
            {"kind":"icon","icon":"account","decorative":true},
            {"kind":"icon","icon":"unmapped","decorative":true}]
        }""").jsonObject
        var writes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"value":"Ada"}"""), schema = schema,
                uischema = ControlElement("#/properties/value", options = options),
                renderers = if (cupertino) cupertinoRenderers else materialRenderers,
                onChange = { writes++ })
        }
        onNodeWithText("ⓘ", useUnmergedTree = true).assertExists()
        onNodeWithContentDescription("Information").assertExists()
        onNodeWithText("Unmapped").assertExists()
        onNodeWithText("◉", useUnmergedTree = true).assertDoesNotExist()
        onNodeWithText("unmapped").assertDoesNotExist()
        onNodeWithContentDescription("Information").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("More information").assertExists()
        onNodeWithContentDescription("Information")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitForIdle()
        onNodeWithText("More information").assertExists()
        assertEquals(0, writes)
    }
}
