package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.russhwolf.settings.Settings

internal class DemoPreferences(private val storage: Settings) {
    fun load(): DemoSettings = DemoSettings(
        themeMode = storage.enumValue(ThemeModeKey, DemoThemeMode.System),
        uiStyle = storage.enumValue(UiStyleKey, DemoUiStyle.System),
        lightMaterialTheme = storage.enumValue(LightMaterialThemeKey, MaterialThemePreset.Purple),
        darkMaterialTheme = storage.enumValue(DarkMaterialThemeKey, MaterialThemePreset.Purple),
        lightCupertinoTheme = storage.enumValue(LightCupertinoThemeKey, CupertinoThemePreset.Blue),
        darkCupertinoTheme = storage.enumValue(DarkCupertinoThemeKey, CupertinoThemePreset.Blue),
        locale = storage.enumValue(LocaleKey, DemoLocale.English),
        demoLayout = storage.enumValue(LayoutKey, DemoLayout.Default),
    )

    fun save(settings: DemoSettings) {
        runCatching {
            storage.putString(ThemeModeKey, settings.themeMode.name)
            storage.putString(UiStyleKey, settings.uiStyle.name)
            storage.putString(LightMaterialThemeKey, settings.lightMaterialTheme.name)
            storage.putString(DarkMaterialThemeKey, settings.darkMaterialTheme.name)
            storage.putString(LightCupertinoThemeKey, settings.lightCupertinoTheme.name)
            storage.putString(DarkCupertinoThemeKey, settings.darkCupertinoTheme.name)
            storage.putString(LocaleKey, settings.locale.name)
            storage.putString(LayoutKey, settings.demoLayout.name)
        }
    }

    private inline fun <reified T : Enum<T>> Settings.enumValue(key: String, default: T): T =
        runCatching { enumValueOf<T>(getString(key, default.name)) }.getOrDefault(default)

    private companion object {
        const val ThemeModeKey = "jsonforms-kotlin-demo-theme-mode"
        const val UiStyleKey = "jsonforms-kotlin-demo-ui-style"
        const val LightMaterialThemeKey = "jsonforms-kotlin-demo-light-material-theme"
        const val DarkMaterialThemeKey = "jsonforms-kotlin-demo-dark-material-theme"
        const val LightCupertinoThemeKey = "jsonforms-kotlin-demo-light-cupertino-theme"
        const val DarkCupertinoThemeKey = "jsonforms-kotlin-demo-dark-cupertino-theme"
        const val LocaleKey = "jsonforms-kotlin-demo-locale"
        const val LayoutKey = "jsonforms-kotlin-demo-layout"
    }
}
