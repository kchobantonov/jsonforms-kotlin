package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import kotlin.test.Test
import kotlin.test.assertEquals

class PresentationRendererStringsTest {
    @Test fun regionalLocalesAndTranslatorOverrides() {
        assertEquals("Image blocked", translateRendererText(null, "en-US", "image.blocked",
            "Image blocked", presentationLocaleBundles))
        assertEquals("Опитай отново", translateRendererText(null, "bg-BG", "image.retry",
            "Retry image", presentationLocaleBundles))
        assertEquals("Image blocked", translateRendererText(null, "fr-FR", "image.blocked",
            "Image blocked", presentationLocaleBundles))
        assertEquals("Custom", translateRendererText({ key, fallback, _ ->
            if (key == "image.blocked") "Custom" else fallback
        }, "en-US", "image.blocked", "Image blocked", presentationLocaleBundles))
    }

    @Test fun presentationDiagnosticKeysHaveLocalizedDefaults() {
        val keys = listOf("link.urlRefused", "link.invalidTarget", "image.noSource",
            "image.scopeNotString", "image.nonStringSource", "image.urlRefused",
            "image.missingAlt", "image.loadFailed", "image.blocked", "image.unavailable", "image.retry")
        for (language in listOf("bg", "bg-BG")) for (key in keys) {
            val localized = translateRendererText(null, language, key, "English default", presentationLocaleBundles)
            assertEquals(false, localized == "English default", "$language: $key")
        }
    }

    @Test fun pendingEditActionStatusUsesOnlyShippedLocales() {
        val key = "button.pendingEdit"
        val fallback = "Finish the pending edit before running this action."
        assertEquals(fallback, translateRendererText(null, "en-US", key, fallback, presentationLocaleBundles))
        assertEquals("Завърши текущата редакция, преди да изпълниш действието.",
            translateRendererText(null, "bg-BG", key, fallback, presentationLocaleBundles))
        assertEquals(fallback, translateRendererText(null, "fr-FR", key, fallback, presentationLocaleBundles))
    }
}
