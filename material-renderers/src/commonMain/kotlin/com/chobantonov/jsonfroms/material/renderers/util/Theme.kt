package com.chobantonov.jsonfroms.material.renderers.util

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.chobantonov.jsonfroms.core.models.ControlElement

enum class MaterialThemePreset(val displayName: String) {
    Blue("Blue"),
    Indigo("Indigo"),
    Purple("Purple"),
    Green("Green"),
    Orange("Orange"),
    Pink("Pink"),
}

private data class MaterialPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)

fun materialColorScheme(
    darkTheme: Boolean,
    preset: MaterialThemePreset = MaterialThemePreset.Purple,
): ColorScheme {
    val palette = materialPalette(darkTheme, preset)
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.primary,
        onSecondary = palette.onPrimary,
        secondaryContainer = palette.primaryContainer,
        onSecondaryContainer = palette.onPrimaryContainer,
        tertiary = palette.primary,
        onTertiary = palette.onPrimary,
        tertiaryContainer = palette.primaryContainer,
        onTertiaryContainer = palette.onPrimaryContainer,
        inversePrimary = palette.primary,
    )
}

private fun materialPalette(darkTheme: Boolean, preset: MaterialThemePreset): MaterialPalette =
    if (darkTheme) {
        when (preset) {
            MaterialThemePreset.Blue -> MaterialPalette(Color(0xFF9ECAFF), Color(0xFF003258), Color(0xFF00497D), Color(0xFFD1E4FF))
            MaterialThemePreset.Indigo -> MaterialPalette(Color(0xFFC1C1FF), Color(0xFF292866), Color(0xFF414080), Color(0xFFE1E0FF))
            MaterialThemePreset.Purple -> MaterialPalette(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF))
            MaterialThemePreset.Green -> MaterialPalette(Color(0xFF92D5A3), Color(0xFF00391C), Color(0xFF00522C), Color(0xFFADF2BE))
            MaterialThemePreset.Orange -> MaterialPalette(Color(0xFFFFB77D), Color(0xFF4F2500), Color(0xFF713700), Color(0xFFFFDCC3))
            MaterialThemePreset.Pink -> MaterialPalette(Color(0xFFFFB0C8), Color(0xFF5E1131), Color(0xFF7A2948), Color(0xFFFFD9E2))
        }
    } else {
        when (preset) {
            MaterialThemePreset.Blue -> MaterialPalette(Color(0xFF0061A4), Color.White, Color(0xFFD1E4FF), Color(0xFF001D36))
            MaterialThemePreset.Indigo -> MaterialPalette(Color(0xFF5050A4), Color.White, Color(0xFFE1E0FF), Color(0xFF0A0A5C))
            MaterialThemePreset.Purple -> MaterialPalette(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D))
            MaterialThemePreset.Green -> MaterialPalette(Color(0xFF006D3B), Color.White, Color(0xFFADF2BE), Color(0xFF00210F))
            MaterialThemePreset.Orange -> MaterialPalette(Color(0xFF934B00), Color.White, Color(0xFFFFDCC3), Color(0xFF301400))
            MaterialThemePreset.Pink -> MaterialPalette(Color(0xFF984061), Color.White, Color(0xFFFFD9E2), Color(0xFF3E001D))
        }
    }

enum class MaterialInputVariant {
    Standard,
    Filled,
    Outlined,
}

val defaultInputVariant: MaterialInputVariant = MaterialInputVariant.Outlined

fun useInputVariant(
    config: Map<String, Any?>,
    uischema: ControlElement,
): MaterialInputVariant {
    val optionVariant = (uischema.options["variant"] as? kotlinx.serialization.json.JsonPrimitive)?.content
    val configVariant = config["variant"] as? String
    return parseInputVariant(optionVariant)
        ?: parseInputVariant(configVariant)
        ?: defaultInputVariant
}

private fun parseInputVariant(value: String?): MaterialInputVariant? =
    when (value?.lowercase()) {
        "standard" -> MaterialInputVariant.Standard
        "filled" -> MaterialInputVariant.Filled
        "outlined" -> MaterialInputVariant.Outlined
        else -> null
    }
