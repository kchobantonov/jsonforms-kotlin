package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class TableValidationLocaleBundlesTest {
    @Test fun compositeAndRowLabelsUseBulgarianDefaults() {
        val i18n = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Contact съдържа грешки", translateCompositeValidationIndicator(i18n, "Contact", null))
        assertEquals("Contact: 1 грешка", translateCompositeValidationIndicator(i18n, "Contact", 1))
        assertEquals("Contact: 3 грешки", translateCompositeValidationIndicator(i18n, "Contact", 3))
        assertEquals("Елемент 2: 1 грешка", translateRowDetailValidationIndicator(i18n, 2, 1))
        assertEquals("Елемент 2: 3 грешки", translateRowDetailValidationIndicator(i18n, 2, 3))
        assertEquals("Contact: 3 errors", translateCompositeValidationIndicator(
            JsonFormsI18nState(locale = "en"), "Contact", 3))
    }

    @Test fun hostReceivesStableKeysAndLocalizedDefaults() {
        val composite = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("composite.validationIndicator", key)
            assertEquals("{label}: {count} грешки", fallback)
            assertEquals("Contact", values["label"])
            assertEquals(3, values["count"])
            "Host cell"
        })
        assertEquals("Host cell", translateCompositeValidationIndicator(composite, "Contact", 3))
        val row = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("rowDetail.validationIndicator", key)
            assertEquals("Елемент {index}: {count} грешки", fallback)
            assertEquals(2, values["index"])
            assertEquals(3, values["count"])
            "Host row"
        })
        assertEquals("Host row", translateRowDetailValidationIndicator(row, 2, 3))
    }
}
