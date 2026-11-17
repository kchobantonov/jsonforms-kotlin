package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.LayoutSizing
import com.chobantonov.jsonfroms.core.mappers.isVerticalSeparator
import com.chobantonov.jsonfroms.core.mappers.resolveHorizontalWidths
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutItemSizing
import com.chobantonov.jsonfroms.core.mappers.resolveWrappedRows
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement

/** Author order is preserved; each wrapped line resolves its own flexible remainder. */
@Composable
fun WrappedHorizontalLayout(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    sizing: LayoutSizing,
    width: Float,
    constrainToWidth: Boolean = false,
) {
    val items = elements.map { resolveLayoutItemSizing(it, sizing.minItemWidth) }
    val rows = resolveWrappedRows(width, sizing, items)
    val rowModifier = if (constrainToWidth) Modifier.width(width.dp) else Modifier.fillMaxWidth()
    Column(rowModifier, verticalArrangement = Arrangement.spacedBy(sizing.gap.dp)) {
        rows.forEach { range ->
            val rowElements = elements.slice(range)
            val widths = resolveHorizontalWidths(width, sizing, items.slice(range))
            val overflow = widths.sum() + (rowElements.size - 1) * sizing.gap > width + 0.5f
            val hasVerticalSeparator = rowElements.any { it is SeparatorElement && isVerticalSeparator(it) }
            val stretch = horizontalStretch(props.uischema.options)
            if (stretch) {
                StretchHorizontalRow(props, rowElements, widths, sizing.gap.dp, overflow, range.first,
                    explicitWidth = width.takeIf { constrainToWidth })
            } else Row(
                modifier = rowModifier
                    .then(if (hasVerticalSeparator) Modifier.height(IntrinsicSize.Min) else Modifier)
                    .then(if (overflow) Modifier.horizontalScroll(rememberScrollState())
                        .testTag("horizontal-layout-scroll") else Modifier),
                horizontalArrangement = horizontalArrangement(props.uischema.options, sizing.gap.dp),
                verticalAlignment = horizontalCrossAlignment(props.uischema.options),
            ) {
                rowElements.forEachIndexed { index, child ->
                    if (child is SpacerElement) {
                        Spacer(Modifier.width(widths[index].dp).then(rowChildHeightModifier(child)))
                    } else {
                        Box(Modifier.width(widths[index].dp)
                            .then(rowChildHeightModifier(child))
                            .then(if (child is SeparatorElement && isVerticalSeparator(child)) Modifier.fillMaxHeight() else Modifier)
                            .testTag("layout-child-${props.path}-${range.first + index}")) {
                            JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
                        }
                    }
                }
            }
        }
    }
}
