package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.validation.ColorChannelView
import com.chobantonov.jsonfroms.core.validation.ColorSaveFormat
import com.chobantonov.jsonfroms.core.validation.HsbColor
import com.chobantonov.jsonfroms.core.validation.RgbaColor
import com.chobantonov.jsonfroms.core.validation.colorSaveFormat
import com.chobantonov.jsonfroms.core.validation.editColorChannel
import com.chobantonov.jsonfroms.core.validation.initialColorView
import com.chobantonov.jsonfroms.core.validation.parseColor
import com.chobantonov.jsonfroms.core.validation.rgbToHsb
import com.chobantonov.jsonfroms.core.validation.serializeColor
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ColorProfileTest {
    @Test fun colorPickerCatalogUsesBulgarianAndHostOverride() {
        assertEquals(setOf("en", "bg"), colorControlLocaleBundles.keys)
        assertEquals(colorControlLocaleBundles["en"]!!.keys,
            colorControlLocaleBundles["bg"]!!.keys)
        assertEquals(colorSwatchLocaleBundles("#ff0000")["en"]!!.keys,
            colorSwatchLocaleBundles("#ff0000")["bg"]!!.keys)
        assertEquals(colorChannelLocaleBundles("red")["en"]!!.keys,
            colorChannelLocaleBundles("red")["bg"]!!.keys)
        assertEquals("Избери цвят", translateRendererText(null, "bg-BG", "color.choose",
            "Choose color", colorControlLocaleBundles))
        assertEquals("Choose color", translateRendererText(null, "de-DE", "color.choose",
            "Choose color", colorControlLocaleBundles))
        assertEquals("Цвят #ff0000", translateRendererText(null, "bg", "color.swatch",
            "Color #ff0000", colorSwatchLocaleBundles("#ff0000")))
        assertEquals("Color #ff0000", translateRendererText(null, "en", "color.swatch",
            "Other", colorSwatchLocaleBundles("#ff0000")))
        assertEquals("Червено стойност", translateRendererText(null, "bg", "color.red.value",
            "Red channel value", colorChannelLocaleBundles("red")))
        assertEquals("Red channel value", translateRendererText(null, "en", "color.red.value",
            "Other", colorChannelLocaleBundles("red")))
        val host = Translator { key, _, _ -> if (key == "shade.color.choose") "Pick shade" else null }
        assertEquals("Pick shade", translateRendererText(host, "bg", "color.choose",
            "Choose color", colorControlLocaleBundles, i18nKeyPrefix = "shade"))
    }

    @Test fun channelViewsAreIndependentOfStorageAndRejectInvalidEdits() {
        assertEquals(ColorChannelView.Hex, initialColorView(ColorSaveFormat.Hex3))
        assertEquals(ColorChannelView.Rgb, initialColorView(ColorSaveFormat.Rgb))
        assertEquals(ColorChannelView.Hsb, initialColorView(ColorSaveFormat.Hsb))
        val red = RgbaColor(255, 0, 0, 0.5)
        assertEquals(HsbColor(0, 100, 100), rgbToHsb(red))
        assertEquals(RgbaColor(0, 255, 0, 0.5),
            editColorChannel(red, ColorChannelView.Hsb, "h", "120"))
        assertEquals(RgbaColor(255, 0, 0, 0.25),
            editColorChannel(red, ColorChannelView.Rgb, "alpha", "25"))
        assertNull(editColorChannel(red, ColorChannelView.Rgb, "r", "12.5"))
        assertNull(editColorChannel(red, ColorChannelView.Hsb, "s", "101"))
        assertNull(editColorChannel(red, ColorChannelView.Hex, "hex", "#abcd"))
    }

    @Test fun acceptsDocumentedInputsWithoutFallback() {
        assertEquals(RgbaColor(170, 187, 204), parseColor("#abc"))
        assertEquals(RgbaColor(1, 2, 3, 128 / 255.0), parseColor("#01020380"))
        assertEquals(RgbaColor(1, 2, 3, 0.5), parseColor("rgba(1, 2, 3, 50%)"))
        assertEquals(RgbaColor(0, 255, 0), parseColor("hsl(120, 100%, 50%)"))
        assertEquals(RgbaColor(0, 255, 0), parseColor("hsb(480, 100%, 100%)"))
        listOf(null, "", "blue", "#abcd", "rgb(1.5, 2, 3)", "rgb(256, 2, 3)",
            "rgb(1 2 3)", "hsl(0, 101%, 50%)", "rgba(1, 2, 3, 2)").forEach { assertNull(parseColor(it), "$it") }
    }

    @Test fun writesEachConfiguredFormatAndRefusesTransparentHex3() {
        val opaque = RgbaColor(255, 0, 0)
        assertEquals("#ff0000", serializeColor(opaque, ColorSaveFormat.Hex))
        assertEquals("#f00", serializeColor(opaque, ColorSaveFormat.Hex3))
        assertEquals("rgb(255, 0, 0)", serializeColor(opaque, ColorSaveFormat.Rgb))
        assertEquals("hsb(0, 100%, 100%)", serializeColor(opaque, ColorSaveFormat.Hsb))
        val transparent = opaque.copy(a = 0.5)
        assertEquals("#ff000080", serializeColor(transparent, ColorSaveFormat.Hex))
        assertEquals("rgba(255, 0, 0, 0.50)", serializeColor(transparent, ColorSaveFormat.Rgb))
        assertEquals("hsba(0, 100%, 100%, 0.50)", serializeColor(transparent, ColorSaveFormat.Hsb))
        assertNull(serializeColor(transparent, ColorSaveFormat.Hex3))
        assertEquals(ColorSaveFormat.Hex, colorSaveFormat("unknown"))
    }
}
