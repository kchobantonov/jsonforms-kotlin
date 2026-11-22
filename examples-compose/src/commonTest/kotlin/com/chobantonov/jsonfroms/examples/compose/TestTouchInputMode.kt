package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager

/** Run clear-action visibility assertions under the touch policy used by mobile hosts. */
@Composable
internal fun TestTouchInputMode() {
    val manager = LocalInputModeManager.current
    LaunchedEffect(manager) { manager.requestInputMode(InputMode.Touch) }
}
