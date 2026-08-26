package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
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
                uiStyle = DemoUiStyle.Cupertino,
                lightMaterialTheme = MaterialThemePreset.Blue,
                darkMaterialTheme = MaterialThemePreset.Orange,
                lightCupertinoTheme = CupertinoThemePreset.Green,
                darkCupertinoTheme = CupertinoThemePreset.Purple,
                locale = DemoLocale.Bulgarian,
                demoLayout = DemoLayout.DemoAndData,
            ),
        )

        val loaded = preferences.load()
        assertEquals(DemoThemeMode.Dark, loaded.themeMode)
        assertEquals(DemoUiStyle.Cupertino, loaded.uiStyle)
        assertEquals(MaterialThemePreset.Blue, loaded.lightMaterialTheme)
        assertEquals(MaterialThemePreset.Orange, loaded.darkMaterialTheme)
        assertEquals(CupertinoThemePreset.Green, loaded.lightCupertinoTheme)
        assertEquals(CupertinoThemePreset.Purple, loaded.darkCupertinoTheme)
        assertEquals(DemoLocale.Bulgarian, loaded.locale)
        assertEquals(DemoLayout.DemoAndData, loaded.demoLayout)
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        val preferences = DemoPreferences(
            MapSettings(
                "jsonforms-kotlin-demo-theme-mode" to "Sepia",
                "jsonforms-kotlin-demo-ui-style" to "Windows",
                "jsonforms-kotlin-demo-light-material-theme" to "Sepia",
                "jsonforms-kotlin-demo-dark-material-theme" to "Neon",
                "jsonforms-kotlin-demo-light-cupertino-theme" to "Sepia",
                "jsonforms-kotlin-demo-dark-cupertino-theme" to "Neon",
                "jsonforms-kotlin-demo-locale" to "French",
                "jsonforms-kotlin-demo-layout" to "Unknown",
            ),
        )

        val loaded = preferences.load()
        assertEquals(DemoThemeMode.System, loaded.themeMode)
        assertEquals(DemoUiStyle.System, loaded.uiStyle)
        assertEquals(MaterialThemePreset.Purple, loaded.lightMaterialTheme)
        assertEquals(MaterialThemePreset.Purple, loaded.darkMaterialTheme)
        assertEquals(CupertinoThemePreset.Blue, loaded.lightCupertinoTheme)
        assertEquals(CupertinoThemePreset.Blue, loaded.darkCupertinoTheme)
        assertEquals(DemoLocale.English, loaded.locale)
        assertEquals(DemoLayout.Default, loaded.demoLayout)
    }

    @Test
    fun systemStyleFollowsPlatformWhileExplicitStylesOverrideIt() {
        assertEquals(DemoUiStyle.Cupertino, resolveUiStyle(DemoUiStyle.System, applePlatform = true))
        assertEquals(DemoUiStyle.Material, resolveUiStyle(DemoUiStyle.System, applePlatform = false))
        assertEquals(DemoUiStyle.Material, resolveUiStyle(DemoUiStyle.Material, applePlatform = true))
        assertEquals(DemoUiStyle.Cupertino, resolveUiStyle(DemoUiStyle.Cupertino, applePlatform = false))
    }
}
