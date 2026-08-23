package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectArrayControl
import com.chobantonov.jsonfroms.core.testers.isPrimitiveArrayControl
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

val MaterialArrayControlRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayControlRenderer(props)
}

@Composable
fun MaterialArrayControlRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialArrayControlRenderer can only render Control elements")
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
    val columns = arrayColumns(stateProps.schema, stateProps.rootSchema)
    val actionsEnabled = stateProps.enabled && !stateProps.readonly
    val showSortButtons = control.booleanOption("showSortButtons", stateProps.config)
    val actionColumnWidth = if (showSortButtons) SortAndDeleteActionColumnWidth else DeleteActionColumnWidth
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
    val noDataText = translatedText(translator, prefix, "noDataMessage", "No data", values)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (stateProps.label.isNotBlank()) {
                        Text(
                            text = stateProps.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    stateProps.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    stateProps.errors.takeIf { it.isNotBlank() }?.let { errors ->
                        Text(
                            text = errors,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                ActionTooltip(addText.tooltip) {
                    FilledIconButton(
                        enabled = actionsEnabled,
                        onClick = {
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

            HorizontalDivider()

            if (columns.any { it.name.isNotEmpty() }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEach { column ->
                        Text(
                            modifier = Modifier.weight(1f),
                            text = column.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (actionsEnabled) Box(modifier = Modifier.width(actionColumnWidth))
                }
                HorizontalDivider()
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = noDataText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items.forEachIndexed { index, _ ->
                if (index > 0) HorizontalDivider()
                val itemPath = composePaths(stateProps.path, index.toString())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    columns.forEach { column ->
                        val cellPath = if (column.name.isEmpty()) {
                            itemPath
                        } else {
                            composePaths(itemPath, column.name)
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            JsonFormsDispatchRenderer(
                                props = props.copy(
                                    schema = column.schema,
                                    rootSchema = stateProps.rootSchema,
                                    uischema = CellUiSchema,
                                    path = cellPath,
                                    enabled = stateProps.enabled,
                                    id = "${stateProps.id}-$index-${column.name}",
                                ),
                            )
                        }
                    }

                    if (actionsEnabled) {
                        Row(
                            modifier = Modifier.width(actionColumnWidth),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showSortButtons) {
                                ActionTooltip(upText.tooltip) {
                                    OutlinedIconButton(
                                        enabled = index > 0,
                                        onClick = {
                                            dispatchProps.moveUp?.invoke(stateProps.path, index)?.invoke()
                                        },
                                    ) {
                                        Icon(ChevronUpIcon, contentDescription = upText.ariaLabel)
                                    }
                                }
                                ActionTooltip(downText.tooltip) {
                                    OutlinedIconButton(
                                        enabled = index < items.lastIndex,
                                        onClick = {
                                            dispatchProps.moveDown?.invoke(stateProps.path, index)?.invoke()
                                        },
                                    ) {
                                        Icon(ChevronDownIcon, contentDescription = downText.ariaLabel)
                                    }
                                }
                            }
                            ActionTooltip(removeText.tooltip) {
                                FilledIconButton(
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                    onClick = {
                                        dispatchProps.removeItems
                                            ?.invoke(stateProps.path, listOf(index))
                                            ?.invoke()
                                    },
                                ) {
                                    Icon(DeleteIcon, contentDescription = removeText.ariaLabel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ArrayColumn(
    val name: String,
    val title: String,
    val schema: JsonSchema,
)

private fun arrayColumns(
    itemSchema: JsonSchema,
    rootSchema: JsonSchema,
): List<ArrayColumn> {
    val properties = itemSchema["properties"] as? JsonObject
    if (properties.isNullOrEmpty()) {
        return listOf(ArrayColumn(name = "", title = "", schema = itemSchema))
    }

    return properties.mapNotNull { (name, value) ->
        val fallback = (value as? JsonObject)?.let(::JsonSchema) ?: return@mapNotNull null
        val schema = Resolve.schema(
            itemSchema,
            "#/properties/${JsonPointer.encode(name)}",
            rootSchema,
        ) ?: fallback
        ArrayColumn(
            name = name,
            title = (schema["title"] as? JsonPrimitive)?.content ?: startCase(name),
            schema = schema,
        )
    }.ifEmpty {
        listOf(ArrayColumn(name = "", title = "", schema = itemSchema))
    }
}

private fun startCase(value: String): String =
    value
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { character ->
                if (character.isLowerCase()) character.titlecase() else character.toString()
            }
        }

private val CellUiSchema = ControlElement(
    scope = "#",
    label = BooleanControlLabel(false),
)

private val DeleteActionColumnWidth = 52.dp
private val SortAndDeleteActionColumnWidth = 156.dp

private fun ControlElement.booleanOption(name: String, config: Map<String, Any?>): Boolean {
    val option = options[name]
    return when (option) {
        is JsonPrimitive -> option.booleanOrNull ?: option.content.toBooleanStrictOrNull()
        else -> null
    } ?: when (val configured = config[name]) {
        is Boolean -> configured
        is JsonPrimitive -> configured.booleanOrNull ?: configured.content.toBooleanStrictOrNull()
        is String -> configured.toBooleanStrictOrNull()
        else -> null
    } ?: false
}

private val AddIcon: ImageVector by lazy {
    lineIcon("Add") {
        moveTo(12f, 5f)
        verticalLineTo(19f)
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }
}

private val ChevronUpIcon: ImageVector by lazy {
    lineIcon("Move up") {
        moveTo(18f, 15f)
        lineTo(12f, 9f)
        lineTo(6f, 15f)
    }
}

private val ChevronDownIcon: ImageVector by lazy {
    lineIcon("Move down") {
        moveTo(6f, 9f)
        lineTo(12f, 15f)
        lineTo(18f, 9f)
    }
}

private val DeleteIcon: ImageVector by lazy {
    lineIcon("Delete") {
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

val materialArrayControlTester: RankedTester = rankWith(
    3,
    or(isObjectArrayControl, isPrimitiveArrayControl),
)
