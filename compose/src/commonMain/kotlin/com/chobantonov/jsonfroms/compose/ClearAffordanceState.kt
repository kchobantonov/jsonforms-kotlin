package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size

class ClearAffordanceState internal constructor() {
    private var fieldFocused by mutableStateOf(false)
    private var fieldHovered by mutableStateOf(false)
    private var clearFocused by mutableStateOf(false)
    private var clearHovered by mutableStateOf(false)

    val fieldModifier: Modifier
        get() = Modifier.onFocusChanged { fieldFocused = it.isFocused }
            .pointerInput(this) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> fieldHovered = true
                            PointerEventType.Exit -> fieldHovered = false
                        }
                    }
                }
            }

    @Composable
    fun clearModifier(size: Dp): Modifier {
        val inputModeManager = LocalInputModeManager.current
        return Modifier.size(size)
            .onFocusChanged { clearFocused = it.isFocused }
            .pointerInput(this) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> clearHovered = true
                            PointerEventType.Exit -> clearHovered = false
                        }
                    }
                }
            }
            .graphicsLayer {
                alpha = if (fieldFocused || fieldHovered || clearFocused || clearHovered ||
                    inputModeManager.inputMode == InputMode.Touch) 1f else 0f
            }
    }
}

@Composable
fun rememberClearAffordanceState(key: Any?): ClearAffordanceState =
    remember(key) { ClearAffordanceState() }
