package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank

private val groupTester: RankedTester = rankWith(
    1,
    uiTypeIs("Group"),
)

val MaterialGroupLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialGroupLayout(props)
}

@Composable
fun MaterialGroupLayout(
    props: ComposeJsonFormsRendererProps,
    elevation: Dp = 2.dp,
    footer: (@Composable () -> Unit)? = null,
) {
    val layout = props.uischema as? GroupElement
        ?: error("MaterialGroupLayout can only render Group elements")
    val layoutProps = mapStateToLayoutProps(
        state = props.state,
        ownProps = OwnPropsOfLayout(
            own = com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )

    if (!layoutProps.visible) {
        return
    }
    val label = layoutProps.label

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(16.dp),
        ) {
            androidx.compose.foundation.layout.Column {
                MaterialLayoutRenderer(
                    props = props.copy(enabled = layoutProps.enabled),
                    elements = layout.elements,
                    direction = LayoutDirection.Column,
                )
                footer?.invoke()
            }
        }
    }
}

val materialGroupTester: RankedTester = withIncreasedRank(
    1,
    groupTester,
)
