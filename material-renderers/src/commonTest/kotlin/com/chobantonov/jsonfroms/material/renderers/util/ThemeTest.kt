package com.chobantonov.jsonfroms.material.renderers.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ThemeTest {
    @Test
    fun everyPresetProvidesDistinctLightAndDarkAccentColors() {
        MaterialThemePreset.entries.forEachIndexed { index, preset ->
            val light = materialColorScheme(darkTheme = false, preset)
            val dark = materialColorScheme(darkTheme = true, preset)

            assertNotEquals(light.primary, dark.primary)
            assertNotEquals(light.primary, light.primaryContainer)
            assertNotEquals(dark.primary, dark.primaryContainer)

            MaterialThemePreset.entries.drop(index + 1).forEach { other ->
                assertNotEquals(light.primary, materialColorScheme(false, other).primary)
                assertNotEquals(dark.primary, materialColorScheme(true, other).primary)
            }
        }
    }

    @Test
    fun purpleRemainsTheDefaultMaterialTheme() {
        listOf(false, true).forEach { darkTheme ->
            val explicit = materialColorScheme(darkTheme, MaterialThemePreset.Purple)
            val default = materialColorScheme(darkTheme)

            assertEquals(explicit.primary, default.primary)
            assertEquals(explicit.onPrimary, default.onPrimary)
            assertEquals(explicit.primaryContainer, default.primaryContainer)
            assertEquals(explicit.onPrimaryContainer, default.onPrimaryContainer)
        }
    }
}
