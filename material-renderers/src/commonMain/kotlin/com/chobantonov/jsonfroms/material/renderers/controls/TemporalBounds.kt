package com.chobantonov.jsonfroms.material.renderers.controls

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.i18n.translateTemporalControlText
import com.chobantonov.jsonfroms.core.i18n.translateTemporalRangeViolation
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.validation.compareTemporalFormatValues
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class TemporalBoundKind { Date, Time, DateTime }

data class TemporalRangeViolation(
    val keyword: String,
    val bound: String,
    val invalidBoundType: Boolean = false,
    val invalidBoundReference: Boolean = false,
)

fun temporalViolationMessage(
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    displayFormat: String,
    kind: TemporalBoundKind,
    violation: TemporalRangeViolation,
): String = if (violation.invalidBoundReference) {
    translateTemporalControlText(i18n, i18nKeyPrefix, "invalidBoundReference",
        "Invalid temporal bound data reference.", displayFormat)
} else if (violation.invalidBoundType) {
    translateTemporalControlText(i18n, i18nKeyPrefix, "invalidBoundType",
        "Referenced temporal bound must be a string.", displayFormat)
} else {
    translateTemporalRangeViolation(i18n, i18nKeyPrefix, displayFormat, violation.keyword,
        formatTemporalBound(violation.bound, kind, displayFormat))
}

private sealed interface ResolvedBound {
    data class Value(val text: String) : ResolvedBound
    data class InvalidType(val raw: String) : ResolvedBound
    data class InvalidReference(val raw: String) : ResolvedBound
    data object Missing : ResolvedBound
}

/** Present a supported schema bound in the control's active display format. */
fun formatTemporalBound(bound: String, kind: TemporalBoundKind, displayFormat: String): String =
    parseTemporalBound(bound, kind)?.let { formatTemporal(it, displayFormat) } ?: bound

/** Checks literal and resolvable Ajv $data bounds at the value's full supported precision. */
fun temporalRangeViolation(
    value: TemporalValue,
    schema: JsonSchema,
    kind: TemporalBoundKind,
    rootData: JsonElement? = null,
    dataPath: String = "",
): TemporalRangeViolation? {
    val checks = listOf(
        "formatMinimum" to false,
        "formatExclusiveMinimum" to true,
        "formatMaximum" to false,
        "formatExclusiveMaximum" to true,
    )
    for ((keyword, exclusive) in checks) {
        val bound = when (val resolved = resolveTemporalBound(schema[keyword], rootData, dataPath)) {
            is ResolvedBound.Value -> resolved.text
            is ResolvedBound.InvalidType -> return TemporalRangeViolation(keyword, resolved.raw,
                invalidBoundType = true)
            is ResolvedBound.InvalidReference -> return TemporalRangeViolation(keyword, resolved.raw,
                invalidBoundReference = true)
            ResolvedBound.Missing -> continue
        }
        val serialized = when (kind) {
            TemporalBoundKind.Date -> formatTemporal(value, DEFAULT_DATE_SAVE_FORMAT)
            TemporalBoundKind.Time -> formatTemporal(value, DEFAULT_TIME_SAVE_FORMAT)
            TemporalBoundKind.DateTime -> formatTemporal(value, DEFAULT_DATE_TIME_SAVE_FORMAT)
        }
        val format = when (kind) {
            TemporalBoundKind.Date -> "date"
            TemporalBoundKind.Time -> "time"
            TemporalBoundKind.DateTime -> "date-time"
        }
        val comparison = compareTemporalFormatValues(format, serialized, bound)
            ?: parseTemporalBound(bound, kind)?.let { parsed -> when (kind) {
                TemporalBoundKind.Date -> value.date.compareTo(parsed.date)
                TemporalBoundKind.Time -> value.time.compareTo(parsed.time)
                TemporalBoundKind.DateTime -> value.dateTime.compareTo(parsed.dateTime)
            } } ?: continue
        val lower = keyword == "formatMinimum" || keyword == "formatExclusiveMinimum"
        val violates = if (lower) comparison < 0 || (exclusive && comparison == 0)
            else comparison > 0 || (exclusive && comparison == 0)
        if (violates) return TemporalRangeViolation(keyword, bound)
    }
    return null
}

/** Whether a date offers at least one selectable whole minute in a date-time picker. */
fun temporalDateHasSelectableMinute(
    date: TemporalValue,
    schema: JsonSchema,
    rootData: JsonElement? = null,
    dataPath: String = "",
    retainedSecond: Int = 0,
): Boolean {
    var firstMinute = 0
    var lastMinute = 23 * 60 + 59
    for ((keyword, lower, exclusive) in listOf(
        Triple("formatMinimum", true, false),
        Triple("formatExclusiveMinimum", true, true),
        Triple("formatMaximum", false, false),
        Triple("formatExclusiveMaximum", false, true),
    )) {
        val bound = when (val resolved = resolveTemporalBound(schema[keyword], rootData, dataPath)) {
            is ResolvedBound.Value -> resolved.text
            is ResolvedBound.InvalidType -> return false
            is ResolvedBound.InvalidReference -> return false
            ResolvedBound.Missing -> continue
        }
        val parsed = parseTemporalBound(bound, TemporalBoundKind.DateTime) ?: continue
        val dayComparison = date.date.compareTo(parsed.date)
        if (lower && dayComparison < 0 || !lower && dayComparison > 0) return false
        if (dayComparison != 0) continue
        val boundMinute = parsed.hour * 60 + parsed.minute
        val boundaryCandidate = date.copy(hour = parsed.hour, minute = parsed.minute,
            second = retainedSecond)
        val comparison = runCatching {
            compareTemporalFormatValues("date-time",
                formatTemporal(boundaryCandidate, DEFAULT_DATE_TIME_SAVE_FORMAT), bound)
        }.getOrNull()
        if (lower) {
            val boundaryRejected = comparison?.let { it < 0 || exclusive && it == 0 }
                ?: (retainedSecond < parsed.second || exclusive && retainedSecond == parsed.second)
            val firstAllowed = boundMinute + if (boundaryRejected) 1 else 0
            firstMinute = maxOf(firstMinute, firstAllowed)
        } else {
            val boundaryRejected = comparison?.let { it > 0 || exclusive && it == 0 }
                ?: (retainedSecond > parsed.second || exclusive && retainedSecond == parsed.second)
            val lastAllowed = boundMinute - if (boundaryRejected) 1 else 0
            lastMinute = minOf(lastMinute, lastAllowed)
        }
    }
    return firstMinute <= lastMinute
}

private fun resolveTemporalBound(
    bound: JsonElement?,
    rootData: JsonElement?,
    dataPath: String,
): ResolvedBound {
    val selected = when (bound) {
        is JsonPrimitive -> bound
        is JsonObject -> {
            if (bound.size != 1) return ResolvedBound.InvalidReference(bound.toString())
            val reference = (bound["\$data"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: return ResolvedBound.InvalidReference(bound.toString())
            if (rootData == null) return ResolvedBound.Missing
            val keyReference = Regex("""(0|[1-9]\d*)#""").matchEntire(reference)
            if (keyReference != null) {
                val parents = keyReference.groupValues[1].toIntOrNull()
                    ?: return ResolvedBound.InvalidReference(reference)
                val segments = dataPath.split('.').filter(String::isNotEmpty)
                val key = segments.getOrNull(segments.size - parents - 1)
                    ?: return ResolvedBound.InvalidReference(reference)
                return ResolvedBound.Value(key)
            }
            val pointer = resolveDataPointer(reference, dataPath)
                ?: return ResolvedBound.InvalidReference(reference)
            if (!validStrictDataPointer(pointer)) return ResolvedBound.InvalidReference(reference)
            resolveStrictDataPointer(rootData, pointer)
        }
        else -> null
    }
    return when (selected) {
        null -> ResolvedBound.Missing
        is JsonPrimitive -> if (selected.isString) ResolvedBound.Value(selected.content)
            else ResolvedBound.InvalidType(selected.toString())
        else -> ResolvedBound.InvalidType(selected.toString())
    }
}

private fun validStrictDataPointer(pointer: String): Boolean =
    pointer.isEmpty() || pointer.startsWith('/') && pointer.drop(1).split('/').all { segment ->
        segment.indices.none { index -> segment[index] == '~' &&
            (index + 1 == segment.length || segment[index + 1] != '0' &&
                segment[index + 1] != '1') }
    }

private fun resolveStrictDataPointer(root: JsonElement, pointer: String): JsonElement? {
    if (pointer.isEmpty()) return root
    if (!pointer.startsWith('/')) return null
    var value: JsonElement? = root
    for (encoded in pointer.drop(1).split('/')) {
        if (encoded.indices.any { index -> encoded[index] == '~' &&
                (index + 1 == encoded.length || encoded[index + 1] != '0' && encoded[index + 1] != '1') })
            return null
        val segment = JsonPointer.decode(encoded)
        value = when (val current = value) {
            is JsonObject -> current[segment]
            is JsonArray -> segment.toIntOrNull()?.takeIf { it.toString() == segment }
                ?.let(current::getOrNull)
            else -> null
        }
    }
    return value
}

/** Resolves Ajv's absolute or parent-relative $data pointer for a control data path. */
private fun resolveDataPointer(reference: String, dataPath: String): String? {
    if (reference.isEmpty() || reference.startsWith('/')) return reference
    val match = Regex("""(0|[1-9]\d*)(?:/(.*))?""").matchEntire(reference) ?: return null
    val parents = match.groupValues[1].toIntOrNull() ?: return null
    val segments = dataPath.split('.').filter(String::isNotEmpty)
    if (parents > segments.size) return null
    val prefix = segments.dropLast(parents).map(JsonPointer::encode)
    val suffix = match.groups[2]?.value?.let { "/$it" }.orEmpty()
    return if (prefix.isEmpty()) suffix else prefix.joinToString("/", prefix = "/") + suffix
}

private fun parseTemporalBound(bound: String, kind: TemporalBoundKind): TemporalValue? = when (kind) {
    TemporalBoundKind.Date -> bound.takeIf {
        Regex("""\d{4}-\d{2}-\d{2}""").matches(it)
    }?.let { parseDateValue(it, listOf(DEFAULT_DATE_SAVE_FORMAT)) }
    TemporalBoundKind.Time -> parseTimeValue(bound,
        listOf(DEFAULT_TIME_SAVE_FORMAT, "HH:mm:ss", "HH:mm", "HH:mmZ"))
    TemporalBoundKind.DateTime -> parseDateTimeValue(bound,
        listOf(DEFAULT_DATE_TIME_SAVE_FORMAT, "YYYY-MM-DDTHH:mm:ss", "YYYY-MM-DDTHH:mm"))
}
