package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
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
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.categorizationHasCategory
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.store.getData
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.util.Runtime
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
    val categories = categorization.elements
        .filterIsInstance<CategoryElement>()
        .filter { category ->
            Runtime.isVisible(category, data, validator, config = config)
        }

    if (categories.isEmpty()) {
        return
    }

    var activeCategory by remember(categorization) { mutableIntStateOf(0) }
    val safeCategory = activeCategory.coerceIn(0, categories.lastIndex)
    val showNavButtons = resolveShowNavButtons(categorization, config)
    val previousLabel = translatedText(translator, null, "Previous", "Previous")
    val nextLabel = translatedText(translator, null, "Next", "Next")
    val labels = categories.map { category ->
        deriveLabelForUISchemaElement(category, translator) ?: category.label
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEachIndexed { index, _ ->
            FilterChip(
                selected = index == safeCategory,
                onClick = { activeCategory = index },
                label = { Text(labels[index]) },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        MaterialLayoutRenderer(
            props = props.copy(enabled = layoutProps.enabled),
            elements = categories[safeCategory].elements,
            direction = LayoutDirection.Column,
        )
    }

    if (showNavButtons) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { activeCategory = (safeCategory - 1).coerceAtLeast(0) },
                enabled = safeCategory > 0,
            ) {
                Text(previousLabel)
            }
            Button(
                onClick = { activeCategory = (safeCategory + 1).coerceAtMost(categories.lastIndex) },
                enabled = safeCategory < categories.lastIndex,
            ) {
                Text(nextLabel)
            }
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
