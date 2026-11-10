package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable

internal actual val supportsWebComponentView: Boolean = false

@Composable
internal actual fun WebComponentOverlay(input: WebComponentInput, onClose: () -> Unit, onDataChange: (String) -> Unit) = Unit
