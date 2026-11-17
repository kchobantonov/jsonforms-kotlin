package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.examples.StateProps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonObject

class ExampleI18nTest {
    private val state = StateProps(
        data = JsonObject(emptyMap()),
        localizedTranslations = mapOf(
            "en" to mapOf("field.label" to "Field", "validation.showMore" to "Show {count} more"),
            "bg" to mapOf("field.label" to "Поле", "validation.showMore" to "Покажи още {count}"),
        ),
    )

    @Test
    fun selectsTranslationsByLanguage() {
        assertEquals("Field", translate("en", "field.label"))
        assertEquals("Поле", translate("bg-BG", "field.label"))
        assertEquals("Поле", translate("bg", "field.label"))
    }

    @Test
    fun fallsBackToDefaultMessageForUnknownLocaleOrKey() {
        assertEquals("Default", translate("fr", "field.label"))
        assertEquals("Default", translate("bg", "missing"))
    }

    @Test
    fun substitutesScalarCatalogParametersAndPreservesUnknownBraces() {
        assertEquals("Show 2 more", exampleI18nForLocale(state, "en").translate
            ?.translate("validation.showMore", null, mapOf("count" to 2)))
        assertEquals("Покажи още 2", exampleI18nForLocale(state, "bg-BG").translate
            ?.translate("validation.showMore", null, mapOf("count" to 2)))
        assertEquals("Keep {unknown}", exampleI18nForLocale(state, "en").translate
            ?.translate("missing", "Keep {unknown}", emptyMap()))
    }

    private fun translate(locale: String, key: String): String? =
        exampleI18nForLocale(state, locale).translate?.translate(key, "Default", emptyMap()) ?: "Default"
}
