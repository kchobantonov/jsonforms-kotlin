package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class ContainsValidationTest {
    private val validator = OptimumCodeJsonSchemaValidator()

    @Test fun containsDefaultsToOneMatchAndReportsEmptyArrayAtItsOwnPath() {
        val schema = schema("""{"type":"object","properties":{"rows":{"type":"array","contains":{"const":"yes"}}}}""")
        val missing = validator.validate(schema, Json.parseToJsonElement("""{"rows":[]}"""))
        assertTrue(missing.any { it.instancePath == "/rows" && it.keyword == "contains" }, missing.toString())
        assertEquals(emptyList(), validator.validate(schema, Json.parseToJsonElement("""{"rows":["no","yes"]}""")))
    }

    @Test fun minAndMaxContainsCountMatchesRatherThanArrayLength() {
        val schema = schema("""{"type":"array","contains":{"const":"yes"},"minContains":2,"maxContains":2}""")
        assertTrue(validator.validate(schema, Json.parseToJsonElement("""["yes","no","no"]""")).isNotEmpty())
        assertEquals(emptyList(), validator.validate(schema, Json.parseToJsonElement("""["yes","no","yes","no"]""")))
        assertTrue(validator.validate(schema, Json.parseToJsonElement("""["yes","yes","yes"]""")).isNotEmpty())
    }

    @Test fun countKeywordsWithoutContainsHaveNoEffect() {
        val schema = schema("""{"type":"array","minContains":2,"maxContains":2}""")
        assertEquals(emptyList(), validator.validate(schema, Json.parseToJsonElement("""[1]""")))
    }

    @Test fun containsCountsPrefixAndTailWithoutChangingTuplePositions() {
        val schema = schema("""{"${'$'}schema":"https://json-schema.org/draft/2020-12/schema","type":"array","prefixItems":[{"type":"string"}],"items":{"type":"integer"},"contains":{"const":"yes"}}""")
        assertEquals(emptyList(), validator.validate(schema, Json.parseToJsonElement("""["yes",1]""")))
        val invalidTail = validator.validate(schema, Json.parseToJsonElement("""["yes","no"]"""))
        assertTrue(invalidTail.any { it.instancePath == "/1" && it.keyword == "type" }, invalidTail.toString())
        assertTrue(invalidTail.none { it.keyword == "contains" }, invalidTail.toString())
    }

    private fun schema(raw: String) = JsonSchema(Json.parseToJsonElement(raw).jsonObject)
}
