package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoTextControl
import com.chobantonov.jsonfroms.material.renderers.controls.MaterialTextControl

internal data class ExtendedControlRenderers(
    val button: ComposeJsonFormsRenderer,
    val color: ComposeJsonFormsRenderer,
    val duration: ComposeJsonFormsRenderer,
    val cron: ComposeJsonFormsRenderer,
    val file: ComposeJsonFormsRenderer,
    val nullValue: ComposeJsonFormsRenderer,
)

internal val materialExtendedControls = ExtendedControlRenderers(
    button = ComposeJsonFormsRenderer { ExtendedButton(it) },
    color = ComposeJsonFormsRenderer { ColorControl(it, cupertino = false) },
    duration = ComposeJsonFormsRenderer { DurationControl(it, cupertino = false) },
    cron = ComposeJsonFormsRenderer { CronControl(it, cupertino = false) },
    file = ComposeJsonFormsRenderer { FileControl(it, MaterialTextControl, cupertino = false) },
    nullValue = ComposeJsonFormsRenderer { NullControl(it, cupertino = false) },
)

@Composable
internal fun FileCell(props: ComposeJsonFormsCellProps, cupertino: Boolean) {
    FileControl(
        ComposeJsonFormsRendererProps(
            state = props.state,
            dispatch = props.dispatch,
            uischema = props.uischema,
            schema = props.schema,
            rootSchema = props.rootSchema,
            path = props.path,
            enabled = props.enabled,
            renderers = props.renderers,
            cells = props.cells,
            id = props.id,
            config = props.config,
        ),
        textRenderer = if (cupertino) CupertinoTextControl else MaterialTextControl,
        cupertino = cupertino,
        compact = true,
    )
}

internal val cupertinoExtendedControls = ExtendedControlRenderers(
    button = ComposeJsonFormsRenderer { ExtendedCupertinoButton(it) },
    color = ComposeJsonFormsRenderer { ColorControl(it, cupertino = true) },
    duration = ComposeJsonFormsRenderer { DurationControl(it, cupertino = true) },
    cron = ComposeJsonFormsRenderer { CronControl(it, cupertino = true) },
    file = ComposeJsonFormsRenderer { FileControl(it, CupertinoTextControl, cupertino = true) },
    nullValue = ComposeJsonFormsRenderer { NullControl(it, cupertino = true) },
)
