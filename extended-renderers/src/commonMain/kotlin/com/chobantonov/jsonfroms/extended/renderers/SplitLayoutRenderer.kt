package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.Tester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

private val hasSplitterVariant: Tester = { uischema, _, _ ->
    (uischema.options["variant"] as? JsonPrimitive)?.contentOrNull?.lowercase() == "splitter"
}

val splitLayoutTester: RankedTester = rankWith(
    5,
    and({ ui, _, _ -> ui.type == "HorizontalLayout" || ui.type == "VerticalLayout" }, hasSplitterVariant),
)

@Composable
internal fun SplitLayout(props: ComposeJsonFormsRendererProps) {
    val elements = when (val element = props.uischema) {
        is HorizontalLayout -> element.elements
        is VerticalLayout -> element.elements
        else -> emptyList()
    }
    if (elements.isEmpty()) return
    val weights = remember(props.uischema, elements.size) {
        val supplied = (props.uischema.options["sizes"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.floatOrNull }
            ?.takeIf { it.size == elements.size && it.all { size -> size > 0f } }
        val initial = if (supplied == null) {
            List(elements.size) { 1f / elements.size }
        } else {
            val total = supplied.sum()
            supplied.map { it / total }
        }
        mutableStateListOf<Float>().apply { addAll(initial) }
    }
    val density = LocalDensity.current
    if (props.uischema is HorizontalLayout) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val totalPixels = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            Row(Modifier.fillMaxWidth().height(300.dp)) {
                elements.forEachIndexed { index, child ->
                    if (index > 0) {
                        Box(Modifier.width(8.dp).fillMaxHeight().background(Color.LightGray).pointerInput(index, totalPixels) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                resize(weights, index - 1, drag.x / totalPixels)
                            }
                        })
                    }
                    Box(Modifier.weight(weights[index]).fillMaxHeight()) { DispatchChild(props, child) }
                }
            }
        }
    } else {
        val heightOption = (props.uischema.options["height"] as? JsonPrimitive)?.contentOrNull
        val heightDp = heightOption?.removeSuffix("rem")?.toFloatOrNull()?.times(16f)
            ?: heightOption?.removeSuffix("dp")?.toFloatOrNull() ?: 360f
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().height(heightDp.dp)) {
            val totalPixels = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
            Column(Modifier.fillMaxWidth().fillMaxHeight()) {
                elements.forEachIndexed { index, child ->
                    if (index > 0) {
                        Box(Modifier.fillMaxWidth().height(8.dp).background(Color.LightGray).pointerInput(index, totalPixels) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                resize(weights, index - 1, drag.y / totalPixels)
                            }
                        })
                    }
                    Box(Modifier.weight(weights[index]).fillMaxWidth()) { DispatchChild(props, child) }
                }
            }
        }
    }
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
