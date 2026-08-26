package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AdditionalPropertiesTest {
    private val schema = buildJsonSchema {
        put("type", "object")
        put("properties", buildJsonObject { put("fixed", buildJsonObject { put("type", "string") }) })
        put("propertyNames", buildJsonObject { put("minLength", 2) })
        put("patternProperties", buildJsonObject {
            put("^count", buildJsonObject { put("type", "integer") })
        })
        put("additionalProperties", buildJsonObject { put("type", "string") })
    }

    @Test
    fun `finds only dynamic keys`() {
        val data = JsonObject(mapOf("fixed" to JsonPrimitive("a"), "extra" to JsonPrimitive("b")))
        assertEquals(listOf("extra"), additionalPropertyKeys(schema, data))
    }

    @Test
    fun `pattern schema takes precedence over additional schema`() {
        val selected = additionalPropertySchema(schema, "counter")
        assertEquals(JsonSchemaTypeValue.Single(JsonSchemaType.Integer), selected?.typeValue)
        assertEquals("counter", selected?.title)
        assertEquals(JsonSchemaTypeValue.Single(JsonSchemaType.String), additionalPropertySchema(schema, "other")?.typeValue)
    }

    @Test
    fun `unconstrained properties use the mixed renderer type set`() {
        val unconstrained = buildJsonSchema { put("additionalProperties", true) }
        val types = (additionalPropertySchema(unconstrained, "value")?.typeValue as JsonSchemaTypeValue.Multiple).values
        assertEquals(7, types.size)
        assertTrue(JsonSchemaType.Null in types)
    }

    @Test
    fun `validates names and respects disabled additional properties`() {
        assertEquals("Property name must contain at least 2 characters", validateAdditionalPropertyName(schema, "x", emptySet()))
        assertEquals("A property with this name already exists", validateAdditionalPropertyName(schema, "fixed", setOf("fixed")))
        assertNull(validateAdditionalPropertyName(schema, "countItems", setOf("fixed")))

        val closed = buildJsonSchema {
            put("patternProperties", buildJsonObject { put("^x", buildJsonObject { put("type", "string") }) })
            put("additionalProperties", false)
        }
        assertNull(validateAdditionalPropertyName(closed, "xValue", emptySet()))
        assertEquals("Property name does not match an allowed pattern", validateAdditionalPropertyName(closed, "other", emptySet()))
        assertFalse(supportsAdditionalProperties(buildJsonSchema { put("additionalProperties", false) }))
        assertTrue(supportsAdditionalProperties(buildJsonSchema {}, mapOf("allowAdditionalPropertiesIfMissing" to true)))
    }
}
