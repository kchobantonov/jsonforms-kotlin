package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

/** Hosts may supply a painter for a semantic identifier; null keeps the portable glyph/fallback. */
typealias InputAdornmentIconResolver = @Composable (String) -> Painter?

val LocalInputAdornmentIconResolver = compositionLocalOf<InputAdornmentIconResolver?> { null }

/** Portable symbols for the semantic icon identifiers used by the input-adornment spec fixture. */
fun inputAdornmentGlyph(icon: String?): String? = when (icon) {
    "information" -> "ⓘ"
    "account" -> "◉"
    "schedule" -> "◷"
    "lock" -> "🔒"
    else -> null
}
