package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.LayoutHeightBounds
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeightBounds
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildWeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnSpacerHeight
import com.chobantonov.jsonfroms.core.mappers.resolveVerticalHeights
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.max
import kotlin.math.roundToInt

/** Measure each child once at its content height, then give weighted slots the bounded remainder. */
@Composable
fun WeightedColumnLayout(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    gap: Dp,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            val stretch = verticalStretch(props.uischema.options)
            elements.forEachIndexed { index, child ->
                if (child is SpacerElement) {
                    Spacer(columnChildWidthModifier(child, stretch)
                        .height(resolveColumnSpacerHeight(child).dp))
                } else {
                    Box(columnChildWidthModifier(child, stretch).testTag("layout-child-${props.path}-$index")) {
                        JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val childConstraints = Constraints(
            minWidth = 0,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val placeables = measurables.map { it.measure(childConstraints) }
        val natural = elements.mapIndexed { index, child ->
            resolveColumnChildHeight(child)?.dp?.roundToPx()?.toFloat() ?: placeables[index].height.toFloat()
        }
        val bounds = elements.map { child ->
            val source = resolveColumnChildHeightBounds(child)
            LayoutHeightBounds(
                source.minimum?.dp?.roundToPx()?.toFloat(),
                source.maximum?.dp?.roundToPx()?.toFloat(),
            )
        }
        val gapPx = gap.roundToPx()
        val heights = resolveVerticalHeights(
            availableHeight = constraints.maxHeight.takeUnless { it == Constraints.Infinity }?.toFloat(),
            gap = gapPx.toFloat(),
            naturalHeights = natural,
            weights = elements.map(::resolveColumnChildWeight),
            bounds = bounds,
        )
        val contentHeight = heights.sum().roundToInt() + max(0, elements.size - 1) * gapPx
        val width = if (constraints.maxWidth == Constraints.Infinity) {
            placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        } else constraints.maxWidth
        val desiredHeight = if (verticalJustificationNeedsSpace(props.uischema.options) &&
            constraints.maxHeight != Constraints.Infinity) constraints.maxHeight else contentHeight
        val height = max(desiredHeight, constraints.minHeight).coerceAtMost(constraints.maxHeight)
        val positions = IntArray(elements.size)
        with(verticalArrangement(props.uischema.options, gap)) {
            arrange(height, heights.map { it.roundToInt() }.toIntArray(), positions)
        }
        val align = (props.uischema.options["align"] as? JsonPrimitive)?.content
        layout(width, height) {
            placeables.forEachIndexed { index, child ->
                val inline = when (align) {
                    "center" -> (width - child.width) / 2
                    "end" -> width - child.width
                    else -> 0
                }
                child.placeRelative(inline, positions[index])
            }
        }
    }
}
