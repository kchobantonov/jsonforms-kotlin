package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.util.JsonPointer
import io.github.optimumcode.json.schema.ErrorCollector
import io.github.optimumcode.json.schema.ValidationError
import io.github.optimumcode.json.schema.extension.ExternalAssertion
import io.github.optimumcode.json.schema.extension.ExternalAssertionContext
import io.github.optimumcode.json.schema.extension.ExternalAssertionFactory
import io.github.optimumcode.json.schema.extension.ExternalLoadingContext
import io.github.optimumcode.json.schema.model.AbstractElement
import io.github.optimumcode.json.schema.model.PrimitiveElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

internal val temporalComparisonKeywords = listOf(
    "formatMinimum", "formatExclusiveMinimum", "formatMaximum", "formatExclusiveMaximum",
)
private val temporalComparableFormats = setOf("date", "time", "date-time", "iso-time", "iso-date-time")

internal fun JsonElement.hasDynamicTemporalBound(): Boolean = when (this) {
    is JsonObject -> entries.any { (key, value) ->
        key in temporalComparisonKeywords && value is JsonObject && "\$data" in value ||
            value.hasDynamicTemporalBound()
    }
    is JsonArray -> any(JsonElement::hasDynamicTemporalBound)
    else -> false
}

@OptIn(io.github.optimumcode.json.schema.ExperimentalApi::class)
internal class TemporalFormatAssertionFactory(
    override val keywordName: String,
    private val schemaRoot: JsonObject,
    private val dataRoot: JsonElement?,
) : ExternalAssertionFactory {
    override fun create(element: JsonElement, context: ExternalLoadingContext): ExternalAssertion {
        val keywordValue = element
        val schemaPath = context.schemaPath
        val validBound = when (keywordValue) {
            is JsonPrimitive -> keywordValue.isString
            is JsonObject -> keywordValue.size == 1 &&
                (keywordValue["\$data"] as? JsonPrimitive)?.isString == true
            else -> false
        }
        if (!validBound) throw TemporalBoundSchemaException(schemaPath.toString(),
            "$keywordName must be a string or a string-valued \$data reference.")
        if (keywordValue is JsonObject) {
            val reference = (keywordValue["\$data"] as JsonPrimitive).content
            if (!validTemporalDataReferenceSyntax(reference))
                throw TemporalBoundSchemaException(schemaPath.toString(),
                    "Invalid temporal bound data reference '$reference'.")
            val relative = Regex("""(0|[1-9]\d*)(#|/.*)?""").matchEntire(reference)
            val depth = directSchemaDataDepth(schemaPath.toString().substringBeforeLast('/', ""))
            if (relative != null && depth != null) {
                val parents = relative.groupValues[1].toIntOrNull()
                val propertyName = relative.groupValues[2] == "#"
                if (parents == null || parents > depth || propertyName && parents == depth)
                    throw TemporalBoundSchemaException(schemaPath.toString(),
                        "Temporal bound data reference '$reference' exceeds schema data depth $depth.")
            }
        }
        val parentPath = schemaPath.toString().substringBeforeLast('/', "")
        val format = (resolveStrictPointer(schemaRoot, parentPath) as? JsonObject)
            ?.get("format")?.let { it as? JsonPrimitive }
            ?.takeIf { it.isString }?.content
            ?: throw TemporalBoundSchemaException(schemaPath.toString(),
                "$keywordName requires a string format keyword.")
        if (format !in temporalComparableFormats) throw TemporalBoundSchemaException(
            schemaPath.toString(),
            "$keywordName cannot compare format '$format'; no comparator is registered.",
        )
        return object : ExternalAssertion {
            override fun validate(
                element: AbstractElement,
                context: ExternalAssertionContext,
                errorCollector: ErrorCollector,
            ): Boolean {
                val value = (element as? PrimitiveElement)?.takeIf { it.isString }?.content
                    ?: return true
                val resolved = resolveBound(element = keywordValue,
                    root = dataRoot, objectPath = context.objectPath.toString())
                val bound = when (resolved) {
                    is TemporalBound.Value -> resolved.text
                    is TemporalBound.InvalidType -> {
                        errorCollector.onError(ValidationError(schemaPath, context.objectPath,
                            "Referenced temporal bound must be a string.",
                            mapOf("bound" to resolved.raw)))
                        return false
                    }
                    is TemporalBound.InvalidReference -> {
                        errorCollector.onError(ValidationError(schemaPath, context.objectPath,
                            "Invalid temporal bound data reference.",
                            mapOf("reference" to resolved.raw)))
                        return false
                    }
                    TemporalBound.Missing -> return true
                }
                val comparison = compareTemporalFormatValues(format, value, bound)
                    ?: return true
                val lower = keywordName == "formatMinimum" || keywordName == "formatExclusiveMinimum"
                val exclusive = keywordName == "formatExclusiveMinimum" || keywordName == "formatExclusiveMaximum"
                val violates = if (lower) comparison < 0 || exclusive && comparison == 0
                    else comparison > 0 || exclusive && comparison == 0
                if (violates) {
                    val wording = when (keywordName) {
                        "formatMinimum" -> "on or after"
                        "formatExclusiveMinimum" -> "after"
                        "formatMaximum" -> "on or before"
                        else -> "before"
                    }
                    errorCollector.onError(ValidationError(schemaPath, context.objectPath,
                        "Value must be $wording $bound.", mapOf("bound" to bound)))
                }
                return !violates
            }

        }
    }
}

internal class TemporalBoundSchemaException(
    val keywordPath: String,
    message: String,
) : IllegalArgumentException(message)

private sealed interface TemporalBound {
    data class Value(val text: String) : TemporalBound
    data class InvalidType(val raw: String) : TemporalBound
    data class InvalidReference(val raw: String) : TemporalBound
    data object Missing : TemporalBound
}

private fun resolveBound(element: JsonElement, root: JsonElement?, objectPath: String): TemporalBound {
    val selected = when (element) {
        is JsonPrimitive -> element
        is JsonObject -> {
            if (element.size != 1) return TemporalBound.InvalidReference(element.toString())
            val reference = (element["\$data"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: return TemporalBound.InvalidReference(element.toString())
            val keyReference = Regex("""(0|[1-9]\d*)#""").matchEntire(reference)
            if (keyReference != null) {
                val parents = keyReference.groupValues[1].toIntOrNull()
                    ?: return TemporalBound.InvalidReference(reference)
                val segments = pointerSegments(objectPath)
                    ?: return TemporalBound.InvalidReference(reference)
                return segments.getOrNull(segments.size - parents - 1)
                    ?.let(TemporalBound::Value) ?: TemporalBound.InvalidReference(reference)
            }
            val pointer = relativePointer(reference, objectPath)
                ?: return TemporalBound.InvalidReference(reference)
            if (pointerSegments(pointer) == null) return TemporalBound.InvalidReference(reference)
            if (root == null) return TemporalBound.Missing
            resolveStrictPointer(root, pointer)
        }
        else -> null
    }
    return when (selected) {
        null -> TemporalBound.Missing
        is JsonPrimitive -> if (selected.isString) TemporalBound.Value(selected.content)
            else TemporalBound.InvalidType(selected.toString())
        else -> TemporalBound.InvalidType(selected.toString())
    }
}

private fun relativePointer(reference: String, objectPath: String): String? {
    if (reference.isEmpty() || reference.startsWith('/')) return reference
    val match = Regex("""(0|[1-9]\d*)(?:/(.*))?""").matchEntire(reference) ?: return null
    val parents = match.groupValues[1].toIntOrNull() ?: return null
    val segments = pointerSegments(objectPath) ?: return null
    if (parents > segments.size) return null
    val prefix = segments.dropLast(parents).joinToString("/") { JsonPointer.encode(it) }
    val suffix = match.groups[2]?.value?.let { "/$it" }.orEmpty()
    return if (prefix.isEmpty()) suffix else "/$prefix$suffix"
}

private fun pointerSegments(pointer: String): List<String>? = when {
    pointer.isEmpty() -> emptyList()
    !pointer.startsWith('/') -> null
    else -> pointer.drop(1).split('/').map { encoded ->
        if (!validPointerSegment(encoded)) return null
        JsonPointer.decode(encoded)
    }
}

private fun validPointerSegment(segment: String): Boolean = segment.indices.none { index ->
    segment[index] == '~' && (index + 1 == segment.length ||
        segment[index + 1] != '0' && segment[index + 1] != '1')
}

private fun validTemporalDataReferenceSyntax(reference: String): Boolean {
    if (reference.isEmpty()) return false
    if (reference.startsWith('/')) return pointerSegments(reference) != null
    val relative = Regex("""(0|[1-9]\d*)(?:#|/(.*))?""").matchEntire(reference)
        ?: return false
    val suffix = relative.groups[2]?.value ?: return true
    return pointerSegments("/$suffix") != null
}

/** Infers data depth only for schema locations with an unambiguous local data path. */
private fun directSchemaDataDepth(parentPath: String): Int? {
    val segments = pointerSegments(parentPath.removePrefix("#")) ?: return null
    var depth = 0
    var index = 0
    while (index < segments.size) {
        when (segments[index]) {
            "properties", "patternProperties" -> {
                if (index + 1 >= segments.size) return null
                depth++
                index += 2
            }
            "items" -> {
                depth++
                index += if (segments.getOrNull(index + 1)?.toIntOrNull() != null) 2 else 1
            }
            "prefixItems" -> {
                if (segments.getOrNull(index + 1)?.toIntOrNull() == null) return null
                depth++
                index += 2
            }
            "additionalProperties", "unevaluatedProperties", "additionalItems",
            "unevaluatedItems", "contains" -> {
                depth++
                index++
            }
            "allOf", "anyOf", "oneOf", "dependentSchemas" -> {
                if (index + 1 >= segments.size) return null
                index += 2
            }
            "if", "then", "else", "not" -> index++
            else -> return null
        }
    }
    return depth
}

private fun resolveStrictPointer(root: JsonElement, pointer: String): JsonElement? {
    val segments = pointerSegments(pointer.removePrefix("#")) ?: return null
    var current: JsonElement? = root
    for (segment in segments) current = when (val container = current) {
        is JsonObject -> container[segment]
        is JsonArray -> segment.toIntOrNull()?.takeIf { it.toString() == segment }
            ?.let(container::getOrNull)
        else -> null
    }
    return current
}

/** Compares supported RFC 3339 temporal strings at their original offset and fraction precision. */
fun compareTemporalFormatValues(format: String, value: String, bound: String): Int? = when (format) {
    "date" -> if (value.isEmpty() || bound.isEmpty()) null else value.compareTo(bound)
    "date-time" -> runCatching { Instant.parse(value).compareTo(Instant.parse(bound)) }.getOrNull()
    "time" -> {
        val left = parseOffsetTime(value)
        val right = parseOffsetTime(bound)
        if (left == null || right == null) null else left.compareTo(right)
    }
    "iso-time" -> compareIsoTime(value, bound)
    "iso-date-time" -> compareIsoDateTime(value, bound)
    else -> null
}

private data class OffsetTime(val second: Long, val fraction: String) : Comparable<OffsetTime> {
    override fun compareTo(other: OffsetTime): Int {
        val seconds = second.compareTo(other.second)
        if (seconds != 0) return seconds
        val width = maxOf(fraction.length, other.fraction.length)
        return fraction.padEnd(width, '0').compareTo(other.fraction.padEnd(width, '0'))
    }
}

private fun parseOffsetTime(text: String): OffsetTime? {
    val match = Regex("""(\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:\d{2})""",
        RegexOption.IGNORE_CASE).matchEntire(text) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    val second = match.groupValues[3].toInt()
    if (hour > 23 || minute > 59 || second > 59) return null
    val fraction = match.groupValues[4].trimEnd('0')
    val zone = match.groupValues[5]
    val offsetSeconds = if (zone.equals("Z", ignoreCase = true)) 0 else {
        val offsetHour = zone.substring(1, 3).toInt()
        val offsetMinute = zone.substring(4, 6).toInt()
        if (offsetHour > 23 || offsetMinute > 59) return null
        (offsetHour * 3600 + offsetMinute * 60) * if (zone[0] == '+') 1 else -1
    }
    return OffsetTime(hour * 3600L + minute * 60L + second - offsetSeconds, fraction)
}
