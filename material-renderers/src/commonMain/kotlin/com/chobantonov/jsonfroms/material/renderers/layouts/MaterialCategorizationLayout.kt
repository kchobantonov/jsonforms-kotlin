package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.categoryDataPresenceStatus
import com.chobantonov.jsonfroms.compose.categoryValidationStatus
import com.chobantonov.jsonfroms.compose.rememberCategoryBoundScopes
import com.chobantonov.jsonfroms.compose.rememberCategorySelectionState
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.isVerticalCategorization
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
    val categories = visibleCategories(categorization, data, validator, config, props.path)
    val selection = rememberCategorySelectionState(categorization, categories, props.config, props.path)
    if (selection.selectedIndex < 0) return
    val safeCategory = selection.selectedIndex
    val tabLabels = categories.map { category ->
        deriveLabelForUISchemaElement(category, translator) ?: category.label
    }

    val vertical = isVerticalCategorization(categorization)
    val tabs: @Composable () -> Unit = {
        categories.forEachIndexed { index, category ->
            val boundScopes = rememberCategoryBoundScopes(category)
            val status = categoryValidationStatus(category, props.config, props.path, boundScopes)
            val dataStatus = categoryDataPresenceStatus(category, props.config, props.path, data, boundScopes)
            val baseModifier = if (vertical) Modifier.fillMaxWidth() else Modifier
            Tab(
                selected = index == safeCategory,
                onClick = { selection.select(index) },
                modifier = if (status == null && dataStatus == null) baseModifier else baseModifier.semantics {
                    contentDescription = listOfNotNull(tabLabels[index], status?.accessibleName, dataStatus)
                        .joinToString(", ")
                },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tabLabels[index])
                        if (status != null) {
                            if (status.count == null) Badge()
                            else Badge { Text(status.count.toString()) }
                        }
                        if (dataStatus != null) Badge(
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = dataStatus
                            },
                        )
                    }
                },
            )
        }
    }
    val panel: @Composable () -> Unit = {
        MaterialLayoutRenderer(
            props = props.copy(enabled = layoutProps.enabled),
            elements = categories[safeCategory].elements,
            direction = LayoutDirection.Column,
        )
    }
    if (vertical) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val navigationWidth = (maxWidth * 0.32f).coerceIn(112.dp, 220.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(navigationWidth)) { tabs() }
                Box(Modifier.weight(1f)) { panel() }
            }
        }
    } else {
        Column {
            PrimaryTabRow(selectedTabIndex = safeCategory) { tabs() }
            Box(Modifier.padding(top = 8.dp)) { panel() }
        }
    }
}

internal fun visibleCategories(
    categorization: CategorizationElement,
    data: JsonData,
    validator: JsonSchemaValidator,
    config: Map<String, Any?> = emptyMap(),
    path: String? = null,
): List<CategoryElement> = com.chobantonov.jsonfroms.core.mappers.visibleCategories(
    categorization, data, validator, config, path,
)

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
