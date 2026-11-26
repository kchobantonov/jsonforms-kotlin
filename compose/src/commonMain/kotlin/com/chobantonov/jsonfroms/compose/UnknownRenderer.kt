package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry

/** Renderer-family hook for the visible fallback when no normal tester wins. */
interface ComposeUnknownRenderer {
    @Composable
    fun RenderUnknown(message: String)
}

@Composable
fun UnknownRenderer(
    type: String,
    message: String? = null,
    renderers: List<JsonFormsRendererRegistryEntry<ComposeJsonFormsRenderer>> = emptyList(),
) {
    val text = message ?: "No applicable $type found."
    val familyRenderer = renderers.firstNotNullOfOrNull { it.renderer as? ComposeUnknownRenderer }
    if (familyRenderer == null) BasicText(text) else familyRenderer.RenderUnknown(text)
}
