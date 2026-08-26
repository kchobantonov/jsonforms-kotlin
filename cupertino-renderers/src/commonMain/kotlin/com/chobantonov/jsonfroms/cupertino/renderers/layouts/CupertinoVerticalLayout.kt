package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

val CupertinoVerticalLayout = ComposeJsonFormsRenderer { props ->
    val layout = props.uischema as? VerticalLayout
        ?: error("CupertinoVerticalLayout can only render VerticalLayout elements")
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
    if (!layoutProps.visible) return@ComposeJsonFormsRenderer
    CupertinoLayoutRenderer(props.copy(enabled = layoutProps.enabled), layout.elements, LayoutDirection.Column)
}

val cupertinoVerticalLayoutTester: RankedTester = withIncreasedRank(100, rankWith(1, uiTypeIs("VerticalLayout")))
