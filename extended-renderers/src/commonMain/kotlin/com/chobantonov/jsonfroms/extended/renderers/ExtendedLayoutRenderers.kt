package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer

internal data class ExtendedLayoutRenderers(
    val split: ComposeJsonFormsRenderer,
    val slot: ComposeJsonFormsRenderer,
)

internal val extendedLayouts = ExtendedLayoutRenderers(
    split = ComposeJsonFormsRenderer { SplitLayout(it) },
    slot = ComposeJsonFormsRenderer { NamedContent(it, ExtendedSlotsConfigKey, "slot") },
)
