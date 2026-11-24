package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.CombinedValidityStatus
import com.chobantonov.jsonfroms.extended.renderers.cupertinoExtendedRenderers
import com.chobantonov.jsonfroms.extended.renderers.materialExtendedRenderers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorControlUiTest {
    @Test fun materialPickerOnlyClearFollowsHoverAndTouchSelection() = pickerOnlyClearVisibility(false)
    @Test fun cupertinoPickerOnlyClearFollowsHoverAndTouchSelection() = pickerOnlyClearVisibility(true)
    @Test fun materialPickerOnlyCanClearImportedNull() = pickerOnlyNullClear(false)
    @Test fun cupertinoPickerOnlyCanClearImportedNull() = pickerOnlyNullClear(true)
    @Test fun materialPickerOnlyClearKeepsKeyboardFocus() = pickerOnlyKeyboardClear(false)
    @Test fun cupertinoPickerOnlyClearKeepsKeyboardFocus() = pickerOnlyKeyboardClear(true)

    private fun pickerOnlyKeyboardClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#abc"}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Choose color")
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Choose color").performKeyInput {
            keyDown(Key.ShiftLeft); keyDown(Key.Tab); keyUp(Key.Tab); keyUp(Key.ShiftLeft)
        }
        onNodeWithContentDescription("Clear value").assertIsFocused()
            .performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("{}"), data)
            assertEquals(1, writes)
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
    }

    private fun pickerOnlyClearVisibility(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#abc"}"""))
        var writes = 0
        var inputMode: InputModeManager? = null
        setContent {
            inputMode = LocalInputModeManager.current
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                i18n = JsonFormsI18nState(locale = "bg"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        runOnIdle { inputMode!!.requestInputMode(InputMode.Touch) }
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
        onNodeWithContentDescription("Избери цвят").performMouseInput {
            moveTo(Offset(-100f, -100f)); moveTo(center)
        }
        onNodeWithContentDescription("Изчисти стойността").assertExists()
        onNodeWithContentDescription("Изчисти стойността").performMouseInput { moveTo(center) }
        waitForIdle()
        onNodeWithText("Изчисти стойността").assertExists()
        onNodeWithContentDescription("Избери цвят").performMouseInput { moveTo(Offset(-100f, -100f)) }
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
        runOnIdle { assertEquals(0, writes) }
        onNodeWithContentDescription("Избери цвят").performTouchInput { click() }
        onNode(hasText("Затвори") and hasClickAction()).performClick()
        onNodeWithContentDescription("Изчисти стойността").assertExists()
        onNodeWithContentDescription("Изчисти стойността")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("{}"), data)
            assertEquals(1, writes)
        }
        onNodeWithContentDescription("Изчисти стойността").assertDoesNotExist()
    }

    private fun pickerOnlyNullClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":null}"""))
        var writes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; writes++ })
        }
        onNodeWithContentDescription("Clear value").assertDoesNotExist()
        onNodeWithContentDescription("Choose color").performMouseInput { moveTo(center) }
        onNodeWithContentDescription("Clear value").assertExists()
        onNodeWithContentDescription("Clear value")
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle {
            assertEquals(Json.parseToJsonElement("{}"), data)
            assertEquals(1, writes)
        }
    }

    @Test fun materialTextEntryPickerOffersClearValue() = textEntryPickerClear(false)
    @Test fun cupertinoTextEntryPickerOffersClearValue() = textEntryPickerClear(true)
    @Test fun materialSpectrumTouchAndFooterClear() = spectrumTouch(false)
    @Test fun cupertinoSpectrumTouchAndFooterClear() = spectrumTouch(true)
    @Test fun materialBulgarianSpectrumKeyboard() = spectrumKeyboard(false)
    @Test fun cupertinoBulgarianSpectrumKeyboard() = spectrumKeyboard(true)
    @Test fun materialPickerHidesClearWhenDisabledByOption() = noFooterClear(false)
    @Test fun cupertinoPickerHidesClearWhenDisabledByOption() = noFooterClear(true)
    @Test fun materialReadonlyClosesPickerAndHidesClear() = readonlyFooterClear(false)
    @Test fun cupertinoReadonlyClosesPickerAndHidesClear() = readonlyFooterClear(true)
    @Test fun materialQuotedClearableKeepsPickerClear() = quotedFooterClear(false)
    @Test fun cupertinoQuotedClearableKeepsPickerClear() = quotedFooterClear(true)
    @Test fun materialQuotedColorTextEntryKeepsInput() = quotedTextEntry(false)
    @Test fun cupertinoQuotedColorTextEntryKeepsInput() = quotedTextEntry(true)

    private fun quotedTextEntry(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#2255aa"}"""),
                schema = schema, uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive("false"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        onAllNodes(hasSetTextAction()).assertCountEquals(1)
        assertEquals(0, changes)
    }

    private fun quotedFooterClear(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000"}"""),
                schema = schema, uischema = ControlElement("#/properties/shade", options = mapOf(
                    "clearable" to JsonPrimitive("false"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithText("Clear value").assertExists()
        assertEquals(0, changes)
    }

    private fun noFooterClear(cupertino: Boolean) = runComposeUiTest {
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000"}"""),
                schema = schema, uischema = ControlElement("#/properties/shade", options = mapOf(
                    "clearable" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Saturation and brightness").assertExists()
        onNodeWithText("Clear value").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun readonlyFooterClear(cupertino: Boolean) = runComposeUiTest {
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000"}"""),
                schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithText("Clear value").assertExists()
        runOnIdle { readonly = true }
        onNodeWithText("Clear value").assertDoesNotExist()
        onNodeWithContentDescription("Saturation and brightness").assertDoesNotExist()
        assertEquals(0, changes)
    }

    private fun spectrumTouch(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        val spectrum = onNodeWithContentDescription("Saturation and brightness")
        onNodeWithContentDescription("Spectrum hue").assertExists()
        val clear = onNodeWithText("Clear value")
        assertEquals(true, clear.fetchSemanticsNode().boundsInRoot.top >
            onNodeWithContentDescription("Hex channel value").fetchSemanticsNode().boundsInRoot.bottom)
        spectrum.performTouchInput { click(center) }
        waitForIdle()
        assertEquals(JsonPrimitive("#804040"), data.jsonObject["shade"])
        assertEquals(1, changes)
        clear.performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["shade"])
        assertEquals(2, changes)
    }

    private fun spectrumKeyboard(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade"),
                i18n = JsonFormsI18nState(locale = "bg"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Избери цвят").performClick()
        val spectrum = onNodeWithContentDescription("Наситеност и яркост")
        onNodeWithContentDescription("Нюанс на спектъра").assertExists()
        spectrum.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        spectrum.performKeyInput { keyDown(Key.DirectionLeft); keyUp(Key.DirectionLeft) }
        waitForIdle()
        assertEquals(JsonPrimitive("#ff0303"), data.jsonObject["shade"])
        assertEquals(1, changes)
    }

    private fun textEntryPickerClear(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#2255aa"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithText("Clear value").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["shade"])
        assertEquals(1, changes)
        onNodeWithText("Clear value").assertDoesNotExist()
    }
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"shade":{"type":"string","format":"color"}}
    }""").jsonObject)

    @Test fun materialPresetUsesConfiguredRgbFormat() = preset(false)
    @Test fun cupertinoPresetUsesConfiguredRgbFormat() = preset(true)
    @Test fun materialPickerOnlyCanClearInBulgarian() = pickerOnly(false)
    @Test fun cupertinoPickerOnlyCanClearInBulgarian() = pickerOnly(true)
    @Test fun materialInvalidImportDoesNotRewriteData() = invalidImport(false)
    @Test fun cupertinoInvalidImportDoesNotRewriteData() = invalidImport(true)
    @Test fun materialChannelsPreserveConfiguredFormatAndAlpha() = channels(false)
    @Test fun cupertinoChannelsPreserveConfiguredFormatAndAlpha() = channels(true)
    @Test fun materialViewSwitchDoesNotWriteAndReopenResets() = viewSwitch(false)
    @Test fun cupertinoViewSwitchDoesNotWriteAndReopenResets() = viewSwitch(true)
    @Test fun materialHsbFieldWritesHsbStorage() = hsbField(false)
    @Test fun cupertinoHsbFieldWritesHsbStorage() = hsbField(true)
    @Test fun materialTransparentHex3RefusesFieldEdit() = transparentHex3(false)
    @Test fun cupertinoTransparentHex3RefusesFieldEdit() = transparentHex3(true)
    @Test fun materialHostReplacementClosesPicker() = hostReplacement(false)
    @Test fun cupertinoHostReplacementClosesPicker() = hostReplacement(true)
    @Test fun materialEmptyPickerKeepsInvalidDraftUntilValidHex() = emptyHex(false)
    @Test fun cupertinoEmptyPickerKeepsInvalidDraftUntilValidHex() = emptyHex(true)
    @Test fun materialDirectTextKeepsIncompleteDraftAndNormalizes() = directText(false)
    @Test fun cupertinoDirectTextKeepsIncompleteDraftAndNormalizes() = directText(true)
    @Test fun materialInvalidImportedTextDoesNotCreatePendingEdit() = invalidImportedText(false)
    @Test fun cupertinoInvalidImportedTextDoesNotCreatePendingEdit() = invalidImportedText(true)
    @Test fun materialColorCatalogUpdatesWithLocaleAndHostOverride() = liveCatalog(false)
    @Test fun cupertinoColorCatalogUpdatesWithLocaleAndHostOverride() = liveCatalog(true)

    private fun liveCatalog(cupertino: Boolean) = runComposeUiTest {
        var locale by mutableStateOf("en")
        var override by mutableStateOf(false)
        var changes = 0
        val translatedKeys = mutableListOf<String>()
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"#ff0000"}"""),
                schema = schema, uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                i18n = JsonFormsI18nState(locale = locale, translate = { key, fallback, _ ->
                    if (key.endsWith("color.choose")) translatedKeys += key
                    when {
                        override && key.endsWith("color.choose") -> "Select shade"
                        override && key.endsWith("color.hex") -> "Hexadecimal"
                        else -> fallback
                    }
                }),
                onChange = { changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Hex channel value").performTextReplacement("#abcd")
        onNodeWithText("Invalid color.").assertExists()
        runOnIdle { locale = "bg-BG" }
        onNodeWithText("Невалиден цвят.").assertExists()
        onNodeWithText("Затвори").assertExists()
        onNodeWithContentDescription("Канали Hex").assertExists()
        onNodeWithText("Затвори").performClick()
        runOnIdle { override = true }
        onNodeWithContentDescription("Select shade").performClick()
        onAllNodesWithText("Hexadecimal").onFirst().assertExists()
        assertEquals(0, changes)
        assertEquals(true, translatedKeys.any { it.endsWith("color.choose") })
    }

    private fun invalidImportedText(cupertino: Boolean) = runComposeUiTest {
        var validity: CombinedValidityStatus? = null
        var changes = 0
        setContent {
            JsonForms(data = Json.parseToJsonElement("""{"shade":"not-a-color"}"""),
                schema = schema, uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { changes++ }, onValidityChange = { validity = it.status })
        }
        onNodeWithText("Invalid color.").assertExists()
        waitForIdle()
        assertEquals(CombinedValidityStatus.Invalid, validity)
        assertEquals(0, changes)
    }

    private fun directText(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        var changes = 0
        var validity: CombinedValidityStatus? = null
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ },
                onValidityChange = { validity = it.status })
        }
        onAllNodesWithText("#ff0000").onFirst().performTextReplacement("rgb(0, 255")
        waitForIdle()
        onNodeWithText("Invalid color.").assertExists()
        assertEquals(JsonPrimitive("#ff0000"), data.jsonObject["shade"])
        assertEquals(0, changes)
        assertEquals(CombinedValidityStatus.Pending, validity)
        onAllNodesWithText("rgb(0, 255").onFirst()
            .performTextReplacement("hsl(120, 100%, 50%)")
        waitForIdle()
        assertEquals(JsonPrimitive("#00ff00"), data.jsonObject["shade"])
        assertEquals(1, changes)
        assertEquals(CombinedValidityStatus.Valid, validity)
    }

    private fun emptyHex(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Hex channel value").performTextReplacement("#abcd")
        onNodeWithText("Invalid color.").assertExists()
        assertEquals(0, changes)
        onNodeWithContentDescription("Hex channel value").performTextReplacement("#abc")
        waitForIdle()
        assertEquals(JsonPrimitive("#aabbcc"), data.jsonObject["shade"])
        assertEquals(1, changes)
    }

    private fun hostReplacement(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        runOnIdle { data = Json.parseToJsonElement("""{"shade":"#0000ff"}""") }
        waitForIdle()
        onNodeWithText("Close").assertDoesNotExist()
        assertEquals(0, changes)
        onNodeWithContentDescription("Choose color").performClick()
        runOnIdle { readonly = true }
        onNodeWithText("Close").assertDoesNotExist()
        assertEquals(JsonPrimitive("#0000ff"), data.jsonObject["shade"])
    }

    private fun viewSwitch(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        assertEquals(true, onNodeWithContentDescription("Hex channels")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithContentDescription("RGB channels").performClick()
        assertEquals(true, onNodeWithContentDescription("RGB channels")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(0, changes)
        onNodeWithText("Close").performClick()
        onNodeWithContentDescription("Choose color").performClick()
        assertEquals(true, onNodeWithContentDescription("Hex channels")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(JsonPrimitive("#ff0000"), data.jsonObject["shade"])
    }

    private fun hsbField(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff0000"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorSaveFormat" to JsonPrimitive("hsb"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose color").performClick()
        assertEquals(true, onNodeWithContentDescription("HSB channels")
            .fetchSemanticsNode().config[SemanticsProperties.Selected])
        onNodeWithContentDescription("Hue channel value").performTextReplacement("120")
        waitForIdle()
        assertEquals(JsonPrimitive("hsb(120, 100%, 100%)"), data.jsonObject["shade"])
        onNodeWithText("Close").assertExists()
    }

    private fun transparentHex3(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#ff000080"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorSaveFormat" to JsonPrimitive("hex3"),
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("RGB channels").performClick()
        onNodeWithContentDescription("Red channel value").performTextReplacement("200")
        onNodeWithText("Transparency cannot be saved as #RGB.").assertExists()
        assertEquals(JsonPrimitive("#ff000080"), data.jsonObject["shade"])
        assertEquals(0, changes)
    }

    private fun channels(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#112233"}"""))
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorSaveFormat" to JsonPrimitive("rgb"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Alpha channel")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        waitForIdle()
        assertEquals(JsonPrimitive("rgba(17, 34, 51, 0.50)"), data.jsonObject["shade"])
        onNodeWithContentDescription("Red channel")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        waitForIdle()
        assertEquals(JsonPrimitive("rgba(255, 34, 51, 0.50)"), data.jsonObject["shade"])
    }

    private fun invalidImport(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"not-a-color"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("not-a-color").assertExists()
        onNodeWithText("Invalid color.").assertExists()
        waitForIdle()
        assertEquals(JsonPrimitive("not-a-color"), data.jsonObject["shade"])
        assertEquals(0, changes)
    }

    private fun preset(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"hsb(120, 100%, 100%)"}"""))
        var readonly by mutableStateOf(false)
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema, readonly = readonly,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorSaveFormat" to JsonPrimitive("rgb"))),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithContentDescription("Choose color").performClick()
        onNodeWithContentDescription("Color #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("rgb(239, 68, 68)"), data.jsonObject["shade"])
        assertEquals(1, changes)
        runOnIdle { readonly = true }
        onNodeWithContentDescription("Color #ef4444").assertDoesNotExist()
    }

    private fun pickerOnly(cupertino: Boolean) = runComposeUiTest {
        var data by mutableStateOf(Json.parseToJsonElement("""{"shade":"#abc"}"""))
        var changes = 0
        setContent {
            JsonForms(data = data, schema = schema,
                uischema = ControlElement("#/properties/shade", options = mapOf(
                    "colorTextEntry" to JsonPrimitive(false), "colorSaveFormat" to JsonPrimitive("hex3"))),
                i18n = JsonFormsI18nState(locale = "bg"),
                renderers = if (cupertino) cupertinoExtendedRenderers else materialExtendedRenderers,
                onChange = { data = it.data; changes++ })
        }
        onNodeWithText("#abc").assertExists()
        onNodeWithContentDescription("Избери цвят").performClick()
        onNodeWithText("Изчисти стойността").performClick()
        waitForIdle()
        assertEquals(null, data.jsonObject["shade"])
        assertEquals(1, changes)
        onNodeWithText("Затвори").assertExists()
        onNodeWithContentDescription("Цвят #ef4444").performClick()
        waitForIdle()
        assertEquals(JsonPrimitive("#e44"), data.jsonObject["shade"])
        assertEquals(2, changes)
    }
}
