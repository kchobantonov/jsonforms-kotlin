package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class PlainTemporalCellTesterTest {
    private fun rank(schema: String, options: Map<String, JsonPrimitive> = emptyMap()) =
        plainTemporalCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())

    @Test fun dateTimeAndDateTimeFormatsUseCompactCells() {
        for (format in listOf("date", "time", "date-time")) {
            assertEquals(104, rank("""{"type":"string","format":"$format"}"""))
            assertEquals(104, rank("""{"type":"string"}""",
                mapOf("format" to JsonPrimitive(format))))
        }
    }

    @Test fun ambiguousAndSpecializedStringsKeepExistingDispatch() {
        for (schema in listOf(
            """{"type":"string"}""",
            """{"type":"string","format":"password"}""",
            """{"type":"number","format":"date"}""",
            """{"type":"string","format":"date","enum":["2024-01-01"]}""",
            """{"type":"string","format":"date","oneOf":[{"const":"2024-01-01"}]}""",
        )) assertEquals(NOT_APPLICABLE, rank(schema), schema)
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string","format":"date"}""",
            mapOf("format" to JsonPrimitive("time"))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"string","format":"date"}""",
            mapOf("variant" to JsonPrimitive("custom"))))
    }
}
