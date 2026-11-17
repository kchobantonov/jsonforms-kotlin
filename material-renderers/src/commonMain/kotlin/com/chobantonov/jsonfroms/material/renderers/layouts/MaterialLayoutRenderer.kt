package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.ReportLayoutDiagnostics
import com.chobantonov.jsonfroms.compose.WeightedColumnLayout
import com.chobantonov.jsonfroms.compose.WrappedHorizontalLayout
import com.chobantonov.jsonfroms.compose.horizontalArrangement
import com.chobantonov.jsonfroms.compose.horizontalCrossAlignment
import com.chobantonov.jsonfroms.compose.horizontalStretch
import com.chobantonov.jsonfroms.compose.horizontalRowWidth
import com.chobantonov.jsonfroms.compose.StretchHorizontalRow
import com.chobantonov.jsonfroms.compose.rowChildHeightModifier
import com.chobantonov.jsonfroms.compose.columnChildWidthModifier
import com.chobantonov.jsonfroms.compose.verticalArrangement
import com.chobantonov.jsonfroms.compose.verticalCrossAlignment
import com.chobantonov.jsonfroms.compose.verticalJustificationNeedsSpace
import com.chobantonov.jsonfroms.compose.verticalStretch
import com.chobantonov.jsonfroms.core.models.SeparatorElement
import com.chobantonov.jsonfroms.core.models.SpacerElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.mappers.isVerticalSeparator
import com.chobantonov.jsonfroms.core.mappers.effectiveLayoutChildren
import com.chobantonov.jsonfroms.core.mappers.resolveColumnSpacerHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnSpacerWeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeightBounds
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildWeight
import com.chobantonov.jsonfroms.core.mappers.resolveHorizontalWidths
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutItemSizing
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutSizing
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator

enum class LayoutDirection {
    Row,
    Column,
}

@Composable
internal fun MaterialLayoutRenderer(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    direction: LayoutDirection,
    reportDiagnostics: Boolean = true,
) {
    val data = getData(props.state)
    val validator = getValidator(props.state)
    val effectiveElements = effectiveLayoutChildren(elements, data, validator, props.path, props.config)
    val sizing = resolveLayoutSizing(props.uischema.options, props.config)
    if (reportDiagnostics) ReportLayoutDiagnostics(props, elements, horizontal = direction == LayoutDirection.Row)

    when (direction) {
        LayoutDirection.Row -> BoxWithConstraints(Modifier.fillMaxWidth()) {
            val measuredWidth = maxWidth.value
            val viewportWidth = with(LocalDensity.current) {
                LocalWindowInfo.current.containerSize.width.toDp().value
            }
            val rowWidth = horizontalRowWidth(measuredWidth, viewportWidth)
            if (rowWidth == null) {
                Column(verticalArrangement = Arrangement.spacedBy(sizing.gap.dp)) {
                    effectiveElements.forEach { child ->
                        JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
                    }
                }
                return@BoxWithConstraints
            }
            val unbounded = !measuredWidth.isFinite()
            val rowModifier = if (unbounded) Modifier.width(rowWidth.dp) else Modifier.fillMaxWidth()
            if (sizing.wrap) {
                WrappedHorizontalLayout(props, effectiveElements, sizing, rowWidth, unbounded)
                return@BoxWithConstraints
            }
            val items = effectiveElements.map { resolveLayoutItemSizing(it, sizing.minItemWidth) }
            val widths = resolveHorizontalWidths(rowWidth, sizing, items)
            val overflow = widths.sum() + (effectiveElements.size - 1) * sizing.gap > rowWidth + 0.5f
            val hasVerticalSeparator = effectiveElements.any { it is SeparatorElement && isVerticalSeparator(it) }
            val stretch = horizontalStretch(props.uischema.options)
            if (stretch) {
                StretchHorizontalRow(props, effectiveElements, widths, sizing.gap.dp, overflow,
                    explicitWidth = rowWidth.takeIf { unbounded })
                return@BoxWithConstraints
            }
            Row(
                modifier = rowModifier
                    .then(if (hasVerticalSeparator) Modifier.height(IntrinsicSize.Min) else Modifier)
                    .then(if (overflow) Modifier.horizontalScroll(rememberScrollState())
                        .testTag("horizontal-layout-scroll") else Modifier),
                horizontalArrangement = horizontalArrangement(props.uischema.options, sizing.gap.dp),
                verticalAlignment = horizontalCrossAlignment(props.uischema.options),
            ) {
                effectiveElements.forEachIndexed { index, child ->
                    if (child is SpacerElement) {
                        Spacer(Modifier.width(widths[index].dp).then(rowChildHeightModifier(child)))
                    } else {
                        Box(modifier = Modifier.width(widths[index].dp)
                            .then(rowChildHeightModifier(child))
                            .then(if (child is SeparatorElement && isVerticalSeparator(child)) Modifier.fillMaxHeight() else Modifier)
                            .testTag("layout-child-${props.path}-$index")) {
                            JsonFormsDispatchRenderer(
                                props = props.copy(
                                    uischema = child,
                                    id = null,
                                ),
                            )
                        }
                    }
                }
            }
        }

        LayoutDirection.Column -> BoxWithConstraints(Modifier.fillMaxWidth()) {
            val boundedHeight = maxHeight.value.isFinite()
            if (boundedHeight && effectiveElements.any { resolveColumnChildWeight(it) != null }) {
                WeightedColumnLayout(props, effectiveElements, sizing.gap.dp)
                return@BoxWithConstraints
            }
            Column(
                modifier = Modifier.fillMaxWidth().then(if (boundedHeight &&
                    verticalJustificationNeedsSpace(props.uischema.options)) Modifier.fillMaxHeight() else Modifier),
                verticalArrangement = verticalArrangement(props.uischema.options, sizing.gap.dp),
                horizontalAlignment = verticalCrossAlignment(props.uischema.options),
            ) {
                effectiveElements.forEachIndexed { index, child ->
                    if (child is SpacerElement) {
                        val height = resolveColumnSpacerHeight(child).dp
                        val weight = resolveColumnSpacerWeight(child)
                        val bounds = resolveColumnChildHeightBounds(child)
                        val modifier = columnChildWidthModifier(child, verticalStretch(props.uischema.options))
                            .then(if (bounds.minimum != null || bounds.maximum != null) Modifier.heightIn(
                            min = (bounds.minimum ?: 0f).dp,
                            max = (bounds.maximum ?: Float.POSITIVE_INFINITY).dp,
                        ) else Modifier)
                        if (boundedHeight && weight != null) Spacer(modifier.weight(weight))
                        else Spacer(modifier.height(height))
                    } else {
                        val height = resolveColumnChildHeight(child)
                        val bounds = resolveColumnChildHeightBounds(child)
                        Box(columnChildWidthModifier(child, verticalStretch(props.uischema.options))
                            .then(if (bounds.minimum != null || bounds.maximum != null) Modifier.heightIn(
                                min = (bounds.minimum ?: 0f).dp,
                                max = (bounds.maximum ?: Float.POSITIVE_INFINITY).dp,
                            ) else Modifier)
                            .then(if (height != null) Modifier.height(height.dp) else Modifier)
                            .testTag("layout-child-${props.path}-$index")) {
                            JsonFormsDispatchRenderer(
                                props = props.copy(
                                    uischema = child,
                                    id = null,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
