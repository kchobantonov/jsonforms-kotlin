package com.chobantonov.jsonfroms.core.util

/** Whether a proposed string edit may commit under the supported maxLength interaction guard. */
fun acceptsRestrictedStringEdit(
    current: String,
    proposed: String,
    maxLength: Int?,
    restrict: Boolean,
): Boolean {
    if (!restrict || maxLength == null || maxLength < 0) return true
    val nextLength = jsonStringLength(proposed)
    if (nextLength <= maxLength) return true
    val currentLength = jsonStringLength(current)
    return currentLength > maxLength && nextLength < currentLength
}

/** JSON Schema string length counts Unicode code points rather than UTF-16 code units. */
fun jsonStringLength(value: String): Int {
    var count = 0
    var index = 0
    while (index < value.length) {
        val current = value[index].code
        if (current in 0xD800..0xDBFF && index + 1 < value.length &&
            value[index + 1].code in 0xDC00..0xDFFF) index++
        count++
        index++
    }
    return count
}
