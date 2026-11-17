package com.chobantonov.jsonfroms.material.renderers.additional

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

data class ListDetailSelection(val index: Int, val source: JsonElement)

/** Preserve an occurrence only when the new array proves which row survived. */
fun reconcileListDetailSelection(
    selection: ListDetailSelection?,
    before: JsonArray,
    after: JsonArray,
): ListDetailSelection? {
    if (selection == null || before === after) return selection
    if (selection.index !in before.indices) return null
    val source = selection.source
    fun same(value: JsonElement): Boolean = when (source) {
        is JsonPrimitive, JsonNull -> value == source
        else -> value === source
    }
    val previousMatches = before.indices.filter { same(before[it]) }
    val nextMatches = after.indices.filter { same(after[it]) }
    if (previousMatches.size != 1 || previousMatches.single() != selection.index || nextMatches.size > 1)
        return null
    if (nextMatches.size == 1) return ListDetailSelection(nextMatches.single(), source)

    // An immutable field edit replaces only the selected row. Other row references
    // remain in place; a broader replacement does not establish row identity.
    if (after.size == before.size && before.indices.all { index ->
            index == selection.index || before[index] === after[index]
        }
    ) return ListDetailSelection(selection.index, after[selection.index])
    return null
}

/** Compose effect keys normally compare structural equality; row identity needs reference changes. */
class CollectionArrayReference(private val array: JsonArray) {
    override fun equals(other: Any?): Boolean = other is CollectionArrayReference && array === other.array
    override fun hashCode(): Int = 0
}
