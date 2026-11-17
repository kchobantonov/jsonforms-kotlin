package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import com.chobantonov.jsonfroms.core.util.acceptsRestrictedNumericEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
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
    fun `validation scopes include rendered dynamic keys but exclude rejected ones`() {
        val closed = buildJsonSchema {
            put("properties", buildJsonObject { put("fixed", buildJsonObject { put("type", "string") }) })
            put("patternProperties", buildJsonObject {
                put("^price_", buildJsonObject { put("type", "number") })
            })
            put("additionalProperties", false)
        }
        val data = JsonObject(linkedMapOf(
            "fixed" to JsonPrimitive("value"),
            "price_total" to JsonPrimitive(1500),
            "other" to JsonPrimitive("rejected"),
        ))
        assertEquals(listOf(listOf("price_total")), additionalPropertyValidationScopes(closed, data))
        assertEquals(emptyList(), additionalPropertyValidationScopes(
            buildJsonSchema { put("additionalProperties", false) }, data))
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

    @Test
    fun `rename preserves count at both property bounds and rejects stale or conflicting edits`() {
        val bounded = buildJsonSchema {
            put("type", "object")
            put("minProperties", 2)
            put("maxProperties", 2)
            put("properties", buildJsonObject { put("fixed", buildJsonObject { put("type", "string") }) })
            put("additionalProperties", buildJsonObject { put("type", "string") })
        }
        val original = JsonObject(linkedMapOf("fixed" to JsonPrimitive("declared"), "extra" to JsonPrimitive("value")))
        val renamed = renameAdditionalProperty(original, original, "extra", "other", bounded)
        assertEquals(listOf("fixed", "other"), renamed.keys.toList())
        assertEquals(JsonPrimitive("value"), renamed["other"])
        assertEquals(2, renamed.size)
        assertEquals(original, renameAdditionalProperty(original, original, "extra", "fixed", bounded))
        val dotted = renameAdditionalProperty(original, original, "extra", "bad.name", bounded)
        assertEquals(JsonPrimitive("value"), dotted["bad.name"])
        assertNull(dotted["bad"])
        val replacement = JsonObject(original.toMap())
        assertEquals(replacement, renameAdditionalProperty(replacement, original, "extra", "other", bounded))
        assertEquals(replacement, renameAdditionalProperty(replacement, original, "extra", "fixed", bounded))
    }

    @Test
    fun `add counts declared fields and rechecks stale data and names`() {
        val bounded = buildJsonSchema {
            put("type", "object")
            put("minProperties", 2)
            put("maxProperties", 2)
            put("properties", buildJsonObject { put("fixed", buildJsonObject { put("type", "string") }) })
            put("additionalProperties", buildJsonObject { put("type", "string") })
        }
        val one = JsonObject(mapOf("fixed" to JsonPrimitive("declared")))
        val two = addAdditionalProperty(one, one, "extra", bounded)
        assertEquals(2, two.size)
        assertEquals(JsonPrimitive(""), two["extra"])
        assertEquals(two, addAdditionalProperty(two, two, "third", bounded))
        assertEquals(two, addAdditionalProperty(two, one, "third", bounded))
        assertEquals(one, addAdditionalProperty(one, one, "fixed", bounded))
        assertEquals(3, addAdditionalProperty(two, two, "third", bounded, restrict = false).size)
    }

    @Test
    fun `literal and blank names preserve exact keys when allowed`() {
        val open = buildJsonSchema {
            put("type", "object")
            put("additionalProperties", buildJsonObject { put("type", "string") })
        }
        val original = JsonObject(emptyMap())
        assertEquals("Property name is required", validateAdditionalPropertyName(open, "", emptySet()))
        assertEquals("Property name is required", validateAdditionalPropertyName(open, "  ", emptySet()))
        val config = mapOf<String, Any?>("allowEmptyPropertyNames" to true)
        assertNull(validateAdditionalPropertyName(open, "", emptySet(), config))
        assertNull(validateAdditionalPropertyName(open, "  ", emptySet(), config))
        assertNull(validateAdditionalPropertyName(open, "sdf.sdf", emptySet()))
        val empty = addAdditionalProperty(original, original, "", open, config)
        assertEquals(JsonPrimitive(""), empty[""])
        assertEquals(empty, addAdditionalProperty(empty, empty, "", open, config))
        val spaced = addAdditionalProperty(empty, empty, "  two  ", open, config)
        assertEquals(JsonPrimitive(""), spaced["  two  "])
        val dotted = renameAdditionalProperty(spaced, spaced, "  two  ", "sdf.sdf", open, config)
        assertEquals(JsonPrimitive(""), dotted["sdf.sdf"])
        assertNull(dotted["sdf"])
    }

    @Test
    fun `isolated clear keeps a typed default but preserves intentional null`() {
        val text = buildJsonSchema { put("type", "string") }
        val nullable = buildJsonSchema { put("type", JsonArray(listOf(JsonPrimitive("null"), JsonPrimitive("string")))) }
        assertEquals(JsonPrimitive(""), preserveIsolatedAdditionalValue(JsonNull, text))
        assertEquals(JsonNull, preserveIsolatedAdditionalValue(JsonNull, nullable))
        assertEquals(JsonPrimitive("value"), preserveIsolatedAdditionalValue(JsonPrimitive("value"), text))
    }

    @Test
    fun `propertyNames applies composed schema and root reference`() {
        val schema = buildJsonSchema {
            put("type", "object")
            put("\$defs", buildJsonObject {
                put("sensorPrefix", buildJsonObject { put("pattern", "^sensor-") })
            })
            put("propertyNames", buildJsonObject {
                put("allOf", JsonArray(listOf(
                    buildJsonObject { put("\$ref", "#/\$defs/sensorPrefix") },
                    buildJsonObject { put("minLength", 12) },
                )))
            })
            put("additionalProperties", buildJsonObject { put("type", "number") })
        }
        val validator = OptimumCodeJsonSchemaValidator()
        assertTrue(validateAdditionalPropertyName(schema, "sensor-x", emptySet(), rootSchema = schema,
            validator = validator) != null)
        assertTrue(validateAdditionalPropertyName(schema, "other-name-long", emptySet(), rootSchema = schema,
            validator = validator) != null)
        assertNull(validateAdditionalPropertyName(schema, "sensor-deck-b", emptySet(), rootSchema = schema,
            validator = validator))
    }

    @Test
    fun `overlapping patterns retain both bounds regardless of declaration order`() {
        fun schema(reverse: Boolean) = buildJsonSchema {
            put("type", "object")
            put("patternProperties", buildJsonObject {
                if (reverse) put("_total$", buildJsonObject { put("maximum", 1000) })
                put("^price_", buildJsonObject { put("type", "number"); put("minimum", 0) })
                if (!reverse) put("_total$", buildJsonObject { put("maximum", 1000) })
            })
            put("additionalProperties", true)
        }
        for (reverse in listOf(false, true)) {
            val root = schema(reverse)
            val selected = requireNotNull(additionalPropertySchema(root, "price_total"))
            assertEquals(2, selected.allOf?.size)
            assertFalse(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(-1), selected, true, root))
            assertFalse(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(1001), selected, true, root))
            assertTrue(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(750), selected, true, root))
            assertEquals(0, additionalPropertySchema(root, "price_other")?.minimum?.toInt())
            assertTrue(additionalPropertySchema(root, "other")?.typeValue is JsonSchemaTypeValue.Multiple)
        }
    }

    @Test
    fun `multiple overlapping bounds use the strongest limit and structural schemas stay conjunctive`() {
        val numeric = buildJsonSchema {
            put("patternProperties", buildJsonObject {
                put("^price_", buildJsonObject { put("type", "number"); put("minimum", 0); put("maximum", 2000) })
                put("_total$", buildJsonObject { put("minimum", 10); put("maximum", 1000) })
                put("price_total", buildJsonObject { put("minimum", 20); put("maximum", 800) })
            })
            put("additionalProperties", true)
        }
        val selected = requireNotNull(additionalPropertySchema(numeric, "price_total"))
        assertEquals(3, selected.allOf?.size)
        assertFalse(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(15), selected, true, numeric))
        assertFalse(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(900), selected, true, numeric))
        assertTrue(acceptsRestrictedNumericEdit(JsonPrimitive(500), JsonPrimitive(750), selected, true, numeric))

        val structural = buildJsonSchema {
            put("patternProperties", buildJsonObject {
                put("^x", buildJsonObject { put("type", "object") })
                put("x$", buildJsonObject { put("required", JsonArray(listOf(JsonPrimitive("left")))) })
            })
            put("additionalProperties", false)
        }
        val conjunctive = requireNotNull(additionalPropertySchema(structural, "x"))
        assertEquals(2, conjunctive.allOf?.size)
        assertEquals(null, conjunctive.typeValue)
    }
}
