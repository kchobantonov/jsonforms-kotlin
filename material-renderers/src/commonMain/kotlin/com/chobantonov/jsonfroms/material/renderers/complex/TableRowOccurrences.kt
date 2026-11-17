package com.chobantonov.jsonfroms.material.renderers.complex

import kotlinx.serialization.json.JsonArray

/** Keeps a table editor with its row when array actions preserve the row object. */
internal class TableRowOccurrences(initial: JsonArray) {
    private var previous = initial
    private var nextId = 0L
    private var ids = initial.indices.map { nextId++ }
    private var pendingMove: Pair<Int, Int>? = null

    fun moved(from: Int, to: Int) {
        pendingMove = from to to
    }

    fun idsFor(current: JsonArray): List<Long> {
        if (current === previous) return ids
        val move = pendingMove
        pendingMove = null
        if (move != null && current.size == previous.size &&
            move.first in previous.indices && move.second in previous.indices) {
            val order = previous.indices.toMutableList().apply { add(move.second, removeAt(move.first)) }
            if (current.indices.all { index -> current[index] === previous[order[index]] }) {
                ids = order.map(ids::get)
                previous = current
                return ids
            }
        }

        // An ordinary field edit replaces one immutable row; its editor keeps the same identity.
        if (current.size == previous.size && previous.indices.count {
                previous[it] !== current[it]
            } == 1) {
            ids = ids.toList()
            previous = current
            return ids
        }

        // Match only unique surviving references. Ambiguous or replaced rows receive new IDs.
        val resolved = MutableList<Long?>(current.size) { null }
        for (newIndex in current.indices) {
            val matchesBefore = previous.indices.filter { previous[it] === current[newIndex] }
            val matchesAfter = current.indices.filter { current[it] === current[newIndex] }
            if (matchesBefore.size == 1 && matchesAfter.size == 1)
                resolved[newIndex] = ids[matchesBefore.single()]
        }
        ids = resolved.map { it ?: nextId++ }
        previous = current
        return ids
    }
}
