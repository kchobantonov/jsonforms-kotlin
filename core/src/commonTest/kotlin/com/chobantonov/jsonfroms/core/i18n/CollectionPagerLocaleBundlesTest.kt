package com.chobantonov.jsonfroms.core.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionPagerLocaleBundlesTest {
    @Test fun bulgarianNavigationAndCountForms() {
        assertEquals("Размер на страница", translateCollectionPagerText(null, "bg-BG",
            "collection.pageSize", "Page size"))
        assertEquals("Предишна страница", translateCollectionPagerText(null, "bg-BG",
            "collection.previous", "Previous page"))
        assertEquals("Следваща страница", translateCollectionPagerText(null, "bg-BG",
            "collection.next", "Next page"))
        assertEquals("2 от 4", translateCollectionPagerText(null, "bg-BG", "collection.page",
            "2 of 4", mapOf("current" to 2, "pages" to 4)))
        assertEquals("Към първата грешка", translateCollectionPagerText(null, "bg-BG",
            "collection.firstError", "Go to first error"))
        assertEquals("Страница 3: 1 грешка", translateCollectionErrorPage(null, "bg-BG", 3, 1))
        assertEquals("Страница 3: 2 грешки", translateCollectionErrorPage(null, "bg-BG", 3, 2))
        assertEquals("Page 3: 1 error", translateCollectionErrorPage(null, "en", 3, 1))
    }

    @Test fun hostReceivesStableErrorPageKeyAndLocalizedDefault() {
        val translator = com.chobantonov.jsonfroms.core.store.Translator { key, fallback, values ->
            assertEquals("collection.errorPage", key)
            assertEquals("Страница {page}: {count} грешки", fallback)
            assertEquals(3, values["page"])
            assertEquals(2, values["count"])
            "Host page"
        }
        assertEquals("Host page", translateCollectionErrorPage(translator, "bg", 3, 2))
    }
}
