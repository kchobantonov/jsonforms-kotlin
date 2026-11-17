package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class TupleLocaleBundlesTest {
    @Test fun validationIndicatorHasBulgarianCountForms() {
        val i18n = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Съдържа грешки", translateTupleValidationIndicator(i18n, null))
        assertEquals("1 грешка", translateTupleValidationIndicator(i18n, 1))
        assertEquals("2 грешки", translateTupleValidationIndicator(i18n, 2))
        assertEquals("2 errors", translateTupleValidationIndicator(JsonFormsI18nState(locale = "en"), 2))
        val override = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("tuple.validationIndicator", key)
            assertEquals("{count} грешки", fallback)
            assertEquals(2, values["count"])
            "Host tuple errors"
        })
        assertEquals("Host tuple errors", translateTupleValidationIndicator(override, 2))
    }

    @Test fun englishAndBulgarianDefaultsFormatPositionAndAction() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        val position = translateTupleRendererText(bulgarian, "tuple.position", "Item 2",
            mapOf("position" to 2))
        assertEquals("Елемент 2", position)
        assertEquals("Изтрий Елемент 2", translateTupleRendererText(bulgarian,
            "tuple.delete", "Delete $position", mapOf("label" to position)))
        assertEquals("Добави елемент", translateTupleRendererText(bulgarian,
            "tuple.add", "Add item"))
        assertEquals("Add item", translateTupleRendererText(JsonFormsI18nState(locale = "en-US"),
            "tuple.add", "Add item"))
        assertEquals("Add item", translateTupleRendererText(JsonFormsI18nState(locale = "de-DE"),
            "tuple.add", "Add item"))
    }

    @Test fun hostTranslatorReceivesLocalizedDefaultAndWins() {
        val state = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("tuple.add", key)
            assertEquals("Добави елемент", fallback)
            assertEquals(emptyMap(), values)
            "Добавяне"
        })
        assertEquals("Добавяне", translateTupleRendererText(state, "tuple.add", "Add item"))
    }

    @Test fun complexPositionUsesBulgarianDefaultsWithoutHostTranslator() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Редактирай Адрес", translateTupleRendererText(bulgarian,
            "tuple.edit", "Edit Address", mapOf("label" to "Адрес")))
        assertEquals("(+2 още)", translateTupleRendererText(bulgarian,
            "composite.summary.more", "(+2 more)", mapOf("count" to 2)))
        assertEquals("3 елемента", translateTupleRendererText(bulgarian,
            "composite.summary.items", "3 items", mapOf("count" to 3)))
        assertEquals("View details", translateTupleRendererText(
            JsonFormsI18nState(locale = "en"), "composite.summary.details", "View details"))
    }
}
