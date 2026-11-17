package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfirmationLocaleBundlesTest {
    @Test fun builtInCopyUsesEnglishAndBulgarianOnly() {
        assertEquals("Change this type?", translateConfirmationText(
            JsonFormsI18nState(locale = "en-US"), "confirm.typeChange.title", "Change this type?"))
        assertEquals("Промяна на типа?", translateConfirmationText(
            JsonFormsI18nState(locale = "bg-BG"), "confirm.typeChange.title", "Change this type?"))
        assertEquals("Изтриване на стойността?", translateConfirmationText(
            JsonFormsI18nState(locale = "bg"), "confirm.delete.title", "Delete this value?"))
        assertEquals("Delete this value?", translateConfirmationText(
            JsonFormsI18nState(locale = "de-DE"), "confirm.delete.title", "Delete this value?"))
    }

    @Test fun hostOverrideReceivesBulgarianDefault() {
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            assertEquals("confirm.accept", key)
            assertEquals("Да", fallback)
            "Потвърди"
        })
        assertEquals("Потвърди", translateConfirmationText(i18n, "confirm.accept", "Yes"))
    }
}
