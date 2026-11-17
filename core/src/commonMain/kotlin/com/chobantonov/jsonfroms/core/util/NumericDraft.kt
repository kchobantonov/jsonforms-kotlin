package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonPrimitive

/** Largest integer whose numeric value survives JavaScript Number conversion unchanged. */
const val MAX_PORTABLE_INTEGER: Long = 9_007_199_254_740_991L

/** Complete numeric text only. Null means empty, incomplete, unsupported, or out of range. */
fun parseIntegerDraft(text: String): JsonPrimitive? =
    text.toLongOrNull()?.takeIf { it in -MAX_PORTABLE_INTEGER..MAX_PORTABLE_INTEGER }
        ?.let(::JsonPrimitive)

/** Reject values whose decimal text changes during Double conversion. */
fun parseNumberDraft(text: String): JsonPrimitive? {
    val number = text.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return null
    if (normalizedDecimal(text) != normalizedDecimal(number.toString())) return null
    return JsonPrimitive(number)
}

internal data class DecimalValue(val negative: Boolean, val digits: String, val exponent: Int)

private val decimalPattern = Regex("""([+-]?)(?:(\d+)(?:\.(\d*))?|\.(\d+))(?:[eE]([+-]?\d+))?""")

internal fun normalizedDecimal(text: String): DecimalValue? {
    val parts = decimalPattern.matchEntire(text)?.groupValues ?: return null
    val fractional = if (parts[2].isNotEmpty()) parts[3] else parts[4]
    val significant = (parts[2] + fractional).trimStart('0')
    if (significant.isEmpty()) return DecimalValue(parts[1] == "-", "0", 0)
    val trailingZeros = significant.takeLastWhile { it == '0' }.length
    val parsedExponent = parts[5].takeIf(String::isNotEmpty)?.toIntOrNull()
    if (parts[5].isNotEmpty() && parsedExponent == null) return null
    val exponent = (parsedExponent ?: 0).toLong() - fractional.length + trailingZeros
    if (exponent !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
    return DecimalValue(parts[1] == "-", significant.dropLast(trailingZeros), exponent.toInt())
}
