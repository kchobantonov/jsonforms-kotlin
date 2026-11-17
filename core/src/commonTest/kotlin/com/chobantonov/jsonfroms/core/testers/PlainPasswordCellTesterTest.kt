package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PlainPasswordCellTesterTest {
    private fun rank(schema: String, options: Map<String, JsonPrimitive> = emptyMap()) =
        plainPasswordCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())

    @Test fun schemaAndUiFormatsSelectCompactPasswordCell() {
        assertEquals(104, rank("""{"type":"string","format":"password"}"""))
        assertEquals(104, rank("""{"type":"string"}""",
            mapOf("format" to JsonPrimitive("password"))))
        assertEquals(104, rank("""{"type":"string","format":"password"}""",
            mapOf("variant" to JsonPrimitive("otp"))))
    }

    @Test fun unrelatedAndAmbiguousSchemasKeepExistingDispatch() {
        for (schema in listOf(
            """{"type":"string"}""",
            """{"type":"number","format":"password"}""",
            """{"type":"string","format":"password","enum":["secret"]}""",
            """{"type":"string","format":"password","oneOf":[{"const":"secret"}]}""",
            """{"type":"string","format":"password","${'$'}ref":"#/${'$'}defs/password"}""",
        )) assertEquals(NOT_APPLICABLE, rank(schema), schema)
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string","format":"password"}""",
            mapOf("format" to JsonPrimitive("date"))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string","format":"password"}""",
            mapOf("variant" to JsonPrimitive("custom"))))
    }
}
