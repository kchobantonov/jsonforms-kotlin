package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val CupertinoHorizontalLayout = ComposeJsonFormsRenderer { props ->
    val layout = props.uischema as? HorizontalLayout
        ?: error("CupertinoHorizontalLayout can only render HorizontalLayout elements")
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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stack = (layout.options["stackOnSmallScreen"] as? JsonPrimitive)?.booleanOrNull == true && maxWidth < 600.dp
        CupertinoLayoutRenderer(
            props.copy(enabled = layoutProps.enabled),
            layout.elements,
            if (stack) LayoutDirection.Column else LayoutDirection.Row,
        )
    }
}

val cupertinoHorizontalLayoutTester: RankedTester = withIncreasedRank(100, rankWith(2, uiTypeIs("HorizontalLayout")))
