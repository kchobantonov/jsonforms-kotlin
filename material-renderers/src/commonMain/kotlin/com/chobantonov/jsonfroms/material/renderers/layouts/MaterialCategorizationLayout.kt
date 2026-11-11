package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Runtime

val MaterialCategorizationLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialCategorizationLayout(props)
}

@Composable
fun MaterialCategorizationLayout(
    props: ComposeJsonFormsRendererProps,
) {
    val categorization = props.uischema as? CategorizationElement
        ?: error("MaterialCategorizationLayout can only render Categorization elements")
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

    val data = getData(props.state)
    val validator = getValidator(props.state)
    val config = layoutProps.config
    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    val categories = visibleCategories(categorization, data, validator, config)
    if (categories.isEmpty()) {
        return
    }

    var activeCategory by remember(categorization) { mutableIntStateOf(0) }
    val safeCategory = activeCategory.coerceIn(0, categories.lastIndex)
    val tabLabels = categories.map { category ->
        deriveLabelForUISchemaElement(category, translator) ?: category.label
    }

    PrimaryTabRow(selectedTabIndex = safeCategory) {
        categories.forEachIndexed { index, _ ->
            Tab(
                selected = index == safeCategory,
                onClick = { activeCategory = index },
                text = { Text(tabLabels[index]) },
            )
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.padding(top = 8.dp),
    ) {
        MaterialLayoutRenderer(
            props = props.copy(enabled = layoutProps.enabled),
            elements = categories[safeCategory].elements,
            direction = LayoutDirection.Column,
        )
    }
}

internal fun visibleCategories(
    categorization: CategorizationElement,
    data: JsonData,
    validator: JsonSchemaValidator,
    config: Map<String, Any?> = emptyMap(),
): List<CategoryElement> = categorization.elements
    .filterIsInstance<CategoryElement>()
    .filter { category -> Runtime.isVisible(category, data, validator, config = config) }

private val isSingleLevelCategorization = and(
    uiTypeIs("Categorization"),
    { uiSchema, _, _ ->
        val categorization = uiSchema as? CategorizationElement
        categorization != null && categorization.elements.all { it is CategoryElement }
    },
)

val materialCategorizationTester: RankedTester = rankWith(
    1,
    isSingleLevelCategorization,
)
