package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PathTest {
    @Test
    fun `resolve `() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "integer")
                })
            })
        }

        assertEquals(
            JsonSchema(buildJsonObject { put("type", "integer") }),
            Resolve.schema(schema, "#/properties/foo", schema),
        )
    }

    @Test
    fun `toDataPath `() {
        assertEquals("foo.bar", toDataPath("#/properties/foo/properties/bar"))
    }

    @Test
    fun `toDataPath replace anyOf`() {
        assertEquals("foo.bar", toDataPath("/anyOf/11/properties/foo/anyOf/11/properties/bar"))
    }

    @Test
    fun `toDataPath replace anyOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/anyOf/1/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple directly nested anyOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/anyOf/1/then/anyOf/0/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple nested properties with anyOf in combination with conditional schema compositions`() {
        assertEquals("foo.bar", toDataPath("/anyOf/1/properties/foo/anyOf/0/then/properties/bar"))
    }

    @Test
    fun `toDataPath replace allOf`() {
        assertEquals("foo.bar", toDataPath("/allOf/11/properties/foo/allOf/11/properties/bar"))
    }

    @Test
    fun `toDataPath replace allOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/allOf/1/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple directly nested allOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/allOf/1/then/allOf/0/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple nested properties with allOf in combination with conditional schema compositions`() {
        assertEquals("foo.bar", toDataPath("/allOf/1/properties/foo/allOf/0/then/properties/bar"))
    }

    @Test
    fun `toDataPath replace oneOf`() {
        assertEquals("foo.bar", toDataPath("/oneOf/11/properties/foo/oneOf/11/properties/bar"))
    }

    @Test
    fun `toDataPath replace oneOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/oneOf/1/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple directly nested oneOf in combination with conditional schema compositions`() {
        assertEquals("foo", toDataPath("/oneOf/1/then/oneOf/0/then/properties/foo"))
    }

    @Test
    fun `toDataPath replace multiple nested properties with oneOf in combination with conditional schema compositions`() {
        assertEquals("foo.bar", toDataPath("/oneOf/1/properties/foo/oneOf/0/then/properties/bar"))
    }

    @Test
    fun `toDataPath replace all combinators`() {
        assertEquals(
            "foo.bar.foobar",
            toDataPath("/oneOf/1/properties/foo/anyOf/1/properties/bar/allOf/1/properties/foobar"),
        )
    }

    @Test
    fun `toDataPath use of keywords`() {
        assertEquals("properties", toDataPath("#/properties/properties"))
    }

    @Test
    fun `toDataPath use of encoded paths`() {
        val fooBar = "foo.bar"
        assertEquals(fooBar, toDataPath("#/properties/$fooBar"))
    }

    @Test
    fun `toDataPath relative with ∕`() {
        assertEquals("foo.bar", toDataPath("/properties/foo/properties/bar"))
    }

    @Test
    fun `toDataPath use of keywords relative with ∕`() {
        assertEquals("properties", toDataPath("/properties/properties"))
    }

    @Test
    fun `toDataPath use of encoded paths relative with ∕`() {
        val fooBar = "foo%2Fbar"
        assertEquals(fooBar, toDataPath("/properties/$fooBar"))
    }

    @Test
    fun `toDataPath relative without ∕`() {
        assertEquals("foo.bar", toDataPath("properties/foo/properties/bar"))
    }

    @Test
    fun `toDataPath use of keywords relative without ∕`() {
        assertEquals("properties", toDataPath("properties/properties"))
    }

    @Test
    fun `toDataPath use of encoded paths relative without ∕`() {
        val fooBar = "foo%2Fbar"
        assertEquals(fooBar, toDataPath("properties/$fooBar"))
    }

    @Test
    fun `toDataPath use of encoded special character in pathname`() {
        assertEquals("foo~bar/baz", toDataPath("properties/foo~0bar~1baz"))
    }

    @Test
    fun `resolve instance`() {
        val instance = buildJsonObject { put("foo", 123) }
        assertEquals(JsonPrimitive(123), Resolve.data(instance, toDataPath("#/properties/foo")))
    }

    @Test
    fun `resolve instance with keywords`() {
        val instance = buildJsonObject { put("properties", 123) }
        assertEquals(JsonPrimitive(123), Resolve.data(instance, toDataPath("#/properties/properties")))
    }

    @Test
    fun `resolve nested instance`() {
        val instance = buildJsonObject {
            put("foo", buildJsonObject { put("bar", 123) })
        }
        assertEquals(JsonPrimitive(123), Resolve.data(instance, toDataPath("#/properties/foo/properties/bar")))
    }

    @Test
    fun `resolve uninitiated instance`() {
        val instance = buildJsonObject { }
        assertEquals(null, Resolve.data(instance, toDataPath("#/properties/foo/properties/bar")))
    }

    @Test
    fun `resolve ref`() {
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("foo", buildJsonObject { put("type", "string") })
            })
            put("type", "object")
            put("properties", buildJsonObject {
                put("foos", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                })
            })
        }
        assertEquals(
            JsonSchema(buildJsonObject { put("type", "string") }),
            Resolve.schema(schema, "#/properties/foos/items", schema),
        )
    }

    @Test
    fun `resolve ref simple`() {
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                        })
                    })
                })
            })
            put("type", "object")
            put("properties", buildJsonObject {
                put("foos", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                })
            })
        }

        assertEquals(
            JsonSchema(
                buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                        })
                    })
                },
            ),
            Resolve.schema(schema, "#/properties/foos/items", schema),
        )
        val refValue = schema.toJsonObject()
            .getValue("definitions").jsonObject
            .getValue("foo").jsonObject
            .getValue("properties").jsonObject
            .getValue("bar").jsonObject
            .getValue("items").jsonObject
            .getValue("\$ref").jsonPrimitive.content
        assertEquals("#/definitions/foo", refValue)
    }

    @Test
    fun `resolve ref complicated`() {
        val schema = buildJsonSchema {
            put("definitions", buildJsonObject {
                put("foo", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("\$ref", "#/definitions/foo2") })
                        })
                    })
                })
                put("foo2", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                        })
                    })
                })
            })
            put("type", "object")
            put("properties", buildJsonObject {
                put("foos", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("\$ref", "#/definitions/foo") })
                })
            })
        }

        assertEquals(
            JsonSchema(
                buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("bar", buildJsonObject {
                            put("type", "array")
                            put("items", buildJsonObject { put("\$ref", "#/definitions/foo2") })
                        })
                    })
                },
            ),
            Resolve.schema(schema, "#/properties/foos/items", schema),
        )
    }
}
