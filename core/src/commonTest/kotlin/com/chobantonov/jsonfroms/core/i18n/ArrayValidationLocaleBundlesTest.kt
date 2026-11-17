package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayValidationLocaleBundlesTest {
    @Test fun summaryAndItemUseRegionalBulgarianDefaults() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Съдържа грешки", translateArrayValidationIndicator(bulgarian, null))
        assertEquals("1 грешка", translateArrayValidationIndicator(bulgarian, 1))
        assertEquals("3 грешки", translateArrayValidationIndicator(bulgarian, 3))
        assertEquals("Елемент 2: съдържа грешки", translateArrayValidationIndicator(bulgarian, null, 2))
        assertEquals("Елемент 2: 1 грешка", translateArrayValidationIndicator(bulgarian, 1, 2))
        assertEquals("Елемент 2: 3 грешки", translateArrayValidationIndicator(bulgarian, 3, 2))
        assertEquals("Item 2: 3 errors", translateArrayValidationIndicator(JsonFormsI18nState(locale = "en"), 3, 2))
    }

    @Test fun hostOverrideReceivesStableKeyAndLocalizedDefault() {
        val i18n = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("array.item.validationIndicator", key)
            assertEquals("Елемент {index}: {count} грешки", fallback)
            assertEquals(2, values["index"])
            assertEquals(3, values["count"])
            "Host array errors"
        })
        assertEquals("Host array errors", translateArrayValidationIndicator(i18n, 3, 2))
    }
}
