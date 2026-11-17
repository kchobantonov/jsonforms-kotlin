package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals

class RowDetailLocaleBundlesTest {
    @Test fun bulgarianDefaultsAndUnsupportedLocaleFallback() {
        assertEquals("Покажи подробностите",
            translateRowDetailText(null, "bg-BG", "rowDetail.show", "Show details"))
        assertEquals("Подробности за елемент 3",
            translateRowDetailText(null, "bg", "rowDetail.title", "Item 3 details",
                mapOf("index" to 3)))
        assertEquals("Затвори",
            translateRowDetailText(null, "bg", "rowDetail.cancel", "Close",
                bundleKey = "rowDetail.close"))
        assertEquals("Show details",
            translateRowDetailText(null, "de-DE", "rowDetail.show", "Show details"))
    }

    @Test fun hostTranslationKeepsStableKeyAndReceivesValues() {
        var requested = ""
        val translator = Translator { key, fallback, values ->
            requested = key
            if (key == "rowDetail.title") "Host item {index}" else fallback
        }
        assertEquals("Host item 4", translateRowDetailText(translator, "bg", "rowDetail.title",
            "Item 4 details", mapOf("index" to 4)))
        assertEquals("rowDetail.title", requested)
    }
}
