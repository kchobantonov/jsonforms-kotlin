package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable

internal actual val supportsWebComponentView: Boolean = false

@Composable
internal actual fun WebComponentOverlay(input: WebComponentInput, onClose: () -> Unit,
    onExitFormOnly: () -> Unit, onDataChange: (String, Boolean) -> Unit,
    onFormAction: (String, String, Boolean) -> Unit) = Unit
