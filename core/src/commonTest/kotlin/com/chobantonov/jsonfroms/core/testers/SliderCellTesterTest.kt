package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class SliderCellTesterTest {
    private fun rank(schema: String, options: Map<String, JsonPrimitive>) =
        sliderCellTester(ControlElement("#", options = options),
            JsonSchema(Json.parseToJsonElement(schema).jsonObject), TesterContext())

    private val bounded = """{"type":"integer","minimum":1,"maximum":6,"default":2,"multipleOf":2}"""

    @Test fun boundedOptInSliderGetsNativeCell() {
        assertEquals(110, rank(bounded, mapOf("slider" to JsonPrimitive(true))))
        assertEquals(110, rank("""{"type":"number","minimum":0,"maximum":1,"default":0.5}""",
            mapOf("slider" to JsonPrimitive(true))))
    }

    @Test fun missingBoundsAndSpecializedSchemasKeepTheirDispatch() {
        assertEquals(NOT_APPLICABLE, rank(bounded, emptyMap()))
        assertEquals(NOT_APPLICABLE, rank(bounded, mapOf("slider" to JsonPrimitive("true"))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"integer","minimum":0,"maximum":10}""",
            mapOf("slider" to JsonPrimitive(true))))
        assertEquals(NOT_APPLICABLE, rank("""{"type":"integer","minimum":0,"maximum":10,"default":2,"enum":[2,4]}""",
            mapOf("slider" to JsonPrimitive(true))))
        assertEquals(NOT_APPLICABLE, rank(bounded,
            mapOf("slider" to JsonPrimitive(true), "format" to JsonPrimitive("currency"))))
    }
}
