package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildWidth
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildWidthBounds
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.max
import kotlin.math.roundToInt

fun verticalCrossAlignment(options: JsonOptions): Alignment.Horizontal =
    when ((options["align"] as? JsonPrimitive)?.content) {
        "center" -> Alignment.CenterHorizontally
        "end" -> Alignment.End
        else -> Alignment.Start
    }

fun verticalStretch(options: JsonOptions): Boolean =
    (options["align"] as? JsonPrimitive)?.content == "stretch"

fun columnChildWidthModifier(element: UiSchemaElement, stretch: Boolean): Modifier {
    val width = resolveColumnChildWidth(element)
    val bounds = resolveColumnChildWidthBounds(element)
    return Modifier
        .then(if (bounds.minimum != null || bounds.maximum != null) Modifier.widthIn(
            min = (bounds.minimum ?: 0f).dp,
            max = (bounds.maximum ?: Float.POSITIVE_INFINITY).dp,
        ) else Modifier)
        .then(when {
            width != null -> Modifier.width(width.dp)
            stretch -> Modifier.fillMaxWidth()
            else -> Modifier
        })
}

fun verticalJustificationNeedsSpace(options: JsonOptions): Boolean =
    (options["justify"] as? JsonPrimitive)?.content in setOf(
        "center", "end", "space-between", "space-around", "space-evenly",
    )

fun verticalArrangement(options: JsonOptions, gap: Dp): Arrangement.Vertical =
    when ((options["justify"] as? JsonPrimitive)?.content) {
        "center" -> Arrangement.spacedBy(gap, Alignment.CenterVertically)
        "end" -> Arrangement.spacedBy(gap, Alignment.Bottom)
        "space-between", "space-around", "space-evenly" ->
            VerticalGapSpaceArrangement(gap, (options["justify"] as JsonPrimitive).content)
        else -> Arrangement.spacedBy(gap, Alignment.Top)
    }

private class VerticalGapSpaceArrangement(
    override val spacing: Dp,
    private val mode: String,
) : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
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
            outPositions[index] = position.roundToInt()
            position += size + gapPx + extra
        }
    }
}
