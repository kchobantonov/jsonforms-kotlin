package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

data class NumericStep(val increment: Double?, val invalidExplicitStep: Boolean = false)

/** An authored step wins; invalid authored values do not silently select another increment. */
fun resolveNumericStep(option: Any?, multipleOf: Double?, integer: Boolean): NumericStep {
    val explicit = when (option) {
        null -> null
        is JsonPrimitive -> option.doubleOrNull
        is Number -> option.toDouble()
        else -> null
    }
    if (option != null) {
        val valid = explicit != null && explicit.isFinite() && explicit > 0.0 &&
            (!integer || explicit % 1.0 == 0.0 && explicit <= MAX_PORTABLE_INTEGER)
        return if (valid) NumericStep(explicit) else NumericStep(null, invalidExplicitStep = true)
    }
    val derived = multipleOf?.takeIf { it.isFinite() && it > 0.0 &&
        (!integer || it % 1.0 == 0.0 && it <= MAX_PORTABLE_INTEGER) }
    return NumericStep(derived ?: if (integer) 1.0 else 0.1)
}

/** Adds decimal increments through scaled integers, avoiding binary Float/Double display artifacts. */
fun stepNumericDraft(current: JsonPrimitive?, step: Double, integer: Boolean, direction: Int): JsonPrimitive? {
    if (current == null || direction !in -1..1 || direction == 0 || !step.isFinite() || step <= 0.0)
        return null
    if (integer) {
        val before = current.content.toLongOrNull() ?: return null
        val increment = step.toLong().takeIf { it.toDouble() == step } ?: return null
        val next = if (direction > 0) before + increment else before - increment
        if (next !in -MAX_PORTABLE_INTEGER..MAX_PORTABLE_INTEGER) return null
        return JsonPrimitive(next)
    }
    val before = normalizedDecimal(current.content) ?: return null
    val increment = normalizedDecimal(step.toString()) ?: return null
    val scale = maxOf(0L, -before.exponent.toLong(), -increment.exponent.toLong())
        .takeIf { it <= 18 }?.toInt() ?: return null
    val left = before.scaledLong(scale) ?: return null
    val right = increment.scaledLong(scale) ?: return null
    val signedStep = if (direction > 0) right else -right
    val next = left + signedStep
    if ((signedStep > 0 && next < left) || (signedStep < 0 && next > left)) return null
    return parseNumberDraft(next.toDecimalText(scale))
}

internal fun DecimalValue.scaledLong(scale: Int): Long? {
    val zeros = exponent.toLong() + scale
    if (zeros < 0 || zeros > 18) return null
    val magnitude = (digits + "0".repeat(zeros.toInt())).toLongOrNull() ?: return null
    return if (negative) -magnitude else magnitude
}

internal fun Long.toDecimalText(scale: Int): String {
    if (scale == 0) return toString()
    val sign = if (this < 0) "-" else ""
    val digits = toString().removePrefix("-").padStart(scale + 1, '0')
    return sign + digits.dropLast(scale) + "." + digits.takeLast(scale).trimEnd('0').ifEmpty { "0" }
}
