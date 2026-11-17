package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val HandleWidth = 24.dp
private val MinListWidth = 280.dp
private val MinDetailWidth = 280.dp

@Composable
fun ListDetailSplitPane(
    identity: Any,
    resizeLabel: String,
    resizable: Boolean = true,
    fraction: Float? = null,
    onFractionChange: ((Float) -> Unit)? = null,
    dividerColor: Color? = null,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit,
) {
    var internalFraction by remember(identity) { mutableFloatStateOf(0.32f) }
    var listHeight by remember(identity) { mutableStateOf(256.dp) }
    var detailHeight by remember(identity) { mutableStateOf(0.dp) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                list(Modifier.fillMaxWidth())
                detail(Modifier.fillMaxWidth())
            }
        } else {
            val usableWidth = maxWidth - HandleWidth
            val minimum = (MinListWidth / usableWidth).coerceAtMost(0.5f)
            val maximum = (1f - MinDetailWidth / usableWidth).coerceAtLeast(0.5f)
            val current = (fraction ?: internalFraction).coerceIn(minimum, maximum)
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val usablePixels = with(density) { usableWidth.toPx() }
            val dividerHeight = maxOf(256.dp, listHeight, detailHeight)
            val directionSign = if (direction == LayoutDirection.Ltr) 1f else -1f
            fun resizeTo(fraction: Float) {
                val next = fraction.coerceIn(minimum, maximum)
                if (onFractionChange != null) onFractionChange(next) else internalFraction = next
            }
            val dragState = rememberDraggableState { delta ->
                resizeTo((fraction ?: internalFraction) + directionSign * delta / usablePixels)
            }
            Row(verticalAlignment = Alignment.Top) {
                list(Modifier.width(usableWidth * current)
                    .onSizeChanged { listHeight = with(density) { it.height.toDp() } }
                    .testTag("list-detail-list-pane"))
                Box(
                    Modifier.width(HandleWidth).height(dividerHeight)
                        .then(if (resizable) Modifier.draggable(dragState, Orientation.Horizontal) else Modifier)
                        .then(if (resizable) Modifier.onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val step = with(density) { 16.dp.toPx() } / usablePixels
                            when (event.key) {
                                Key.DirectionLeft -> resizeTo((fraction ?: internalFraction) - directionSign * step)
                                Key.DirectionRight -> resizeTo((fraction ?: internalFraction) + directionSign * step)
                                Key.MoveHome -> resizeTo(minimum)
                                Key.MoveEnd -> resizeTo(maximum)
                                else -> return@onKeyEvent false
                            }
                            true
                        } else Modifier)
                        .then(if (resizable) Modifier.semantics {
                            contentDescription = resizeLabel
                            stateDescription = "${(current * 100).toInt()}%"
                            progressBarRangeInfo = ProgressBarRangeInfo(current, minimum..maximum)
                            setProgress { requested ->
                                resizeTo(requested)
                                true
                            }
                        } else Modifier)
                        .then(if (resizable) Modifier.focusable() else Modifier)
                        .testTag("list-detail-splitter"),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        Modifier.width(2.dp).height(dividerHeight),
                        color = dividerColor ?: MaterialTheme.colorScheme.outlineVariant,
                    ) {}
                }
                detail(Modifier.weight(1f)
                    .onSizeChanged { detailHeight = with(density) { it.height.toDp() } }
                    .testTag("list-detail-detail-pane"))
            }
        }
    }
}
