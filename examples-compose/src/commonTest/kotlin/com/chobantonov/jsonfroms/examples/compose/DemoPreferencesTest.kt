package com.chobantonov.jsonfroms.examples.compose

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoPreferencesTest {
    @Test
    fun preferencesRoundTrip() {
        val storage = MapSettings()
        val preferences = DemoPreferences(storage)

        preferences.save(
            DemoSettings(
                themeMode = DemoThemeMode.Dark,
                locale = DemoLocale.Bulgarian,
                demoLayout = DemoLayout.DemoAndData,
            ),
        )

        val loaded = preferences.load()
        assertEquals(DemoThemeMode.Dark, loaded.themeMode)
        assertEquals(DemoLocale.Bulgarian, loaded.locale)
        assertEquals(DemoLayout.DemoAndData, loaded.demoLayout)
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        val preferences = DemoPreferences(
            MapSettings(
                "jsonforms-kotlin-demo-theme-mode" to "Sepia",
                "jsonforms-kotlin-demo-locale" to "French",
                "jsonforms-kotlin-demo-layout" to "Unknown",
            ),
        )

        val loaded = preferences.load()
        assertEquals(DemoThemeMode.System, loaded.themeMode)
        assertEquals(DemoLocale.English, loaded.locale)
        assertEquals(DemoLayout.Default, loaded.demoLayout)
    }
}
