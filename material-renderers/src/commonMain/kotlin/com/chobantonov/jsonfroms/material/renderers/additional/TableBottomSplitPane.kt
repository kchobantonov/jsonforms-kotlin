package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun TableBottomSplitPane(
    identity: Any,
    resizeLabel: String,
    resizable: Boolean,
    tableHeightDp: Float? = null,
    onTableHeightChange: ((Float) -> Unit)? = null,
    table: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit,
) {
    var internalHeight by remember(identity) { mutableFloatStateOf(280f) }
    val tableHeight = tableHeightDp ?: internalHeight
    val density = LocalDensity.current
    fun resizeTo(height: Float) {
        val next = height.coerceIn(160f, 380f)
        if (onTableHeightChange != null) onTableHeightChange(next) else internalHeight = next
    }
    val dragState = rememberDraggableState { delta ->
        resizeTo(tableHeight + with(density) { delta.toDp().value })
    }
    Column(Modifier.fillMaxWidth().height(560.dp)) {
        Box(Modifier.fillMaxWidth().height(tableHeight.dp).verticalScroll(rememberScrollState())) {
            table(Modifier.fillMaxWidth())
        }
        Box(
            Modifier.fillMaxWidth().height(20.dp)
                .then(if (resizable) Modifier.draggable(dragState, Orientation.Vertical) else Modifier)
                .then(if (resizable) Modifier.onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> resizeTo(tableHeight - 16f)
                        Key.DirectionDown -> resizeTo(tableHeight + 16f)
                        Key.MoveHome -> resizeTo(160f)
                        Key.MoveEnd -> resizeTo(380f)
                        else -> return@onKeyEvent false
                    }
                    true
                } else Modifier)
                .then(if (resizable) Modifier.semantics {
                    contentDescription = resizeLabel
                    stateDescription = "${tableHeight.toInt()} dp"
                    progressBarRangeInfo = ProgressBarRangeInfo(tableHeight, 160f..380f)
                    setProgress { requested -> resizeTo(requested); true }
                } else Modifier)
                .then(if (resizable) Modifier.focusable() else Modifier)
                .testTag("table-detail-bottom-splitter"),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }
        Box(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            detail(Modifier.fillMaxWidth())
        }
    }
}
