package com.chobantonov.jsonfroms.core

import com.chobantonov.jsonfroms.core.util.JsonPointer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class JsonPointerTest {
    @Test
    fun resolvesNestedObjectPointers() {
        val payload = buildJsonObject {
            put("person", buildJsonObject {
                put("name", "Ada")
            })
        }

        val resolved = JsonPointer.resolve(payload, "#/person/name")

        assertEquals("\"Ada\"", resolved.toString())
    }

    @Test
    fun resolvesArrayPointers() {
        val payload = buildJsonObject {
            put("tags", buildJsonArray {
                add(JsonPrimitive("one"))
                add(JsonPrimitive("two"))
            })
        }

        val resolved = JsonPointer.resolve(payload, "#/tags/1")

        assertEquals("\"two\"", resolved.toString())
    }
}
