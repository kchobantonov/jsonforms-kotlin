package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs

val MaterialLabelRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialLabelRenderer(props)
}

@Composable
fun MaterialLabelRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val labelElement = props.uischema as? LabelElement
        ?: error("MaterialLabelRenderer can only render Label elements")
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

    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    val text = deriveLabelForUISchemaElement(labelElement, translator).orEmpty()
    if (text.isBlank()) {
        return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
    )
}

val materialLabelRendererTester: RankedTester = rankWith(
    1,
    uiTypeIs("Label"),
)
