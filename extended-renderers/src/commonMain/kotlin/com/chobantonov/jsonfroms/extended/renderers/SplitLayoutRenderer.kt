package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutSizing
import com.chobantonov.jsonfroms.core.mappers.effectiveLayoutChildren
import com.chobantonov.jsonfroms.core.mappers.resolveLayoutItemSizing
import com.chobantonov.jsonfroms.core.mappers.resolveHorizontalWidths
import com.chobantonov.jsonfroms.core.mappers.LayoutWidthMode
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeight
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildHeightBounds
import com.chobantonov.jsonfroms.core.mappers.resolveColumnChildWeight
import com.chobantonov.jsonfroms.core.mappers.resolveVerticalHeights
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.Tester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlin.math.roundToInt

private val hasSplitterVariant: Tester = { uischema, _, _ ->
    uischema.options["variant"] == JsonPrimitive("splitter")
}

val splitLayoutTester: RankedTester = rankWith(
    5,
    and({ ui, _, _ -> ui.type == "HorizontalLayout" || ui.type == "VerticalLayout" }, hasSplitterVariant),
)

@Composable
internal fun SplitLayout(props: ComposeJsonFormsRendererProps) {
    val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    val wrapDiagnostic = splitWrapDiagnostic(props.uischema.options, props.config)
    val resizableDiagnostic = splitResizableDiagnostic(props.uischema.options)
    val spanDiagnostics = splitSpanDiagnostics(props.uischema)
    LaunchedEffect(reporter, wrapDiagnostic, resizableDiagnostic, spanDiagnostics, props.path) {
        (listOfNotNull(wrapDiagnostic, resizableDiagnostic) + spanDiagnostics).forEach {
            reporter?.report(it.copy(details = it.details + ("path" to props.path)))
        }
    }
    val authoredElements = when (val element = props.uischema) {
        is HorizontalLayout -> element.elements
        is VerticalLayout -> element.elements
        else -> emptyList()
    }
    val data = getData(props.state)
    val validator = getValidator(props.state)
    val elements = effectiveLayoutChildren(authoredElements, data, validator, props.path, props.config)
    if (elements.isEmpty()) return
    val resizable = splitResizable(props.uischema.options)
    val resizeLabel = props.state.jsonforms.i18n.translate?.translate(
        "split.resize", "Resize panes", emptyMap(),
    ) ?: "Resize panes"
    val density = LocalDensity.current
    val separatorExtent = 8f + resolveLayoutSizing(props.uischema.options, props.config).gap
    if (props.uischema is HorizontalLayout) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val totalPixels = with(density) {
                (maxWidth.value - (elements.size - 1) * separatorExtent).coerceAtLeast(0f).dp.toPx()
            }.coerceAtLeast(1f)
            val width = maxWidth.value
            val weights = remember(props.uischema, elements) {
                mutableStateListOf<Float>().apply {
                    addAll(initialHorizontalSplitWeights(elements, width, props.uischema.options, props.config))
                }
            }
            var userResized by remember(props.uischema, elements) { mutableStateOf(false) }
            LaunchedEffect(width, separatorExtent, props.config, userResized) {
                if (!userResized) {
                    val initial = initialHorizontalSplitWeights(elements, width, props.uischema.options, props.config)
                    initial.forEachIndexed { index, value ->
                        if (kotlin.math.abs(weights[index] - value) > 0.0001f) weights[index] = value
                    }
                }
            }
            HorizontalSplitPaneLayout(
                props, elements, weights, totalPixels, separatorExtent, resizable, resizeLabel,
            ) { userResized = true }
        }
    } else {
        val heightOption = (props.uischema.options["height"] as? JsonPrimitive)?.contentOrNull
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val explicitHeight = parseSplitterHeight(heightOption)
            val parentHeight = maxHeight.value.takeIf { it.isFinite() }
            val heightDp = explicitHeight ?: parentHeight ?: 360f
            val fallback = explicitHeight == null && parentHeight == null
            LaunchedEffect(reporter, fallback, props.path) {
                if (fallback) reporter?.report(UiDiagnostic(
                    code = "layout.indefiniteHeight",
                    severity = UiDiagnosticSeverity.Warning,
                    message = "Vertical splitter needs a definite height; using 360dp.",
                    details = mapOf("path" to props.path, "elementType" to "VerticalLayout"),
                ))
            }
            androidx.compose.foundation.layout.BoxWithConstraints(
                Modifier.fillMaxWidth().height(heightDp.dp).testTag("split-layout-frame"),
            ) {
                val totalPixels = with(density) {
                    (maxHeight.value - (elements.size - 1) * separatorExtent).coerceAtLeast(0f).dp.toPx()
                }.coerceAtLeast(1f)
                val weights = remember(props.uischema, elements) {
                    mutableStateListOf<Float>().apply { addAll(initialVerticalSplitWeights(elements, heightDp, separatorExtent)) }
                }
                val naturalHeights = remember(props.uischema, elements) {
                    mutableStateListOf<Float>().apply { addAll(List(elements.size) { 0f }) }
                }
                var userResized by remember(props.uischema, elements) { mutableStateOf(false) }
                LaunchedEffect(heightDp, separatorExtent, naturalHeights.toList(), userResized) {
                    if (!userResized) {
                        val initial = initialVerticalSplitWeights(elements, heightDp, separatorExtent, naturalHeights)
                        initial.forEachIndexed { index, value ->
                            if (kotlin.math.abs(weights[index] - value) > 0.0001f) weights[index] = value
                        }
                    }
                }
                val overconstrained = weights.sum() > 1.0001f
                val paneHeight = (heightDp - (elements.size - 1) * separatorExtent).coerceAtLeast(0f)
                Column(Modifier.fillMaxWidth().fillMaxHeight().then(
                    if (overconstrained) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                ).testTag("split-layout-content")) {
                    elements.forEachIndexed { index, child ->
                        if (index > 0) {
                            SplitterHandle(false, index - 1, totalPixels, separatorExtent, weights, resizable, resizeLabel) {
                                userResized = true
                            }
                        }
                        val paneSize = if (overconstrained) Modifier.height((weights[index] * paneHeight).dp)
                            else Modifier.weight(weights[index])
                        Box(paneSize.fillMaxWidth().clipToBounds()
                            .verticalScroll(rememberScrollState()).testTag("split-layout-pane-$index")) {
                            Box(Modifier.wrapContentHeight(unbounded = true).onSizeChanged { size ->
                                val natural = with(density) { size.height.toDp().value }
                                if (kotlin.math.abs(naturalHeights[index] - natural) > 0.5f) {
                                    naturalHeights[index] = natural
                                }
                            }) { DispatchChild(props, child) }
                        }
                    }
                    val spare = if (overconstrained) 0f else (1f - weights.sum()).coerceAtLeast(0f)
                    if (spare > 0.0001f) Spacer(Modifier.weight(spare))
                }
            }
        }
    }
}

@Composable
private fun HorizontalSplitPaneLayout(
    props: ComposeJsonFormsRendererProps,
    elements: List<UiSchemaElement>,
    weights: MutableList<Float>,
    totalPixels: Float,
    separatorExtent: Float,
    resizable: Boolean,
    resizeLabel: String,
    onUserResize: () -> Unit,
) {
    Layout(
        modifier = Modifier.fillMaxWidth(),
        content = {
            elements.forEachIndexed { index, child ->
                Box { DispatchChild(props, child) }
                if (index < elements.lastIndex) {
                    SplitterHandle(true, index, totalPixels, separatorExtent, weights, resizable, resizeLabel, onUserResize)
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth.takeUnless { it == Constraints.Infinity } ?: constraints.minWidth
        val separatorPx = separatorExtent.dp.roundToPx()
        val paneSpace = (width - elements.lastIndex * separatorPx).coerceAtLeast(0)
        val totalWeight = weights.sum().takeIf { it > 0f && it.isFinite() } ?: 1f
        var remaining = paneSpace
        val paneWidths = elements.indices.map { index ->
            val size = if (index == elements.lastIndex) remaining else
                (paneSpace * weights[index] / totalWeight).roundToInt().coerceIn(0, remaining)
            remaining -= size
            size
        }
        val panes = paneWidths.mapIndexed { index, paneWidth ->
            measurables[index * 2].measure(Constraints(
                minWidth = paneWidth,
                maxWidth = paneWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ))
        }
        val height = (panes.maxOfOrNull { it.height } ?: 0)
            .coerceAtLeast(constraints.minHeight).coerceAtMost(constraints.maxHeight)
        val dividers = (0 until elements.lastIndex).map { index ->
            measurables[index * 2 + 1].measure(Constraints.fixed(separatorPx, height))
        }
        layout(width, height) {
            var x = 0
            panes.forEachIndexed { index, pane ->
                pane.placeRelative(x, 0)
                x += paneWidths[index]
                if (index < dividers.size) {
                    dividers[index].placeRelative(x, 0)
                    x += separatorPx
                }
            }
        }
    }
}

internal fun parseSplitterHeight(value: String?): Float? {
    val raw = value?.trim() ?: return null
    val height = when {
        raw.endsWith("rem") -> raw.removeSuffix("rem").toFloatOrNull()?.times(16f)
        raw.endsWith("dp") || raw.endsWith("px") -> raw.dropLast(2).toFloatOrNull()
        else -> raw.toFloatOrNull()
    }
    return height?.takeIf { it.isFinite() && it >= 0f }
}

internal fun initialVerticalSplitWeights(
    elements: List<UiSchemaElement>, availableHeight: Float, separatorExtent: Float = 24f,
    naturalHeights: List<Float> = emptyList(),
): List<Float> {
    if (elements.isEmpty()) return emptyList()
    val paneHeight = (availableHeight - (elements.size - 1) * separatorExtent).coerceAtLeast(0f)
    val fixed = elements.map(::resolveColumnChildHeight)
    val heights = resolveVerticalHeights(
        availableHeight = paneHeight,
        gap = 0f,
        naturalHeights = fixed.mapIndexed { index, value -> value ?: (naturalHeights.getOrNull(index) ?: 0f) },
        weights = elements.mapIndexed { index, child ->
            if (fixed[index] != null) null else resolveColumnChildWeight(child)
        },
        bounds = elements.map(::resolveColumnChildHeightBounds),
    )
    val total = heights.sum()
    return if (total > 0f && total.isFinite() && paneHeight > 0f) {
        heights.map { (it / paneHeight).coerceAtLeast(0.0001f) }
    }
    else List(elements.size) { 1f / elements.size }
}

internal fun initialHorizontalSplitWeights(
    elements: List<UiSchemaElement>,
    availableWidth: Float,
    options: JsonOptions,
    config: Map<String, Any?>,
): List<Float> {
    if (elements.isEmpty()) return emptyList()
    val baseSizing = resolveLayoutSizing(options, config)
    val sizing = baseSizing.copy(gap = 8f + baseSizing.gap)
    val items = elements.map { element ->
        val item = resolveLayoutItemSizing(element, sizing.minItemWidth)
        if (item.mode is LayoutWidthMode.Span) item.copy(mode = LayoutWidthMode.Weight(1f)) else item
    }
    val widths = resolveHorizontalWidths(availableWidth, sizing, items)
    val total = widths.sum()
    return if (total > 0f && total.isFinite()) widths.map { (it / total).coerceAtLeast(0.0001f) }
    else List(elements.size) { 1f / elements.size }
}

internal fun splitSpanDiagnostics(layout: UiSchemaElement): List<UiDiagnostic> {
    val children = when (layout) {
        is HorizontalLayout -> layout.elements
        is VerticalLayout -> layout.elements
        else -> emptyList()
    }
    return children.mapIndexedNotNull { index, child ->
        if ((child.options["layout"] as? JsonObject)?.get("span") == null) return@mapIndexedNotNull null
        UiDiagnostic(
            code = "layout.unsupportedOption",
            severity = UiDiagnosticSeverity.Warning,
            message = "Splitter pane layout.span is unsupported; using Auto.",
            details = mapOf("source" to "options.layout", "key" to "span", "elementType" to "SplitLayout", "index" to index),
        )
    }
}

internal fun splitResizable(options: JsonOptions): Boolean {
    val raw = options["resizable"] as? JsonPrimitive ?: return true
    return if (raw.isString) true else raw.booleanOrNull ?: true
}

internal fun splitResizableDiagnostic(options: JsonOptions): UiDiagnostic? {
    if (!options.containsKey("resizable")) return null
    val raw = options["resizable"] as? JsonPrimitive
    if (raw != null && !raw.isString && raw.booleanOrNull != null) return null
    return UiDiagnostic(
        code = "layout.invalidOption",
        severity = UiDiagnosticSeverity.Warning,
        message = "options.resizable must be a boolean; using true.",
        details = mapOf("source" to "options", "key" to "resizable", "elementType" to "SplitLayout"),
    )
}

@Composable
private fun SplitterHandle(
    horizontal: Boolean,
    boundary: Int,
    totalPixels: Float,
    separatorExtent: Float,
    weights: MutableList<Float>,
    resizable: Boolean,
    label: String,
    onUserResize: () -> Unit = {},
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val current = weights[boundary]
    val combined = current + weights[boundary + 1]
    val minimum = minOf(0.1f, combined / 3f)
    val direction = if (horizontal && layoutDirection == LayoutDirection.Rtl) -1f else 1f
    val slot = if (horizontal) Modifier.width(separatorExtent.dp).fillMaxHeight()
        else Modifier.fillMaxWidth().height(separatorExtent.dp)
    val handle = if (horizontal) Modifier.width(8.dp).fillMaxHeight()
        else Modifier.fillMaxWidth().height(8.dp)
    Box(slot) {
        Box(handle.align(Alignment.Center).background(Color.LightGray)
            .then(if (resizable) Modifier.pointerInput(boundary, totalPixels, horizontal, direction, weights) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val delta = if (horizontal) drag.x * direction else drag.y
                    onUserResize()
                    resize(weights, boundary, delta / totalPixels)
                }
            } else Modifier)
            .then(if (resizable) Modifier.onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = with(density) { 16.dp.toPx() } / totalPixels
                val change = when (event.key) {
                    Key.DirectionLeft -> if (horizontal) -step * direction else return@onKeyEvent false
                    Key.DirectionRight -> if (horizontal) step * direction else return@onKeyEvent false
                    Key.DirectionUp -> if (!horizontal) -step else return@onKeyEvent false
                    Key.DirectionDown -> if (!horizontal) step else return@onKeyEvent false
                    Key.MoveHome -> minimum - weights[boundary]
                    Key.MoveEnd -> combined - minimum - weights[boundary]
                    else -> return@onKeyEvent false
                }
                onUserResize()
                resize(weights, boundary, change)
                true
            } else Modifier)
            .then(if (resizable) Modifier.semantics {
                contentDescription = label
                stateDescription = "${(current * 100).toInt()}%"
                progressBarRangeInfo = ProgressBarRangeInfo(current, minimum..(combined - minimum))
                setProgress { requested ->
                    onUserResize()
                    resize(weights, boundary, requested - weights[boundary])
                    true
                }
            } else Modifier)
            .then(if (resizable) Modifier.focusable() else Modifier)
            .testTag("split-layout-divider-$boundary"))
    }
}

internal fun splitWrapDiagnostic(options: JsonOptions, config: Map<String, Any?>): UiDiagnostic? {
    if (!resolveLayoutSizing(options, config).wrap) return null
    val source = if (options.containsKey("wrap")) "options" else "config.jsonformsExtended.layoutDefaults"
    return UiDiagnostic(
        code = "layout.unsupportedOption",
        severity = UiDiagnosticSeverity.Warning,
        message = "$source.wrap cannot be used with a splitter; ignored.",
        details = mapOf("source" to source, "key" to "wrap", "elementType" to "SplitLayout"),
    )
}

private fun resize(weights: MutableList<Float>, boundary: Int, change: Float) {
    val combined = weights[boundary] + weights[boundary + 1]
    val minimum = minOf(0.1f, combined / 3f)
    val first = (weights[boundary] + change).coerceIn(minimum, combined - minimum)
    weights[boundary] = first
    weights[boundary + 1] = combined - first
}

@Composable
internal fun DispatchChild(props: ComposeJsonFormsRendererProps, child: UiSchemaElement) {
    JsonFormsDispatchRenderer(props.copy(uischema = child, id = null))
}
