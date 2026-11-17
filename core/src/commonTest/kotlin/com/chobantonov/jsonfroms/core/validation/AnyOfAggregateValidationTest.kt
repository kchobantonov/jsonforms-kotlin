package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class AnyOfAggregateValidationTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""{
      "type":"object","properties":{"contact":{"type":"object","anyOf":[
        {"type":"object","required":["email"],"properties":{"email":{"type":"string"}}},
        {"type":"object","required":["phone"],"properties":{"phone":{"type":"string"}}}
      ]}}
    }""").jsonObject)

    @Test
    fun failedBranchesIncludeAggregateAnyOfError() {
        val validator = OptimumCodeJsonSchemaValidator()
        val invalid = validator.validate(schema, Json.parseToJsonElement("""{"contact":{}}"""))
        assertEquals(listOf("required", "required", "anyOf"), invalid.map { it.keyword }, invalid.toString())
        assertEquals(listOf("/contact", "/contact", "/contact"), invalid.map { it.instancePath })
        assertEquals("/properties/contact/anyOf", invalid.last().schemaPath)

        assertEquals(emptyList(), validator.validate(schema,
            Json.parseToJsonElement("""{"contact":{"email":"a@example.com"}}""")))
    }
}
