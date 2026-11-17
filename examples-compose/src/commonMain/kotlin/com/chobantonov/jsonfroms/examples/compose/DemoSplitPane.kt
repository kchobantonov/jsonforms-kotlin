package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val HandleWidth = 24.dp
private val MinPaneWidth = 280.dp

@Composable
internal fun DemoSplitPane(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    resizeLabel: String,
    modifier: Modifier = Modifier,
    form: @Composable (Modifier) -> Unit,
    data: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < MinPaneWidth * 2 + HandleWidth) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                form(Modifier.weight(1f).fillMaxWidth().testTag("demo-form-pane"))
                data(Modifier.weight(1f).fillMaxWidth().testTag("demo-data-pane"))
            }
        } else {
            val usableWidth = maxWidth - HandleWidth
            val minimum = (MinPaneWidth / usableWidth).coerceAtMost(0.5f)
            val maximum = (1f - MinPaneWidth / usableWidth).coerceAtLeast(0.5f)
            val current = fraction.coerceIn(minimum, maximum)
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val usablePixels = with(density) { usableWidth.toPx() }
            val directionSign = if (direction == LayoutDirection.Ltr) 1f else -1f
            fun resizeTo(value: Float) = onFractionChange(value.coerceIn(minimum, maximum))
            val drag = rememberDraggableState { delta ->
                resizeTo(fraction + directionSign * delta / usablePixels)
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                form(Modifier.width(usableWidth * current).fillMaxHeight().testTag("demo-form-pane"))
                Box(
                    Modifier.width(HandleWidth).fillMaxHeight()
                        .draggable(drag, Orientation.Horizontal)
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                            val step = with(density) { 16.dp.toPx() } / usablePixels
                            when (event.key) {
                                Key.DirectionLeft -> resizeTo(current - directionSign * step)
                                Key.DirectionRight -> resizeTo(current + directionSign * step)
                                Key.MoveHome -> resizeTo(minimum)
                                Key.MoveEnd -> resizeTo(maximum)
                                else -> return@onKeyEvent false
                            }
                            true
                        }
                        .semantics {
                            contentDescription = resizeLabel
                            stateDescription = "${(current * 100).toInt()}%"
                            progressBarRangeInfo = ProgressBarRangeInfo(current, minimum..maximum)
                            setProgress { requested -> resizeTo(requested); true }
                        }
                        .focusable()
                        .testTag("demo-splitter"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant))
                }
                data(Modifier.weight(1f).fillMaxHeight().testTag("demo-data-pane"))
            }
        }
    }
}
