package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chobantonov.jsonfroms.core.mappers.DetailDialogOptions
import com.chobantonov.jsonfroms.core.mappers.DialogDimensionAxis

/** Modal geometry shared by Material and Cupertino; each family supplies its own surface and actions. */
@Composable
fun DetailDialogFrame(
    options: DetailDialogOptions,
    resizeLabel: String,
    onDismiss: () -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable (maximized: Boolean, toggleMaximize: () -> Unit) -> Unit,
    footer: @Composable RowScope.() -> Unit,
    surface: @Composable (Modifier, @Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    var maximized by remember { mutableStateOf(false) }
    var userWidth by remember { mutableStateOf<Dp?>(null) }
    var userHeight by remember { mutableStateOf<Dp?>(null) }
    var measuredHeight by remember { mutableStateOf(0.dp) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val focus = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val viewportHeightPx = with(density) { maxHeight.toPx() }
            fun dimension(axis: DialogDimensionAxis): Dp? {
                val chosen = if (axis == DialogDimensionAxis.Width) options.width else options.height
                return chosen?.pixels(viewportWidthPx, viewportHeightPx, density.density, axis)
                    ?.let { with(density) { it.toDp() } }
            }
            val minWidth = minOf(320.dp, maxWidth)
            val minHeight = minOf(240.dp, maxHeight)
            val viewportWidth = maxWidth
            val viewportHeight = maxHeight
            val width = if (maximized) maxWidth else
                (userWidth ?: dimension(DialogDimensionAxis.Width) ?: 560.dp).coerceIn(minWidth, maxWidth)
            val exactHeight = if (maximized) maxHeight else
                (userHeight ?: dimension(DialogDimensionAxis.Height))?.coerceIn(minHeight, maxHeight)
            val actualHeight = exactHeight ?: measuredHeight.coerceAtLeast(minHeight)
            val maxX = ((maxWidth - width) / 2).value.coerceAtLeast(0f)
            val maxY = ((maxHeight - actualHeight) / 2).value.coerceAtLeast(0f)
            val shownX = if (maximized) 0f else offsetX.coerceIn(-maxX, maxX)
            val shownY = if (maximized) 0f else offsetY.coerceIn(-maxY, maxY)
            fun move(dx: Float, dy: Float) {
                offsetX = (offsetX + dx).coerceIn(-maxX, maxX)
                offsetY = (offsetY + dy).coerceIn(-maxY, maxY)
            }
            fun resize(dw: Float, dh: Float) {
                userWidth = (width + dw.dp).coerceIn(minWidth, maxWidth)
                userHeight = (actualHeight + dh.dp).coerceIn(minHeight, maxHeight)
            }
            surface(
                Modifier.offset(x = shownX.dp, y = shownY.dp)
                    .width(width)
                    .then(if (exactHeight != null) Modifier.height(exactHeight) else Modifier.heightIn(max = maxHeight))
                    .onSizeChanged { measuredHeight = with(density) { it.height.toDp() } }
                    .focusRequester(focus).focusable().testTag("row-detail-dialog-frame"),
            ) {
                Box(Modifier.then(if (exactHeight != null) Modifier.fillMaxSize() else Modifier)) {
                    Column(Modifier.then(if (exactHeight != null) Modifier.fillMaxSize() else Modifier)) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)
                                .then(if (options.draggable && !maximized)
                                    Modifier.pointerInput(maxX, maxY) {
                                        detectDragGestures { change, amount ->
                                            change.consume()
                                            move(with(density) { amount.x.toDp().value },
                                                with(density) { amount.y.toDp().value })
                                        }
                                    }
                                        .onKeyEvent { event ->
                                            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                            when (event.key) {
                                                Key.DirectionLeft -> move(-16f, 0f)
                                                Key.DirectionRight -> move(16f, 0f)
                                                Key.DirectionUp -> move(0f, -16f)
                                                Key.DirectionDown -> move(0f, 16f)
                                                else -> return@onKeyEvent false
                                            }
                                            true
                                        }.focusable()
                                else Modifier)
                                .testTag("row-detail-dialog-title")) { title() }
                            actions(maximized) { maximized = !maximized }
                        }
                        Box(
                            Modifier.fillMaxWidth()
                                .then(if (exactHeight != null) Modifier.weight(1f) else Modifier.heightIn(max = 420.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .testTag("row-detail-dialog-body"),
                        ) { content() }
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp,
                                end = if (options.resizable && !maximized) 36.dp else 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            content = footer,
                        )
                    }
                    if (options.resizable && !maximized) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).width(28.dp).height(28.dp)
                                .pointerInput(width, actualHeight, viewportWidth, viewportHeight) {
                                    detectDragGestures { change, amount ->
                                        change.consume()
                                        resize(with(density) { amount.x.toDp().value },
                                            with(density) { amount.y.toDp().value })
                                    }
                                }
                                .onKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                    when (event.key) {
                                        Key.DirectionLeft -> resize(-16f, 0f)
                                        Key.DirectionRight -> resize(16f, 0f)
                                        Key.DirectionUp -> resize(0f, -16f)
                                        Key.DirectionDown -> resize(0f, 16f)
                                        else -> return@onKeyEvent false
                                    }
                                    true
                                }
                                .semantics {
                                    contentDescription = resizeLabel
                                    stateDescription = "${width.value.toInt()} by ${actualHeight.value.toInt()} dp"
                                    progressBarRangeInfo = ProgressBarRangeInfo(width.value,
                                        minWidth.value..viewportWidth.value)
                                    setProgress { requested -> resize(requested - width.value, 0f); true }
                                }
                                .focusable().testTag("row-detail-dialog-resize"),
                        ) {
                            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                                val color = Color.Gray.copy(alpha = 0.8f)
                                drawLine(color, Offset(size.width * 0.55f, size.height),
                                    Offset(size.width, size.height * 0.55f), strokeWidth = 2.dp.toPx())
                                drawLine(color, Offset(size.width * 0.2f, size.height),
                                    Offset(size.width, size.height * 0.2f), strokeWidth = 2.dp.toPx())
                            }
                        }
                    }
                }
            }
            LaunchedEffect(focus) { focus.requestFocus() }
        }
    }
}
