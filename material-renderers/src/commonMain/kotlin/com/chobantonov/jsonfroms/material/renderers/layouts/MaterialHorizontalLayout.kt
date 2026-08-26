package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private val DefaultSmallScreenBreakpoint = 600.dp

val MaterialHorizontalLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialHorizontalLayout(props)
}

@Composable
fun MaterialHorizontalLayout(
    props: ComposeJsonFormsRendererProps,
) {
    val layout = props.uischema as? HorizontalLayout
        ?: error("MaterialHorizontalLayout can only render HorizontalLayout elements")
    val layoutProps = mapStateToLayoutProps(
        props.state,
        OwnPropsOfLayout(
            own = OwnPropsOfRenderer(
                uischema = layout,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )
    if (!layoutProps.visible) return
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        MaterialLayoutRenderer(
            props = props.copy(enabled = layoutProps.enabled),
            elements = layout.elements,
            direction = horizontalLayoutDirection(layout, maxWidth),
        )
    }
}

internal fun horizontalLayoutDirection(
    layout: HorizontalLayout,
    availableWidth: Dp,
): LayoutDirection {
    val stackOnSmallScreen =
        (layout.options["stackOnSmallScreen"] as? JsonPrimitive)?.booleanOrNull == true
    return if (stackOnSmallScreen && availableWidth < DefaultSmallScreenBreakpoint) {
        LayoutDirection.Column
    } else {
        LayoutDirection.Row
    }
}

val materialHorizontalLayoutTester: RankedTester = rankWith(
    2,
    uiTypeIs("HorizontalLayout"),
)
