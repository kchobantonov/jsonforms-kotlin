package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.OptimumCodeJsonSchemaValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class LocalReferenceRebaseTest {
    @Test fun nestedRootReferenceResolvesAndDataLiteralsStayUntouched() {
        val root = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","${'$'}defs":{"minimumCode":{"type":"string","minLength":3},"code":{"${'$'}ref":"#/${'$'}defs/minimumCode"}}}"""
        ).jsonObject)
        val extracted = JsonSchema(Json.parseToJsonElement(
            """{"type":"object","properties":{"code":{"${'$'}ref":"#/${'$'}defs/code"}},"default":{"${'$'}ref":"#/data"}}"""
        ).jsonObject)
        val rebased = rebaseLocalSchemaReferences(extracted, root)
        val rewrittenCode = rebased.value["properties"]!!.jsonObject["code"]!!.jsonObject["${'$'}ref"]
        assertEquals(JsonPrimitive("#/definitions/__jsonforms_root/${'$'}defs/code"), rewrittenCode)
        assertEquals(JsonPrimitive("#/data"), rebased.value["default"]!!.jsonObject["${'$'}ref"])
        val rootCode = rebased.value["definitions"]!!.jsonObject["__jsonforms_root"]!!.jsonObject["${'$'}defs"]!!
            .jsonObject["code"]!!.jsonObject["${'$'}ref"]
        assertEquals(JsonPrimitive("#/definitions/__jsonforms_root/${'$'}defs/minimumCode"), rootCode)
        val validator = OptimumCodeJsonSchemaValidator().compile(rebased)
        assertTrue(validator.validate(Json.parseToJsonElement("""{"code":"x"}""")).isNotEmpty())
        assertTrue(validator.validate(Json.parseToJsonElement("""{"code":"valid"}""")).isEmpty())
        val literals = rebaseLocalSchemaReferences(JsonSchema(Json.parseToJsonElement(
            """{"default":{"${'$'}ref":"#/data"},"enum":[{"${'$'}ref":"#/data"}],"const":{"${'$'}ref":"#/data"}}"""
        ).jsonObject), root)
        assertEquals(JsonPrimitive("#/data"), literals.value["enum"]!!.jsonArray[0].jsonObject["${'$'}ref"])
        assertEquals(JsonPrimitive("#/data"), literals.value["const"]!!.jsonObject["${'$'}ref"])
    }
}
