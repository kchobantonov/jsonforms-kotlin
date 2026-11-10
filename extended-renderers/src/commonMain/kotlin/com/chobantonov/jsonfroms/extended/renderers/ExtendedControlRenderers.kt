package com.chobantonov.jsonfroms.extended.renderers

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl

internal data class ExtendedControlRenderers(
    val button: ComposeJsonFormsRenderer,
    val color: ComposeJsonFormsRenderer,
    val duration: ComposeJsonFormsRenderer,
    val file: ComposeJsonFormsRenderer,
    val nullValue: ComposeJsonFormsRenderer,
)

internal val materialExtendedControls = ExtendedControlRenderers(
    button = ComposeJsonFormsRenderer { ExtendedButton(it) },
    color = ComposeJsonFormsRenderer { ColorControl(it, MaterialTextControl) },
    duration = ComposeJsonFormsRenderer { DurationControl(it, MaterialTextControl) },
    file = ComposeJsonFormsRenderer { FileControl(it, MaterialTextControl) },
    nullValue = ComposeJsonFormsRenderer { NullControl(it) },
)

internal val cupertinoExtendedControls = ExtendedControlRenderers(
    button = ComposeJsonFormsRenderer { ExtendedButton(it) },
    color = ComposeJsonFormsRenderer { ColorControl(it, CupertinoTextControl) },
    duration = ComposeJsonFormsRenderer { DurationControl(it, CupertinoTextControl) },
    file = ComposeJsonFormsRenderer { FileControl(it, CupertinoTextControl) },
    nullValue = ComposeJsonFormsRenderer { NullControl(it) },
)
