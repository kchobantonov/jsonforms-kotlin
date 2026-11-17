package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeightBounds
import kotlin.math.max

/** Stretch intrinsic-capable children without asking nested SubcomposeLayouts for an intrinsic size. */
@Composable
fun StretchHorizontalRow(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    widths: List<Float>,
    gap: Dp,
    overflow: Boolean = false,
    firstIndex: Int = 0,
    explicitWidth: Float? = null,
) {
    Layout(
        modifier = (explicitWidth?.let { Modifier.width(it.dp) } ?: Modifier.fillMaxWidth()).then(
            if (overflow) Modifier.horizontalScroll(rememberScrollState()).testTag("horizontal-layout-scroll")
            else Modifier,
        ),
        content = {
            elements.forEachIndexed { index, child ->
                if (child is SpacerElement) {
                    Spacer(Modifier.width(widths[index].dp).then(rowChildHeightModifier(child)))
                } else {
                    Box(Modifier.width(widths[index].dp).then(rowChildHeightModifier(child))
                        .testTag("layout-child-${props.path}-${firstIndex + index}")) {
                        JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val widthsPx = widths.map { it.dp.roundToPx().coerceAtLeast(0) }
        val natural = IntArray(elements.size)
        val measured = arrayOfNulls<Placeable>(elements.size)
        measurables.forEachIndexed { index, child ->
            val childConstraints = Constraints(
                minWidth = widthsPx[index], maxWidth = widthsPx[index],
                minHeight = 0, maxHeight = Constraints.Infinity,
            )
            natural[index] = try {
                child.maxIntrinsicHeight(widthsPx[index])
            } catch (error: IllegalStateException) {
                if (error.message?.contains("SubcomposeLayout") != true) throw error
                child.measure(childConstraints).also { measured[index] = it }.height
            }
        }
        val rowHeight = (natural.maxOrNull() ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeables = measurables.mapIndexed { index, child ->
            val element = elements[index]
            val fixed = resolveColumnChildHeight(element) != null
            val maximum = resolveColumnChildHeightBounds(element).maximum?.dp?.roundToPx()
            val targetHeight = when {
                fixed -> natural[index]
                maximum != null -> minOf(rowHeight, maximum)
                else -> rowHeight
            }
            measured[index] ?: child.measure(Constraints(
                minWidth = widthsPx[index], maxWidth = widthsPx[index],
                minHeight = targetHeight, maxHeight = targetHeight,
            ))
        }
        val width = if (constraints.maxWidth == Constraints.Infinity) {
            widthsPx.sum() + max(0, elements.size - 1) * gap.roundToPx()
        } else constraints.maxWidth
        val positions = IntArray(elements.size)
        with(horizontalArrangement(props.uischema.options, gap)) {
            arrange(width, widthsPx.toIntArray(), layoutDirection, positions)
        }
        layout(width.coerceAtLeast(constraints.minWidth), rowHeight) {
            placeables.forEachIndexed { index, child -> child.placeRelative(positions[index], 0) }
        }
    }
}
