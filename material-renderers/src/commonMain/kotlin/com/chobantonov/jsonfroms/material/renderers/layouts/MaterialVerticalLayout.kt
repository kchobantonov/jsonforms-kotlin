package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
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
    MaterialLayoutRenderer(
        props = props,
        elements = layout.elements,
        direction = LayoutDirection.Column,
    )
}

val materialVerticalLayoutTester: RankedTester = rankWith(
    1,
    uiTypeIs("VerticalLayout"),
)
