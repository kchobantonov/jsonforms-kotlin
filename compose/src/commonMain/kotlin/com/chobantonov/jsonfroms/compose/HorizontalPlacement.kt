package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeightBounds
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.max
import kotlin.math.roundToInt

/** An unbounded scroll parent uses the viewport as an explicit row width. */
fun horizontalRowWidth(measuredWidth: Float, viewportWidth: Float): Float? =
    if (measuredWidth.isFinite()) measuredWidth.coerceAtLeast(0f)
    else viewportWidth.takeIf { it.isFinite() && it > 0f }

fun horizontalArrangement(options: JsonOptions, gap: Dp): Arrangement.Horizontal =
    when ((options["justify"] as? JsonPrimitive)?.content) {
        "center" -> Arrangement.spacedBy(gap, Alignment.CenterHorizontally)
        "end" -> Arrangement.spacedBy(gap, Alignment.End)
        "space-between", "space-around", "space-evenly" ->
            GapSpaceArrangement(gap, (options["justify"] as JsonPrimitive).content)
        else -> Arrangement.spacedBy(gap, Alignment.Start)
    }

fun horizontalCrossAlignment(options: JsonOptions): Alignment.Vertical =
    when ((options["align"] as? JsonPrimitive)?.content) {
        "center" -> Alignment.CenterVertically
        "end" -> Alignment.Bottom
        else -> Alignment.Top
    }

fun horizontalStretch(options: JsonOptions): Boolean =
    (options["align"] as? JsonPrimitive)?.content == "stretch"

fun rowChildHeightModifier(element: UiSchemaElement): Modifier {
    val height = resolveColumnChildHeight(element)
    val bounds = resolveColumnChildHeightBounds(element)
    return Modifier
        .then(if (bounds.minimum != null || bounds.maximum != null) Modifier.heightIn(
            min = (bounds.minimum ?: 0f).dp,
            max = (bounds.maximum ?: Float.POSITIVE_INFINITY).dp,
        ) else Modifier)
        .then(if (height != null) Modifier.height(height.dp) else Modifier)
}

/** CSS-like distribution keeps the authored gap as a minimum. */
private class GapSpaceArrangement(
    override val spacing: Dp,
    private val mode: String,
) : Arrangement.Horizontal {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return
        val gapPx = spacing.roundToPx()
        val free = max(0, totalSize - sizes.sum() - max(0, sizes.size - 1) * gapPx).toFloat()
        val extra = when (mode) {
            "space-between" -> if (sizes.size > 1) free / (sizes.size - 1) else 0f
            "space-around" -> free / sizes.size
            else -> free / (sizes.size + 1)
        }
        var position = when (mode) {
            "space-around" -> extra / 2f
            "space-evenly" -> extra
            else -> 0f
        }
        sizes.forEachIndexed { index, size ->
            val left = position.roundToInt()
            outPositions[index] = if (layoutDirection == LayoutDirection.Rtl) totalSize - left - size else left
            position += size + gapPx + extra
        }
    }
}
