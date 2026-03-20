package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs

val MaterialHorizontalLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialHorizontalLayout(props)
}

@Composable
fun MaterialHorizontalLayout(
    props: ComposeJsonFormsRendererProps,
) {
    val layout = props.uischema as? HorizontalLayout
        ?: error("MaterialHorizontalLayout can only render HorizontalLayout elements")
    MaterialLayoutRenderer(
        props = props,
        elements = layout.elements,
        direction = LayoutDirection.Row,
    )
}

val materialHorizontalLayoutTester: RankedTester = rankWith(
    2,
    uiTypeIs("HorizontalLayout"),
)
