package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal class DemoPreferences(private val storage: Settings) {
    fun load(): DemoSettings {
        val snapshot = runCatching { storage.getStringOrNull(SnapshotKey) }
            .getOrElse { return DemoSettings() }
        return if (snapshot == null) loadLegacy() else decode(snapshot)
    }

    private fun loadLegacy(): DemoSettings = DemoSettings(
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
            storage.putString(SnapshotKey, buildJsonObject {
                put("version", SnapshotVersion)
                put("themeMode", settings.themeMode.name)
                put("uiStyle", settings.uiStyle.name)
                put("lightMaterialTheme", settings.lightMaterialTheme.name)
                put("darkMaterialTheme", settings.darkMaterialTheme.name)
                put("lightCupertinoTheme", settings.lightCupertinoTheme.name)
                put("darkCupertinoTheme", settings.darkCupertinoTheme.name)
                put("locale", settings.locale.name)
                put("demoLayout", settings.demoLayout.name)
            }.toString())
        }
    }

    private fun decode(snapshot: String): DemoSettings {
        val values = runCatching { Json.parseToJsonElement(snapshot) as? JsonObject }.getOrNull()
            ?: return DemoSettings()
        if ((values["version"] as? JsonPrimitive)?.intOrNull != SnapshotVersion) return DemoSettings()
        return DemoSettings(
            themeMode = values.enumValue("themeMode", DemoThemeMode.System),
            uiStyle = values.enumValue("uiStyle", DemoUiStyle.System),
            lightMaterialTheme = values.enumValue("lightMaterialTheme", MaterialThemePreset.Purple),
            darkMaterialTheme = values.enumValue("darkMaterialTheme", MaterialThemePreset.Purple),
            lightCupertinoTheme = values.enumValue("lightCupertinoTheme", CupertinoThemePreset.Blue),
            darkCupertinoTheme = values.enumValue("darkCupertinoTheme", CupertinoThemePreset.Blue),
            locale = values.enumValue("locale", DemoLocale.English),
            demoLayout = values.enumValue("demoLayout", DemoLayout.Default),
        )
    }

    private inline fun <reified T : Enum<T>> JsonObject.enumValue(key: String, default: T): T {
        val name = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return default
        return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
    }

    private inline fun <reified T : Enum<T>> Settings.enumValue(key: String, default: T): T =
        runCatching { enumValueOf<T>(getString(key, default.name)) }.getOrDefault(default)

    private companion object {
        const val SnapshotVersion = 1
        const val SnapshotKey = "jsonforms-kotlin-demo-settings"
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
