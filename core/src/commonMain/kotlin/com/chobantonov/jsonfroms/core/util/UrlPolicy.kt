package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Shared destination policy for Link, ImageView and future Markdown output. */
data class UrlPolicy(
    val allowedSchemes: Set<String> = setOf("https", "http", "mailto"),
    val allowRelative: Boolean = true,
    val allowImageDataUrls: Boolean = false,
)

private fun policyMember(container: Any?, key: String): Any? = when (container) {
    is Map<*, *> -> container[key]
    is JsonObject -> container[key]
    else -> null
}

private fun policyBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is JsonPrimitive -> value.booleanOrNull
    else -> null
}

private fun policySchemes(value: Any?): Set<String>? {
    val strings = when (value) {
        is JsonArray -> value.map { (it as? JsonPrimitive)?.takeIf { part -> part.isString }?.content ?: return null }
        is List<*> -> value.map { it as? String ?: return null }
        else -> return null
    }
    return strings.map { it.lowercase() }.toSet()
}

fun resolveUrlPolicy(config: Map<String, Any?>): UrlPolicy {
    val policy = policyMember(policyMember(config["jsonformsExtended"], "security"), "urlPolicy")
    val defaults = UrlPolicy()
    return UrlPolicy(
        allowedSchemes = policySchemes(policyMember(policy, "allowedSchemes")) ?: defaults.allowedSchemes,
        allowRelative = policyBoolean(policyMember(policy, "allowRelative")) ?: defaults.allowRelative,
        allowImageDataUrls = policyBoolean(policyMember(policy, "allowImageDataUrls")) ?: defaults.allowImageDataUrls,
    )
}

/** Rejects browser-normalized scheme tricks before consulting the configured allowlist. */
fun isAllowedUrl(url: String, policy: UrlPolicy = UrlPolicy()): Boolean {
    if (url.isEmpty()) return true
    if (url.any { it.code <= 0x20 || it.code == 0x7f || it == '\\' } || url.startsWith("//")) return false
    val colon = url.indexOf(':')
    val firstDelimiter = listOf(url.indexOf('/'), url.indexOf('?'), url.indexOf('#'))
        .filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
    if (colon < 0 || colon > firstDelimiter) return policy.allowRelative
    val scheme = url.substring(0, colon)
    if (!scheme.matches(Regex("[A-Za-z][A-Za-z0-9+.-]*"))) return false
    return scheme.lowercase() in policy.allowedSchemes
}

/** Image data URLs require a separate opt-in even when "data" is in allowedSchemes. */
fun isAllowedImageUrl(url: String, policy: UrlPolicy = UrlPolicy()): Boolean {
    if (!url.startsWith("data:", ignoreCase = true)) return isAllowedUrl(url, policy)
    if (!policy.allowImageDataUrls || url.any { it.code <= 0x20 || it.code == 0x7f }) return false
    val separator = url.indexOfAny(charArrayOf(';', ','), startIndex = 5)
    if (separator < 0 || url.indexOf(',', startIndex = separator) < 0) return false
    val mime = url.substring(5, separator).lowercase()
    return mime.matches(Regex("image/[a-z0-9.+-]+"))
}
