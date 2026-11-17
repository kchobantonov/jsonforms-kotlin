package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.max
import kotlin.math.min

/** Numeric dimensions use platform logical units. The Compose adapter maps them to dp. */
data class LayoutSizing(
    val gap: Float,
    val gridColumns: Int,
    val wrap: Boolean,
    val minItemWidth: Float?,
)

sealed interface LayoutWidthMode {
    data class Fixed(val width: Float) : LayoutWidthMode
    data class Span(val columns: Int) : LayoutWidthMode
    data class Weight(val value: Float) : LayoutWidthMode
}

data class LayoutItemSizing(
    val mode: LayoutWidthMode,
    val minWidth: Float?,
    val maxWidth: Float?,
)

fun resolveLayoutSizing(options: JsonOptions, config: Map<String, Any?>): LayoutSizing {
    val defaults = layoutDefaults(config)
    fun fallback(key: String): Any? = when (defaults) {
        is Map<*, *> -> defaults[key]
        is JsonObject -> defaults[key]
        else -> null
    }
    val gap = logicalDimension(options["gap"]) ?: logicalDimension(fallback("gap")) ?: 16f
    val columns = positiveInt(options["gridColumns"]) ?: positiveInt(fallback("gridColumns")) ?: 16
    val wrap = booleanValue(options["wrap"]) ?: booleanValue(fallback("wrap")) ?: false
    return LayoutSizing(gap, columns, wrap, logicalDimension(options["minItemWidth"]))
}

fun resolveLayoutItemSizing(element: UiSchemaElement, defaultMinWidth: Float?): LayoutItemSizing {
    val options = element.options["layout"] as? JsonObject
    val span = positiveInt(options?.get("span"))
    val weight = positiveFloat(options?.get("weight"))
    val width = logicalDimension(options?.get("width"))
        ?: (element as? SpacerElement)?.takeIf { span == null && weight == null }?.let(::resolveSpacerSize)
        ?: (element as? SeparatorElement)?.takeIf { span == null && weight == null && isVerticalSeparator(it) }?.let { 1f }
    val mode = when {
        width != null -> LayoutWidthMode.Fixed(width)
        span != null -> LayoutWidthMode.Span(span)
        weight != null -> LayoutWidthMode.Weight(weight)
        else -> LayoutWidthMode.Weight(1f)
    }
    return LayoutItemSizing(
        mode = mode,
        minWidth = logicalDimension(options?.get("minWidth")) ?: defaultMinWidth,
        maxWidth = logicalDimension(options?.get("maxWidth")),
    )
}

fun isVerticalSeparator(element: SeparatorElement): Boolean = booleanValue(element.options["vertical"]) == true

fun resolveSpacerSize(element: SpacerElement): Float = logicalDimension(element.size) ?: 32f

fun resolveColumnSpacerHeight(element: SpacerElement): Float =
    logicalDimension((element.options["layout"] as? JsonObject)?.get("height")) ?: resolveSpacerSize(element)

/** A fixed height is a primary sizing mode for any vertical-layout child. */
fun resolveColumnChildHeight(element: UiSchemaElement): Float? =
    logicalDimension((element.options["layout"] as? JsonObject)?.get("height"))

data class LayoutHeightBounds(val minimum: Float?, val maximum: Float?)

fun resolveColumnChildHeightBounds(element: UiSchemaElement): LayoutHeightBounds {
    val layout = element.options["layout"] as? JsonObject
    val minimum = logicalDimension(layout?.get("minHeight"))
    val maximum = logicalDimension(layout?.get("maxHeight"))?.coerceAtLeast(minimum ?: 0f)
    return LayoutHeightBounds(minimum, maximum)
}

fun resolveColumnChildWidth(element: UiSchemaElement): Float? =
    logicalDimension((element.options["layout"] as? JsonObject)?.get("width"))

data class LayoutWidthBounds(val minimum: Float?, val maximum: Float?)

fun resolveColumnChildWidthBounds(element: UiSchemaElement): LayoutWidthBounds {
    val layout = element.options["layout"] as? JsonObject
    val minimum = logicalDimension(layout?.get("minWidth"))
    val maximum = logicalDimension(layout?.get("maxWidth"))?.coerceAtLeast(minimum ?: 0f)
    return LayoutWidthBounds(minimum, maximum)
}

fun resolveColumnSpacerWeight(element: SpacerElement): Float? {
    return resolveColumnChildWeight(element)
}

/** Fixed height wins over weight; Auto has no vertical weight. */
fun resolveColumnChildWeight(element: UiSchemaElement): Float? {
    val layout = element.options["layout"] as? JsonObject ?: return null
    if (logicalDimension(layout["height"]) != null) return null
    return positiveFloat(layout["weight"])
}

/** Preserve each child's natural height, then distribute only spare bounded height. */
fun resolveVerticalHeights(
    availableHeight: Float?,
    gap: Float,
    naturalHeights: List<Float>,
    weights: List<Float?>,
    bounds: List<LayoutHeightBounds>,
): List<Float> {
    require(naturalHeights.size == weights.size && weights.size == bounds.size)
    val heights = naturalHeights.indices.map { index ->
        val bound = bounds[index]
        naturalHeights[index].coerceAtLeast(bound.minimum ?: 0f)
            .coerceAtMost(bound.maximum ?: Float.POSITIVE_INFINITY)
    }.toMutableList()
    if (availableHeight == null || !availableHeight.isFinite()) return heights
    var remaining = max(0f, availableHeight - max(0, heights.size - 1) * gap - heights.sum())
    val flexible = weights.indices.filter { weights[it]?.let { value -> value.isFinite() && value > 0f } == true }
        .toMutableList()
    while (remaining > 0f && flexible.isNotEmpty()) {
        val totalWeight = flexible.sumOf { weights[it]!!.toDouble() }.toFloat()
        val capped = flexible.firstOrNull { index ->
            val share = remaining * weights[index]!! / totalWeight
            heights[index] + share > (bounds[index].maximum ?: Float.POSITIVE_INFINITY)
        }
        if (capped == null) {
            flexible.forEach { index -> heights[index] += remaining * weights[index]!! / totalWeight }
            break
        }
        val cap = bounds[capped].maximum!!
        remaining -= max(0f, cap - heights[capped])
        heights[capped] = cap
        flexible.remove(capped)
    }
    return heights
}

/** Fixed and span widths are resolved against the whole grid; flexible widths divide the remainder. */
fun resolveHorizontalWidths(
    rowWidth: Float,
    sizing: LayoutSizing,
    items: List<LayoutItemSizing>,
): List<Float> {
    if (items.isEmpty()) return emptyList()
    val width = max(0f, rowWidth)
    val gap = sizing.gap
    val resolved = FloatArray(items.size)
    val flexible = mutableSetOf<Int>()
    items.forEachIndexed { index, item ->
        val base = when (val mode = item.mode) {
            is LayoutWidthMode.Fixed -> mode.width
            is LayoutWidthMode.Span -> {
                val span = min(mode.columns, sizing.gridColumns)
                gridSpanWidth(width, gap, sizing.gridColumns, span)
            }
            is LayoutWidthMode.Weight -> {
                flexible += index
                0f
            }
        }
        if (index !in flexible) resolved[index] = base.coerceIn(
            item.minWidth ?: 0f,
            max(item.minWidth ?: 0f, item.maxWidth ?: Float.POSITIVE_INFINITY),
        )
    }
    var remaining = max(0f, width - (items.size - 1) * gap - resolved.sum())
    while (flexible.isNotEmpty()) {
        val totalWeight = flexible.sumOf { (items[it].mode as LayoutWidthMode.Weight).value.toDouble() }.toFloat()
        val constrained = flexible.firstOrNull { index ->
            val item = items[index]
            val proposed = remaining * (item.mode as LayoutWidthMode.Weight).value / totalWeight
            proposed < (item.minWidth ?: 0f) || proposed > (item.maxWidth ?: Float.POSITIVE_INFINITY)
        }
        if (constrained == null) {
            flexible.forEach { index ->
                resolved[index] = remaining * (items[index].mode as LayoutWidthMode.Weight).value / totalWeight
            }
            break
        }
        val item = items[constrained]
        val proposed = remaining * (item.mode as LayoutWidthMode.Weight).value / totalWeight
        val constrainedWidth = proposed.coerceIn(
            item.minWidth ?: 0f,
            max(item.minWidth ?: 0f, item.maxWidth ?: Float.POSITIVE_INFINITY),
        )
        resolved[constrained] = constrainedWidth
        remaining = max(0f, remaining - constrainedWidth)
        flexible.remove(constrained)
    }
    val deficit = resolved.sum() + (items.size - 1) * gap - width
    if (deficit > 0f) {
        val capacity = items.indices.map { index ->
            val minimum = when (items[index].mode) {
                is LayoutWidthMode.Fixed -> items[index].minWidth ?: resolved[index]
                is LayoutWidthMode.Span -> items[index].minWidth ?: 0f
                is LayoutWidthMode.Weight -> resolved[index]
            }
            max(0f, resolved[index] - minimum)
        }
        val totalCapacity = capacity.sum()
        if (totalCapacity > 0f) {
            val shrink = min(deficit, totalCapacity)
            items.indices.forEach { index ->
                resolved[index] -= shrink * capacity[index] / totalCapacity
            }
        }
    }
    return resolved.toList()
}

/** The full-grid span width, expressed as a fraction of the row minus its gap correction. */
fun gridSpanWidth(rowWidth: Float, gap: Float, gridColumns: Int, span: Int): Float {
    val fraction = min(span, gridColumns).toFloat() / gridColumns
    return max(0f, fraction * rowWidth - (1f - fraction) * gap)
}

/** Keep authored order while moving a child to the next row when its resolved fit cannot share this one. */
fun resolveWrappedRows(
    rowWidth: Float,
    sizing: LayoutSizing,
    items: List<LayoutItemSizing>,
): List<IntRange> {
    if (items.isEmpty()) return emptyList()
    if (!sizing.wrap) return listOf(items.indices)
    val gap = sizing.gap
    val width = max(0f, rowWidth)
    fun fit(item: LayoutItemSizing): Float {
        val preferred = when (val mode = item.mode) {
            is LayoutWidthMode.Fixed -> mode.width
            is LayoutWidthMode.Span -> {
                val span = min(mode.columns, sizing.gridColumns)
                gridSpanWidth(width, gap, sizing.gridColumns, span)
            }
            is LayoutWidthMode.Weight -> item.minWidth ?: 0f
        }
        return preferred.coerceIn(
            item.minWidth ?: 0f,
            max(item.minWidth ?: 0f, item.maxWidth ?: Float.POSITIVE_INFINITY),
        )
    }
    val rows = mutableListOf<IntRange>()
    var start = 0
    var occupied = 0f
    items.forEachIndexed { index, item ->
        val next = fit(item)
        if (index > start && occupied + gap + next > width) {
            rows += start until index
            start = index
            occupied = next
        } else {
            occupied += if (index == start) next else gap + next
        }
    }
    rows += start until items.size
    return rows
}

internal fun positiveInt(value: Any?): Int? {
    val number = numericValue(value) ?: return null
    if (!number.isFinite() || number <= 0 || number > Int.MAX_VALUE || number % 1.0 != 0.0) return null
    return number.toInt()
}

internal fun positiveFloat(value: Any?): Float? {
    val number = numericValue(value)?.toFloat() ?: return null
    return number.takeIf { it.isFinite() && it > 0f }
}

internal fun logicalDimension(value: Any?): Float? {
    val raw = when (value) {
        is JsonPrimitive -> value.content
        is Number -> value.toString()
        is String -> value
        else -> return null
    }
    val trimmed = raw.trim()
    val number = when {
        trimmed.endsWith("dp") || trimmed.endsWith("px") -> trimmed.dropLast(2).trim().toFloatOrNull()
        else -> trimmed.toFloatOrNull()
    }
    return number?.takeIf { it.isFinite() && it >= 0f }
}

internal fun layoutDefaults(config: Map<String, Any?>): Any? =
    (config["jsonformsExtended"] as? Map<*, *>)?.get("layoutDefaults")
        ?: (config["jsonformsExtended"] as? JsonObject)?.get("layoutDefaults")

private fun numericValue(value: Any?): Double? = when (value) {
    is JsonPrimitive -> if (value.isString) null else value.content.toDoubleOrNull()
    is Number -> value.toDouble()
    else -> null
}

internal fun booleanValue(value: Any?): Boolean? = when (value) {
    is JsonPrimitive -> if (value.isString) null else value.content.toBooleanStrictOrNull()
    is Boolean -> value
    else -> null
}
