package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class RadioChoiceCellTesterTest {
    private fun rank(schema: String, options: Map<String, JsonPrimitive>) =
        radioChoiceCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())

    @Test fun authoredRadioWinsForEnumAndConstOneOf() {
        val radio = mapOf("format" to JsonPrimitive("radio"))
        assertEquals(120, rank("""{"enum":[1,"1"]}""", radio))
        assertEquals(120, rank("""{"oneOf":[{"const":1},{"const":"1"}]}""", radio))
        assertEquals(120, rank("""{"const":true}""", radio))
    }

    @Test fun otherContractsDoNotBecomeRadioCells() {
        val radio = mapOf("format" to JsonPrimitive("radio"))
        assertEquals(NOT_APPLICABLE, rank("""{"enum":[1]}""", emptyMap()))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"number"}""", radio))
        assertEquals(NOT_APPLICABLE, rank("""{"oneOf":[{"type":"number"}]}""", radio))
        assertEquals(NOT_APPLICABLE, rank("""{"\u0024ref":"#/definitions/x","enum":[1]}""", radio))
        assertEquals(NOT_APPLICABLE, rank("""{"enum":[1]}""",
            radio + ("variant" to JsonPrimitive("cards"))))
    }
}
