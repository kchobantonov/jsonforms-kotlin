package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isMixedArrayControl
import com.chobantonov.jsonfroms.core.testers.isObjectArrayWithNesting
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.getControlPath
import com.chobantonov.jsonfroms.core.util.getDetailUiSchema
import com.chobantonov.jsonfroms.core.util.getFirstPrimitiveProp
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val MaterialArrayLayoutRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayLayoutRenderer(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialArrayLayoutRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialArrayLayoutRenderer can only render Control elements")
    val stateProps = mapStateToArrayControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
                uischemas = props.state.jsonforms.uischemas,
            ),
        ),
    )
    val dispatchProps = mapDispatchToArrayControlProps(
        dispatch = Dispatch { action ->
            props.dispatch(action)
            action
        },
    )
    if (!stateProps.visible) return

    val items = (stateProps.data as? JsonArray).orEmpty()
    val actionsEnabled = stateProps.enabled && !stateProps.readonly
    val initCollapsed = control.booleanOption("initCollapsed", stateProps.config)
    val collapseNewItems = control.booleanOption("collapseNewItems", stateProps.config)
    val showSortButtons = control.booleanOption("showSortButtons", stateProps.config)
    val restrict = control.booleanOption("restrict", stateProps.config)
    val hideAvatar = control.booleanOption("hideAvatar", stateProps.config)
    val hideSummaryValidation = control.booleanOption("hideArraySummaryValidation", stateProps.config)
    val canAdd = actionsEnabled && (!restrict || stateProps.arraySchema.maxItems?.let { items.size < it } != false)
    val canRemove = actionsEnabled && (!restrict || stateProps.arraySchema.minItems?.let { items.size > it } != false)
    var expandedIndex by remember(stateProps.schema, stateProps.path, initCollapsed) {
        mutableStateOf(if (initCollapsed || items.isEmpty()) null else 0)
    }
    val detailUiSchema = getDetailUiSchema(control)
        ?: findMatchingUISchema(stateProps.uischemas)(stateProps.schema, control.scope, stateProps.path)
        ?: Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema)
    val childLabelProp = (control.options["childLabelProp"] as? JsonPrimitive)
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: getFirstPrimitiveProp(stateProps.schema)
    val translator = props.state.jsonforms.i18n.translate
    val prefix = stateProps.i18nKeyPrefix
    val values = mapOf("label" to stateProps.label)
    val addText = translatedActionText(
        translator, prefix, "addTooltip", "addAriaLabel", "Add item", values = values,
    )
    val upText = translatedActionText(
        translator, prefix, "up", "upAriaLabel", "Move up", values = values,
    )
    val downText = translatedActionText(
        translator, prefix, "down", "downAriaLabel", "Move down", values = values,
    )
    val removeText = translatedActionText(
        translator, prefix, "removeTooltip", "removeAriaLabel", "Delete", "Delete item", values,
    )
    val expandText = translatedActionText(
        translator, prefix, "expandTooltip", "expandAriaLabel", "Expand item", values = values,
    )
    val collapseText = translatedActionText(
        translator, prefix, "collapseTooltip", "collapseAriaLabel", "Collapse item", values = values,
    )
    val noDataText = translatedText(translator, prefix, "noDataMessage", "No data", values)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = computeLabel(
                                stateProps.label,
                                stateProps.required,
                                stateProps.config["hideRequiredAsterisk"] as? Boolean ?: false,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (stateProps.childErrors.isNotEmpty() && !hideSummaryValidation) {
                            ErrorIndicator(stateProps.childErrors)
                        }
                    }
                    stateProps.description?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    stateProps.errors.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                ActionTooltip(addText.tooltip) {
                    FilledIconButton(
                        enabled = canAdd,
                        onClick = {
                            if (!collapseNewItems) expandedIndex = items.size
                            dispatchProps.addItem(
                                stateProps.path,
                                createDefaultValue(stateProps.schema, stateProps.rootSchema),
                            )()
                        },
                    ) {
                        Icon(AddIcon, contentDescription = addText.ariaLabel)
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(noDataText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            items.forEachIndexed { index, item ->
                val itemPath = composePaths(stateProps.path, index.toString())
                val isExpanded = expandedIndex == index
                val itemErrors = errorsForArrayItem(stateProps.childErrors, itemPath)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedIndex = if (isExpanded) null else index }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!hideAvatar) ItemNumberAvatar(index + 1, itemErrors)
                            Text(
                                modifier = Modifier.weight(1f),
                                text = computeChildLabel(item, index, childLabelProp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (showSortButtons) {
                                ActionTooltip(upText.tooltip) {
                                    OutlinedIconButton(
                                        enabled = actionsEnabled && index > 0,
                                        onClick = {
                                            dispatchProps.moveUp?.invoke(stateProps.path, index)?.invoke()
                                            expandedIndex = expandedIndex.swap(index, index - 1)
                                        },
                                    ) {
                                        Icon(ChevronUpIcon, contentDescription = upText.ariaLabel)
                                    }
                                }
                                ActionTooltip(downText.tooltip) {
                                    OutlinedIconButton(
                                        enabled = actionsEnabled && index < items.lastIndex,
                                        onClick = {
                                            dispatchProps.moveDown?.invoke(stateProps.path, index)?.invoke()
                                            expandedIndex = expandedIndex.swap(index, index + 1)
                                        },
                                    ) {
                                        Icon(ChevronDownIcon, contentDescription = downText.ariaLabel)
                                    }
                                }
                            }
                            ActionTooltip(removeText.tooltip) {
                                FilledIconButton(
                                    enabled = canRemove,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                    onClick = {
                                        dispatchProps.removeItems?.invoke(stateProps.path, listOf(index))?.invoke()
                                        expandedIndex = expandedIndex.afterRemoval(index, items.size - 1)
                                    },
                                ) {
                                    Icon(DeleteIcon, contentDescription = removeText.ariaLabel)
                                }
                            }
                            val expansionText = if (isExpanded) collapseText else expandText
                            ActionTooltip(expansionText.tooltip) {
                                Icon(
                                    imageVector = if (isExpanded) CollapseIcon else ExpandIcon,
                                    contentDescription = expansionText.ariaLabel,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (isExpanded) {
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            ) {
                                JsonFormsDispatchRenderer(
                                    props = props.copy(
                                        schema = stateProps.schema,
                                        rootSchema = stateProps.rootSchema,
                                        uischema = detailUiSchema,
                                        path = itemPath,
                                        enabled = stateProps.enabled,
                                        id = "${stateProps.id}-layout-$index",
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemNumberAvatar(number: Int, errors: List<ErrorObject>) {
    Box {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), style = MaterialTheme.typography.titleSmall)
            }
        }
        if (errors.isNotEmpty()) {
            TooltipBox(
                modifier = Modifier.align(Alignment.TopEnd),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(errors.joinToString("\n") { it.message }) } },
                state = rememberTooltipState(),
            ) {
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            errors.size.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorIndicator(errors: List<ErrorObject>) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(errors.joinToString("\n") { it.message }) } },
        state = rememberTooltipState(),
    ) {
        Surface(
            modifier = Modifier.size(22.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = errors.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

internal fun computeChildLabel(item: JsonElement, index: Int, childLabelProp: String?): String {
    val propValue = childLabelProp?.let { Resolve.data(item, it) } as? JsonPrimitive
    return propValue?.contentOrNull?.takeIf { it.isNotBlank() } ?: "Item ${index + 1}"
}

internal fun errorsForArrayItem(errors: List<ErrorObject>, itemPath: String): List<ErrorObject> =
    errors.filter { error ->
        val errorPath = getControlPath(error)
        errorPath == itemPath || errorPath.startsWith("$itemPath.")
    }

private fun ControlElement.booleanOption(name: String, config: Map<String, Any?>): Boolean {
    val local = (options[name] as? JsonPrimitive)?.booleanOrNull
    if (local != null) return local
    return when (val global = config[name]) {
        is Boolean -> global
        is JsonPrimitive -> global.booleanOrNull == true
        else -> false
    }
}

private fun Int?.swap(first: Int, second: Int): Int? = when (this) {
    first -> second
    second -> first
    else -> this
}

private fun Int?.afterRemoval(removedIndex: Int, remainingSize: Int): Int? = when {
    this == null -> null
    remainingSize <= 0 -> null
    this > removedIndex -> this - 1
    this == removedIndex -> removedIndex.coerceAtMost(remainingSize - 1)
    else -> this
}

private val AddIcon = lineIcon("Add") {
    moveTo(12f, 5f)
    verticalLineTo(19f)
    moveTo(5f, 12f)
    horizontalLineTo(19f)
}
private val ChevronUpIcon = lineIcon("Move up") {
    moveTo(18f, 15f)
    lineTo(12f, 9f)
    lineTo(6f, 15f)
}
private val ChevronDownIcon = lineIcon("Move down") {
    moveTo(6f, 9f)
    lineTo(12f, 15f)
    lineTo(18f, 9f)
}
private val CollapseIcon = lineIcon("Collapse") {
    moveTo(18f, 15f)
    lineTo(12f, 9f)
    lineTo(6f, 15f)
}
private val ExpandIcon = lineIcon("Expand") {
    moveTo(6f, 9f)
    lineTo(12f, 15f)
    lineTo(18f, 9f)
}
private val DeleteIcon = lineIcon("Delete") {
    moveTo(3f, 6f)
    horizontalLineTo(21f)
    moveTo(8f, 6f)
    verticalLineTo(4f)
    curveTo(8f, 3.45f, 8.45f, 3f, 9f, 3f)
    horizontalLineTo(15f)
    curveTo(15.55f, 3f, 16f, 3.45f, 16f, 4f)
    verticalLineTo(6f)
    moveTo(19f, 6f)
    lineTo(18f, 20f)
    curveTo(17.95f, 20.55f, 17.5f, 21f, 17f, 21f)
    horizontalLineTo(7f)
    curveTo(6.45f, 21f, 6.05f, 20.55f, 6f, 20f)
    lineTo(5f, 6f)
    moveTo(10f, 11f)
    verticalLineTo(17f)
    moveTo(14f, 11f)
    verticalLineTo(17f)
}

private fun lineIcon(
    name: String,
    draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
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

val materialArrayLayoutTester: RankedTester = rankWith(
    4,
    { uiSchema, schema, context ->
        isObjectArrayWithNesting(uiSchema, schema, context) ||
            isMixedArrayControl(uiSchema, schema, context)
    },
)
