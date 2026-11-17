package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
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
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoDropdownChevron
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationAccordionTester
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText

val CupertinoCategorizationAccordionLayout = ComposeJsonFormsRenderer { props ->
    val categorization = props.uischema as? CategorizationElement ?: return@ComposeJsonFormsRenderer
    val layoutProps = mapStateToLayoutProps(
        props.state,
        OwnPropsOfLayout(own = OwnPropsOfRenderer(
            uischema = props.uischema, schema = props.schema, enabled = props.enabled,
            path = props.path, renderers = props.renderers, cells = props.cells,
        )),
    )
    if (!layoutProps.visible) return@ComposeJsonFormsRenderer
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
    val colors = LocalCupertinoColors.current
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
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colors.tertiaryBackground),
            ) {
                CupertinoButton(
                    onClick = { selection.toggle(index) },
                    colors = CupertinoButtonDefaults.plainButtonColors(),
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
                        CupertinoText(label, Modifier.weight(1f))
                        if (status != null) CupertinoText(
                            status.count?.toString() ?: "●", color = colors.destructive,
                        )
                        if (dataStatus != null) CupertinoText(
                            "●",
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = dataStatus
                            },
                            color = colors.accent,
                        )
                        CupertinoDropdownChevron(open, colors.secondaryLabel)
                    }
                }
                if (open) {
                    Column(Modifier.padding(16.dp)) {
                        CupertinoLayoutRenderer(
                            props.copy(enabled = layoutProps.enabled),
                            category.elements, LayoutDirection.Column,
                        )
                    }
                }
            }
        }
    }
}

val cupertinoCategorizationAccordionTester = materialCategorizationAccordionTester
