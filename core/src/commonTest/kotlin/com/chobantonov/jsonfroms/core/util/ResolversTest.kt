package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ResolversTest {
    @Test
    fun `resolveSchema - resolves refs`() {
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
            put("type", "object")
            put("properties", buildJsonObject {
                put("foos", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject {
                        put("\$ref", "#/definitions/foo")
                    })
                })
            })
        }

        assertEquals(
            JsonSchema(buildJsonObject { put("type", "string") }),
            Resolve.schema(schema, "#/properties/foos/items", schema),
        )
    }

    @Test
    fun `resolveSchema - finds combinator children via simple property path`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("description", buildJsonObject {
                    put("oneOf", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("name", buildJsonObject { put("type", "string") })
                            })
                        })
                        add(buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("index", buildJsonObject { put("type", "number") })
                            })
                        })
                    })
                })
            })
            put("anyOf", buildJsonArray {
                add(buildJsonObject {
                    put("then", buildJsonObject {
                        put("properties", buildJsonObject {
                            put("lastname", buildJsonObject { put("type", "string") })
                        })
                    })
                    put("else", buildJsonObject {
                        put("properties", buildJsonObject {
                            put("firstname", buildJsonObject { put("type", "string") })
                        })
                    })
                })
            })
        }

        assertEquals("string", resolveSchema(schema, "#/properties/description/properties/name", schema)?.toJsonObject()?.get("type")?.toString()?.trim('"'))
        assertEquals("number", resolveSchema(schema, "#/properties/description/properties/index", schema)?.toJsonObject()?.get("type")?.toString()?.trim('"'))
        assertEquals("string", resolveSchema(schema, "#/properties/lastname", schema)?.toJsonObject()?.get("type")?.toString()?.trim('"'))
        assertEquals("string", resolveSchema(schema, "#/properties/firstname", schema)?.toJsonObject()?.get("type")?.toString()?.trim('"'))
        assertNull(resolveSchema(schema, "#/properties/description/properties/notfound", schema))
    }

    @Test
    fun `resolveSchema - decodes encoded characters`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo / ~ bar", buildJsonObject { put("type", "integer") })
            })
        }

        assertEquals("integer", resolveSchema(schema, "#/properties/foo ~1 ~0 bar", schema)?.toJsonObject()?.get("type")?.toString()?.trim('"'))
        assertNull(resolveSchema(schema, "#/properties/foo / bar", schema))
    }

    @Test
    fun `resolveSchema - root ref`() {
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
            put("\$ref", "#/definitions/foo")
        }

        assertEquals(JsonSchema(buildJsonObject { put("type", "string") }), resolveSchema(schema, "#", schema))
    }
}
