package com.chobantonov.jsonfroms.examples.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DemoActionLocaleTest {
    @Test
    fun changeLangUsesRequestedLocaleForStatusOnFirstClick() {
        val params = JsonObject(mapOf(
            "lang" to JsonPrimitive("bg"),
            "statusByLocale" to JsonObject(mapOf(
                "en" to JsonPrimitive("Language changed to Bulgarian"),
                "bg" to JsonPrimitive("Езикът е сменен на български"),
            )),
        ))

        val requestedLocale = requestedDemoLocale("changeLang", params)
        assertEquals(DemoLocale.Bulgarian, requestedLocale)
        assertEquals("Езикът е сменен на български", demoActionStatus(params, requestedLocale!!))
    }

    @Test
    fun specSetLocaleActionIsRecognized() {
        val params = JsonObject(mapOf("locale" to JsonPrimitive("bg")))

        assertEquals(DemoLocale.Bulgarian, requestedDemoLocale("setLocale", params))
        assertNull(requestedDemoLocale("submit", params))
    }
}
