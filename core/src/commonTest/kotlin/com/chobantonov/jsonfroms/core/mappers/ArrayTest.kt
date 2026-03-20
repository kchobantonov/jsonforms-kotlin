package com.chobantonov.jsonfroms.core.mappers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonPrimitive

class ArrayTest {
    @Test
    fun `Move up should move item up by one index`() {
        val array = listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)

        assertEquals(listOf(1, 3, 2, 4, 5).map(::JsonPrimitive), moveUp(array, 2))
    }

    @Test
    fun `Move up should not change array if item to move is the first item `() {
        val array = listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)

        assertEquals(array, moveUp(array, 0))
    }

    @Test
    fun `Move down should move item down by one index`() {
        val array = listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)

        assertEquals(listOf(1, 2, 4, 3, 5).map(::JsonPrimitive), moveDown(array, 2))
    }

    @Test
    fun `Move down should not change array if item to move is the last item `() {
        val array = listOf(1, 2, 3, 4, 5).map(::JsonPrimitive)

        assertEquals(array, moveDown(array, 4))
    }
}
