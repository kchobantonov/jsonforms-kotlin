package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PlainEnumCellTesterTest {
    private fun rank(schema: String, options: Map<String, JsonPrimitive> = emptyMap()) =
        plainEnumCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())
    private fun oneOfRank(schema: String, options: Map<String, JsonPrimitive> = emptyMap()) =
        plainOneOfEnumCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())
    private fun constRank(schema: String, options: Map<String, JsonPrimitive> = emptyMap()) =
        plainConstCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())

    @Test fun explicitEnumUsesTypedCompactSelector() {
        assertEquals(103, rank("""{"enum":[1,"1",true]}"""))
        assertEquals(103, rank("""{"type":"string","enum":["a","b"]}"""))
    }

    @Test fun otherChoiceContractsRetainTheirRenderer() {
        for (schema in listOf(
            """{"const":"a"}""",
            """{"oneOf":[{"const":1}]}""",
            """{"enum":[1],"anyOf":[{"type":"number"}]}""",
            """{"\u0024ref":"#/definitions/x","enum":[1]}""",
            """{"type":"number"}""",
        )) assertEquals(NOT_APPLICABLE, rank(schema), schema)
        assertEquals(NOT_APPLICABLE, rank("""{"enum":[1]}""",
            mapOf("variant" to JsonPrimitive("radio"))))
    }

    @Test fun constOneOfUsesTypedCompactSelector() {
        assertEquals(105, oneOfRank("""{"oneOf":[{"const":1,"title":"Same"},{"const":"1","title":"Same"}]}"""))
        for (schema in listOf(
            """{"oneOf":[{"type":"string"}]}""",
            """{"const":1}""",
            """{"enum":[1]}""",
            """{"oneOf":[{"const":1}],"allOf":[{"type":"number"}]}""",
        )) assertEquals(NOT_APPLICABLE, oneOfRank(schema), schema)
        assertEquals(NOT_APPLICABLE, oneOfRank("""{"oneOf":[{"const":1}]}""",
            mapOf("format" to JsonPrimitive("radio"))))
    }

    @Test fun scalarConstUsesSelectorButBooleanAgreementKeepsItsToggle() {
        assertEquals(104, constRank("""{"const":"ready"}"""))
        assertEquals(104, constRank("""{"const":"true"}"""))
        assertEquals(104, constRank("""{"const":5}"""))
        for (schema in listOf(
            """{"const":true}""",
            """{"const":false}""",
            """{"const":null}""",
            """{"const":{"x":1}}""",
            """{"const":"ready","enum":["ready"]}""",
            """{"const":5,"oneOf":[{"const":5}]}""",
        )) assertEquals(NOT_APPLICABLE, constRank(schema), schema)
        assertEquals(NOT_APPLICABLE, constRank("""{"const":5}""",
            mapOf("format" to JsonPrimitive("radio"))))
    }
}
