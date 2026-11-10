package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.runtime.Composable

internal data class WebComponentInput(
    val data: String,
    val schema: String?,
    val uischema: String?,
    val readonly: Boolean,
)

internal expect val supportsWebComponentView: Boolean

@Composable
internal expect fun WebComponentOverlay(
    input: WebComponentInput,
    onClose: () -> Unit,
    onDataChange: (String) -> Unit,
)
