package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class MixedTest {
    private val schema = buildJsonSchema {
        put("type", buildJsonArray {
            add(JsonPrimitive("string"))
            add(JsonPrimitive("boolean"))
            add(JsonPrimitive("null"))
        })
    }

    @Test
    fun `extracts types and creates a concrete schema`() {
        assertEquals(
            listOf(JsonSchemaType.String, JsonSchemaType.Boolean, JsonSchemaType.Null),
            mixedSchemaTypes(schema),
        )
        assertEquals(JsonSchemaTypeValue.Single(JsonSchemaType.Boolean), concreteMixedSchema(schema, JsonSchemaType.Boolean).typeValue)
        val arrayItems = assertNotNull(concreteMixedSchema(schema, JsonSchemaType.Array).items)
        val itemSchema = (arrayItems as com.chobantonov.jsonfroms.core.models.JsonSchemaOrSchemas.Single).schema
        assertEquals(JsonSchemaType.entries, mixedSchemaTypes(itemSchema))
    }

    @Test
    fun `detects JSON data types`() {
        assertEquals(JsonSchemaType.Null, jsonDataType(JsonNull))
        assertEquals(JsonSchemaType.Object, jsonDataType(JsonObject(emptyMap())))
        assertEquals(JsonSchemaType.Array, jsonDataType(JsonArray(emptyList())))
        assertEquals(JsonSchemaType.Boolean, jsonDataType(JsonPrimitive(true)))
        assertEquals(JsonSchemaType.Integer, jsonDataType(JsonPrimitive(2)))
        assertEquals(JsonSchemaType.Number, jsonDataType(JsonPrimitive(2.5)))
        assertEquals(JsonSchemaType.String, jsonDataType(JsonPrimitive("two")))
    }

    @Test
    fun `number selection accepts integral values without becoming integer`() {
        assertTrue(mixedTypeSupportsData(JsonSchemaType.Number, JsonSchemaType.Integer))
        assertTrue(mixedTypeSupportsData(JsonSchemaType.Integer, JsonSchemaType.Integer))
        assertFalse(mixedTypeSupportsData(JsonSchemaType.Integer, JsonSchemaType.Number))
        assertFalse(mixedTypeSupportsData(JsonSchemaType.Integer, JsonSchemaType.String))
    }

    @Test
    fun `array and object selections open mixed details immediately`() {
        assertTrue(shouldOpenMixedDetails(JsonSchemaType.Array))
        assertTrue(shouldOpenMixedDetails(JsonSchemaType.Object))
        assertFalse(shouldOpenMixedDetails(JsonSchemaType.String))
        assertFalse(shouldOpenMixedDetails(JsonSchemaType.Null))
        assertFalse(shouldOpenMixedDetails(null))
    }

    @Test
    fun `builds paths for deeply nested objects and arrays`() {
        val mixedSchema = buildJsonSchema {
            put("type", buildJsonArray {
                JsonSchemaType.entries.forEach { add(JsonPrimitive(it.name.lowercase())) }
            })
            put("additionalProperties", JsonPrimitive(true))
        }
        val data = buildJsonObject {
            put("person", buildJsonObject {
                put("addresses", buildJsonArray {
                    add(buildJsonObject { put("city", JsonPrimitive("Sofia")) })
                })
            })
        }

        val tree = buildMixedTree(data, mixedSchema)
        val person = tree.children.single()
        val addresses = person.children.single()
        val firstAddress = addresses.children.single()
        val city = firstAddress.children.single()

        assertEquals("person.addresses.0.city", city.path)
        assertEquals("person.addresses.0", city.parentPath)
        assertTrue(person.canRename)
        assertTrue(city.canDelete)

        val structuralTree = buildMixedTree(data, mixedSchema, includePrimitives = false)
        assertTrue(structuralTree.children.single().children.single().children.single().children.isEmpty())
    }

    @Test
    fun `builds object tree when properties contain boolean schemas`() {
        val mixedObjectSchema = buildJsonSchema {
            put("type", buildJsonArray {
                JsonSchemaType.entries.forEach { add(JsonPrimitive(it.name.lowercase())) }
            })
            put("properties", buildJsonObject {
                put("unconstrained", true)
                put("disallowed", false)
                put("nested", buildJsonObject { put("type", "object") })
            })
        }
        val data = buildJsonObject {
            put("unconstrained", "value")
            put("disallowed", "invalid but renderable")
            put("nested", buildJsonObject { put("child", true) })
        }

        val tree = buildMixedTree(data, mixedObjectSchema)

        assertEquals(listOf("unconstrained", "disallowed", "nested"), tree.children.map { it.key })
        assertTrue(tree.children.none { it.canRename })
        assertEquals("nested.child", tree.children.last().children.single().path)
    }
}
