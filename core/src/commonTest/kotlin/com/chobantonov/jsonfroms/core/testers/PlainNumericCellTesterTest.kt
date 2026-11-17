package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PlainNumericCellTesterTest {
    private val context = TesterContext()
    private fun rank(json: String, options: Map<String, JsonPrimitive> = emptyMap()): Int =
        plainNumericCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(json).jsonObject), context)

    @Test fun ordinaryIntegerAndNumberUseNumericCell() {
        assertEquals(2, rank("""{"type":"integer","minimum":0,"maximum":10}"""))
        assertEquals(2, rank("""{"type":"number","multipleOf":0.5}"""))
        assertEquals(2, rank("""{"type":"integer"}""", mapOf("step" to JsonPrimitive(2))))
    }

    @Test fun specializedNumericContractsStayOnRendererPath() {
        for (json in listOf(
            """{"type":"number","format":"currency"}""",
            """{"type":"integer","enum":[1,2]}""",
            """{"type":"number","const":1}""",
            """{"type":"integer","oneOf":[{"const":1}]}""",
            """{"type":"number","anyOf":[{"minimum":0}]}""",
            """{"type":"number","allOf":[{"maximum":10}]}""",
            """{"\u0024ref":"#/definitions/count"}""",
            """{"type":"string"}""",
        )) assertEquals(NOT_APPLICABLE, rank(json), json)
        assertEquals(NOT_APPLICABLE, rank("""{"type":"number"}""",
            mapOf("slider" to JsonPrimitive(true))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"integer"}""",
            mapOf("format" to JsonPrimitive(true))))
    }
}
