package com.chobantonov.jsonfroms.cupertino.renderers.layouts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToLayoutProps
import com.chobantonov.jsonfroms.core.mappers.visibleCategories
import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.layouts.materialCategorizationTester
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import kotlinx.serialization.json.JsonPrimitive

val CupertinoCategorizationLayout = ComposeJsonFormsRenderer {
    CupertinoCategorizationContent(it, stepper = false)
}
val cupertinoCategorizationTester = materialCategorizationTester

@Composable
internal fun CupertinoCategorizationContent(
    props: ComposeJsonFormsRendererProps,
    stepper: Boolean,
) {
    val categorization = props.uischema as? CategorizationElement
        ?: error("CupertinoCategorizationLayout can only render Categorization elements")
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
    val data = getData(props.state)
    val categories = visibleCategories(
        categorization, data, getValidator(props.state), layoutProps.config, props.path,
    )
    val selection = rememberCategorySelectionState(categorization, categories, props.config, props.path)
    if (selection.selectedIndex < 0) return
    val translator = props.state.jsonforms.i18n.translate ?: defaultTranslator
    val selected = selection.selectedIndex
    val colors = LocalCupertinoColors.current

    val vertical = isVerticalCategorization(categorization)
    val navigation: @Composable () -> Unit = {
        categories.forEachIndexed { index, category ->
            val label = deriveLabelForUISchemaElement(category, translator) ?: category.label
            val boundScopes = rememberCategoryBoundScopes(category)
            val status = categoryValidationStatus(category, props.config, props.path, boundScopes)
            val dataStatus = categoryDataPresenceStatus(category, props.config, props.path, data, boundScopes)
            val baseModifier = if (vertical) Modifier.fillMaxWidth() else Modifier
            CupertinoButton(
                onClick = { selection.select(index) },
                colors = if (index == selected) CupertinoButtonDefaults.filledButtonColors()
                else CupertinoButtonDefaults.plainButtonColors(),
                modifier = if (status == null && dataStatus == null) baseModifier else baseModifier.semantics {
                    contentDescription = listOfNotNull(label, status?.accessibleName, dataStatus)
                        .joinToString(", ")
                },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CupertinoText(label)
                    if (status != null) CupertinoText(
                        status.count?.toString() ?: "●",
                        color = colors.destructive,
                    )
                    if (dataStatus != null) CupertinoText(
                        "●",
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = dataStatus
                        },
                        color = colors.accent,
                    )
                }
            }
        }
    }
    val panelAndActions: @Composable () -> Unit = {
        CupertinoLayoutRenderer(
            props.copy(enabled = layoutProps.enabled),
            categories[selected].elements,
            LayoutDirection.Column,
        )
        val showNavButtons = stepper && when (val value = categorization.options["showNavButtons"]) {
            is JsonPrimitive -> if (value.isString) false else value.content.toBooleanStrictOrNull() ?: false
            else -> layoutProps.config["showNavButtons"] as? Boolean ?: false
        }
        if (showNavButtons) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CupertinoButton(
                    onClick = { selection.select((selected - 1).coerceAtLeast(0)) },
                    enabled = selected > 0,
                    colors = CupertinoButtonDefaults.plainButtonColors(),
                ) { CupertinoText(translator.translate("Previous", "Previous", emptyMap()) ?: "Previous") }
                CupertinoButton(
                    onClick = { selection.select((selected + 1).coerceAtMost(categories.lastIndex)) },
                    enabled = selected < categories.lastIndex,
                    colors = CupertinoButtonDefaults.filledButtonColors(),
                ) { CupertinoText(translator.translate("Next", "Next", emptyMap()) ?: "Next") }
            }
        }
    }
    if (vertical) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val navigationWidth = (maxWidth * 0.32f).coerceIn(112.dp, 220.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    Modifier.width(navigationWidth),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { navigation() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    panelAndActions()
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { navigation() }
            panelAndActions()
        }
    }
}
