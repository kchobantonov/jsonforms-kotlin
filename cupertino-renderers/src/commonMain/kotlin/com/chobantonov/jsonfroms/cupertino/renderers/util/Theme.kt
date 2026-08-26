package com.chobantonov.jsonfroms.cupertino.renderers.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.slapps.cupertino.theme.CupertinoTheme as LibraryCupertinoTheme
import com.slapps.cupertino.theme.darkColorScheme as libraryDarkColorScheme
import com.slapps.cupertino.theme.lightColorScheme as libraryLightColorScheme

@Immutable
data class CupertinoColors(
    val accent: Color,
    val background: Color,
    val secondaryBackground: Color,
    val tertiaryBackground: Color,
    val label: Color,
    val secondaryLabel: Color,
    val separator: Color,
    val destructive: Color,
    /** A higher-contrast outline for interactive controls. */
    val controlBorder: Color = separator,
)

enum class CupertinoThemePreset(val displayName: String) {
    Blue("Blue"),
    Indigo("Indigo"),
    Purple("Purple"),
    Green("Green"),
    Orange("Orange"),
    Pink("Pink"),
}

fun cupertinoColors(
    darkTheme: Boolean,
    preset: CupertinoThemePreset = CupertinoThemePreset.Blue,
): CupertinoColors {
    val accent = when (preset) {
        CupertinoThemePreset.Blue -> if (darkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
        CupertinoThemePreset.Indigo -> if (darkTheme) Color(0xFF5E5CE6) else Color(0xFF5856D6)
        CupertinoThemePreset.Purple -> if (darkTheme) Color(0xFFBF5AF2) else Color(0xFFAF52DE)
        CupertinoThemePreset.Green -> if (darkTheme) Color(0xFF30D158) else Color(0xFF34C759)
        CupertinoThemePreset.Orange -> if (darkTheme) Color(0xFFFF9F0A) else Color(0xFFFF9500)
        CupertinoThemePreset.Pink -> if (darkTheme) Color(0xFFFF375F) else Color(0xFFFF2D55)
    }
    return if (darkTheme) {
        CupertinoColors(
            accent = accent,
            background = Color.Black,
            secondaryBackground = Color(0xFF1C1C1E),
            tertiaryBackground = Color(0xFF2C2C2E),
            label = Color.White,
            secondaryLabel = Color(0xFFAEAEB2),
            separator = Color(0xFF38383A),
            destructive = Color(0xFFFF453A),
            // Native separators are translucent and disappear when used as full field outlines.
            controlBorder = Color(0xFF787880),
        )
    } else {
        CupertinoColors(
            accent = accent,
            background = Color(0xFFF2F2F7),
            secondaryBackground = Color.White,
            tertiaryBackground = Color(0xFFE5E5EA),
            label = Color.Black,
            secondaryLabel = Color(0xFF6C6C70),
            separator = Color(0xFFC6C6C8),
            destructive = Color(0xFFFF3B30),
            controlBorder = Color(0xFFAEAEB2),
        )
    }
}

private val LightColors = cupertinoColors(darkTheme = false)

val LocalCupertinoColors = staticCompositionLocalOf { LightColors }

@Composable
fun CupertinoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CupertinoTheme(darkTheme, CupertinoThemePreset.Blue, content)
}

@Composable
fun CupertinoTheme(
    darkTheme: Boolean,
    preset: CupertinoThemePreset,
    content: @Composable () -> Unit,
) {
    CupertinoTheme(darkTheme, cupertinoColors(darkTheme, preset), content)
}

@Composable
fun CupertinoTheme(
    darkTheme: Boolean,
    colors: CupertinoColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCupertinoColors provides colors) {
        LibraryCupertinoTheme(
            colorScheme = if (darkTheme) {
                libraryDarkColorScheme(
                    accent = colors.accent,
                    link = colors.accent,
                    separator = colors.separator,
                    opaqueSeparator = colors.controlBorder,
                    systemGroupedBackground = colors.background,
                    secondarySystemGroupedBackground = colors.secondaryBackground,
                    tertiarySystemGroupedBackground = colors.tertiaryBackground,
                    systemBackground = colors.background,
                    secondarySystemBackground = colors.secondaryBackground,
                    tertiarySystemBackground = colors.tertiaryBackground,
                )
            } else {
                libraryLightColorScheme(
                    accent = colors.accent,
                    link = colors.accent,
                    separator = colors.separator,
                    opaqueSeparator = colors.controlBorder,
                    systemGroupedBackground = colors.background,
                    secondarySystemGroupedBackground = colors.secondaryBackground,
                    tertiarySystemGroupedBackground = colors.tertiaryBackground,
                    systemBackground = colors.background,
                    secondarySystemBackground = colors.secondaryBackground,
                    tertiarySystemBackground = colors.tertiaryBackground,
                )
            },
            content = content,
        )
    }
}

@Composable
fun CupertinoAppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CupertinoAppTheme(darkTheme, CupertinoThemePreset.Blue, content)
}

@Composable
fun CupertinoAppTheme(
    darkTheme: Boolean,
    preset: CupertinoThemePreset,
    content: @Composable () -> Unit,
) {
    CupertinoAppTheme(darkTheme, cupertinoColors(darkTheme, preset), content)
}

@Composable
fun CupertinoAppTheme(
    darkTheme: Boolean,
    colors: CupertinoColors,
    content: @Composable () -> Unit,
) {
    val materialColors = cupertinoMaterialColorScheme(darkTheme, colors)
    CupertinoTheme(darkTheme, colors) {
        MaterialTheme(
            colorScheme = materialColors,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(10.dp),
                medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(22.dp),
            ),
            content = content,
        )
    }
}

internal fun cupertinoMaterialColorScheme(darkTheme: Boolean, colors: CupertinoColors): ColorScheme =
    if (darkTheme) darkColorScheme(
        primary = colors.accent, onPrimary = Color.White,
        primaryContainer = colors.tertiaryBackground, onPrimaryContainer = colors.label,
        secondary = colors.accent, secondaryContainer = colors.tertiaryBackground,
        onSecondaryContainer = colors.label, background = colors.background,
        onBackground = colors.label, surface = colors.secondaryBackground, onSurface = colors.label,
        surfaceDim = colors.background, surfaceBright = colors.secondaryBackground,
        surfaceContainerLowest = colors.background, surfaceContainerLow = colors.secondaryBackground,
        surfaceContainer = colors.secondaryBackground, surfaceContainerHigh = colors.secondaryBackground,
        surfaceContainerHighest = colors.tertiaryBackground,
        surfaceVariant = colors.tertiaryBackground, onSurfaceVariant = colors.secondaryLabel,
        outline = colors.controlBorder, outlineVariant = colors.separator, error = colors.destructive,
    ) else lightColorScheme(
        primary = colors.accent, onPrimary = Color.White,
        primaryContainer = colors.tertiaryBackground, onPrimaryContainer = colors.label,
        secondary = colors.accent, secondaryContainer = colors.tertiaryBackground,
        onSecondaryContainer = colors.label, background = colors.background,
        onBackground = colors.label, surface = colors.secondaryBackground, onSurface = colors.label,
        surfaceDim = colors.background, surfaceBright = colors.secondaryBackground,
        surfaceContainerLowest = colors.background, surfaceContainerLow = colors.secondaryBackground,
        surfaceContainer = colors.secondaryBackground, surfaceContainerHigh = colors.secondaryBackground,
        surfaceContainerHighest = colors.tertiaryBackground,
        surfaceVariant = colors.tertiaryBackground, onSurfaceVariant = colors.secondaryLabel,
        outline = colors.controlBorder, outlineVariant = colors.separator, error = colors.destructive,
    )
