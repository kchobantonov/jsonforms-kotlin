package com.chobantonov.jsonfroms.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class JsonPathTest {
    @Test
    fun resolvesAndUpdatesArrayItemsWithComposedDotPaths() {
        val data = buildJsonObject {
            put("comments", buildJsonArray {
                add(JsonPrimitive("one string"))
                add(JsonPrimitive("two strings"))
            })
        }
        val secondItemPath = composePaths("comments", "1")

        assertEquals("comments.1", secondItemPath)
        assertEquals(JsonPrimitive("two strings"), JsonPath.get(data, secondItemPath))

        val updated = JsonPath.set(data, secondItemPath, JsonPrimitive("updated"))
        assertEquals(JsonPrimitive("updated"), JsonPath.get(updated, secondItemPath))
    }
}
