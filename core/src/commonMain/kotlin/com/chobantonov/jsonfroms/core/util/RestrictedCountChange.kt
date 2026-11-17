package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** Applies collection bounds to the result, while permitting steps toward validity. */
fun acceptsRestrictedCountChange(
    current: Int,
    proposed: Int,
    minItems: Int?,
    maxItems: Int?,
    restrict: Boolean,
): Boolean {
    if (!restrict || proposed == current) return true
    if (proposed < current && minItems != null && proposed < minItems) return false
    if (proposed > current && maxItems != null && proposed > maxItems) return false
    return true
}

/** Appends only to the array observed when Add was opened, rechecking the resulting count. */
fun appendArrayItemIfAllowed(
    current: JsonElement?,
    snapshot: JsonElement?,
    item: JsonElement,
    maxItems: Int?,
    restrict: Boolean,
): JsonElement? {
    if (current !== snapshot) return current
    val array = current as? JsonArray ?: JsonArray(emptyList())
    if (!acceptsRestrictedCountChange(array.size, array.size + 1,
            null, maxItems, restrict)) return current
    return JsonArray(array + item)
}
