package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class CombinatorsTest {
    private val rootSchema = buildJsonSchema {
        put("type", "object")
        put("properties", buildJsonObject {
            put("widget", buildJsonObject {
                put("anyOf", buildJsonArray {
                    add(buildJsonObject {
                        put("\$ref", "#/definitions/Dua")
                    })
                    add(buildJsonObject {
                        put("\$ref", "#/definitions/Lipa")
                    })
                })
            })
        })
        put("definitions", buildJsonObject {
            put("Dua", buildJsonObject {
                put("title", "Dua")
                put("type", "object")
                put("properties", buildJsonObject {
                    put("name", buildJsonObject { put("type", "string") })
                })
            })
            put("Lipa", buildJsonObject {
                put("title", "Lipa")
                put("type", "object")
                put("properties", buildJsonObject {
                    put("name", buildJsonObject { put("type", "string") })
                })
            })
        })
    }

    private val rootSchemaWithOverrides = buildJsonSchema {
        put("type", "object")
        put("properties", buildJsonObject {
            put("widget", buildJsonObject {
                put("anyOf", buildJsonArray {
                    add(buildJsonObject {
                        put("\$ref", "#/definitions/Dua")
                        put("title", "DuaOverride")
                    })
                    add(buildJsonObject {
                        put("\$ref", "#/definitions/Lipa")
                        put("title", "LipaOverride")
                    })
                })
            })
        })
        put("definitions", rootSchema.toJsonObject()["definitions"]!!.jsonObject)
    }

    private val schemaWithoutRefs = buildJsonSchema {
        put("type", "object")
        put("properties", buildJsonObject {
            put("widget", buildJsonObject {
                put("anyOf", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                    add(buildJsonObject {
                        put("type", "object")
                        put("properties", buildJsonObject {
                            put("name", buildJsonObject { put("type", "string") })
                        })
                    })
                })
            })
        })
    }

    private val control = ControlElement(scope = "#")

    @Test
    fun `createCombinatorRenderInfos - uses titles for labels when subschemas are refs`() {
        val anyOf = rootSchema.toJsonObject()["properties"]!!.jsonObject["widget"]!!.jsonObject["anyOf"]!!.jsonArray
            .map { JsonSchema(it.jsonObject) }

        val (duaRenderInfo, lipaRenderInfo) = createCombinatorRenderInfos(
            anyOf,
            rootSchema,
            CombinatorKeyword.anyOf,
            control,
            "widget",
            emptyList(),
        )

        assertEquals("Dua", duaRenderInfo.label)
        assertEquals("Lipa", lipaRenderInfo.label)
    }

    @Test
    fun `createCombinatorRenderInfos - uses overrides for labels when subschemas are refs`() {
        val anyOf = rootSchemaWithOverrides.toJsonObject()["properties"]!!.jsonObject["widget"]!!.jsonObject["anyOf"]!!.jsonArray
            .map { JsonSchema(it.jsonObject) }

        val (duaRenderInfo, lipaRenderInfo) = createCombinatorRenderInfos(
            anyOf,
            rootSchemaWithOverrides,
            CombinatorKeyword.anyOf,
            control,
            "widget",
            emptyList(),
        )

        assertEquals("DuaOverride", duaRenderInfo.label)
        assertEquals("LipaOverride", lipaRenderInfo.label)
    }

    @Test
    fun `createCombinatorRenderInfos - uses keyword + index when no labels provided`() {
        val anyOf = schemaWithoutRefs.toJsonObject()["properties"]!!.jsonObject["widget"]!!.jsonObject["anyOf"]!!.jsonArray
            .map { JsonSchema(it.jsonObject) }

        val (duaRenderInfo, lipaRenderInfo) = createCombinatorRenderInfos(
            anyOf,
            schemaWithoutRefs,
            CombinatorKeyword.anyOf,
            control,
            "widget",
            emptyList(),
        )

        assertEquals("anyOf-0", duaRenderInfo.label)
        assertEquals("anyOf-1", lipaRenderInfo.label)
    }
}
