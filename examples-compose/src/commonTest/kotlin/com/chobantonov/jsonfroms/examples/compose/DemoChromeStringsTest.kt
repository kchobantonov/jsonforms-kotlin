package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoChromeStringsTest {
    @Test
    fun shippedLocalesCoverEveryDemoTextKey() {
        val keys = DemoTextKey.entries.toSet()
        assertEquals(DemoLocale.entries.toSet(), demoChromeTranslations.keys)
        DemoLocale.entries.forEach { locale ->
            val translations = demoChromeTranslations.getValue(locale)
            assertEquals(keys, translations.keys, "Missing or extra demo strings for $locale")
            assertTrue(translations.values.all(String::isNotBlank))
        }
    }

    @Test
    fun lookupUsesSelectedLocale() {
        assertEquals("Show example menu", demoChromeText(DemoTextKey.ShowExampleMenu, DemoLocale.English))
        assertEquals("Покажи менюто с примери",
            demoChromeText(DemoTextKey.ShowExampleMenu, DemoLocale.Bulgarian))
        assertEquals("← Compose form", demoChromeText(DemoTextKey.BackToComposeForm, DemoLocale.English))
        assertEquals("← Compose формуляр", demoChromeText(DemoTextKey.BackToComposeForm, DemoLocale.Bulgarian))
        assertEquals("Web component", demoChromeText(DemoTextKey.WebComponentTitle, DemoLocale.English))
        assertEquals("Уеб компонент", demoChromeText(DemoTextKey.WebComponentTitle, DemoLocale.Bulgarian))
        assertEquals("Web component form", demoChromeText(DemoTextKey.WebComponentFormFrame, DemoLocale.English))
        assertEquals("Формуляр с уеб компонент", demoChromeText(DemoTextKey.WebComponentFormFrame, DemoLocale.Bulgarian))
    }
}
