package com.chobantonov.jsonfroms.cupertino.renderers.additional

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.compositionLocalOf
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors

internal val LocalCupertinoGroupSupportingLabel = compositionLocalOf { false }

val CupertinoLabelRenderer = ComposeJsonFormsRenderer { props ->
    val label = props.uischema as? LabelElement
        ?: error("CupertinoLabelRenderer can only render Label elements")
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
    if (!layoutProps.visible) return@ComposeJsonFormsRenderer
    val text = deriveLabelForUISchemaElement(
        label,
        props.state.jsonforms.i18n.translate ?: defaultTranslator,
    ).orEmpty()
    if (text.isNotBlank()) {
        val supporting = LocalCupertinoGroupSupportingLabel.current
        if (supporting) Text(text, style = MaterialTheme.typography.bodySmall,
            color = LocalCupertinoColors.current.secondaryLabel)
        else Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

val cupertinoLabelRendererTester: RankedTester = withIncreasedRank(100, rankWith(1, uiTypeIs("Label")))
