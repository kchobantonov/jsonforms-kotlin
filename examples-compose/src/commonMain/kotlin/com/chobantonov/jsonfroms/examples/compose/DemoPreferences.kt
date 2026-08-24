package com.chobantonov.jsonfroms.examples.compose

import com.russhwolf.settings.Settings

internal class DemoPreferences(private val storage: Settings) {
    fun load(): DemoSettings = DemoSettings(
        themeMode = storage.enumValue(ThemeModeKey, DemoThemeMode.System),
        locale = storage.enumValue(LocaleKey, DemoLocale.English),
        demoLayout = storage.enumValue(LayoutKey, DemoLayout.Default),
    )

    fun save(settings: DemoSettings) {
        runCatching {
            storage.putString(ThemeModeKey, settings.themeMode.name)
            storage.putString(LocaleKey, settings.locale.name)
            storage.putString(LayoutKey, settings.demoLayout.name)
        }
    }

    private inline fun <reified T : Enum<T>> Settings.enumValue(key: String, default: T): T =
        runCatching { enumValueOf<T>(getString(key, default.name)) }.getOrDefault(default)

    private companion object {
        const val ThemeModeKey = "jsonforms-kotlin-demo-theme-mode"
        const val LocaleKey = "jsonforms-kotlin-demo-locale"
        const val LayoutKey = "jsonforms-kotlin-demo-layout"
    }
}
