package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupRendererLocaleBundlesTest {
    @Test fun validationIndicatorUsesBulgarianDefaultsAndHostOverride() {
        val bulgarian = JsonFormsI18nState(locale = "bg-BG")
        assertEquals("Съдържа грешки", translateGroupValidationIndicator(bulgarian, null))
        assertEquals("1 грешка", translateGroupValidationIndicator(bulgarian, 1))
        assertEquals("3 грешки", translateGroupValidationIndicator(bulgarian, 3))
        assertEquals("3 errors", translateGroupValidationIndicator(JsonFormsI18nState(locale = "en"), 3))
        val override = JsonFormsI18nState(locale = "bg", translate = { key, fallback, values ->
            assertEquals("group.validationIndicator", key)
            assertEquals(3, values["count"])
            assertEquals("{count} грешки", fallback)
            "Host errors"
        })
        assertEquals("Host errors", translateGroupValidationIndicator(override, 3))
    }

    @Test fun regionalDefaultsAndDocumentedFallback() {
        val keys = mapOf("group.label" to "Group", "group.expanded" to "Expanded",
            "group.collapsed" to "Collapsed", "group.dataIndicator" to "Contains data")
        for (locale in listOf("bg", "bg-BG")) for ((key, default) in keys) {
            assertEquals(false, translateGroupRendererText(JsonFormsI18nState(locale = locale),
                key, default) == default, "$locale: $key")
        }
        assertEquals("Collapsed", translateGroupRendererText(JsonFormsI18nState(locale = "fr"),
            "group.collapsed", "Collapsed"))
    }

    @Test fun formTranslatorOverridesSelectedBundleDefault() {
        var received: String? = null
        val i18n = JsonFormsI18nState(locale = "bg-BG", translate = { key, fallback, _ ->
            assertEquals("group.collapsed", key)
            received = fallback
            "Host collapsed"
        })
        assertEquals("Host collapsed", translateGroupRendererText(i18n, "group.collapsed", "Collapsed"))
        assertEquals("Свита", received)
    }
}
