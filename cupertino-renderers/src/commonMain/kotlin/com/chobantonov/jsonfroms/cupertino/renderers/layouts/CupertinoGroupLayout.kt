package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors

val CupertinoGroupLayout = ComposeJsonFormsRenderer { props ->
    CupertinoGroupLayout(props)
}

@Composable
internal fun CupertinoGroupLayout(
    props: com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps,
    footer: (@Composable () -> Unit)? = null,
) {
    val layout = props.uischema as? GroupElement
        ?: error("CupertinoGroupLayout can only render Group elements")
    val layoutProps = mapStateToLayoutProps(
        state = props.state,
        ownProps = OwnPropsOfLayout(
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )
    if (!layoutProps.visible) return
    val colors = LocalCupertinoColors.current
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (!layoutProps.label.isNullOrBlank()) {
            Text(
                layoutProps.label!!,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.labelLarge,
                color = colors.secondaryLabel,
            )
        }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(colors.tertiaryBackground).padding(16.dp),
        ) {
            Column {
                CupertinoLayoutRenderer(
                    props.copy(enabled = layoutProps.enabled),
                    layout.elements,
                    LayoutDirection.Column,
                )
                footer?.invoke()
            }
        }
    }
}

val cupertinoGroupTester: RankedTester = withIncreasedRank(100, rankWith(2, uiTypeIs("Group")))
