package com.chobantonov.jsonfroms.material.renderers.complex

import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class CombinatorPropertiesTest {
    @Test
    fun `oneOf-only schema has no separately renderable properties`() {
        val schema = buildJsonSchema {
            put("oneOf", buildJsonArray {
                add(buildJsonObject { put("type", "string") })
                add(buildJsonObject { put("type", "number") })
            })
        }

        assertNull(schemaWithoutCombinatorProperties(schema, "oneOf"))
    }

    @Test
    fun `anyOf-only schema has no separately renderable properties`() {
        val schema = buildJsonSchema {
            put("anyOf", buildJsonArray {
                add(buildJsonObject { put("type", "string") })
                add(buildJsonObject { put("type", "number") })
            })
        }

        assertNull(schemaWithoutCombinatorProperties(schema, "anyOf"))
    }

    @Test
    fun `properties next to combinator are retained`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string") })
            })
            put("oneOf", buildJsonArray {
                add(buildJsonObject { put("type", "object") })
            })
        }

        val remainder = assertNotNull(schemaWithoutCombinatorProperties(schema, "oneOf"))

        assertEquals(
            setOf("name"),
            remainder.toJsonObject()["properties"]!!.jsonObject.keys,
        )
        assertNull(remainder.toJsonObject()["oneOf"])
    }
}
