package com.chobantonov.jsonfroms.core.util

private val usedIds: MutableSet<String> = linkedSetOf()

private fun makeId(
    idBase: String,
    iteration: Int,
): String = if (iteration <= 1) idBase else idBase + iteration.toString()

private fun isUniqueId(
    idBase: String,
    iteration: Int,
): Boolean = makeId(idBase, iteration) !in usedIds

fun createId(proposedId: String?): String {
    val safeId = proposedId ?: "undefined"
    var tries = 0
    while (!isUniqueId(safeId, tries)) {
        tries++
    }
    val newId = makeId(safeId, tries)
    usedIds += newId
    return newId
}

fun removeId(id: String): Boolean = usedIds.remove(id)

fun clearAllIds() {
    usedIds.clear()
}
