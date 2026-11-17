package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DemoActionLocaleTest {
    @Test
    fun demoOffersOnlyEnglishAndBulgarian() {
        assertEquals(listOf(DemoLocale.English, DemoLocale.Bulgarian), DemoLocale.entries)
    }

    @Test
    fun statusMessagesUseTheFormLocale() {
        val params = JsonObject(mapOf(
            "lang" to JsonPrimitive("bg"),
            "statusByLocale" to JsonObject(mapOf(
                "en" to JsonPrimitive("Language changed to Bulgarian"),
                "bg" to JsonPrimitive("Езикът е сменен на български"),
            )),
        ))

        assertEquals("Езикът е сменен на български",
            demoActionStatus(params, DemoLocale.Bulgarian))
    }

    @Test
    fun specSetLocaleActionIsRecognized() {
        val params = JsonObject(mapOf("locale" to JsonPrimitive("bg")))

        assertEquals(DemoLocale.Bulgarian, requestedDemoLocale("setLocale", params))
        assertEquals(DemoLocale.Bulgarian, requestedDemoLocale("changeLang",
            JsonObject(mapOf("lang" to JsonPrimitive("bg")))))
        assertEquals(DemoLocale.Bulgarian, requestedDemoLocale("setLocale",
            JsonObject(mapOf("locale" to JsonPrimitive("bg-BG")))))
        assertEquals(DemoLocale.English, requestedDemoLocale("changeLang",
            JsonObject(mapOf("lang" to JsonPrimitive("en_US")))))
        assertNull(requestedDemoLocale("changeLang",
            JsonObject(mapOf("lang" to JsonPrimitive("de")))))
        assertNull(requestedDemoLocale("setLocale",
            JsonObject(mapOf("locale" to JsonPrimitive("de-DE")))))
        assertNull(requestedDemoLocale("setLocale",
            JsonObject(mapOf("locale" to JsonPrimitive(123)))))
        assertNull(requestedDemoLocale("submit", params))
    }
}
