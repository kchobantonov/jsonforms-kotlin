package com.chobantonov.jsonfroms.core.util

private data class PrefixState(val used: MutableSet<Int> = mutableSetOf(), var next: Int = 0)

private val idSlots = mutableMapOf<String, Pair<String, Int>>()
private val prefixStates = mutableMapOf<String, PrefixState>()

private fun makeId(base: String, index: Int): String = if (index <= 1) base else "$base$index"

fun createId(proposedId: String?): String {
    val base = proposedId ?: "undefined"
    val state = prefixStates.getOrPut(base) { PrefixState() }
    var index = state.next
    while (index in state.used || makeId(base, index) in idSlots) index++
    val id = makeId(base, index)
    state.used += index
    state.next = index + 1
    idSlots[id] = base to index
    return id
}

fun removeId(id: String): Boolean {
    val (base, index) = idSlots.remove(id) ?: return false
    val state = prefixStates[base] ?: return true
    state.used.remove(index)
    state.next = minOf(state.next, index)
    if (state.used.isEmpty()) prefixStates.remove(base)
    return true
}

fun clearAllIds() {
    idSlots.clear()
    prefixStates.clear()
}

/** Override these functions when a host needs its own control ID strategy. */
object Id {
    var createId: (String?) -> String = { com.chobantonov.jsonfroms.core.util.createId(it) }
    var removeId: (String) -> Boolean = { com.chobantonov.jsonfroms.core.util.removeId(it) }
    var clearAllIds: () -> Unit = { com.chobantonov.jsonfroms.core.util.clearAllIds() }
}
