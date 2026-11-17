package com.chobantonov.jsonfroms.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class UrlPolicyTest {
    @Test fun defaultPolicyMatchesPortableProfile() {
        assertEquals(setOf("https", "http", "mailto"), resolveUrlPolicy(emptyMap()).allowedSchemes)
        assertTrue(resolveUrlPolicy(emptyMap()).allowRelative)
        assertFalse(resolveUrlPolicy(emptyMap()).allowImageDataUrls)
        listOf("https://example.com", "http://example.com", "mailto:a@b.c",
            "/help", "help", "./help", "../help", "?q=1", "#anchor", "").forEach {
            assertTrue(isAllowedUrl(it), it)
        }
        listOf("javascript:alert(1)", "JavaScript:alert(1)", "data:text/html,<script>",
            "file:///etc/passwd", "//evil.example.com", "java\nscript:alert(1)",
            "java\tscript:alert(1)", "  javascript:alert(1)", "\\\\evil.example.com").forEach {
            assertFalse(isAllowedUrl(it), it)
        }
    }

    @Test fun namespacedConfigControlsSchemesAndRelativeUrls() {
        val policy = resolveUrlPolicy(mapOf("jsonformsExtended" to mapOf(
            "security" to mapOf("urlPolicy" to mapOf(
                "allowedSchemes" to listOf("HTTPS"),
                "allowRelative" to false,
            )),
        )))
        assertEquals(setOf("https"), policy.allowedSchemes)
        assertTrue(isAllowedUrl("https://example.com", policy))
        assertFalse(isAllowedUrl("http://example.com", policy))
        assertFalse(isAllowedUrl("/help", policy))
        assertEquals(UrlPolicy(), resolveUrlPolicy(mapOf("security" to mapOf(
            "urlPolicy" to mapOf("allowedSchemes" to listOf("file")),
        ))))
    }

    @Test fun jsonConfigAndImageDataGateUseSamePolicy() {
        val json = Json.parseToJsonElement(
            """{"security":{"urlPolicy":{"allowedSchemes":["https","data"],"allowRelative":false,"allowImageDataUrls":true}}}"""
        ) as JsonObject
        val policy = resolveUrlPolicy(mapOf("jsonformsExtended" to json))
        assertFalse(isAllowedImageUrl("/image.png", policy))
        assertTrue(isAllowedImageUrl("https://example.com/image.png", policy))
        assertTrue(isAllowedImageUrl("data:image/png;base64,YQ==", policy))
        assertTrue(isAllowedImageUrl("data:image/svg+xml,<svg></svg>", policy))
        assertFalse(isAllowedImageUrl("data:text/html,<script>", policy))
        assertFalse(isAllowedImageUrl("data:image/png;base64,YQ==",
            policy.copy(allowImageDataUrls = false)))
        assertFalse(isAllowedImageUrl("data:text/html,<script>",
            policy.copy(allowedSchemes = setOf("https", "data"))))
    }
}
