package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumericDraftTest {
    @Test fun numericStepsResolvePrecedenceAndKeepDecimalValuesExact() {
        assertEquals(0.25, resolveNumericStep(null, 0.25, integer = false).increment)
        assertEquals(0.5, resolveNumericStep(JsonPrimitive(0.5), 0.25, integer = false).increment)
        assertEquals(1.0, resolveNumericStep(null, 0.25, integer = true).increment)
        assertEquals(3.0, resolveNumericStep(null, 3.0, integer = true).increment)
        assertEquals(NumericStep(null, true), resolveNumericStep(JsonPrimitive(0.5), 3.0, integer = true))
        assertEquals(NumericStep(null, true), resolveNumericStep(JsonPrimitive(-1), null, integer = false))
        assertEquals(JsonPrimitive(0.3), stepNumericDraft(JsonPrimitive(0.2), 0.1, false, 1))
        assertEquals(JsonPrimitive(9.75), stepNumericDraft(JsonPrimitive(9.5), 0.25, false, 1))
        assertEquals(JsonPrimitive(8L), stepNumericDraft(JsonPrimitive(5), 3.0, true, 1))
        assertEquals(JsonPrimitive(2L), stepNumericDraft(JsonPrimitive(5), 3.0, true, -1))
        assertNull(stepNumericDraft(JsonPrimitive(MAX_PORTABLE_INTEGER), 1.0, true, 1))
        assertNull(stepNumericDraft(JsonPrimitive(1e100), 0.1, false, 1))
    }

    @Test fun restrictedBoundsPreventNewViolationsAndPermitRepair() {
        val inclusive = JsonSchema(Json.parseToJsonElement(
            """{"type":"number","minimum":0,"maximum":10}""").jsonObject)
        fun accepts(current: Int, next: Int, schema: JsonSchema = inclusive, restrict: Boolean = true) =
            acceptsRestrictedNumericEdit(JsonPrimitive(current), JsonPrimitive(next), schema, restrict)
        assertEquals(false, accepts(7, 12))
        assertEquals(false, accepts(7, -2))
        assertEquals(true, accepts(7, 10))
        assertEquals(true, accepts(7, 12, restrict = false))
        assertEquals(true, accepts(20, 15))
        assertEquals(false, accepts(20, 21))
        assertEquals(false, accepts(20, -1))
        assertEquals(true, accepts(-5, -3))
        assertEquals(false, accepts(-5, -6))
        assertEquals(true, accepts(-5, 0))

        val exclusive = JsonSchema(Json.parseToJsonElement(
            """{"type":"number","exclusiveMinimum":0,"exclusiveMaximum":10}""").jsonObject)
        assertEquals(false, accepts(5, 0, exclusive))
        assertEquals(false, accepts(5, 10, exclusive))
        assertEquals(true, accepts(0, 1, exclusive))
        assertEquals(true, accepts(10, 9, exclusive))
        val legacyExclusive = JsonSchema(Json.parseToJsonElement(
            """{"type":"number","minimum":0,"maximum":10,"exclusiveMinimum":true,"exclusiveMaximum":true}""").jsonObject)
        assertEquals(false, accepts(5, 0, legacyExclusive))
        assertEquals(false, accepts(5, 10, legacyExclusive))
    }

    @Test fun allOfNumericBoundsIntersectWithoutMergingAlternatives() {
        val root = JsonSchema(Json.parseToJsonElement("""{
          "definitions":{"nonNegative":{"type":"integer","minimum":0}},
          "type":"object","properties":{
            "minLength":{"allOf":[{"${'$'}ref":"#/definitions/nonNegative"},{"default":0}]},
            "bounded":{"type":"number","minimum":-10,"allOf":[{"minimum":0},{"exclusiveMinimum":2},{"maximum":10},{"exclusiveMaximum":8}]},
            "alternatives":{"type":"integer","anyOf":[{"maximum":10},{"minimum":20}]}
          }
        }""").jsonObject)
        val properties = root.value.getValue("properties").jsonObject
        fun property(name: String) = JsonSchema(properties.getValue(name).jsonObject)
        fun accepts(name: String, current: Int, next: Int, restrict: Boolean = true) =
            acceptsRestrictedNumericEdit(JsonPrimitive(current), JsonPrimitive(next), property(name), restrict, root)

        assertEquals(false, accepts("minLength", 1, -1))
        assertEquals(true, accepts("minLength", -3, -2))
        assertEquals(true, accepts("minLength", -3, 0))
        assertEquals(true, accepts("minLength", 1, -1, restrict = false))
        assertEquals(false, accepts("bounded", 4, 2))
        assertEquals(true, accepts("bounded", 4, 3))
        assertEquals(false, accepts("bounded", 4, 8))
        assertEquals(true, accepts("bounded", 4, 7))
        assertEquals(true, accepts("alternatives", 5, 25))
    }

    @Test fun integerParserCommitsOnlyCompleteLongValues() {
        assertEquals(JsonPrimitive(19L), parseIntegerDraft("19"))
        assertEquals(JsonPrimitive(MAX_PORTABLE_INTEGER), parseIntegerDraft(MAX_PORTABLE_INTEGER.toString()))
        for (text in listOf("", "-", "1.9", "1e3", "19x", "9007199254740992",
            "9223372036854775808"))
            assertNull(parseIntegerDraft(text), text)
    }

    @Test fun numberParserRejectsIncompleteAndNonfiniteValues() {
        assertEquals(JsonPrimitive(1.9), parseNumberDraft("1.9"))
        assertEquals(JsonPrimitive(1000.0), parseNumberDraft("1e3"))
        assertEquals(JsonPrimitive(1.0), parseNumberDraft("1.000"))
        for (text in listOf("", "-", ".", "1e", "19x", "NaN", "Infinity", "1e309",
            "9007199254740993", "1.0000000000000001", "1e-1000"))
            assertNull(parseNumberDraft(text), text)
    }
}
