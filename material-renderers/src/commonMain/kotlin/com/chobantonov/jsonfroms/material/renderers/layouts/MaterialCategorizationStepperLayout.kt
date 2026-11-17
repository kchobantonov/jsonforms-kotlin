package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.categorizationHasCategory
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.material.renderers.translatedText
import kotlinx.serialization.json.JsonPrimitive

val MaterialCategorizationStepperLayout: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialCategorizationStepperLayout(props)
}

@Composable
fun MaterialCategorizationStepperLayout(
    props: ComposeJsonFormsRendererProps,
) {
    val categorization = props.uischema as? CategorizationElement
        ?: error("MaterialCategorizationStepperLayout can only render Categorization elements")
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
    val showNavButtons = resolveShowNavButtons(categorization, config)
    val previousLabel = translatedText(translator, null, "Previous", "Previous")
    val nextLabel = translatedText(translator, null, "Next", "Next")
    val labels = categories.map { category ->
        deriveLabelForUISchemaElement(category, translator) ?: category.label
    }

    val vertical = isVerticalCategorization(categorization)
    val steps: @Composable () -> Unit = {
        categories.forEachIndexed { index, category ->
            val boundScopes = rememberCategoryBoundScopes(category)
            val status = categoryValidationStatus(category, props.config, props.path, boundScopes)
            val dataStatus = categoryDataPresenceStatus(category, props.config, props.path, data, boundScopes)
            val baseModifier = if (vertical) Modifier.fillMaxWidth() else Modifier
            FilterChip(
                selected = index == safeCategory,
                onClick = { selection.select(index) },
                modifier = if (status == null && dataStatus == null) baseModifier else baseModifier.semantics {
                    contentDescription = listOfNotNull(labels[index], status?.accessibleName, dataStatus)
                        .joinToString(", ")
                },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(labels[index])
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
    val panelAndActions: @Composable () -> Unit = {
        MaterialLayoutRenderer(
            props = props.copy(enabled = layoutProps.enabled),
            elements = categories[safeCategory].elements,
            direction = LayoutDirection.Column,
        )
        if (showNavButtons) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { selection.select((safeCategory - 1).coerceAtLeast(0)) },
                    enabled = safeCategory > 0,
                ) { Text(previousLabel) }
                Button(
                    onClick = { selection.select((safeCategory + 1).coerceAtMost(categories.lastIndex)) },
                    enabled = safeCategory < categories.lastIndex,
                ) { Text(nextLabel) }
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
                ) { steps() }
                Column(Modifier.weight(1f)) { panelAndActions() }
            }
        }
    } else {
        Column {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { steps() }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) { panelAndActions() }
        }
    }
}

private fun resolveShowNavButtons(
    categorization: CategorizationElement,
    config: Map<String, Any?>,
): Boolean =
    when (val value = categorization.options["showNavButtons"]) {
        is JsonPrimitive -> value.toBooleanOrNullCompat() ?: config["showNavButtons"] as? Boolean ?: false
        else -> config["showNavButtons"] as? Boolean ?: false
    }

private fun JsonPrimitive.toBooleanOrNullCompat(): Boolean? = when (content.lowercase()) {
    "true" -> true
    "false" -> false
    else -> null
}

private val isStepperCategorization = and(
    uiTypeIs("Categorization"),
    { uiSchema, _, _ -> categorizationHasCategory(uiSchema) },
    optionIs("variant", "stepper"),
)

val materialCategorizationStepperTester: RankedTester = rankWith(
    2,
    isStepperCategorization,
)
