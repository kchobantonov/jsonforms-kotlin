package com.chobantonov.jsonfroms.material.renderers.additional

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.ActionText
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfArrayControl
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaTypeIs
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.getFirstPrimitiveProp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val MaterialListWithDetailRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialListWithDetailRenderer(props)
}

@Composable
fun MaterialListWithDetailRenderer(props: ComposeJsonFormsRendererProps) {
    val listWithDetail = props.uischema as? ListWithDetailElement
        ?: error("MaterialListWithDetailRenderer can only render ListWithDetail elements")
    val controlProxy = ControlElement(
        scope = listWithDetail.scope,
        i18n = listWithDetail.i18n,
        rule = listWithDetail.rule,
        options = listWithDetail.options,
    )
    val stateProps = mapStateToArrayControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = controlProxy,
            own = OwnPropsOfRenderer(
                uischema = controlProxy,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
                uischemas = props.state.jsonforms.uischemas,
            ),
        ),
    )
    val dispatchProps = mapDispatchToArrayControlProps(Dispatch { action ->
        props.dispatch(action)
        action
    })
    if (!stateProps.visible) return

    val items = (stateProps.data as? JsonArray).orEmpty()
    val actionsEnabled = stateProps.enabled && !stateProps.readonly
    val translator = props.state.jsonforms.i18n.translate
    val prefix = stateProps.i18nKeyPrefix
    val values = mapOf("label" to stateProps.label)
    val addText = translatedActionText(
        translator, prefix, "addTooltip", "addAriaLabel", "Add item", values = values,
    )
    val removeText = translatedActionText(
        translator, prefix, "removeTooltip", "removeAriaLabel", "Delete", "Delete item", values,
    )
    val noDataText = translatedText(translator, prefix, "noDataMessage", "No data", values)
    val noSelectionText = translatedText(translator, prefix, "noSelectionMessage", "No selection", values)
    val childLabelProp = listWithDetail.stringOption("childLabelProp")
        ?: listWithDetail.stringOption("elementLabelProp")
        ?: getFirstPrimitiveProp(stateProps.schema)
    var selectedIndex by remember(stateProps.schema, stateProps.path) {
        mutableStateOf(if (items.isEmpty()) null else 0)
    }
    LaunchedEffect(items.size) {
        selectedIndex = when {
            items.isEmpty() -> null
            selectedIndex == null -> 0
            selectedIndex !in items.indices -> items.lastIndex
            else -> selectedIndex
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    computeLabel(
                        stateProps.label,
                        stateProps.required,
                        stateProps.config["hideRequiredAsterisk"] as? Boolean ?: false,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                stateProps.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                stateProps.errors.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            ActionTooltip(addText.tooltip) {
                FilledIconButton(
                    enabled = actionsEnabled,
                    onClick = {
                        selectedIndex = items.size
                        dispatchProps.addItem(
                            stateProps.path,
                            createDefaultValue(stateProps.schema, stateProps.rootSchema),
                        )()
                    },
                ) { Icon(AddIcon, contentDescription = addText.ariaLabel) }
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val remove: (Int) -> Unit = { index ->
                dispatchProps.removeItems?.invoke(stateProps.path, listOf(index))?.invoke()
                selectedIndex = selectedIndex.afterRemoval(index, items.size - 1)
            }
            if (maxWidth < 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ItemList(
                        Modifier.fillMaxWidth(), items, childLabelProp, selectedIndex, actionsEnabled,
                        noDataText, removeText, onSelect = { selectedIndex = it }, onRemove = remove,
                    )
                    DetailPane(props, stateProps, listWithDetail, selectedIndex, items, noSelectionText)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ItemList(
                        Modifier.width(320.dp), items, childLabelProp, selectedIndex, actionsEnabled,
                        noDataText, removeText, onSelect = { selectedIndex = it }, onRemove = remove,
                    )
                    Box(Modifier.weight(1f)) {
                        DetailPane(props, stateProps, listWithDetail, selectedIndex, items, noSelectionText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemList(
    modifier: Modifier,
    items: List<JsonElement>,
    childLabelProp: String?,
    selectedIndex: Int?,
    actionsEnabled: Boolean,
    noDataText: String,
    removeText: ActionText,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Surface(
        modifier.heightIn(min = 256.dp, max = 384.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(noDataText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(6.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEachIndexed { index, item ->
                    val selected = selectedIndex == index
                    Card(
                        Modifier.fillMaxWidth().clickable { onSelect(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                Modifier.size(42.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text((index + 1).toString(), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Text(
                                computeItemLabel(item, index, childLabelProp),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            ActionTooltip(removeText.tooltip) {
                                FilledIconButton(
                                    modifier = Modifier.size(40.dp),
                                    enabled = actionsEnabled,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                    onClick = { onRemove(index) },
                                ) { Icon(DeleteIcon, contentDescription = removeText.ariaLabel) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPane(
    props: ComposeJsonFormsRendererProps,
    stateProps: StatePropsOfArrayControl,
    listWithDetail: ListWithDetailElement,
    selectedIndex: Int?,
    items: List<JsonElement>,
    noSelectionText: String,
) {
    if (selectedIndex == null || selectedIndex !in items.indices) {
        Text(noSelectionText, style = MaterialTheme.typography.titleMedium)
        return
    }
    val itemUiSchema = remember(stateProps.schema, stateProps.rootSchema, listWithDetail.scope) {
        runCatching {
            findMatchingUISchema(stateProps.uischemas)(
                stateProps.schema,
                listWithDetail.scope,
                stateProps.path,
            ) ?: Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema)
        }.getOrElse { ControlElement(scope = "#") }
    }
    JsonFormsDispatchRenderer(
        props.copy(
            schema = stateProps.schema,
            rootSchema = stateProps.rootSchema,
            uischema = itemUiSchema,
            path = composePaths(stateProps.path, selectedIndex.toString()),
            enabled = stateProps.enabled,
            id = "${stateProps.id}-$selectedIndex",
        ),
    )
}

private fun ListWithDetailElement.stringOption(name: String): String? =
    (options[name] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

internal fun computeItemLabel(item: JsonElement, index: Int, childLabelProp: String?): String {
    if (childLabelProp == null) return (index + 1).toString()

    val propValue = Resolve.data(item, childLabelProp) as? JsonPrimitive
    return propValue?.contentOrNull?.takeIf { it.isNotBlank() }.orEmpty()
}

private fun Int?.afterRemoval(removedIndex: Int, remainingSize: Int): Int? = when {
    this == null || remainingSize <= 0 -> null
    this > removedIndex -> this - 1
    this == removedIndex -> removedIndex.coerceAtMost(remainingSize - 1)
    else -> this
}

private val AddIcon = lineIcon("Add") {
    moveTo(12f, 5f); verticalLineTo(19f); moveTo(5f, 12f); horizontalLineTo(19f)
}
private val DeleteIcon = lineIcon("Delete") {
    moveTo(3f, 6f); horizontalLineTo(21f)
    moveTo(8f, 6f); verticalLineTo(4f); curveTo(8f, 3.45f, 8.45f, 3f, 9f, 3f)
    horizontalLineTo(15f); curveTo(15.55f, 3f, 16f, 3.45f, 16f, 4f); verticalLineTo(6f)
    moveTo(19f, 6f); lineTo(18f, 20f); curveTo(17.95f, 20.55f, 17.5f, 21f, 17f, 21f)
    horizontalLineTo(7f); curveTo(6.45f, 21f, 6.05f, 20.55f, 6f, 20f); lineTo(5f, 6f)
    moveTo(10f, 11f); verticalLineTo(17f); moveTo(14f, 11f); verticalLineTo(17f)
}
private fun lineIcon(
    name: String,
    draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name, 24.dp, 24.dp, 24f, 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = draw,
    )
}.build()

val materialListWithDetailTester: RankedTester = rankWith(
    4,
    and(uiTypeIs("ListWithDetail"), schemaTypeIs("array")),
)
