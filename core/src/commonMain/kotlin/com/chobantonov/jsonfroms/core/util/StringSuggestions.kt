package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Authored suggestions are hints for a free-form string, not enum constraints. */
fun matchingStringSuggestions(options: Map<String, JsonElement>, input: String): List<String> {
    if (input.isBlank()) return emptyList()
    val authored = options["suggestion"] as? JsonArray ?: return emptyList()
    return authored.mapNotNull { element ->
        (element as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
    }.filter { it.isNotBlank() && !it.equals(input, ignoreCase = true) &&
        it.contains(input, ignoreCase = true) }.distinct().take(8)
}
