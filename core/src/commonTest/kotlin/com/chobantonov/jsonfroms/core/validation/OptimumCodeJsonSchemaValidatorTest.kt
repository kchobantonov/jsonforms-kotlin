package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class OptimumCodeJsonSchemaValidatorTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    @Test
    fun genericSchemaUriUsesLatestSupportedDialect() {
        val schema = buildJsonSchema {
            put("\$schema", "http://json-schema.org/schema#")
            put("type", "string")
            put("minLength", 3)
        }

        val errors = validator.validate(schema, JsonPrimitive("x"))

        assertEquals(1, errors.size)
        assertEquals("minLength", errors.single().keyword)
    }

    @Test
    fun unsupportedSchemaUriBecomesValidationError() {
        val schema = buildJsonSchema {
            put("\$schema", "https://example.com/not-a-json-schema-dialect")
            put("type", "object")
        }

        val errors = validator.validate(schema, buildJsonObject {})

        assertEquals(1, errors.size)
        assertEquals("\$schema", errors.single().keyword)
        assertEquals("/\$schema", errors.single().schemaPath)
        assertTrue(errors.single().message.startsWith("Unable to compile JSON Schema:"))
    }
}
