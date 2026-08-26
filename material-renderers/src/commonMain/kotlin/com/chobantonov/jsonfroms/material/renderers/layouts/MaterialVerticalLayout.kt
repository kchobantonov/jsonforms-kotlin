package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs

val MaterialVerticalLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialVerticalLayout(props)
}

@Composable
fun MaterialVerticalLayout(
    props: ComposeJsonFormsRendererProps,
) {
    val layout = props.uischema as? VerticalLayout
        ?: error("MaterialVerticalLayout can only render VerticalLayout elements")
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
    MaterialLayoutRenderer(
        props = props.copy(enabled = layoutProps.enabled),
        elements = layout.elements,
        direction = LayoutDirection.Column,
    )
}

val materialVerticalLayoutTester: RankedTester = rankWith(
    1,
    uiTypeIs("VerticalLayout"),
)
