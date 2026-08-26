package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.examples.StateProps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject

class ExampleI18nTest {
    private val state = StateProps(
        data = JsonObject(emptyMap()),
        localizedTranslations = mapOf(
            "en" to mapOf("field.label" to "Field"),
            "de" to mapOf("field.label" to "Feld"),
            "bg" to mapOf("field.label" to "Поле"),
        ),
    )

    @Test
    fun selectsTranslationsByLanguage() {
        assertEquals("Field", translate("en", "field.label"))
        assertEquals("Feld", translate("de-DE", "field.label"))
        assertEquals("Поле", translate("bg", "field.label"))
    }

    @Test
    fun fallsBackToDefaultMessageForUnknownLocaleOrKey() {
        assertEquals("Default", translate("fr", "field.label"))
        assertEquals("Default", translate("de", "missing"))
    }

    private fun translate(locale: String, key: String): String? =
        exampleI18nForLocale(state, locale).translate?.translate(key, "Default", emptyMap()) ?: "Default"
}
