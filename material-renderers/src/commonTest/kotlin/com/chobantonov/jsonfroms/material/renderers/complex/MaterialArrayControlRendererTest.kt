package com.chobantonov.jsonfroms.material.renderers.complex

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.material.renderers.layouts.materialArrayLayoutTester
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaterialArrayControlRendererTest {
    @Test
    fun discriminatedFlatAlternativesUseAutomaticTable() {
        for (keyword in listOf("oneOf", "anyOf")) {
            val schema = JsonSchema(Json.parseToJsonElement("""{
                "type":"array","items":{"type":"object","$keyword":[
                    {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}},
                        "required":["mode","note"]},
                    {"properties":{"mode":{"const":"secret"},
                        "secret":{"type":"string","format":"password"}},
                        "required":["mode","secret"]}
                ]}
            }""").jsonObject)
            val control = ControlElement("#")
            val context = TesterContext(rootSchema = schema)
            assertTrue(materialArrayControlTester(control, schema, context) >
                materialArrayLayoutTester(control, schema, context))
        }
    }

    @Test
    fun authoredColumnDefsSelectPortableTableForDifferentAlternativeFields() {
        val schema = JsonSchema(Json.parseToJsonElement("""{
            "type":"array","items":{"type":"object","oneOf":[
                {"properties":{"mode":{"const":"plain"},"note":{"type":"string"}}},
                {"properties":{"mode":{"const":"secret"},"secret":{"type":"string"}}}
            ]}
        }""").jsonObject)
        val columns = Json.parseToJsonElement("""[{"field":"mode"},{"field":"note"},{"field":"secret"}]""")
        val authored = ControlElement("#", options = mapOf("columnDefs" to columns))
        val plain = ControlElement("#")
        val context = TesterContext(rootSchema = schema)
        assertTrue(materialArrayControlTester(authored, schema, context) >
            materialArrayLayoutTester(authored, schema, context))
        assertTrue(materialArrayLayoutTester(plain, schema, context) >
            materialArrayControlTester(plain, schema, context))
        val configOnly = TesterContext(rootSchema = schema, config = mapOf("columnDefs" to columns))
        assertTrue(materialArrayLayoutTester(plain, schema, configOnly) >
            materialArrayControlTester(plain, schema, configOnly))
    }

    @Test
    fun mixedArrayItemsUseSingleExpandedItemLayout() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject {
                put("type", buildJsonArray {
                    add(JsonPrimitive("string"))
                    add(JsonPrimitive("object"))
                    add(JsonPrimitive("array"))
                })
            })
        })
        val control = ControlElement(scope = "#")
        val context = TesterContext(rootSchema = schema)

        assertTrue(materialArrayLayoutTester(control, schema, context) > materialArrayControlTester(control, schema, context))
    }

    @Test
    fun simpleObjectItemsUseCompactTableRows() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string") })
                put("age", buildJsonObject { put("type", "integer") })
            })
        })

        assertTrue(shouldRenderArrayItemsAsTable(schema, schema))
    }

    @Test
    fun nestedObjectItemsUseVerticalCards() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("description", buildJsonObject { put("type", "string") })
                put("attribution", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("contributor", buildJsonObject { put("type", "string") })
                    })
                })
            })
        })

        assertFalse(shouldRenderArrayItemsAsTable(schema, schema))
    }

    @Test
    fun nestedArrayItemsUseVerticalCards() {
        val schema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("name", buildJsonObject { put("type", "string") })
                put("qualifiers", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("type", "string") })
                })
            })
        })

        assertFalse(shouldRenderArrayItemsAsTable(schema, schema))
    }

    @Test
    fun referencedNestedObjectsUseVerticalCards() {
        val rootSchema = JsonSchema(buildJsonObject {
            put("definitions", buildJsonObject {
                put("attribution", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("contributor", buildJsonObject { put("type", "string") })
                    })
                })
            })
        })
        val itemSchema = JsonSchema(buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("description", buildJsonObject { put("type", "string") })
                put("attribution", buildJsonObject { put("\$ref", "#/definitions/attribution") })
            })
        })

        assertFalse(shouldRenderArrayItemsAsTable(itemSchema, rootSchema))
    }
}
