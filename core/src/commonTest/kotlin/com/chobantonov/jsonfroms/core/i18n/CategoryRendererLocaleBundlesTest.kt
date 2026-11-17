package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryRendererLocaleBundlesTest {
    @Test fun regionalBulgarianAndHostOverride() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Съдържа грешки", translateCategoryValidationIndicator(bulgarian, null))
        assertEquals("1 грешка", translateCategoryValidationIndicator(bulgarian, 1))
        assertEquals("3 грешки", translateCategoryValidationIndicator(bulgarian, 3))
        assertEquals("3 errors", translateCategoryValidationIndicator(JsonFormsI18nState(locale = "en"), 3))
        val override = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("category.validationIndicator", key)
            assertEquals("{count} грешки", fallback)
            assertEquals(3, values["count"])
            "Host category errors"
        })
        assertEquals("Host category errors", translateCategoryValidationIndicator(override, 3))
    }
}
