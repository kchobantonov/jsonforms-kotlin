package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.i18n.lookupRendererCatalogString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Color picker chrome uses stable keys so hosts can override English/Bulgarian copy. */
internal val colorControlLocaleBundles: Map<String, JsonObject> = mapOf(
    "en" to JsonObject(mapOf(
        "color.invalid" to JsonPrimitive("Invalid color."),
        "color.hex3Transparency" to JsonPrimitive("Transparency cannot be saved as #RGB."),
        "color.close" to JsonPrimitive("Close"),
        "color.choose" to JsonPrimitive("Choose color"),
        "color.clear" to JsonPrimitive("Clear"),
        "color.hex" to JsonPrimitive("Hex"),
        "color.rgb" to JsonPrimitive("RGB"),
        "color.hsb" to JsonPrimitive("HSB"),
        "color.hexChannels" to JsonPrimitive("Hex channels"),
        "color.rgbChannels" to JsonPrimitive("RGB channels"),
        "color.hsbChannels" to JsonPrimitive("HSB channels"),
        "color.hexValue" to JsonPrimitive("Hex channel value"),
        "color.red" to JsonPrimitive("Red"),
        "color.green" to JsonPrimitive("Green"),
        "color.blue" to JsonPrimitive("Blue"),
        "color.hue" to JsonPrimitive("Hue"),
        "color.spectrumHue" to JsonPrimitive("Spectrum hue"),
        "color.spectrum" to JsonPrimitive("Saturation and brightness"),
        "color.saturation" to JsonPrimitive("Saturation"),
        "color.brightness" to JsonPrimitive("Brightness"),
        "color.alpha" to JsonPrimitive("Alpha"),
        "color.alphaChannel" to JsonPrimitive("Alpha channel"),
        "color.alphaValue" to JsonPrimitive("Alpha channel value"),
    )),
    "bg" to JsonObject(mapOf(
        "color.invalid" to JsonPrimitive("Невалиден цвят."),
        "color.hex3Transparency" to JsonPrimitive("Прозрачността не може да се запази като #RGB."),
        "color.close" to JsonPrimitive("Затвори"),
        "color.choose" to JsonPrimitive("Избери цвят"),
        "color.clear" to JsonPrimitive("Изчисти"),
        "color.hex" to JsonPrimitive("Hex"),
        "color.rgb" to JsonPrimitive("RGB"),
        "color.hsb" to JsonPrimitive("HSB"),
        "color.hexChannels" to JsonPrimitive("Канали Hex"),
        "color.rgbChannels" to JsonPrimitive("Канали RGB"),
        "color.hsbChannels" to JsonPrimitive("Канали HSB"),
        "color.hexValue" to JsonPrimitive("Шестнадесетичен цвят"),
        "color.red" to JsonPrimitive("Червено"),
        "color.green" to JsonPrimitive("Зелено"),
        "color.blue" to JsonPrimitive("Синьо"),
        "color.hue" to JsonPrimitive("Нюанс"),
        "color.spectrumHue" to JsonPrimitive("Нюанс на спектъра"),
        "color.spectrum" to JsonPrimitive("Наситеност и яркост"),
        "color.saturation" to JsonPrimitive("Наситеност"),
        "color.brightness" to JsonPrimitive("Яркост"),
        "color.alpha" to JsonPrimitive("Прозрачност"),
        "color.alphaChannel" to JsonPrimitive("Прозрачност"),
        "color.alphaValue" to JsonPrimitive("Прозрачност стойност"),
    )),
)

internal fun colorSwatchLocaleBundles(hex: String): Map<String, JsonObject> =
    colorControlLocaleBundles.mapValues { (locale, catalog) ->
        JsonObject(catalog + ("color.swatch" to JsonPrimitive(
            if (locale == "bg") "Цвят $hex" else "Color $hex")))
    }

internal fun colorChannelLocaleBundles(key: String): Map<String, JsonObject> =
    colorControlLocaleBundles.mapValues { (locale, catalog) ->
        val label = lookupRendererCatalogString(catalog, "color.$key") ?: key
        JsonObject(catalog + mapOf(
            "color.$key.channel" to JsonPrimitive(if (locale == "bg") "$label канал" else "$label channel"),
            "color.$key.value" to JsonPrimitive(if (locale == "bg") "$label стойност" else "$label channel value"),
        ))
    }
