package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.compose.TupleComplexPosition
import com.chobantonov.jsonfroms.compose.tupleForbiddenTailErrorText
import com.chobantonov.jsonfroms.compose.tupleOmittedPositionErrorText
import com.chobantonov.jsonfroms.core.actions.JsonFormsAction
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.i18n.tupleLocaleBundles
import com.chobantonov.jsonfroms.core.i18n.translateTupleRendererText
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnostic
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnosticReporter
import com.chobantonov.jsonfroms.core.mappers.TupleDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.mappers.tupleVariantDiagnosticCode
import com.chobantonov.jsonfroms.core.mappers.TuplePositionScope
import com.chobantonov.jsonfroms.core.mappers.resolveTuplePositionScope
import com.chobantonov.jsonfroms.core.mappers.TupleSchema
import com.chobantonov.jsonfroms.core.mappers.tupleInitialValue
import com.chobantonov.jsonfroms.core.mappers.isComplexTuplePosition
import com.chobantonov.jsonfroms.core.mappers.TupleTail
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.collectionPageWindow
import com.chobantonov.jsonfroms.core.mappers.pageForFirstVisibleItem
import com.chobantonov.jsonfroms.core.mappers.resolveCollectionPagination
import com.chobantonov.jsonfroms.core.mappers.mapStateToControlWithDetailProps
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.resolveTupleSchema
import com.chobantonov.jsonfroms.core.mappers.resolveTuplePresentation
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.appendArrayItemIfAllowed
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.material.renderers.complex.materialTupleTester
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialCollectionPager
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive
import com.slapps.cupertino.default
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

val cupertinoTupleTester: RankedTester = materialTupleTester
val CupertinoTupleRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { CupertinoTupleRenderer(it) }

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun CupertinoTupleRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement ?: return
    val state = mapStateToControlWithDetailProps(props.state, OwnPropsOfControl(
        controlUiSchema = control,
        own = OwnPropsOfRenderer(
            uischema = control, schema = props.schema, enabled = props.enabled, path = props.path,
            renderers = props.renderers, cells = props.cells, uischemas = props.state.jsonforms.uischemas,
        ),
    ))
    if (!state.visible) return
    val explicitVariant = (control.options["variant"] as? JsonPrimitive)?.contentOrNull == "tuple"
    val tuple = resolveTupleSchema(state.schema, explicitVariant)
    if (tuple == null) {
        if (explicitVariant) {
            val diagnostic = TupleDiagnostic(tupleVariantDiagnosticCode(state.schema), state.path)
            val reporter = state.config[TupleDiagnosticReporterConfigKey] as? TupleDiagnosticReporter
            LaunchedEffect(reporter, diagnostic) { reporter?.report(diagnostic) }
            val message = translateTupleRendererText(props.state.jsonforms.i18n,
                "tuple.configuration", "Tuple configuration is unsupported")
            CupertinoText(message)
        }
        return
    }
    val presentation = resolveTuplePresentation(control.options, state.config)
    val storedValue = Resolve.data(props.state.jsonforms.core.data, state.path)
    val items = (storedValue as? JsonArray) ?: JsonArray(emptyList())
    val tailCount = (items.size - tuple.fixedCount).coerceAtLeast(0)
    val localPagination = (control.options["additionalItems"] as? JsonObject)?.get("pagination")
    val pagination = resolveCollectionPagination(
        localPagination?.let { mapOf("pagination" to it) } ?: emptyMap(),
        state.config, kind = "additionalItems",
    )
    var pageSize by remember(state.path, state.schema) { mutableStateOf(pagination.pageSize) }
    var currentPage by remember(state.path, state.schema) { mutableStateOf(1) }
    var pendingAddedTailIndex by remember(state.path, state.schema) { mutableStateOf<Int?>(null) }
    LaunchedEffect(pagination) { pageSize = pagination.pageSize; currentPage = 1 }
    LaunchedEffect(tailCount, pageSize, pagination.enabled) {
        val added = pendingAddedTailIndex
        currentPage = if (pagination.enabled && added != null && added < tailCount)
            added / pageSize + 1
        else collectionPageWindow(tailCount, currentPage, pageSize, pagination.enabled).page
        pendingAddedTailIndex = null
    }
    val tailWindow = collectionPageWindow(tailCount, currentPage, pageSize, pagination.enabled)
    val context = useJsonForms()
    val pendingEditor = context.controller.hasPendingOwnedWork
    val errorsByIndex = context.containerErrorIndex.countsByImmediateChild(state.path)
    val tailErrorPages = if (pagination.enabled) (0 until tailCount)
        .mapNotNull { relative ->
            errorsByIndex[(relative + tuple.fixedCount).toString()]?.let { relative / pageSize + 1 to it }
        }.groupBy({ it.first }, { it.second }).mapValues { (_, counts) -> counts.sum() }
        .filterKeys { it != tailWindow.page }
    else emptyMap()
    val interactive = state.enabled && !state.readonly
    val restrict = resolveRestrict(control.options["restrict"], state.config)
    val actionPolicy = resolveArrayActionPolicy(control.options, state.config)
    val canAdd = interactive && !pendingEditor && !actionPolicy.disableAdd &&
        tuple.tail != TupleTail.Forbidden && items.size >= tuple.fixedCount &&
        (!restrict || state.schema.maxItems?.let { items.size < it } != false)
    val canRemove = interactive && !pendingEditor && !actionPolicy.disableRemove &&
        (!restrict || state.schema.minItems?.let { items.size > it } != false)
    val removalGuard = remember(state.schema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, state.path, items) {
        ContainsRemovalGuard(state.schema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, state.path, items)
    }
    fun canRemoveAt(index: Int) = !restrict || removalGuard.canRemove(index)
    val translator = props.state.jsonforms.i18n.translate
    fun translated(key: String, fallback: String, values: Map<String, Any?> = emptyMap()): String =
        translateTupleRendererText(props.state.jsonforms.i18n, key, fallback, values)
    var pendingRemoval by remember(state.path, state.schema, control) { mutableStateOf<PendingTupleRemoval?>(null) }
    DisposableEffect(state.path, state.schema, control) {
        onDispose { pendingRemoval = null }
    }
    LaunchedEffect(items, canRemove, props.state.jsonforms.core.data) {
        if (pendingRemoval?.let { !canRemove || it.snapshot != items || !canRemoveAt(it.index) } == true)
            pendingRemoval = null
    }
    fun remove(index: Int, snapshot: JsonArray) {
        if (!canRemove || items != snapshot || !tuple.canRemoveTrailing(index, snapshot.size) || !canRemoveAt(index)) return
        val result = JsonArray(snapshot.filterIndexed { position, _ -> position != index })
        props.dispatch(UpdateAction(state.path, updater = { current -> if (current == snapshot) result else current }))
    }
    fun requestRemoval(index: Int) {
        if (!canRemove || index !in items.indices || !tuple.canRemoveTrailing(index, items.size) || !canRemoveAt(index)) return
        val snapshot = JsonArray(items)
        val decision = resolveConfirmationDecision(
            ConfirmationOperation.Delete, "additionalItems", listOf(items[index]), control.options, state.config,
        )
        if (decision.required) pendingRemoval = PendingTupleRemoval(index, snapshot)
        else remove(index, snapshot)
    }

    val detailOption = control.options["detail"]
    val positionLayout = (detailOption as? JsonObject)?.let {
        runCatching { it.toUiSchemaElement() }.getOrNull()
    }
    val colors = LocalCupertinoColors.current
    val arrayErrors = listOf(
        state.errors,
        tupleForbiddenTailErrorText(props, state.path, state.schema, tuple.fixedCount, control,
            context.containerErrorIndex),
        positionLayout?.let { tupleOmittedPositionErrorText(props, state.path, tuple, it,
            context.containerErrorIndex) }.orEmpty(),
    ).filter(String::isNotBlank).joinToString("\n")
    val outline = if (arrayErrors.isNotBlank()) colors.destructive else colors.separator
    val container = if (presentation.showBorder) Modifier.fillMaxWidth()
        .border(BorderStroke(1.dp, outline), RoundedCornerShape(8.dp)).padding(12.dp)
    else Modifier.fillMaxWidth().padding(vertical = 8.dp)
    Column(container, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CupertinoText(state.label.ifBlank { state.schema.title ?: translated("tuple.title", "Tuple") })
        if (arrayErrors.isNotBlank()) CupertinoText(arrayErrors)
        storedValue?.takeIf { it !is JsonArray }?.let { imported ->
            val label = translateRendererText(translator, props.state.jsonforms.i18n.locale,
                "tuple.invalidCurrentValue", "Invalid current value", tupleLocaleBundles)
            CupertinoText("$label: $imported")
        }
        val fixedPositions: @Composable () -> Unit = {
            tuple.prefix.indices.forEach { index ->
                val slotSchema = tuple.schemaAt(index)
                val fallback = slotSchema?.title ?: translated(
                    "tuple.position", "Item ${index + 1}", mapOf("position" to index + 1),
                )
                val key = (slotSchema?.get("i18n") as? JsonPrimitive)?.contentOrNull?.let { "$it.label" }
                val label = key?.let { translated(it, fallback) } ?: fallback
                Column(
                    modifier = if (presentation.vertical) Modifier.fillMaxWidth() else Modifier.widthIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (slotSchema != null) CupertinoTuplePosition(
                        props, state.path, state.rootSchema, state.schema, slotSchema, tuple, items,
                        index, label, control, interactive,
                    )
                    else {
                        CupertinoText(label)
                        CupertinoText(translated("tuple.invalidPosition", "Unsupported position schema"))
                    }
                }
            }
        }
        if (detailOption != null && positionLayout == null) {
            val diagnostic = TupleDiagnostic("tuple.detail.invalid", state.path, control.scope)
            val reporter = state.config[TupleDiagnosticReporterConfigKey] as? TupleDiagnosticReporter
            LaunchedEffect(reporter, diagnostic) { reporter?.report(diagnostic) }
            CupertinoText(translated("tuple.invalidDetail", "Unsupported tuple layout"))
        }
        if (positionLayout != null) {
            val positionEntry = JsonFormsRendererRegistryEntry(
                tester = { ui, _, _ ->
                    val child = ui as? ControlElement
                    when (child?.let { resolveTuplePositionScope(it.scope, tuple) }) {
                        is TuplePositionScope.Whole, TuplePositionScope.Invalid -> 2000
                        else -> -1
                    }
                },
                renderer = ComposeJsonFormsRenderer { nested ->
                    val child = nested.uischema as ControlElement
                    when (val position = resolveTuplePositionScope(child.scope, tuple)) {
                        is TuplePositionScope.Whole -> {
                            val slotSchema = tuple.schemaAt(position.index)
                            if (slotSchema != null) {
                                val fallback = slotSchema.title ?: translated(
                                    "tuple.position", "Item ${position.index + 1}",
                                    mapOf("position" to position.index + 1),
                                )
                                val key = child.i18n?.let { "$it.label" }
                                    ?: (slotSchema["i18n"] as? JsonPrimitive)?.contentOrNull?.let { "$it.label" }
                                val label = key?.let { translated(it, fallback) } ?: fallback
                                CupertinoTuplePosition(
                                    nested, state.path, state.rootSchema, state.schema, slotSchema, tuple, items,
                                    position.index, label, control, interactive, child,
                                )
                            }
                        }
                        TuplePositionScope.Invalid -> {
                            val diagnostic = TupleDiagnostic("tuple.position.invalid", state.path, child.scope)
                            val reporter = state.config[TupleDiagnosticReporterConfigKey] as? TupleDiagnosticReporter
                            LaunchedEffect(reporter, diagnostic) { reporter?.report(diagnostic) }
                            CupertinoText(translated("tuple.invalidPosition", "Unsupported position schema"))
                        }
                        else -> Unit
                    }
                },
            )
            JsonFormsDispatchRenderer(props.copy(
                schema = state.schema, rootSchema = state.rootSchema,
                uischema = positionLayout, path = state.path,
                renderers = listOf(positionEntry) + props.renderers,
            ))
        }
        else if (presentation.vertical) Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { fixedPositions() }
        else FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { fixedPositions() }
        if (items.size > tuple.fixedCount || tuple.tail != TupleTail.Forbidden) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CupertinoText(translated("tuple.additionalItems", "Additional items"))
                if (tuple.tail != TupleTail.Forbidden) CupertinoButton(
                    enabled = canAdd,
                    colors = CupertinoButtonDefaults.filledButtonColors(),
                    onClick = {
                        if (!canAdd) return@CupertinoButton
                        val tailSchema = tuple.schemaAt(items.size) ?: return@CupertinoButton
                        val value = if (tuple.tail == TupleTail.Unconstrained) kotlinx.serialization.json.JsonNull
                            else createDefaultValue(tailSchema, state.rootSchema)
                        val snapshot = storedValue
                        pendingAddedTailIndex = tailCount
                        props.dispatch(UpdateAction(state.path, updater = { current ->
                            appendArrayItemIfAllowed(current, snapshot, value, state.schema.maxItems, restrict)
                        }))
                    },
                ) { CupertinoText(translated("tuple.add", "Add item")) }
            }
            (tailWindow.start until tailWindow.endExclusive).forEach { relative ->
                val index = relative + tuple.fixedCount
                val label = translated("tuple.position", "Item ${index + 1}", mapOf("position" to index + 1))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val tailSchema = tuple.schemaAt(index)
                    if (tailSchema != null) TuplePosition(props, state.path, state.rootSchema, tailSchema, tuple, index, label)
                    else {
                        CupertinoText(label)
                        CupertinoText(items[index].toString())
                    }
                    CupertinoButton(
                        enabled = canRemove && canRemoveAt(index),
                        colors = CupertinoButtonDefaults.plainButtonColors(),
                        onClick = { requestRemoval(index) },
                    ) { CupertinoText(translated("tuple.delete", "Delete $label", mapOf("label" to label))) }
                }
            }
            MaterialCollectionPager(
                pagination, tailWindow, pageSize, tailCount, tailErrorPages, translator,
                canNavigate = !pendingEditor,
                onChange = { requestedPage, requestedSize ->
                    if (pendingEditor) return@MaterialCollectionPager
                    currentPage = if (requestedSize == pageSize) requestedPage else
                        pageForFirstVisibleItem(tailWindow.start, requestedSize)
                    pageSize = requestedSize
                },
            )
        }
    }
    val request = pendingRemoval?.takeIf {
        canRemove && it.snapshot == items && tuple.canRemoveTrailing(it.index, items.size) && canRemoveAt(it.index)
    }
    if (request != null) CupertinoAlertDialog(
        onDismissRequest = { if (pendingRemoval === request) pendingRemoval = null },
        title = { CupertinoText(translated("confirm.delete.title", "Delete this value?")) },
        message = { CupertinoText(translated("confirm.delete.message", "The value will be removed. This cannot be undone from here.")) },
        buttons = {
            cancel(onClick = { if (pendingRemoval === request) pendingRemoval = null }) {
                CupertinoText(translated("confirm.decline", "No"))
            }
            destructive(onClick = {
                if (pendingRemoval === request) remove(request.index, request.snapshot)
                if (pendingRemoval === request) pendingRemoval = null
            }) { CupertinoText(translated("confirm.accept", "Yes")) }
        },
    )
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoTuplePosition(
    props: ComposeJsonFormsRendererProps,
    path: String,
    rootSchema: JsonSchema,
    arraySchema: JsonSchema,
    schema: JsonSchema,
    tuple: TupleSchema,
    items: JsonArray,
    index: Int,
    label: String,
    control: ControlElement,
    interactive: Boolean,
    positionControl: ControlElement? = null,
) {
    val complex = isComplexTuplePosition(schema, rootSchema)
    if (!complex) {
        TuplePosition(props, path, rootSchema, schema, tuple, index, label, positionControl)
        return
    }
    val schemaPath = if (arraySchema["prefixItems"] is JsonArray) "#/prefixItems/$index" else "#/items/$index"
    TupleComplexPosition(
        props, path, rootSchema, schema, schemaPath, tuple, index, label, items, control, interactive,
        summaryContent = { summary, indicatorLabel, indicatorCount, ownErrorText, editLabel, onEdit ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CupertinoText(label)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SelectionContainer { CupertinoText(summary, Modifier.weight(1f)) }
                    if (indicatorLabel != null) CupertinoText(
                        indicatorCount?.toString() ?: "●",
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = indicatorLabel
                        },
                        color = LocalCupertinoColors.current.destructive,
                    )
                    CupertinoButton(
                        enabled = interactive,
                        colors = CupertinoButtonDefaults.plainButtonColors(),
                        modifier = Modifier.semantics { contentDescription = editLabel },
                        onClick = onEdit,
                    ) { CupertinoText("✎") }
                }
                if (ownErrorText != null) CupertinoText(
                    ownErrorText, color = LocalCupertinoColors.current.destructive,
                )
            }
        },
        dialogContent = { title, content, applyLabel, cancelLabel, clearLabel, onClear, onApply, onCancel ->
            CupertinoAlertDialog(
                onDismissRequest = onCancel,
                title = { CupertinoText(title) },
                message = { Box(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { content() } },
                buttons = {
                    cancel(onClick = onCancel) { CupertinoText(cancelLabel) }
                    if (clearLabel != null) default(
                        enabled = onClear != null, onClick = { onClear?.invoke() },
                    ) { CupertinoText(clearLabel) }
                    default(onClick = onApply) { CupertinoText(applyLabel) }
                },
            )
        },
    )
}

@Composable
private fun TuplePosition(
    props: ComposeJsonFormsRendererProps,
    path: String,
    rootSchema: JsonSchema,
    schema: JsonSchema,
    tuple: TupleSchema,
    index: Int,
    label: String,
    control: ControlElement? = null,
) {
    val positionPath = composePaths(path, index.toString())
    val positionDispatch: (JsonFormsAction) -> Unit = { action ->
        if (action is UpdateAction && action.path == positionPath && index < tuple.fixedCount) {
            props.dispatch(UpdateAction(path, updater = { current ->
                val values = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                val updated = action.updater(values.getOrNull(index)) ?: return@UpdateAction current
                if (index >= values.size) {
                    for (missing in values.size until index) {
                        val initial = tuple.schemaAt(missing)?.let { tupleInitialValue(it, rootSchema) }
                            ?: return@UpdateAction current
                        values += initial
                    }
                    values += updated
                } else values[index] = updated
                JsonArray(values)
            }, context = action.context))
        } else props.dispatch(action)
    }
    JsonFormsDispatchRenderer(props.copy(
        dispatch = positionDispatch,
        schema = schema, rootSchema = rootSchema,
        uischema = control?.copy(scope = "#", label = control.label ?: StringControlLabel(label))
            ?: ControlElement(scope = "#", label = StringControlLabel(label)),
        path = composePaths(path, index.toString()),
        id = "${props.id}-tuple-$index",
    ))
}

private data class PendingTupleRemoval(val index: Int, val snapshot: JsonArray)
