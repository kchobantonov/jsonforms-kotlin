package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SliderPositionsTest {
    private fun profile(schema: String) = buildSliderPositions(
        JsonSchema(Json.parseToJsonElement(schema).jsonObject))

    @Test fun multiplesBeginAtZeroRatherThanTrackMinimum() {
        val result = profile("""{"type":"integer","minimum":1,"maximum":6,"default":2,"multipleOf":2}""")
        assertEquals(listOf(2L, 4L, 6L).map(::JsonPrimitive), result.values)
        assertEquals(null, result.error)
        assertEquals(1, result.indexOf(JsonPrimitive(4)))
        assertEquals(-1, result.indexOf(JsonPrimitive(3)))
    }

    @Test fun fractionalMultipleOfPreservesIntegerSchema() {
        val result = profile("""{"type":"integer","minimum":1,"maximum":6,"default":2,"multipleOf":1.5}""")
        assertEquals(listOf(JsonPrimitive(3L), JsonPrimitive(6L)), result.values)
    }

    @Test fun exclusiveBoundsAndDecimalPositionsStayExact() {
        val result = profile("""{"type":"number","minimum":0,"exclusiveMinimum":true,"exclusiveMaximum":1,"default":0.25,"multipleOf":0.25}""")
        assertEquals(listOf(0.25, 0.5, 0.75).map(::JsonPrimitive), result.values)
    }

    @Test fun incompatibleRangeReportsNoPosition() {
        val result = profile("""{"type":"integer","minimum":1,"maximum":1,"default":1,"multipleOf":2}""")
        assertEquals(SliderPositionError.NoPosition, result.error)
    }

    @Test fun denseRangeReportsDiagnostic() {
        val result = profile("""{"type":"number","minimum":0,"maximum":100,"default":0,"multipleOf":0.001}""")
        assertEquals(SliderPositionError.TooManyPositions, result.error)
    }
}
