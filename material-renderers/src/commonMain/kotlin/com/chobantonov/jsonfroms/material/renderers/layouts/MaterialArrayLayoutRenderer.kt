package com.chobantonov.jsonfroms.material.renderers.layouts

import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText

import com.chobantonov.jsonfroms.core.i18n.translateArrayValidationIndicator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.JsonFormsItemScope
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.material.renderers.complex.arrayRendererLocaleBundles
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getI18nKey
import com.chobantonov.jsonfroms.core.i18n.translateErrorForPath
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrSchemas
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.groupArrayErrors
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.resolveContainerValidationOptions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isMixedArrayControl
import com.chobantonov.jsonfroms.core.testers.isObjectArrayControl
import com.chobantonov.jsonfroms.core.testers.isPrimitiveArrayControl
import com.chobantonov.jsonfroms.core.testers.isObjectArrayWithNesting
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.hasFlatAlternativeObjectProperties
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.appendArrayItemIfAllowed
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.core.util.resolveDetailUiSchema
import com.chobantonov.jsonfroms.core.util.getFirstPrimitiveProp
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val MaterialArrayLayoutRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayLayoutRenderer(props)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialArrayLayoutRenderer(
    props: ComposeJsonFormsRendererProps,
    confirmationDialog: @Composable (String, String, String, String, () -> Unit, () -> Unit) -> Unit =
        { title, message, accept, decline, onConfirm, onDismiss ->
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = onConfirm) { Text(accept) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(decline) } },
            )
        },
) {
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

    val storedValue = Resolve.data(props.state.jsonforms.core.data, stateProps.path)
    val items = (storedValue as? JsonArray).orEmpty()
    val actionsEnabled = stateProps.enabled && !stateProps.readonly
    val initCollapsed = control.booleanOption("initCollapsed", stateProps.config)
    val collapseNewItems = control.booleanOption("collapseNewItems", stateProps.config)
    val showSortButtons = control.booleanOption("showSortButtons", stateProps.config)
    val restrict = resolveRestrict(control.options["restrict"], stateProps.config)
    val hideAvatar = control.booleanOption("hideAvatar", stateProps.config)
    val hideSummaryValidation = control.booleanOption("hideArraySummaryValidation", stateProps.config)
    val validationOptions = resolveContainerValidationOptions(
        control.options, stateProps.config, defaultIndicator = true,
    )
    val arrayErrors = remember(stateProps.childErrors, stateProps.path) {
        groupArrayErrors(stateProps.childErrors, stateProps.path)
    }
    val summaryErrors = arrayErrors.descendants
    val actionPolicy = resolveArrayActionPolicy(control.options, stateProps.config)
    val canAdd = actionsEnabled && !actionPolicy.disableAdd &&
        (!restrict || stateProps.arraySchema.maxItems?.let { items.size < it } != false)
    val canRemove = actionsEnabled && !actionPolicy.disableRemove &&
        (!restrict || stateProps.arraySchema.minItems?.let { items.size > it } != false)
    val removalGuard = remember(stateProps.arraySchema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, stateProps.path, items) {
        ContainsRemovalGuard(stateProps.arraySchema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, stateProps.path, JsonArray(items))
    }
    fun canRemoveAt(index: Int) = !restrict || removalGuard.canRemove(index)
    var expandedIndex by remember(stateProps.schema, stateProps.path, initCollapsed) {
        mutableStateOf(if (initCollapsed || items.isEmpty()) null else 0)
    }
    var pendingRemoval by remember(stateProps.path) { mutableStateOf<PendingArrayRemoval?>(null) }
    var appliedRemoval by remember(stateProps.path) { mutableStateOf<AppliedArrayRemoval?>(null) }
    LaunchedEffect(items, canRemove, props.state.jsonforms.core.data) {
        if (pendingRemoval?.let { !canRemove || it.snapshot != items || !canRemoveAt(it.index) } == true)
            pendingRemoval = null
        appliedRemoval?.let { applied ->
            if (items != applied.snapshot) {
                if (items == applied.result) expandedIndex = expandedIndex.afterRemoval(applied.index, items.size)
                appliedRemoval = null
            }
        }
    }
    fun remove(index: Int, snapshot: JsonArray) {
        if (!canRemove || items != snapshot || index !in snapshot.indices || !canRemoveAt(index)) return
        val result = JsonArray(snapshot.filterIndexed { position, _ -> position != index })
        appliedRemoval = AppliedArrayRemoval(index, snapshot, result)
        props.dispatch(UpdateAction(path = stateProps.path, updater = { current ->
            if (current == snapshot) result else current
        }))
    }
    val childLabelProp = resolveChildLabelProp(control, stateProps.schema)
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
                        if (summaryErrors.isNotEmpty() &&
                            validationOptions.showIndicator && !hideSummaryValidation
                        ) {
                            val count = summaryErrors.size.takeIf { validationOptions.showCount }
                            ErrorIndicator(
                                summaryErrors,
                                count,
                                translateArrayValidationIndicator(props.state.jsonforms.i18n, count),
                            )
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
                            modifier = Modifier.semantics { error(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    storedValue?.takeIf { it !is JsonArray }?.let { imported ->
                        val label = translateRendererText(translator, props.state.jsonforms.i18n.locale,
                            "array.invalidCurrentValue", "Invalid current value", arrayRendererLocaleBundles)
                        Text("$label: $imported", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                ActionTooltip(addText.tooltip) {
                    FilledIconButton(
                        enabled = canAdd,
                        onClick = {
                            if (!canAdd) return@FilledIconButton
                            if (!collapseNewItems) expandedIndex = items.size
                            val item = createDefaultValue(stateProps.schema, stateProps.rootSchema)
                            props.dispatch(UpdateAction(stateProps.path, updater = { current ->
                                if (!canAdd || !stateProps.enabled || stateProps.readonly) current else
                                    appendArrayItemIfAllowed(current, storedValue, item,
                                        stateProps.arraySchema.maxItems, restrict)
                            }))
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
                val itemErrors = arrayErrors.byIndex[index].orEmpty()
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
                            if (!hideAvatar) ItemNumberAvatar(index + 1)
                            Text(
                                modifier = Modifier.weight(1f),
                                text = computeChildLabel(item, index, childLabelProp),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (itemErrors.isNotEmpty() && validationOptions.showIndicator) {
                                val count = itemErrors.size.takeIf { validationOptions.showCount }
                                ErrorIndicator(
                                    itemErrors,
                                    count,
                                    translateArrayValidationIndicator(props.state.jsonforms.i18n,
                                        count, index + 1),
                                )
                            }
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
                                    enabled = canRemove && canRemoveAt(index),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                    onClick = {
                                        if (!canRemove || index !in items.indices || !canRemoveAt(index)) return@FilledIconButton
                                        val snapshot = JsonArray(items)
                                        val decision = resolveConfirmationDecision(
                                            ConfirmationOperation.Delete, "arrayLayout", listOf(items[index]),
                                            control.options, stateProps.config,
                                        )
                                        if (decision.required) pendingRemoval = PendingArrayRemoval(index, snapshot)
                                        else remove(index, snapshot)
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
                            val detailUiSchema = resolveDetailUiSchema(control,
                                registered = { findMatchingUISchema(stateProps.uischemas)(
                                    stateProps.schema, control.scope, itemPath,
                                ) },
                                generated = { Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema) },
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            ) {
                                JsonFormsItemScope(item) {
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
    val request = pendingRemoval?.takeIf {
        canRemove && it.snapshot == items && it.index in items.indices && canRemoveAt(it.index)
    }
    if (request != null) {
        fun translated(key: String, fallback: String) =
            translateConfirmationText(props.state.jsonforms.i18n, key, fallback)
        confirmationDialog(
            translated("confirm.delete.title", "Delete this value?"),
            translated("confirm.delete.message", "The value will be removed. This cannot be undone from here."),
            translated("confirm.accept", "Yes"),
            translated("confirm.decline", "No"),
            {
                if (pendingRemoval === request) remove(request.index, request.snapshot)
                if (pendingRemoval === request) pendingRemoval = null
            },
            { if (pendingRemoval === request) pendingRemoval = null },
        )
    }
}

private data class PendingArrayRemoval(val index: Int, val snapshot: JsonArray)
private data class AppliedArrayRemoval(val index: Int, val snapshot: JsonArray, val result: JsonArray)

@Composable
private fun ItemNumberAvatar(number: Int) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ErrorIndicator(
    errors: List<ErrorObject>,
    count: Int?,
    label: String,
    includeMessageInName: Boolean = false,
) {
    val form = useJsonForms().state.jsonforms
    val i18n = form.i18n
    val rootSchema = form.core.schema
    var open by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val translatedErrors = errors.map { error ->
        translateErrorForPath(error, i18n.translateError ?: defaultErrorTranslator,
            i18n.translate ?: defaultTranslator, summaryErrorSchema(error, rootSchema),
            path = summaryErrorSegments(error).joinToString("."))
    }
    val message = translatedErrors.joinToString("\n")
    if (open) {
        val visible = if (expanded) errors.indices else errors.indices.take(3)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(label) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    visible.forEach { index ->
                        val error = errors[index]
                        val parts = error.instancePath.split('/').filter(String::isNotEmpty)
                            .map(JsonPointer::decode)
                        val row = parts.firstNotNullOfOrNull { it.toIntOrNull() }
                        val field = summaryFieldLabel(error, rootSchema, i18n.translate ?: defaultTranslator)
                        val rowLabel = row?.let {
                            validationSummaryText(i18n, "validation.row", "Row ${it + 1}",
                                "Ред ${it + 1}", mapOf("index" to it + 1))
                        }
                        val context = listOfNotNull(rowLabel, field).joinToString(" · ")
                            .ifEmpty { error.instancePath.ifEmpty { label } }
                        Text("$context: ${translatedErrors[index]}", Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    }
                    if (errors.size > 3) {
                        val key = if (expanded) "validation.showLess" else "validation.showMore"
                        val fallback = if (expanded) "Show less" else "Show ${errors.size - 3} more"
                        val bg = if (expanded) "Покажи по-малко" else "Покажи още ${errors.size - 3}"
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(validationSummaryText(i18n, key, fallback, bg,
                                mapOf("count" to errors.size - 3)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text(validationSummaryText(i18n, "validation.close", "Close", "Затвори"))
                }
            },
        )
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(message) } },
        state = rememberTooltipState(),
    ) {
        Surface(
            modifier = Modifier.size(22.dp).clearAndSetSemantics {
                contentDescription = if (includeMessageInName) "$label: $message" else label
                stateDescription = message
            }.clickable { expanded = false; open = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = count?.toString() ?: "●",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun summaryFieldLabel(error: ErrorObject, root: JsonSchema, translator: Translator): String? {
    val segments = summaryErrorSegments(error)
    val field = segments.lastOrNull()?.takeUnless { it.toIntOrNull() != null } ?: return null
    val schema = summaryErrorSchema(error, root)
    val fallback = schema?.title?.takeIf(String::isNotBlank) ?: field
    val key = getI18nKey(schema, null, segments.joinToString("."), "label")
    return translator.translate(key, fallback, emptyMap())?.takeIf(String::isNotBlank) ?: fallback
}

private fun summaryErrorSegments(error: ErrorObject): List<String> =
    error.instancePath.split('/').filter(String::isNotEmpty).map(JsonPointer::decode) +
        listOfNotNull(error.params["missingProperty"]?.takeIf(String::isNotBlank))

private fun summaryErrorSchema(error: ErrorObject, root: JsonSchema): JsonSchema? {
    val segments = summaryErrorSegments(error)
    val field = segments.lastOrNull()
    val schemaPointer = error.schemaPath?.substringBeforeLast('/')?.let { parent ->
        if (error.keyword == "required" && error.params["missingProperty"] != null && field != null) {
            "$parent/properties/${JsonPointer.encode(field)}"
        } else parent
    }
    return schemaPointer?.let { Resolve.schema(root, it, root) }
        ?: schemaForDataSegments(root, segments)
}

private fun schemaForDataSegments(root: JsonSchema, segments: List<String>): JsonSchema? {
    var pointer = "#"
    var schema = root
    for (segment in segments) {
        pointer += if (segment.toIntOrNull() != null) {
            val index = segment.toInt()
            when {
                schema.prefixItems?.getOrNull(index) != null -> "/prefixItems/$index"
                schema.items is JsonSchemaOrSchemas.Multiple -> "/items/$index"
                else -> "/items"
            }
        } else "/properties/${JsonPointer.encode(segment)}"
        schema = Resolve.schema(root, pointer, root) ?: return null
    }
    return schema
}

private fun validationSummaryText(
    i18n: JsonFormsI18nState,
    key: String,
    english: String,
    bulgarian: String,
    values: Map<String, Any?> = emptyMap(),
): String = translateRendererText(
    i18n.translate, i18n.locale, key, english,
    mapOf("bg" to JsonObject(mapOf(key to JsonPrimitive(bulgarian)))), values,
)

internal fun computeChildLabel(item: JsonElement, index: Int, childLabelProp: String?): String {
    val propValue = childLabelProp?.let { Resolve.data(item, it) } as? JsonPrimitive
    return propValue?.contentOrNull?.takeIf { it.isNotBlank() } ?: "Item ${index + 1}"
}

fun resolveChildLabelProp(uischema: UiSchemaElement, schema: JsonSchema): String? =
    (uischema.options["elementLabelProp"] as? JsonPrimitive)
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: (uischema.options["childLabelProp"] as? JsonPrimitive)
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
        ?: getFirstPrimitiveProp(schema)

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
            (isMixedArrayControl(uiSchema, schema, context) &&
                !hasFlatAlternativeObjectArray(uiSchema as? ControlElement, schema, context.rootSchema)) ||
            (hasNestedArrayDetail(uiSchema as? ControlElement) &&
                (isObjectArrayControl(uiSchema, schema, context) ||
                    isPrimitiveArrayControl(uiSchema, schema, context)))
    },
)

private fun hasFlatAlternativeObjectArray(
    control: ControlElement?, schema: JsonSchema?, rootSchema: JsonSchema?,
): Boolean {
    if (control == null || schema == null) return false
    val root = rootSchema ?: schema
    val array = Resolve.schema(schema, control.scope, root) ?: return false
    val item = Resolve.schema(array, "items", root) ?: return false
    return hasFlatAlternativeObjectProperties(item, root)
}

private fun hasNestedArrayDetail(control: ControlElement?): Boolean = when (val detail = control?.options?.get("detail")) {
    is JsonObject -> (detail["type"] as? JsonPrimitive)?.isString == true
    is JsonPrimitive -> detail.isString && !detail.content.equals("DEFAULT", ignoreCase = true)
    else -> false
}
