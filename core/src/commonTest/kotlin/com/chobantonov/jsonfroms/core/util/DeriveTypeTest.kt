package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DeriveTypeTest {
    @Test
    fun `derive type with type`() {
        val schema = buildJsonSchema {
            put("type", "string")
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("string", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - properties object`() {
        val schema = buildJsonSchema {
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "string")
                })
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("object", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - additionalProperties object`() {
        val schema = buildJsonSchema {
            put("additionalProperties", buildJsonObject {
                put("type", "string")
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("object", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - items array`() {
        val schema = buildJsonSchema {
            put("items", buildJsonObject {
                put("type", "string")
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("array", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - enum`() {
        val schema = buildJsonSchema {
            put("enum", buildJsonArray {
                add(JsonPrimitive("foo"))
                add(JsonPrimitive("bar"))
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("string", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - enum with two types`() {
        val schema = buildJsonSchema {
            put("enum", buildJsonArray {
                add(JsonPrimitive("foo"))
                add(JsonPrimitive("bar"))
                add(buildJsonObject {
                    put("properties", buildJsonObject {
                        put("foo", buildJsonObject {
                            put("type", "string")
                        })
                    })
                })
            })
        }

        assertEquals(2, deriveTypes(schema).size)
        assertEquals("string", deriveTypes(schema)[0])
        assertEquals("object", deriveTypes(schema)[1])
    }

    @Test
    fun `derive type with type - union`() {
        val schema = buildJsonSchema {
            put("type", buildJsonArray {
                add(JsonPrimitive("string"))
                add(JsonPrimitive("number"))
            })
        }

        assertEquals(2, deriveTypes(schema).size)
        assertEquals(listOf("string", "number"), deriveTypes(schema))
    }

    @Test
    fun `derive type with type - allOf first has type`() {
        val schema = buildJsonSchema {
            put("allOf", buildJsonArray {
                add(buildJsonObject {
                    put("type", "string")
                })
                add(buildJsonObject {
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("foo"))
                        add(JsonPrimitive("bar"))
                    })
                })
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("string", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type with type - allOf other has type`() {
        val schema = buildJsonSchema {
            put("allOf", buildJsonArray {
                add(buildJsonObject {
                    put("enum", buildJsonArray {
                        add(JsonPrimitive("foo"))
                        add(JsonPrimitive("bar"))
                    })
                })
                add(buildJsonObject {
                    put("type", "string")
                })
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("string", deriveTypes(schema)[0])
    }

    @Test
    fun `derive type w∕o type - allOf other has type`() {
        val schema = buildJsonSchema {
            put("allOf", buildJsonArray {
                add(buildJsonObject {
                    put("properties", buildJsonObject {
                        put("foo", buildJsonObject {
                            put("type", "string")
                        })
                    })
                })
                add(buildJsonObject {
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "string")
                        })
                    })
                })
            })
        }

        assertEquals(1, deriveTypes(schema).size)
        assertEquals("object", deriveTypes(schema)[0])
    }
}
