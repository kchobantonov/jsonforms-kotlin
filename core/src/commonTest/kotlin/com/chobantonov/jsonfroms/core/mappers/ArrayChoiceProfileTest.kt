package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrayChoiceProfileTest {
    private fun schema(source: String) = JsonSchema(Json.parseToJsonElement(source).jsonObject)

    @Test fun finiteStringEnumAndTitledOneOfRemainTyped() {
        val enum = schema("""{"type":"array","uniqueItems":true,"items":{"type":"string","enum":["ops","fin"]}}""")
        assertTrue(supportsMultiSelectArray(enum, enum))
        assertEquals(listOf(JsonPrimitive("ops"), JsonPrimitive("fin")),
            finiteStringArrayChoices(enum, enum)?.map { it.value })
        val oneOf = schema("""{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":"ops","title":"Operations"},{"const":"fin","title":"Finance"}]}}""")
        assertEquals(listOf("Operations", "Finance"), finiteStringArrayChoices(oneOf, oneOf)?.map { it.label })
    }

    @Test fun rejectsNonUniqueFreeAndStructuredChoices() {
        val nonUnique = schema("""{"type":"array","items":{"type":"string","enum":["ops"]}}""")
        assertFalse(supportsMultiSelectArray(nonUnique, nonUnique))
        val free = schema("""{"type":"array","uniqueItems":true,"items":{"type":"string"}}""")
        assertNull(finiteStringArrayChoices(free, free))
        val structured = schema("""{"type":"array","uniqueItems":true,"items":{"oneOf":[{"const":{"code":"ops"},"title":"Operations"}]}}""")
        assertNull(finiteStringArrayChoices(structured, structured))
        val mixed = schema("""{"type":"array","uniqueItems":true,"items":{"enum":["ops",1]}}""")
        assertNull(finiteStringArrayChoices(mixed, mixed))
    }

    @Test fun transactionsPreserveOrderIdentityAndStaleTargets() {
        val choices = listOf(StringArrayChoice(JsonPrimitive("1"), "One"),
            StringArrayChoice(JsonPrimitive("2"), "Two"))
        val original = JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("1")))
        assertEquals(original, addFiniteStringChoice(original, JsonPrimitive("1"), choices, 3, true))
        assertEquals(original, addFiniteStringChoice(original, JsonPrimitive("unknown"), choices, 3, true))
        assertEquals(JsonArray(original + JsonPrimitive("2")),
            addFiniteStringChoice(original, JsonPrimitive("2"), choices, 3, true))
        assertEquals(JsonArray(listOf(JsonPrimitive("2"))),
            addFiniteStringChoice(JsonPrimitive("invalid imported shape"), JsonPrimitive("2"), choices, 3, true))
        assertEquals(original, removeArrayChoiceOccurrence(original, 0, JsonPrimitive("1"), 0, true))
        assertEquals(JsonArray(listOf(JsonPrimitive(1))),
            removeArrayChoiceOccurrence(original, 1, JsonPrimitive("1"), 1, true))
        assertEquals(original, removeArrayChoiceOccurrence(original, 1, JsonPrimitive("1"), 2, true))
        val duplicates = JsonArray(listOf(JsonPrimitive("1"), JsonPrimitive("1")))
        assertEquals(JsonArray(listOf(JsonPrimitive("1"))),
            removeArrayChoiceOccurrence(duplicates, 1, JsonPrimitive("1"), 1, true))
    }

    @Test fun searchRequiresTrueAndLocalFalseWins() {
        assertFalse(choiceAutocomplete(null, emptyMap()))
        assertTrue(choiceAutocomplete(null, mapOf("autocomplete" to true)))
        assertFalse(choiceAutocomplete(JsonPrimitive(false), mapOf("autocomplete" to true)))
        assertTrue(choiceAutocomplete(JsonPrimitive(true), mapOf("autocomplete" to false)))
        assertFalse(choiceAutocomplete(JsonPrimitive("true"), emptyMap()))
        assertTrue(choiceAutocomplete(JsonPrimitive("false"), mapOf("autocomplete" to true)))
        assertFalse(choiceAutocomplete(null, mapOf("autocomplete" to JsonPrimitive("true"))))
    }

    @Test fun chipsAllowFreeDuplicatesAndRemoveOnlyActedOccurrence() {
        val free = schema("""{"type":"array","items":{"type":"string","minLength":2}}""")
        assertTrue(supportsChipsArray(free, free))
        assertNull(finiteChipsChoices(free, free))
        val first = addChipValue(null, "a", null, false, 2, true)
        val repeated = addChipValue(first, "a", null, false, 2, true)
        assertEquals(JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("a"))), repeated)
        assertEquals(repeated, addChipValue(repeated, "b", null, false, 2, true))
        assertEquals(JsonArray(listOf(JsonPrimitive("a"))),
            removeArrayChoiceOccurrence(repeated, 1, JsonPrimitive("a"), 1, true))
        assertEquals(repeated, removeArrayChoiceOccurrence(repeated, 1, JsonPrimitive("b"), 0, true))
    }

    @Test fun chipsFiniteConstAndUniqueRules() {
        val finite = schema("""{"type":"array","uniqueItems":true,"items":{"type":"string","const":"ops","title":"Operations"}}""")
        assertTrue(supportsChipsArray(finite, finite))
        val choices = finiteChipsChoices(finite, finite)
        assertEquals(listOf("Operations"), choices?.map { it.label })
        val selected = addChipValue(null, "ops", choices, true, null, true)
        assertEquals(selected, addChipValue(selected, "ops", choices, true, null, true))
        assertEquals(selected, addChipValue(selected, "unknown", choices, true, null, true))
        val structured = schema("""{"type":"array","items":{"const":{"x":1}}}""")
        assertFalse(supportsChipsArray(structured, structured))
    }

    @Test fun replacementPreservesCountAndRejectsStaleOrDuplicateTargets() {
        val choices = listOf("ops", "fin", "qa").map {
            StringArrayChoice(JsonPrimitive(it), it)
        }
        val current = JsonArray(listOf(JsonPrimitive("ops"), JsonPrimitive("fin")))
        assertEquals(JsonArray(listOf(JsonPrimitive("qa"), JsonPrimitive("fin"))),
            replaceArrayChoiceOccurrence(current, 0, JsonPrimitive("ops"),
                JsonPrimitive("qa"), choices, true))
        assertEquals(current, replaceArrayChoiceOccurrence(current, 0, JsonPrimitive("stale"),
            JsonPrimitive("qa"), choices, true))
        assertEquals(current, replaceArrayChoiceOccurrence(current, 0, JsonPrimitive("ops"),
            JsonPrimitive("fin"), choices, true))
        assertEquals(current, replaceArrayChoiceOccurrence(current, 0, JsonPrimitive("ops"),
            JsonPrimitive("unknown"), choices, true))
    }
}
