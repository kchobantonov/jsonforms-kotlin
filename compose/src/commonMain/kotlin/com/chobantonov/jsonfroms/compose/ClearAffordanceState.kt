package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size

internal class ClearAffordanceSelection {
    var selectedKey by mutableStateOf<Any?>(null)
}

internal val LocalClearAffordanceSelection = compositionLocalOf<ClearAffordanceSelection?> { null }

class ClearAffordanceState internal constructor(
    private val key: Any?,
    private val selection: ClearAffordanceSelection?,
) {
    private var fieldFocused by mutableStateOf(false)
    private var fieldHovered by mutableStateOf(false)
    private var fieldSelectedWithoutProvider by mutableStateOf(false)
    private var clearFocused by mutableStateOf(false)
    private var clearHovered by mutableStateOf(false)

    fun isVisible(): Boolean = fieldFocused || fieldHovered || clearFocused || clearHovered ||
        (selection?.selectedKey == key && key != null) || fieldSelectedWithoutProvider

    fun selectField() {
        if (selection != null && key != null) selection.selectedKey = key
        else fieldSelectedWithoutProvider = true
    }

    val fieldModifier: Modifier
        get() = Modifier.onFocusChanged { fieldFocused = it.hasFocus }
            .pointerInput(this) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Press -> selectField()
                            PointerEventType.Enter -> fieldHovered = true
                            PointerEventType.Exit -> fieldHovered = false
                        }
                    }
                }
            }

    fun clearModifier(size: Dp): Modifier {
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
    }
}

@Composable
fun rememberClearAffordanceState(key: Any?): ClearAffordanceState =
    LocalClearAffordanceSelection.current.let { selection ->
        remember(key, selection) { ClearAffordanceState(key, selection) }
    }

@Composable
fun rememberClearAffordanceSelectionReset(): () -> Unit {
    val selection = LocalClearAffordanceSelection.current
    return remember(selection) { { selection?.selectedKey = null } }
}
