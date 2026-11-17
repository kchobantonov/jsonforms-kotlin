package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

enum class SliderPositionError { InvalidRange, InvalidIncrement, NoPosition, TooManyPositions, UnsupportedPrecision }

data class SliderPositions(
    val values: List<JsonPrimitive> = emptyList(),
    val error: SliderPositionError? = null,
) {
    fun indexOf(value: JsonPrimitive?): Int = values.indexOfFirst { it.doubleOrNull == value?.doubleOrNull }
    fun nearestIndex(value: Double?): Int = if (value == null || values.isEmpty()) 0 else
        values.indices.minByOrNull { kotlin.math.abs(values[it].doubleOrNull!! - value) } ?: 0
}

/** Enumerates zero-based schema multiples, with exact bound and integer checks. */
fun buildSliderPositions(schema: JsonSchema, maxPositions: Int = 10_001): SliderPositions {
    val lower = schema.exclusiveMinimumNumber ?: schema.minimum ?: return SliderPositions(error = SliderPositionError.InvalidRange)
    val upper = schema.exclusiveMaximumNumber ?: schema.maximum ?: return SliderPositions(error = SliderPositionError.InvalidRange)
    if (!lower.isFinite() || !upper.isFinite() || lower > upper) return SliderPositions(error = SliderPositionError.InvalidRange)
    val step = schema.multipleOf ?: 1.0
    if (!step.isFinite() || step <= 0.0) return SliderPositions(error = SliderPositionError.InvalidIncrement)
    val lowerDecimal = normalizedDecimal(lower.toString()) ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val upperDecimal = normalizedDecimal(upper.toString()) ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val stepDecimal = normalizedDecimal(step.toString()) ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val scale = maxOf(0L, -lowerDecimal.exponent.toLong(), -upperDecimal.exponent.toLong(),
        -stepDecimal.exponent.toLong()).takeIf { it <= 18 }?.toInt()
        ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val factor = ("1" + "0".repeat(scale)).toLongOrNull()
        ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val minScaled = lowerDecimal.scaledLong(scale)
        ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val maxScaled = upperDecimal.scaledLong(scale)
        ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val rawStep = stepDecimal.scaledLong(scale)
        ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    val integer = schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Integer)
    val stride = if (integer) {
        val integerStep = rawStep / greatestCommonDivisor(rawStep, factor)
        if (integerStep > MAX_PORTABLE_INTEGER / factor)
            return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
        integerStep * factor
    } else rawStep
    if (stride <= 0) return SliderPositions(error = SliderPositionError.UnsupportedPrecision)

    var first = ceilDiv(minScaled, stride)
    var last = floorDiv(maxScaled, stride)
    val lowerExclusive = schema.exclusiveMinimumNumber != null || schema.exclusiveMinimumBoolean == true
    val upperExclusive = schema.exclusiveMaximumNumber != null || schema.exclusiveMaximumBoolean == true
    if (lowerExclusive && safeMultiply(first, stride) == minScaled) first++
    if (upperExclusive && safeMultiply(last, stride) == maxScaled) last--
    if (last < first) return SliderPositions(error = SliderPositionError.NoPosition)
    if (first < -MAX_PORTABLE_INTEGER || last > MAX_PORTABLE_INTEGER ||
        last.toDouble() - first.toDouble() + 1 > maxPositions)
        return SliderPositions(error = SliderPositionError.TooManyPositions)

    val positions = (first..last).map { index ->
        val scaled = safeMultiply(index, stride)
            ?: return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
        if (integer) {
            (scaled / factor).takeIf { it in -MAX_PORTABLE_INTEGER..MAX_PORTABLE_INTEGER }
                ?.let(::JsonPrimitive)
        } else parseNumberDraft(scaled.toDecimalText(scale))
    }
    if (positions.any { it == null }) return SliderPositions(error = SliderPositionError.UnsupportedPrecision)
    return SliderPositions(positions.filterNotNull())
}

private fun safeMultiply(value: Long, positiveMultiplier: Long): Long? =
    if (value > Long.MAX_VALUE / positiveMultiplier || value < Long.MIN_VALUE / positiveMultiplier)
        null else value * positiveMultiplier

private fun greatestCommonDivisor(a: Long, b: Long): Long {
    var left = a
    var right = b
    while (right != 0L) {
        val remainder = left % right
        left = right
        right = remainder
    }
    return left
}

private fun floorDiv(a: Long, b: Long): Long {
    val quotient = a / b
    return if (a < 0 && a % b != 0L) quotient - 1 else quotient
}

private fun ceilDiv(a: Long, b: Long): Long {
    val quotient = a / b
    return if (a > 0 && a % b != 0L) quotient + 1 else quotient
}
