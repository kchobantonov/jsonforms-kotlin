package com.chobantonov.jsonfroms.core.generators

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UiSchemaTest {
    private fun s(value: String) = kotlinx.serialization.json.JsonPrimitive(value)

    @Test
    fun `generate ui schema for Control element by resolving refs`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject {
                put("type", buildJsonObject {
                    put("type", "string")
                    put("const", "Control")
                    put("default", "Control")
                })
                put("label", buildJsonObject {
                    put("type", "string")
                })
                put("scope", buildJsonObject {
                    put("\$ref", "#/definitions/scope")
                })
                put("rule", buildJsonObject {
                    put("\$ref", "#/definitions/rule")
                })
            })
            put("required", buildJsonArray {
                add(s("type"))
                add(s("scope"))
            })
            put("definitions", buildJsonObject {
                put("scope", buildJsonObject {
                    put("type", "string")
                    put("pattern", "^#\\\\/properties\\\\/{1}")
                })
                put("rule", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("effect", buildJsonObject {
                            put("type", "string")
                            put("enum", buildJsonArray {
                                add(s("HIDE"))
                                add(s("SHOW"))
                                add(s("DISABLE"))
                                add(s("ENABLE"))
                            })
                        })
                        put("condition", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                put("type", buildJsonObject {
                                    put("type", "string")
                                    put("const", "LEAF")
                                })
                                put("scope", buildJsonObject {
                                    put("\$ref", "#/definitions/scope")
                                })
                                put("expectedValue", buildJsonObject {
                                    put("type", buildJsonArray {
                                        add(s("string"))
                                        add(s("integer"))
                                        add(s("number"))
                                        add(s("boolean"))
                                    })
                                })
                            })
                            put("required", buildJsonArray {
                                add(s("type"))
                                add(s("scope"))
                                add(s("expectedValue"))
                            })
                        })
                    })
                    put("required", buildJsonArray {
                        add(s("effect"))
                        add(s("condition"))
                    })
                })
            })
        }

        assertUiSchemaEquals(
            schema,
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/type"),
                    ControlElement(scope = "#/properties/label"),
                    ControlElement(scope = "#/properties/scope"),
                    ControlElement(scope = "#/properties/rule"),
                ),
            ),
        )
    }

    @Test
    fun `generate ui schema for schema w∕o properties`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
            },
            VerticalLayout(elements = emptyList()),
        )
    }

    @Test
    fun `generate ui schema for schema with one property`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("name", buildJsonObject {
                        put("type", "string")
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#/properties/name"))),
        )
    }

    @Test
    fun `generate ui schema for schema without object root`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "string")
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Test
    fun `generate ui schema for schema with unspecified object root`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("properties", buildJsonObject {
                    put("age", buildJsonObject {
                        put("type", "integer")
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#/properties/age"))),
        )
    }

    @Test
    fun `nested object not expanded`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", "string")
                    })
                    put("private", buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject {
                                put("type", "string")
                            })
                        })
                    })
                })
            },
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/id"),
                    ControlElement(scope = "#/properties/private"),
                ),
            ),
        )
    }

    @Test
    fun `don't ignore non-json-schema id attributes`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", "string")
                    })
                    put("name", buildJsonObject {
                        put("type", "string")
                    })
                })
            },
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/id"),
                    ControlElement(scope = "#/properties/name"),
                ),
            ),
        )
    }

    @Test
    fun `generate ui schema for schema with multiple properties`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject { put("type", "string"); put("format", "objectId") })
                    put("lastName", buildJsonObject { put("type", "string") })
                    put("email", buildJsonObject { put("type", "string") })
                    put("firstName", buildJsonObject { put("type", "string") })
                    put("gender", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray { add(s("Male")); add(s("Female")) })
                    })
                    put("active", buildJsonObject { put("type", "boolean") })
                    put("registrationTime", buildJsonObject { put("type", "string"); put("format", "date-time") })
                    put("weight", buildJsonObject { put("type", "number") })
                    put("height", buildJsonObject { put("type", "integer") })
                    put("nationality", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add(s("German"))
                            add(s("French"))
                            add(s("UK"))
                            add(s("US"))
                            add(s("Spanish"))
                            add(s("Italian"))
                            add(s("Russian"))
                        })
                    })
                    put("birthDate", buildJsonObject { put("type", "string"); put("format", "date-time") })
                })
                put("additionalProperties", false)
                put("required", buildJsonArray { add(s("id")); add(s("lastName")); add(s("email")) })
            },
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/id"),
                    ControlElement(scope = "#/properties/lastName"),
                    ControlElement(scope = "#/properties/email"),
                    ControlElement(scope = "#/properties/firstName"),
                    ControlElement(scope = "#/properties/gender"),
                    ControlElement(scope = "#/properties/active"),
                    ControlElement(scope = "#/properties/registrationTime"),
                    ControlElement(scope = "#/properties/weight"),
                    ControlElement(scope = "#/properties/height"),
                    ControlElement(scope = "#/properties/nationality"),
                    ControlElement(scope = "#/properties/birthDate"),
                ),
            ),
        )
    }

    @Test
    fun `generate named array control`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("comments", buildJsonObject {
                        put("type", "array")
                        put("items", buildJsonObject {
                            put("properties", buildJsonObject {
                                put("msg", buildJsonObject { put("type", "string") })
                            })
                        })
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#/properties/comments"))),
        )
    }

    @Test
    fun `generate unnamed array control`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("type", "array")
                put("items", buildJsonObject {
                    put("properties", buildJsonObject {
                        put("msg", buildJsonObject { put("type", "string") })
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Test
    fun `generate unnamed array control w∕o type`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("items", buildJsonObject {
                    put("properties", buildJsonObject {
                        put("msg", buildJsonObject { put("type", "string") })
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Ignore
    @Test
    fun `generate for empty schema`() {
        val schema = buildJsonSchema {}
        assertEquals<Any?>(null, generateDefaultUISchema(schema))
    }

    @Ignore
    @Test
    fun `generate for null schema`() {
        assertEquals<Any?>(null, null)
    }

    @Ignore
    @Test
    fun `generate for undefined schema`() {
        assertEquals<Any?>(null, null)
    }

    @Test
    fun `generate control for oneOf`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("oneOf", buildJsonArray {
                    add(buildJsonObject {
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                    add(buildJsonObject { put("type", "number") })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Test
    fun `generate control for anyOf`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("anyOf", buildJsonArray {
                    add(buildJsonObject {
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                    add(buildJsonObject { put("type", "number") })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Test
    fun `generate control for allOf`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("allOf", buildJsonArray {
                    add(buildJsonObject {
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                    add(buildJsonObject { put("type", "number") })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#"))),
        )
    }

    @Test
    fun `no separate control for oneOf in array`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("properties", buildJsonObject {
                    put("myarray", buildJsonObject {
                        put("items", buildJsonObject {
                            put("oneOf", buildJsonArray {
                                add(buildJsonObject {
                                    put("properties", buildJsonObject {
                                        put("name", buildJsonObject { put("type", "string") })
                                    })
                                })
                                add(buildJsonObject { put("type", "number") })
                            })
                        })
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#/properties/myarray"))),
        )
    }

    @Test
    fun `generate control for nested oneOf`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("properties", buildJsonObject {
                    put("myarray", buildJsonObject {
                        put("properties", buildJsonObject {
                            put("nameOrAge", buildJsonObject {
                                put("oneOf", buildJsonArray {
                                    add(buildJsonObject {
                                        put("properties", buildJsonObject {
                                            put("name", buildJsonObject { put("type", "string") })
                                        })
                                    })
                                    add(buildJsonObject { put("type", "number") })
                                })
                            })
                        })
                    })
                })
            },
            VerticalLayout(elements = listOf(ControlElement(scope = "#/properties/myarray"))),
        )
    }

    @Test
    fun `encode "∕" in generated ui schema`() {
        assertUiSchemaEquals(
            buildJsonSchema {
                put("properties", buildJsonObject {
                    put("some / initial / value", buildJsonObject {
                        put("type", "integer")
                    })
                })
            },
            VerticalLayout(
                elements = listOf(
                    ControlElement(scope = "#/properties/some ~1 initial ~1 value"),
                ),
            ),
        )
    }

    private fun assertUiSchemaEquals(
        schema: JsonSchema,
        expected: VerticalLayout,
    ) {
        assertEquals(expected, generateDefaultUISchema(schema))
    }
}
