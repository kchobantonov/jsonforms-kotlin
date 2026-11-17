package com.chobantonov.jsonfroms.material.renderers.layouts

import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.categoryDataPresenceStatus
import com.chobantonov.jsonfroms.compose.categoryValidationStatus
import com.chobantonov.jsonfroms.compose.rememberCategoryBoundScopes
import com.chobantonov.jsonfroms.compose.rememberAccordionSelectionState
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfLayout
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.mappers.resolveAccordionCollapsed
import com.chobantonov.jsonfroms.core.mappers.visibleCategories
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs

val MaterialCategorizationAccordionLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer {
    MaterialCategorizationAccordionLayout(it)
}

@Composable
fun MaterialCategorizationAccordionLayout(props: ComposeJsonFormsRendererProps) {
    val categorization = props.uischema as? CategorizationElement ?: return
    val layoutProps = mapStateToLayoutProps(
        props.state,
        OwnPropsOfLayout(own = OwnPropsOfRenderer(
            uischema = props.uischema, schema = props.schema, enabled = props.enabled,
            path = props.path, renderers = props.renderers, cells = props.cells,
        )),
    )
    if (!layoutProps.visible) return
    val data = getData(props.state)
    val categories = visibleCategories(
        categorization, data, getValidator(props.state), layoutProps.config, props.path,
    )
    val selection = rememberAccordionSelectionState(
        categorization, categories,
        resolveAccordionCollapsed(categorization.options, props.config),
        props.config, props.path,
    )
    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEachIndexed { index, category ->
            val label = deriveLabelForUISchemaElement(category, translator) ?: category.label
            val boundScopes = rememberCategoryBoundScopes(category)
            val status = categoryValidationStatus(category, props.config, props.path, boundScopes)
            val dataStatus = categoryDataPresenceStatus(category, props.config, props.path, data, boundScopes)
            val open = selection.openIndex == index
            val stateText = translateGroupRendererText(props.state.jsonforms.i18n,
                if (open) "group.expanded" else "group.collapsed",
                if (open) "Expanded" else "Collapsed")
            Card(Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { selection.toggle(index) },
                    modifier = Modifier.fillMaxWidth().semantics {
                        stateDescription = stateText
                        if (status != null || dataStatus != null) {
                            contentDescription = listOfNotNull(label, status?.accessibleName, dataStatus)
                                .joinToString(", ")
                        }
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, Modifier.weight(1f))
                        if (status != null) {
                            if (status.count == null) Badge()
                            else Badge { Text(status.count.toString()) }
                        }
                        if (dataStatus != null) Badge(
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = dataStatus
                            },
                        )
                        Icon(
                            JsonFormsActionIcons.ChevronRight, contentDescription = null,
                            modifier = Modifier.rotate(if (open) 90f else 0f),
                        )
                    }
                }
                if (open) {
                    Column(Modifier.padding(16.dp)) {
                        MaterialLayoutRenderer(
                            props.copy(enabled = layoutProps.enabled),
                            category.elements,
                            LayoutDirection.Column,
                        )
                    }
                }
            }
        }
    }
}

val materialCategorizationAccordionTester: RankedTester = rankWith(
    3,
    and(
        uiTypeIs("Categorization"),
        { uiSchema, _, _ ->
            (uiSchema as? CategorizationElement)?.elements?.all { it is CategoryElement } == true
        },
        optionIs("variant", "accordion"),
    ),
)
