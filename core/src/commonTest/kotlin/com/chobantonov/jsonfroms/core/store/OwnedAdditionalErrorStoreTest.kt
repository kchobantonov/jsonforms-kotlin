package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OwnedAdditionalErrorStoreTest {
    private fun error(path: String, message: String) = ErrorObject(instancePath = path, message = message)

    @Test
    fun oneEditorCannotClearHostOrOtherEditorErrors() {
        val host = error("/email", "server")
        val first = error("/email", "draft A")
        val second = error("/email", "draft B")
        val store = OwnedAdditionalErrorStore(hostErrors = listOf(host))
            .publish("editor-a", "/email", listOf(first))
            .publish("editor-b", "/email", listOf(second))
        assertEquals(listOf(host, first, second), store.errors)
        assertEquals(listOf(host, second), store.clear("editor-a").errors)
        assertEquals(listOf(error("/email", "new server"), first, second),
            store.replaceHost(listOf(error("/email", "new server"))).errors)
    }

    @Test
    fun movingAnIdentifiedOwnerRelocatesOnlyItsOwnTargetAndDescendants() {
        val host = error("/items/1/code", "server")
        val owned = listOf(error("/items/1", "draft"), error("/items/1/code", "child"))
        val moved = OwnedAdditionalErrorStore(hostErrors = listOf(host))
            .publish("editor", "/items/1", owned)
            .move("editor", "/items/0")
        assertEquals(listOf(host, error("/items/0", "draft"), error("/items/0/code", "child")), moved.errors)
    }

    @Test
    fun affectedOwnersUsePointerSegmentBoundaries() {
        val store = OwnedAdditionalErrorStore()
            .publish("first", "/items/1", listOf(error("/items/1", "one")))
            .publish("second", "/items/10", listOf(error("/items/10", "ten")))
            .publish("other", "/other", listOf(error("/other", "other")))
        assertEquals(setOf("first"), store.ownersAffectedBy("/items/1/name"))
        assertEquals(setOf("first", "second"), store.ownersAffectedBy("/items"))
        assertEquals(emptySet(), store.ownersAffectedBy("/unrelated"))
    }

    @Test
    fun pendingBelongsToItsOwnerAndClearsOnPublicationOrDisposal() {
        val store = OwnedAdditionalErrorStore()
            .setPending("a", true)
            .setPending("b", true)
        assertTrue(store.pending)
        val published = store.publish("a", "/a", listOf(error("/a", "invalid")))
        assertTrue(published.pending)
        val afterDisposal = published.clear("b")
        assertFalse(afterDisposal.pending)
        assertEquals(listOf(error("/a", "invalid")), afterDisposal.errors)
    }
}
