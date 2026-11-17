package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.cupertino.renderers.util.CupertinoThemePreset
import com.chobantonov.jsonfroms.material.renderers.util.MaterialThemePreset
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.intOrNull
import kotlin.test.assertTrue

class DemoPreferencesTest {
    @Test
    fun unavailableStorageDoesNotPreventDefaultsOrSaving() {
        val blocked = object : Settings by MapSettings() {
            override fun getStringOrNull(key: String): String? = error("storage unavailable")
            override fun putString(key: String, value: String) { error("storage unavailable") }
        }
        val preferences = DemoPreferences(blocked)
        assertEquals(DemoSettings(), preferences.load())
        preferences.save(DemoSettings(locale = DemoLocale.Bulgarian))
    }

    @Test
    fun versionedSnapshotContainsOnlyDurableVisualPreferences() {
        val storage = MapSettings()
        DemoPreferences(storage).save(DemoSettings(
            locale = DemoLocale.Bulgarian,
            formLocaleOverride = DemoLocale.English,
            formDarkThemeOverride = true,
            direction = DemoDirection.Rtl,
            validationMode = com.chobantonov.jsonfroms.core.store.ValidationMode.NoValidation,
            readonly = true,
            enableFilterErrorsBeforeTouch = true,
        ))
        assertEquals(setOf("jsonforms-kotlin-demo-settings"), storage.keys)
        val values = Json.parseToJsonElement(storage.getString("jsonforms-kotlin-demo-settings", "")).jsonObject
        assertEquals(1, (values["version"] as JsonPrimitive).intOrNull)
        assertEquals("Bulgarian", (values["locale"] as JsonPrimitive).content)
        assertTrue(listOf("direction", "validationMode", "readonly", "enableFilterErrorsBeforeTouch",
            "formLocaleOverride", "formDarkThemeOverride")
            .none(values::containsKey))
        assertEquals(null, DemoPreferences(storage).load().formLocaleOverride)
        assertEquals(null, DemoPreferences(storage).load().formDarkThemeOverride)
    }

    @Test
    fun corruptOrFutureSnapshotUsesDefaultsInsteadOfStaleLegacyValues() {
        val storage = MapSettings("jsonforms-kotlin-demo-locale" to "Bulgarian")
        for (snapshot in listOf("{broken", "{}", """{"version":2,"locale":"Bulgarian"}""")) {
            storage.putString("jsonforms-kotlin-demo-settings", snapshot)
            assertEquals(DemoSettings(), DemoPreferences(storage).load())
        }
        storage.putString("jsonforms-kotlin-demo-settings", """{"version":1,"locale":"German","themeMode":"Dark"}""")
        val loaded = DemoPreferences(storage).load()
        assertEquals(DemoLocale.English, loaded.locale)
        assertEquals(DemoThemeMode.Dark, loaded.themeMode)
    }

    @Test
    fun legacyPreferencesLoadUntilTheFirstVersionedSave() {
        val storage = MapSettings(
            "jsonforms-kotlin-demo-locale" to "Bulgarian",
            "jsonforms-kotlin-demo-theme-mode" to "Dark",
        )
        val preferences = DemoPreferences(storage)
        val legacy = preferences.load()
        assertEquals(DemoLocale.Bulgarian, legacy.locale)
        assertEquals(DemoThemeMode.Dark, legacy.themeMode)
        preferences.save(legacy)
        storage.putString("jsonforms-kotlin-demo-locale", "English")
        assertEquals(DemoLocale.Bulgarian, preferences.load().locale)
    }

    @Test
    fun sessionErrorAndAdditionalPropertySettingsPreserveAuthoredValuesUntilChanged() {
        val authored = mapOf<String, Any?>(
            "enableFilterErrorsBeforeTouch" to true,
            "filterErrorKeywordsBeforeTouch" to listOf("required"),
            "allowAdditionalPropertiesIfMissing" to true,
        )
        assertEquals(authored, DemoSettings().applyTo(authored).filterKeys { it in authored })
        val changed = DemoSettings(
            enableFilterErrorsBeforeTouch = false,
            filterErrorKeywordsBeforeTouch = emptyList(),
            allowAdditionalPropertiesByDefault = false,
        ).applyTo(authored)
        assertEquals(false, changed["enableFilterErrorsBeforeTouch"])
        assertEquals(emptyList<String>(), changed["filterErrorKeywordsBeforeTouch"])
        assertEquals(false, changed["allowAdditionalPropertiesIfMissing"])
        assertEquals(listOf("required", "pattern"), parseErrorKeywords(" required, pattern, required, "))
        assertEquals(listOf("required"), configuredErrorKeywords(mapOf(
            "filterErrorKeywordsBeforeTouch" to JsonArray(listOf(JsonPrimitive("required"))),
        )))

        val storage = MapSettings()
        DemoPreferences(storage).save(DemoSettings(
            enableFilterErrorsBeforeTouch = true,
            filterErrorKeywordsBeforeTouch = listOf("required"),
            allowAdditionalPropertiesByDefault = true,
        ))
        val loaded = DemoPreferences(storage).load()
        assertEquals(null, loaded.enableFilterErrorsBeforeTouch)
        assertEquals(null, loaded.filterErrorKeywordsBeforeTouch)
        assertEquals(null, loaded.allowAdditionalPropertiesByDefault)
    }

    @Test
    fun demoOffersOnlyEnglishAndBulgarian() {
        assertEquals(listOf(DemoLocale.English, DemoLocale.Bulgarian), DemoLocale.entries)
    }

    @Test
    fun retiredBrowserLanguagePreferenceFallsBackToEnglish() {
        val storage = MapSettings()
        storage.putString("jsonforms-kotlin-demo-locale", "BrowserLanguage")
        assertEquals(DemoLocale.English, DemoPreferences(storage).load().locale)
    }

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
                direction = DemoDirection.Rtl,
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
        assertEquals(DemoDirection.Ltr, loaded.direction)
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
