package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

    @Test
    fun treatsBracketCharactersAsPartOfAPropertyName() {
        val data = buildJsonObject {
            put("prop[0]", "before")
        }

        assertEquals(JsonPrimitive("before"), JsonPath.get(data, "prop[0]"))

        val updated = JsonPath.set(data, "prop[0]", JsonPrimitive("after"))
        assertEquals(JsonPrimitive("after"), updated.jsonObject["prop[0]"])
    }

    @Test
    fun treatsNumericSegmentAsAPropertyNameInsideAnObject() {
        val data = buildJsonObject {
            put("15", "before")
        }

        assertEquals(JsonPrimitive("before"), JsonPath.get(data, "15"))

        val updated = JsonPath.set(data, "15", JsonPrimitive("after"))
        assertEquals(JsonPrimitive("after"), updated.jsonObject["15"])
    }

    @Test
    fun usesSchemaToCreateMissingArrayContainers() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("comments", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("type", "string")
                    })
                })
            })
        })

        val updated = JsonPath.set(
            root = buildJsonObject { },
            path = "comments.0",
            value = JsonPrimitive("first"),
            schema = schema,
        )

        assertEquals(JsonPrimitive("first"), updated.jsonObject["comments"]?.jsonArray?.get(0))
    }
}
