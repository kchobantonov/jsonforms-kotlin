package com.chobantonov.jsonfroms.material.renderers.complex

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TableRowOccurrencesTest {
    @Test fun editedRowKeepsIdentityAndMoveCarriesItToNewIndex() {
        val first = JsonObject(mapOf("file" to JsonPrimitive("old")))
        val second = JsonObject(mapOf("file" to JsonPrimitive("other")))
        val initial = JsonArray(listOf(first, second))
        val tracker = TableRowOccurrences(initial)
        val firstId = tracker.idsFor(initial)[0]
        val secondId = tracker.idsFor(initial)[1]
        val edited = JsonObject(mapOf("file" to JsonPrimitive("large")))
        assertEquals(listOf(firstId, secondId), tracker.idsFor(JsonArray(listOf(edited, second))))
        tracker.moved(0, 1)
        assertEquals(listOf(secondId, firstId), tracker.idsFor(JsonArray(listOf(second, edited))))
    }

    @Test fun duplicateRowReferencesFollowExplicitMoveButReplacementGetsNewIdentity() {
        val shared = JsonObject(mapOf("file" to JsonPrimitive("same")))
        val initial = JsonArray(listOf(shared, shared))
        val tracker = TableRowOccurrences(initial)
        val ids = tracker.idsFor(initial)
        assertNotEquals(ids[0], ids[1])
        tracker.moved(0, 1)
        assertEquals(ids.reversed(), tracker.idsFor(JsonArray(listOf(shared, shared))))
        val replacement = JsonArray(listOf(
            JsonObject(mapOf("file" to JsonPrimitive("new-a"))),
            JsonObject(mapOf("file" to JsonPrimitive("new-b"))),
        ))
        val replacedIds = tracker.idsFor(replacement)
        assertNotEquals(ids[0], replacedIds[0])
        assertNotEquals(ids[1], replacedIds[1])
    }
}
