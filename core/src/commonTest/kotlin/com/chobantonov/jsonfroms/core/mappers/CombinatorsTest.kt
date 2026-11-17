package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class CombinatorsTest {
    @Test
    fun `branch detail obeys inline generate and registered precedence`() {
        val branch = buildJsonSchema {
            put("type", "object")
            put("properties", buildJsonObject { put("name", buildJsonObject { put("type", "string") }) })
        }
        val registry = listOf(JsonFormsUISchemaRegistryEntry(
            tester = { _, _, _ -> 10 }, uischema = LabelElement("Registered"),
        ))
        fun selected(detail: kotlinx.serialization.json.JsonElement) = createCombinatorRenderInfos(
            listOf(branch), branch, CombinatorKeyword.oneOf,
            ControlElement("#", options = mapOf("detail" to detail)), "", registry,
        ).single().uischema
        assertEquals(LabelElement("Registered"), selected(JsonPrimitive("REGISTERED")))
        assertEquals("VerticalLayout", selected(JsonPrimitive("gEnErAtE")).type)
        assertEquals(LabelElement("Inline"), selected(kotlinx.serialization.json.Json.parseToJsonElement(
            """{"type":"Label","text":"Inline"}""")))
    }

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

    @Test
    fun `findFittingSchemaIndex selects branch matching current object data`() {
        val branches = buildJsonArray {
            add(buildJsonObject {
                put("type", "object")
                put("required", buildJsonArray {
                    add(JsonPrimitive("street_address"))
                    add(JsonPrimitive("city"))
                    add(JsonPrimitive("state"))
                })
                put("additionalProperties", false)
                put("properties", buildJsonObject {
                    put("street_address", buildJsonObject { put("type", "string") })
                    put("city", buildJsonObject { put("type", "string") })
                    put("state", buildJsonObject { put("type", "string") })
                })
            })
            add(buildJsonObject {
                put("type", "object")
                put("required", buildJsonArray {
                    add(JsonPrimitive("name"))
                    add(JsonPrimitive("mail"))
                })
                put("additionalProperties", false)
                put("properties", buildJsonObject {
                    put("name", buildJsonObject { put("type", "string") })
                    put("mail", buildJsonObject {
                        put("type", "string")
                        put("format", "email")
                    })
                })
            })
        }.map { JsonSchema(it.jsonObject) }
        val data = buildJsonObject {
            put("name", "User")
            put("mail", "mail@example.com")
        }

        assertEquals(
            1,
            findFittingSchemaIndex(branches, data, OptimumCodeJsonSchemaValidator()),
        )
    }

    @Test
    fun `findFittingSchemaIndex returns null when no branch structurally matches`() {
        val branches = buildJsonArray {
            add(buildJsonObject {
                put("type", "string")
            })
            add(buildJsonObject {
                put("type", "number")
            })
        }.map { JsonSchema(it.jsonObject) }

        assertNull(
            findFittingSchemaIndex(
                branches,
                buildJsonObject { put("value", true) },
                OptimumCodeJsonSchemaValidator(),
            ),
        )
    }

    @Test
    fun `findFittingSchemaIndex prefers a fully valid branch before structural fallback`() {
        val branches = listOf(
            JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{
                "text":{"type":"string","maxLength":5}},"required":["text"]}""").jsonObject),
            JsonSchema(Json.parseToJsonElement("""{"type":"object","properties":{
                "text":{"type":"string","minLength":10}},"required":["text"]}""").jsonObject),
        )
        val validator = OptimumCodeJsonSchemaValidator()
        assertEquals(1, findFittingSchemaIndex(branches,
            Json.parseToJsonElement("""{"text":"Longer text"}"""), validator))
        assertEquals(0, findFittingSchemaIndex(branches,
            Json.parseToJsonElement("""{"text":"No"}"""), validator))
        // Neither branch validates, but the object still needs an editor for repair.
        assertEquals(0, findFittingSchemaIndex(branches,
            Json.parseToJsonElement("""{"text":"abcdefg"}"""), validator))
    }

    @Test
    fun `findFittingSchemaIndex resolves nested root definitions while matching a branch`() {
        val root = JsonSchema(Json.parseToJsonElement("""{
            "definitions": {
                "leaf": {"type":"object","properties":{"kind":{"const":"leaf"}},"required":["kind"]},
                "folder": {"type":"object","properties":{"kind":{"const":"folder"},
                    "children":{"type":"array","items":{"${'$'}ref":"#/definitions/leaf"}}},
                    "required":["kind","children"],"additionalProperties":false}
            }
        }""").jsonObject)
        val file = JsonSchema(Json.parseToJsonElement("""{"type":"object",
            "properties":{"kind":{"const":"file"}},"required":["kind"]}""").jsonObject)
        val folder = requireNotNull(resolveSchema(root, "#/definitions/folder", root))
        val data = Json.parseToJsonElement("""{"kind":"folder","children":[{"kind":"leaf"}]}""")

        assertNull(findFittingSchemaIndex(listOf(file, folder), data, OptimumCodeJsonSchemaValidator()))
        assertEquals(1, findFittingSchemaIndex(listOf(file, folder), data,
            OptimumCodeJsonSchemaValidator(), root))
        val invalidChild = Json.parseToJsonElement("""{"kind":"folder","children":[{"kind":"other"}]}""")
        assertEquals(1, findFittingSchemaIndex(listOf(file, folder), invalidChild,
            OptimumCodeJsonSchemaValidator(), root))
        val wrongRoot = Json.parseToJsonElement("""{"kind":"other","children":[]}""")
        assertNull(findFittingSchemaIndex(listOf(file, folder), wrongRoot,
            OptimumCodeJsonSchemaValidator(), root))

        val wrongNested = JsonSchema(Json.parseToJsonElement("""{"type":"object",
            "properties":{"kind":{"const":"folder"},"child":{"type":"object",
                "properties":{"tag":{"const":"wrong"}}}}}""").jsonObject)
        val rightNested = JsonSchema(Json.parseToJsonElement("""{"type":"object",
            "properties":{"kind":{"const":"folder"},"child":{"type":"object",
                "properties":{"tag":{"const":"right"}}}}}""").jsonObject)
        val rightData = Json.parseToJsonElement("""{"kind":"folder","child":{"tag":"right"}}""")
        assertEquals(1, findFittingSchemaIndex(listOf(wrongNested, rightNested), rightData,
            OptimumCodeJsonSchemaValidator()))
    }
}
