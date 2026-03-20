package com.chobantonov.jsonfroms.core.generators

import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SchemaTest {
    @Test
    fun `default schema generation basic types`() {
        val instance = buildJsonObject {
            put("boolean", false)
            put("number", 3.14)
            put("integer", 3)
            put("string", "PI")
            put("null", JsonPrimitive(null as String?))
        }

        val schema = generateJsonSchema(instance)

        assertEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("boolean", buildJsonObject { put("type", "boolean") })
                    put("number", buildJsonObject { put("type", "number") })
                    put("integer", buildJsonObject { put("type", "integer") })
                    put("string", buildJsonObject { put("type", "string") })
                    put("null", buildJsonObject { put("type", "null") })
                })
                put("additionalProperties", true)
                put("required", buildJsonArray {
                    add(JsonPrimitive("boolean"))
                    add(JsonPrimitive("number"))
                    add(JsonPrimitive("integer"))
                    add(JsonPrimitive("string"))
                    add(JsonPrimitive("null"))
                })
            },
            schema,
        )
    }

    @Test
    fun `default schema generation array types`() {
        val instance = buildJsonObject {
            put("emptyArray", buildJsonArray {})
            put("booleanArray", buildJsonArray {
                add(JsonPrimitive(false))
                add(JsonPrimitive(false))
            })
            put("numberArray", buildJsonArray {
                add(JsonPrimitive(3.14))
                add(JsonPrimitive(2.71))
            })
            put("integerArray", buildJsonArray {
                add(JsonPrimitive(3))
                add(JsonPrimitive(2))
            })
            put("stringArray", buildJsonArray {
                add(JsonPrimitive("PI"))
                add(JsonPrimitive("e"))
            })
            put("nullArray", buildJsonArray {
                add(JsonPrimitive(null as String?))
                add(JsonPrimitive(null as String?))
            })
        }

        val schema = generateJsonSchema(instance)

        assertEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("emptyArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject {})
                    })
                    put("booleanArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "boolean") })
                    })
                    put("numberArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "number") })
                    })
                    put("integerArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "integer") })
                    })
                    put("stringArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "string") })
                    })
                    put("nullArray", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject { put("type", "null") })
                    })
                })
                put("additionalProperties", true)
                put("required", buildJsonArray {
                    add(JsonPrimitive("emptyArray"))
                    add(JsonPrimitive("booleanArray"))
                    add(JsonPrimitive("numberArray"))
                    add(JsonPrimitive("integerArray"))
                    add(JsonPrimitive("stringArray"))
                    add(JsonPrimitive("nullArray"))
                })
            },
            schema,
        )
    }

    @Ignore
    @Test
    fun `default schema generation tuple array types`() {
        val instance = buildJsonObject {
            put("tupleArray", buildJsonArray {
                add(JsonPrimitive(3.14))
                add(JsonPrimitive("PI"))
            })
        }

        val schema = generateJsonSchema(instance)

        assertEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("tupleArray", buildJsonObject {
                        put("type", "array")
                        put(
                            "items",
                            buildJsonArray {
                                add(buildJsonObject { put("type", "number") })
                                add(buildJsonObject { put("type", "string") })
                            },
                        )
                    })
                })
                put("additionalProperties", true)
                put("required", buildJsonArray {
                    add(JsonPrimitive("tupleArray"))
                })
            },
            schema,
        )
    }

    @Test
    fun `default schema generation `() {
        val instance = buildJsonObject {
            put("address", buildJsonObject {
                put("streetAddress", "21 2nd Street")
                put("city", "New York")
            })
            put("phoneNumber", buildJsonArray {
                add(
                    buildJsonObject {
                        put("location", "home")
                        put("code", 44)
                        put("private", true)
                    },
                )
            })
        }

        val schema = generateJsonSchema(instance)

        assertEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("address", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("streetAddress", buildJsonObject { put("type", "string") })
                            put("city", buildJsonObject { put("type", "string") })
                        })
                        put("additionalProperties", true)
                        put("required", buildJsonArray {
                            add(JsonPrimitive("streetAddress"))
                            add(JsonPrimitive("city"))
                        })
                    })
                    put("phoneNumber", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("location", buildJsonObject { put("type", "string") })
                                put("code", buildJsonObject { put("type", "integer") })
                                put("private", buildJsonObject { put("type", "boolean") })
                            })
                            put("additionalProperties", true)
                            put("required", buildJsonArray {
                                add(JsonPrimitive("location"))
                                add(JsonPrimitive("code"))
                                add(JsonPrimitive("private"))
                            })
                        })
                    })
                })
                put("additionalProperties", true)
                put("required", buildJsonArray {
                    add(JsonPrimitive("address"))
                    add(JsonPrimitive("phoneNumber"))
                })
            },
            schema,
        )
    }

    @Test
    fun `schema generation with options `() {
        val instance = buildJsonObject {
            put("address", buildJsonObject {
                put("streetAddress", "21 2nd Street")
                put("city", "New York")
            })
        }

        val schema = generateJsonSchema(
            instance,
            JsonSchemaGenerationOptions(
                additionalProperties = false,
                requiredProperties = { props ->
                    val keys = props.keys.toList()
                    if (props.isNotEmpty()) {
                        listOf(keys.first())
                    } else {
                        emptyList()
                    }
                },
            ),
        )

        assertEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("address", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("streetAddress", buildJsonObject { put("type", "string") })
                            put("city", buildJsonObject { put("type", "string") })
                        })
                        put("additionalProperties", false)
                        put("required", buildJsonArray {
                            add(JsonPrimitive("streetAddress"))
                        })
                    })
                })
                put("additionalProperties", false)
                put("required", buildJsonArray {
                    add(JsonPrimitive("address"))
                })
            },
            schema,
        )
    }
}
