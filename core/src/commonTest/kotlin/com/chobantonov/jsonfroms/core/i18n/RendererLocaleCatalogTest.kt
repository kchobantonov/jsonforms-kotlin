package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class RendererLocaleCatalogTest {
    private val english = Json.parseToJsonElement("""{
      "markup.unsupported":"Direct key",
      "markup":{"unsupported":"Nested key","disabled":"Disabled from bundle"},
      "constructor":"Literal key",
      "invalid":42
    }""") as JsonObject
    private val bulgarian = Json.parseToJsonElement("""{
      "markup":{"unsupported":"Неподдържан формат"}
    }""") as JsonObject

    @Test fun exactOwnKeyWinsAndNestedStringIsUsedWhenExactIsAbsent() {
        assertEquals("Direct key", lookupRendererCatalogString(english, "markup.unsupported"))
        assertEquals("Disabled from bundle", lookupRendererCatalogString(english, "markup.disabled"))
        assertEquals("Literal key", lookupRendererCatalogString(english, "constructor"))
        assertNull(lookupRendererCatalogString(english, "invalid"))
        assertNull(lookupRendererCatalogString(english, "markup.missing"))
    }

    @Test fun formCatalogOverridesLocaleBundleWhichOverridesDocumentedDefault() {
        val bundles = mapOf("en" to english, "bg" to bulgarian)
        var observed = ""
        val partialTranslator = Translator { key, fallback, _ ->
            observed = "$key=$fallback"
            if (key == "markup.unsupported") "Form text" else null
        }
        assertEquals("Form text", translateRendererText(partialTranslator, "en-US",
            "markup.unsupported", "Unsupported", bundles))
        assertEquals("markup.unsupported=Direct key", observed)
        assertEquals("Disabled from bundle", translateRendererText(partialTranslator, "en-GB",
            "markup.disabled", "Disabled", bundles))
        assertEquals("Неподдържан формат", translateRendererText(null, "bg_BG",
            "markup.unsupported", "Unsupported", bundles))
        assertEquals("Unsupported", translateRendererText(null, "fr-CA",
            "markup.unsupported", "Unsupported", bundles))
    }

    @Test fun regionalBundleFallsBackPerKeyToBaseLanguage() {
        val regional = Json.parseToJsonElement("""{"markup":{"other":"Regional"}}""") as JsonObject
        val bundles = mapOf("en-US" to regional, "en" to english)
        assertEquals("Regional", translateRendererText(null, "en-US",
            "markup.other", "Other", bundles))
        assertEquals("Direct key", translateRendererText(null, "en-US",
            "markup.unsupported", "Unsupported", bundles))
    }

    @Test fun scopedFormKeyUsesUnscopedRendererBundleKey() {
        var observed = ""
        val translator = Translator { key, fallback, _ ->
            observed = "$key=$fallback"
            null
        }
        val result = translateRendererText(translator, "en", "markup.unsupported",
            "Unsupported", mapOf("en" to english), i18nKeyPrefix = "account")
        assertEquals("Direct key", result)
        assertEquals("account.markup.unsupported=Direct key", observed)
    }
}
