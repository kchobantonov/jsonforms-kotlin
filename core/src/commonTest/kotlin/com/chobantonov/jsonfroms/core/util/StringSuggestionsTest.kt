package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StringSuggestionsTest {
    private val options = Json.parseToJsonElement(
        """{"suggestion":["Portland","Seattle","Sacramento","Salt Lake City",7,"Seattle",""]}"""
    ).jsonObject

    @Test fun suggestionsFilterWithoutConstrainingInput() {
        assertEquals(listOf("Seattle"), matchingStringSuggestions(options, "sea"))
        assertEquals(listOf("Sacramento", "Salt Lake City"), matchingStringSuggestions(options, "Sa"))
        assertEquals(emptyList(), matchingStringSuggestions(options, "Boise"))
        assertEquals(emptyList(), matchingStringSuggestions(options, "Seattle"))
        assertEquals(emptyList(), matchingStringSuggestions(options, ""))
    }
}
