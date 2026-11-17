package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PlainStringCellTesterTest {
    private val context = TesterContext()
    private fun schema(json: String) = JsonSchema(Json.parseToJsonElement(json).jsonObject)
    private fun rank(json: String, options: Map<String, JsonElement> = emptyMap()) =
        plainStringCellTester(ControlElement("#", options = options), schema(json), context)

    @Test fun plainStringAndMultilineCanUseCompactCell() {
        assertEquals(1, rank("""{"type":"string","maxLength":8}"""))
        assertEquals(1, rank("""{"type":"string"}""", mapOf("multi" to JsonPrimitive(true))))
        assertEquals(1, rank("""{"type":"string"}""", mapOf("mask" to JsonPrimitive("##-##"))))
    }

    @Test fun specializedStringContractsKeepTheirOwnRenderer() {
        for (json in listOf(
            """{"type":"string","format":"date"}""",
            """{"type":"string","format":"password"}""",
            """{"type":"string","enum":["a","b"]}""",
            """{"type":"string","const":"a"}""",
            """{"type":"string","oneOf":[{"const":"a"}]}""",
            """{"type":"string","anyOf":[{"const":"a"}]}""",
            """{"type":"string","allOf":[{"maxLength":8}]}""",
            """{"\u0024ref":"#/definitions/name"}""",
        )) assertEquals(NOT_APPLICABLE, rank(json), json)
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string"}""",
            mapOf("format" to JsonPrimitive("password"))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string"}""",
            mapOf("variant" to JsonPrimitive("otp"))))
    }

    @Test fun materialInputStyleNamespaceDoesNotSelectPortableVariant() {
        assertEquals(1, rank("""{"type":"string"}""", mapOf(
            "material" to JsonObject(mapOf("inputVariant" to JsonPrimitive("filled"))),
        )))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string"}""", mapOf(
            "variant" to JsonPrimitive("otp"),
        )))
    }
}
