package com.chobantonov.jsonfroms.core.validation

import io.github.optimumcode.json.schema.FormatValidator
import io.github.optimumcode.json.schema.model.AbstractElement
import io.github.optimumcode.json.schema.model.PrimitiveElement
import kotlin.math.roundToInt

/** Color profile adapted from jsonforms-react-renderer-common/colorFormat.ts
 * (revision 150fd688ae09f0f3b28e989649f3daf1d9f6d91d, MIT).
 * Copyright (c) 2024 Krasimir Chobantonov; see repository LICENSE.
 */
data class RgbaColor(val r: Int, val g: Int, val b: Int, val a: Double = 1.0)

enum class ColorSaveFormat { Hex, Hex3, Rgb, Hsb }

enum class ColorChannelView { Hex, Rgb, Hsb }

fun initialColorView(format: ColorSaveFormat): ColorChannelView = when (format) {
    ColorSaveFormat.Hex, ColorSaveFormat.Hex3 -> ColorChannelView.Hex
    ColorSaveFormat.Rgb -> ColorChannelView.Rgb
    ColorSaveFormat.Hsb -> ColorChannelView.Hsb
}

data class HsbColor(val h: Int, val s: Int, val b: Int)

fun rgbToHsb(color: RgbaColor): HsbColor {
    val r = color.r / 255.0; val g = color.g / 255.0; val b = color.b / 255.0
    val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
    val rawHue = when {
        delta == 0.0 -> 0.0
        max == r -> 60 * (((g - b) / delta) % 6)
        max == g -> 60 * ((b - r) / delta + 2)
        else -> 60 * ((r - g) / delta + 4)
    }
    return HsbColor(((rawHue.roundToInt() % 360) + 360) % 360,
        if (max == 0.0) 0 else (delta / max * 100).roundToInt(),
        (max * 100).roundToInt())
}

fun editColorChannel(
    base: RgbaColor?, view: ColorChannelView, key: String, input: String,
): RgbaColor? {
    if (view == ColorChannelView.Hex && key == "hex")
        return if (input.startsWith('#')) parseColor(input) else null
    val color = base ?: return null
    if (key == "alpha") {
        val percent = input.toDoubleOrNull() ?: return null
        return if (percent.isFinite() && percent in 0.0..100.0)
            color.copy(a = percent / 100.0) else null
    }
    val number = input.toDoubleOrNull() ?: return null
    if (!number.isFinite()) return null
    return when (view) {
        ColorChannelView.Hex -> null
        ColorChannelView.Rgb -> {
            if (number % 1.0 != 0.0 || number !in 0.0..255.0) return null
            when (key) {
                "r" -> color.copy(r = number.toInt())
                "g" -> color.copy(g = number.toInt())
                "b" -> color.copy(b = number.toInt())
                else -> null
            }
        }
        ColorChannelView.Hsb -> {
            val old = rgbToHsb(color)
            val values = when (key) {
                "h" -> Triple(number, old.s.toDouble(), old.b.toDouble())
                "s" -> Triple(old.h.toDouble(), number, old.b.toDouble())
                "b" -> Triple(old.h.toDouble(), old.s.toDouble(), number)
                else -> return null
            }
            if (values.first !in 0.0..360.0 || values.second !in 0.0..100.0 ||
                values.third !in 0.0..100.0) return null
            parseColor("hsb(${values.first}, ${values.second}%, ${values.third}%)")?.copy(a = color.a)
        }
    }
}

fun colorSaveFormat(value: String?): ColorSaveFormat = when (value) {
    "hex3" -> ColorSaveFormat.Hex3
    "rgb" -> ColorSaveFormat.Rgb
    "hsb" -> ColorSaveFormat.Hsb
    else -> ColorSaveFormat.Hex
}

private val colorHex = Regex("^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
private val colorFunction = Regex("^(rgba?|hsla?|hsba?)\\(([^()]*)\\)$", RegexOption.IGNORE_CASE)
private val colorNumber = Regex("^[+-]?(?:\\d+\\.?\\d*|\\.\\d+)%?$")

fun parseColor(value: String?): RgbaColor? {
    val text = value?.trim() ?: return null
    if (colorHex.matches(text)) {
        val digits = text.drop(1).let { if (it.length == 3) it.flatMap { c -> listOf(c, c) }.joinToString("") else it }
        return RgbaColor(digits.substring(0, 2).toInt(16), digits.substring(2, 4).toInt(16),
            digits.substring(4, 6).toInt(16), if (digits.length == 8) digits.substring(6, 8).toInt(16) / 255.0 else 1.0)
    }
    val match = colorFunction.matchEntire(text) ?: return null
    val name = match.groupValues[1].lowercase()
    val args = match.groupValues[2].split(',').map { it.trim() }
    if (args.size !in 3..4 || args.any { !colorNumber.matches(it) }) return null
    val numbers = args.map { it.removeSuffix("%").toDoubleOrNull() ?: return null }
    if (numbers.any { !it.isFinite() }) return null
    val alpha = if (args.size == 4) numbers[3] / (if (args[3].endsWith('%')) 100.0 else 1.0) else 1.0
    if (alpha !in 0.0..1.0) return null
    if (name.startsWith("rgb")) {
        if ((0..2).any { args[it].endsWith('%') || numbers[it] !in 0.0..255.0 || numbers[it] % 1.0 != 0.0 }) return null
        return RgbaColor(numbers[0].toInt(), numbers[1].toInt(), numbers[2].toInt(), alpha)
    }
    if (args[0].endsWith('%') || numbers[1] !in 0.0..100.0 || numbers[2] !in 0.0..100.0) return null
    val h = ((numbers[0].roundToInt() % 360) + 360) % 360
    val s = numbers[1] / 100.0
    val third = numbers[2] / 100.0
    val rgb = if (name.startsWith("hsl")) {
        val chroma = (1 - kotlin.math.abs(2 * third - 1)) * s
        val x = chroma * (1 - kotlin.math.abs((h / 60.0) % 2 - 1))
        val m = third - chroma / 2
        channelsFromHue(h, chroma, x, m)
    } else {
        val chroma = third * s
        val x = chroma * (1 - kotlin.math.abs((h / 60.0) % 2 - 1))
        channelsFromHue(h, chroma, x, third - chroma)
    }
    return rgb.copy(a = alpha)
}

private fun channelsFromHue(h: Int, c: Double, x: Double, m: Double): RgbaColor {
    val components = when (h / 60) {
        0 -> listOf(c, x, 0.0)
        1 -> listOf(x, c, 0.0)
        2 -> listOf(0.0, c, x)
        3 -> listOf(0.0, x, c)
        4 -> listOf(x, 0.0, c)
        else -> listOf(c, 0.0, x)
    }
    return RgbaColor(((components[0] + m) * 255).roundToInt(),
        ((components[1] + m) * 255).roundToInt(), ((components[2] + m) * 255).roundToInt())
}

fun serializeColor(color: RgbaColor, format: ColorSaveFormat): String? {
    require(listOf(color.r, color.g, color.b).all { it in 0..255 } && color.a in 0.0..1.0)
    val opaque = color.a >= 1.0
    fun hex(channel: Int) = channel.toString(16).padStart(2, '0')
    fun alpha() = (color.a * 100).roundToInt().let { "${it / 100}.${(it % 100).toString().padStart(2, '0')}" }
    return when (format) {
        ColorSaveFormat.Hex -> "#${hex(color.r)}${hex(color.g)}${hex(color.b)}" +
            if (opaque) "" else hex((color.a * 255).roundToInt())
        ColorSaveFormat.Hex3 -> if (!opaque) null else "#" + listOf(color.r, color.g, color.b)
            .joinToString("") { (it / 17.0).roundToInt().toString(16) }
        ColorSaveFormat.Rgb -> if (opaque) "rgb(${color.r}, ${color.g}, ${color.b})"
            else "rgba(${color.r}, ${color.g}, ${color.b}, ${alpha()})"
        ColorSaveFormat.Hsb -> {
            val (h, s, brightness) = rgbToHsb(color)
            if (opaque) "hsb($h, $s%, $brightness%)" else "hsba($h, $s%, $brightness%, ${alpha()})"
        }
    }
}

internal val colorFormatValidator = object : FormatValidator {
    override fun validate(element: AbstractElement) =
        if ((element as? PrimitiveElement)?.takeIf { it.isString }?.content
                ?.let(::parseColor) != null) FormatValidator.Valid()
        else FormatValidator.Invalid()
}
