package com.chobantonov.jsonfroms.material.renderers.complex

import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText

import com.chobantonov.jsonfroms.core.i18n.translateArrayValidationIndicator
import com.chobantonov.jsonfroms.core.i18n.translateCompositeValidationIndicator
import com.chobantonov.jsonfroms.core.i18n.translateRowDetailValidationIndicator
import com.chobantonov.jsonfroms.core.i18n.translateRowDetailText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsCellProps
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.DispatchCell
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.JsonFormsItemScope
import com.chobantonov.jsonfroms.compose.IsolatedDetailDraftForm
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.compose.useJsonForms
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.material.renderers.layouts.ErrorIndicator
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialCollectionPager
import com.chobantonov.jsonfroms.material.renderers.additional.CollectionArrayReference
import com.chobantonov.jsonfroms.material.renderers.additional.DetailDialogFrame
import com.chobantonov.jsonfroms.material.renderers.additional.ListDetailSelection
import com.chobantonov.jsonfroms.material.renderers.additional.ListDetailSplitPane
import com.chobantonov.jsonfroms.material.renderers.additional.TableBottomSplitPane
import com.chobantonov.jsonfroms.material.renderers.additional.reconcileListDetailSelection
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnostic
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticSeverity
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.ArrayTableColumnDef
import com.chobantonov.jsonfroms.core.mappers.resolveArrayTableColumnDefs
import com.chobantonov.jsonfroms.core.mappers.tableColumnHeaderLabel
import com.chobantonov.jsonfroms.core.mappers.collectionPageWindow
import com.chobantonov.jsonfroms.core.mappers.applyAgGridPaginationOverrides
import com.chobantonov.jsonfroms.core.mappers.ArrayRowDetailPresentation
import com.chobantonov.jsonfroms.core.mappers.ArrayRowDetailPlacement
import com.chobantonov.jsonfroms.core.mappers.ArrayRowDetailOptions
import com.chobantonov.jsonfroms.core.mappers.DetailDialogOptions
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfArrayControl
import com.chobantonov.jsonfroms.core.mappers.resolveArrayRowDetailOptions
import com.chobantonov.jsonfroms.core.mappers.resolveDetailDialogOptions
import com.chobantonov.jsonfroms.core.mappers.resolveArrayTableCellOptions
import com.chobantonov.jsonfroms.core.mappers.arrayTableCellPreviewResult
import com.chobantonov.jsonfroms.core.mappers.resolveArrayTableCellTypeIndicator
import com.chobantonov.jsonfroms.core.mappers.resolveCompositeDialogActions
import com.chobantonov.jsonfroms.core.mappers.canEmptyCompositeDraft
import com.chobantonov.jsonfroms.core.mappers.resolveTableLabelSummary
import com.chobantonov.jsonfroms.core.mappers.tableCellErrors
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.core.i18n.deriveLabelForUISchemaElement
import com.chobantonov.jsonfroms.core.i18n.translateRendererText
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.extractDefaults
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.groupArrayErrors
import com.chobantonov.jsonfroms.core.mappers.pageForFirstVisibleItem
import com.chobantonov.jsonfroms.core.mappers.resolveCollectionPagination
import com.chobantonov.jsonfroms.core.mappers.resolveContainerValidationOptions
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.TesterContext
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isObjectArrayControl
import com.chobantonov.jsonfroms.core.testers.isMixedArrayControl
import com.chobantonov.jsonfroms.core.testers.isPrimitiveArrayControl
import com.chobantonov.jsonfroms.core.testers.optionIs
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.activeEditingSchemaForScopedProperty
import com.chobantonov.jsonfroms.core.util.alternativeObjectColumns
import com.chobantonov.jsonfroms.core.util.selectableAlternativeObjectBranches
import com.chobantonov.jsonfroms.core.util.commonAlternativeProperties
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.core.util.appendArrayItemIfAllowed
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.core.util.deriveTypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

val MaterialArrayControlRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayControlRenderer(props)
}

@Composable
fun MaterialArrayControlRenderer(
    props: ComposeJsonFormsRendererProps,
    rowDetailDialog: @Composable (
        String, @Composable () -> Unit, String, String, String?, String?, Boolean, DetailDialogOptions,
        String, String, String, String, () -> Unit, () -> Unit,
    ) -> Unit = { title, content, apply, cancel, applyTooltip, cancelTooltip, canApply, options,
                  maximize, restore, close, resize, onApply, onCancel ->
        DetailDialogFrame(
            options = options,
            resizeLabel = resize,
            onDismiss = onCancel,
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            actions = { maximized, toggle ->
                if (options.maximizable) TextButton(onClick = toggle,
                    modifier = Modifier.testTag("row-detail-dialog-maximize")) {
                    Text(if (maximized) restore else maximize)
                }
                TextButton(onClick = onCancel, modifier = Modifier.semantics {
                    contentDescription = close
                }) { Text("×") }
            },
            footer = {
                if (cancelTooltip == null) TextButton(onClick = onCancel) { Text(cancel) }
                else ActionTooltip(cancelTooltip) {
                    TextButton(onClick = onCancel) { Text(cancel) }
                }
                if (canApply) {
                    if (applyTooltip == null) TextButton(onClick = onApply) { Text(apply) }
                    else ActionTooltip(applyTooltip) {
                        TextButton(onClick = onApply) { Text(apply) }
                    }
                }
            },
            surface = { modifier, body ->
                Surface(modifier = modifier, shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) { body() }
            },
            content = content,
        )
    },
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

    val storedValue = Resolve.data(props.state.jsonforms.core.data, stateProps.path)
    val itemsArray = storedValue as? JsonArray ?: JsonArray(emptyList())
    val items = itemsArray.toList()
    val rowOccurrences = remember(stateProps.path, stateProps.schema) {
        TableRowOccurrences(itemsArray)
    }
    val rowIds = rowOccurrences.idsFor(itemsArray)
    val agGridRequested = (control.options["variant"] as? JsonPrimitive)?.contentOrNull == "ag-grid"
    val columns = arrayColumns(
        stateProps.schema, stateProps.rootSchema,
        resolveArrayTableColumnDefs(control.options, agGridRequested),
    )
    val addBranchChoices = selectableAlternativeObjectBranches(stateProps.schema, stateProps.rootSchema)
    var addBranchExpanded by remember(stateProps.path, stateProps.schema) { mutableStateOf(false) }
    var addBranchSnapshot by remember(stateProps.path, stateProps.schema) { mutableStateOf<JsonArray?>(null) }
    val reporter = stateProps.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
    var reportedAgGrid by remember(stateProps.path, agGridRequested) { mutableStateOf(false) }
    LaunchedEffect(reporter, stateProps.path, agGridRequested) {
        if (agGridRequested && !reportedAgGrid && reporter != null) {
            reporter.report(UiDiagnostic(
            code = "variant.unsupported",
            severity = UiDiagnosticSeverity.Warning,
            message = "AG Grid is unavailable in this renderer; using the portable array table.",
            details = mapOf("variant" to "ag-grid", "path" to stateProps.path),
            ))
            reportedAgGrid = true
        }
    }
    val renderAsTable = agGridRequested || control.booleanOption("table", stateProps.config) ||
        "columnDefs" in control.options ||
        shouldRenderArrayItemsAsTable(stateProps.schema, stateProps.rootSchema)
    val rowDetail = resolveArrayRowDetailOptions(control.options)?.takeIf { renderAsTable }
    val dialogRowDetail = rowDetail?.takeIf { it.presentation == ArrayRowDetailPresentation.Dialog }
    val panelRowDetail = rowDetail?.takeIf { it.presentation == ArrayRowDetailPresentation.Panel }
    val showActions = stateProps.enabled && !stateProps.readonly
    val showDetailAction = rowDetail != null && (stateProps.enabled || stateProps.readonly)
    val showTableActions = showActions || showDetailAction
    val pendingEditor = useJsonForms().controller.hasPendingOwnedWork
    val actionsEnabled = showActions && !pendingEditor
    val restrict = resolveRestrict(control.options["restrict"], stateProps.config)
    val hideSummaryValidation = control.booleanOption("hideArraySummaryValidation", stateProps.config)
    val validationOptions = resolveContainerValidationOptions(
        control.options, stateProps.config, defaultIndicator = true,
    )
    val groupedErrors = remember(stateProps.childErrors, stateProps.path) {
        groupArrayErrors(stateProps.childErrors, stateProps.path)
    }
    val summaryErrors = groupedErrors.descendants
    val portablePagination = resolveCollectionPagination(
        control.options, stateProps.config, defaultEnabled = renderAsTable,
    )
    val pagination = if (agGridRequested) applyAgGridPaginationOverrides(
        portablePagination, control.options["agGridOptions"] as? JsonObject,
    ) else portablePagination
    var pageSize by remember(stateProps.schema, stateProps.path) { mutableStateOf(pagination.pageSize) }
    var currentPage by remember(stateProps.schema, stateProps.path) { mutableStateOf(1) }
    var previousSize by remember(stateProps.schema, stateProps.path) { mutableStateOf(items.size) }
    var pendingAddedIndex by remember(stateProps.path) { mutableStateOf<Int?>(null) }
    LaunchedEffect(pagination) {
        pageSize = pagination.pageSize
        currentPage = 1
    }
    LaunchedEffect(items.size, pageSize, pagination.enabled) {
        val added = pendingAddedIndex
        if (pagination.enabled && added != null && items.size > previousSize)
            currentPage = added / pageSize + 1
        else currentPage = currentPage.coerceAtMost(
            collectionPageWindow(items.size, currentPage, pageSize, pagination.enabled).pages,
        )
        pendingAddedIndex = null
        previousSize = items.size
    }
    val pageWindow = collectionPageWindow(items.size, currentPage, pageSize, pagination.enabled)
    val visibleIndices = pageWindow.start until pageWindow.endExclusive
    val errorPages = if (pagination.enabled) groupedErrors.byIndex.entries
        .groupBy({ it.key / pageSize + 1 }, { it.value.size })
        .mapValues { (_, counts) -> counts.sum() }
        .filterKeys { it != pageWindow.page }
    else emptyMap()
    val firstErrorIndex = groupedErrors.byIndex.keys.filter { it in items.indices }.minOrNull()
    val actionPolicy = resolveArrayActionPolicy(control.options, stateProps.config)
    val canAdd = actionsEnabled && !actionPolicy.disableAdd &&
        (!restrict || stateProps.arraySchema.maxItems?.let { items.size < it } != false)
    val appliedOptions = stateProps.config + control.options
    val showUnfocusedDescription = strictControlBooleanOption(
        appliedOptions, "showUnfocusedDescription") == true
    val hideRequiredAsterisk = strictControlBooleanOption(
        appliedOptions, "hideRequiredAsterisk") == true
    var addFocused by remember(stateProps.path) { mutableStateOf(false) }
    val visibleDescription = stateProps.description?.takeIf {
        it.isNotBlank() && (addFocused || showUnfocusedDescription)
    }
    val requiredState = translateControlRequired(props.state.jsonforms.i18n)
        .takeIf { stateProps.required }
    val accessibleState = listOfNotNull(requiredState, visibleDescription).joinToString(". ")
    val addFocusRequester = rememberInitialControlFocusRequester(
        stateProps.path, appliedOptions, canAdd)
    val canRemove = actionsEnabled && !actionPolicy.disableRemove &&
        (!restrict || stateProps.arraySchema.minItems?.let { items.size > it } != false)
    val removalGuard = remember(stateProps.arraySchema, props.state.jsonforms.core.validator,
        props.state.jsonforms.core.data, stateProps.path, itemsArray) {
        ContainsRemovalGuard(stateProps.arraySchema, props.state.jsonforms.core.validator,
            props.state.jsonforms.core.data, stateProps.path, itemsArray)
    }
    fun canRemoveAt(index: Int) = !restrict || removalGuard.canRemove(index)
    var pendingRemoval by remember(stateProps.path) { mutableStateOf<PendingTableRemoval?>(null) }
    var pendingCellRemoval by remember(stateProps.path, stateProps.schema, control.options["cells"]) {
        mutableStateOf<PendingCompositeCellRemoval?>(null)
    }
    var rowDetailSession by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf<RowDetailDraftSession?>(null)
    }
    var rowDetailPending by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf(false)
    }
    var rowDetailActiveFocus by remember(stateProps.path) { mutableStateOf<FocusRequester?>(null) }
    var rowDetailReturnFocus by remember(stateProps.path) { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(rowDetailReturnFocus) {
        val target = rowDetailReturnFocus ?: return@LaunchedEffect
        withFrameNanos { }
        // The opener can disappear while a dialog is open, for example after a host replacement.
        try { target.requestFocus() } catch (_: IllegalStateException) { }
        if (rowDetailActiveFocus === target) rowDetailActiveFocus = null
        rowDetailReturnFocus = null
    }
    var cellDetailSession by remember(stateProps.path, stateProps.schema, control.options["cells"]) {
        mutableStateOf<CellDetailDraftSession?>(null)
    }
    var cellDetailPending by remember(stateProps.path, stateProps.schema, control.options["cells"]) {
        mutableStateOf(false)
    }
    val dialogOpen = rowDetailSession != null || cellDetailSession != null
    val backgroundCellsEnabled = stateProps.enabled && !dialogOpen
    val backgroundActionsEnabled = actionsEnabled && !dialogOpen
    var panelVisible by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf(panelRowDetail?.collapsed != true)
    }
    var panelRightFraction by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableFloatStateOf(0.32f)
    }
    var panelBottomHeight by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableFloatStateOf(280f)
    }
    var panelSelection by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf<ListDetailSelection?>(null)
    }
    var panelSelectionGeneration by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf(0)
    }
    var previousPanelItems by remember(stateProps.path, stateProps.schema, control.options["rowDetail"]) {
        mutableStateOf(itemsArray, referentialEqualityPolicy())
    }
    LaunchedEffect(CollectionArrayReference(itemsArray), canAdd, dialogOpen) {
        if (addBranchExpanded && (!canAdd || dialogOpen || itemsArray !== addBranchSnapshot))
            addBranchExpanded = false
    }
    var pendingPanelChange by remember(stateProps.path) { mutableStateOf<PendingPanelChange?>(null) }
    LaunchedEffect(CollectionArrayReference(itemsArray), canRemove, showActions, pendingEditor,
        props.state.jsonforms.core.data) {
        if (pendingRemoval?.let { !canRemove || it.snapshot !== itemsArray || !canRemoveAt(it.index) } == true)
            pendingRemoval = null
        if (pendingCellRemoval?.let { !showActions || pendingEditor || it.snapshot !== itemsArray } == true)
            pendingCellRemoval = null
        if (rowDetailSession?.let { it.snapshot !== itemsArray } == true) rowDetailSession = null
        if (cellDetailSession?.let { it.snapshot !== itemsArray } == true) cellDetailSession = null
        if (itemsArray !== previousPanelItems) {
            val oldIndex = panelSelection?.index
            val local = pendingPanelChange
            panelSelection = if (local != null && local.result == itemsArray) {
                local.selectedIndex?.takeIf { it in itemsArray.indices }
                    ?.let { ListDetailSelection(it, itemsArray[it]) }
            } else reconcileListDetailSelection(panelSelection, previousPanelItems, itemsArray)
            if (panelSelection?.index != oldIndex) panelSelectionGeneration++
            previousPanelItems = itemsArray
            pendingPanelChange = null
        }
    }
    fun selectPanel(index: Int) {
        if (dialogOpen || panelRowDetail == null || pendingEditor || index !in items.indices) return
        if (panelSelection?.index != index) panelSelectionGeneration++
        panelSelection = ListDetailSelection(index, itemsArray[index])
    }
    fun revealRowDetail(index: Int) {
        if (dialogOpen || pendingEditor || index !in items.indices) return
        if (dialogRowDetail != null) {
            rowDetailPending = false
            rowDetailSession = RowDetailDraftSession(
                index = index, snapshot = itemsArray, original = items[index],
                root = props.state.jsonforms.core.data,
            )
        } else if (panelRowDetail != null) {
            selectPanel(index)
            panelVisible = true
        }
    }
    fun remove(index: Int, snapshot: JsonArray) {
        if (dialogOpen || !canRemove || itemsArray !== snapshot || index !in snapshot.indices || !canRemoveAt(index)) return
        val result = JsonArray(snapshot.filterIndexed { position, _ -> position != index })
        if (panelRowDetail != null) pendingPanelChange = PendingPanelChange(result,
            panelSelection?.index?.let { selected -> when {
                selected == index -> null
                selected > index -> selected - 1
                else -> selected
            } })
        props.dispatch(UpdateAction(path = stateProps.path, updater = { current ->
            if (current === snapshot) result else current
        }))
    }
    fun removeCell(request: PendingCompositeCellRemoval) {
        if (!showActions || pendingEditor || itemsArray !== request.snapshot ||
            request.index !in items.indices) return
        val row = items[request.index] as? JsonObject ?: return
        if (row !== request.originalRow || row[request.field] !== request.value) return
        val replacement = request.replacementRow ?: JsonObject(row.toMutableMap().apply { remove(request.field) })
        val result = JsonArray(items.mapIndexed { index, item ->
            if (index == request.index) replacement else item
        })
        props.dispatch(UpdateAction(path = stateProps.path, updater = { current ->
            if (current === request.snapshot) result else current
        }))
        cellDetailSession = null
    }
    fun requestCellRemoval(index: Int, field: String, value: JsonElement) {
        if (dialogOpen || !showActions || pendingEditor || index !in items.indices) return
        val row = items[index] as? JsonObject ?: return
        if (row[field] !== value || value !is JsonObject && value !is JsonArray) return
        val request = PendingCompositeCellRemoval(index, field, itemsArray, row, value)
        val decision = resolveConfirmationDecision(
            ConfirmationOperation.Delete, "compositeCell", listOf(value), control.options, stateProps.config,
        )
        if (decision.required) pendingCellRemoval = request else removeCell(request)
    }
    fun move(from: Int, to: Int) {
        if (!backgroundActionsEnabled || from !in items.indices || to !in items.indices || pendingEditor) return
        if (panelRowDetail != null) {
            val result = items.toMutableList().apply { add(to, removeAt(from)) }
            val selected = panelSelection?.index
            pendingPanelChange = PendingPanelChange(JsonArray(result), when (selected) {
                from -> to
                to -> from
                else -> selected
            })
        }
        val command = if (to < from) dispatchProps.moveUp?.invoke(stateProps.path, from)
            else dispatchProps.moveDown?.invoke(stateProps.path, from)
        if (command != null) {
            rowOccurrences.moved(from, to)
            command()
        }
    }
    fun requestRemoval(index: Int) {
        if (dialogOpen || !canRemove || index !in items.indices || !canRemoveAt(index)) return
        val snapshot = itemsArray
        val decision = resolveConfirmationDecision(
            ConfirmationOperation.Delete, "arrayTable", listOf(items[index]),
            control.options, stateProps.config,
        )
        if (decision.required) pendingRemoval = PendingTableRemoval(index, snapshot)
        else remove(index, snapshot)
    }
    val showSortButtons = control.booleanOption("showSortButtons", stateProps.config)
    val actionColumnWidth = if (showSortButtons) SortAndDeleteActionColumnWidth else DeleteActionColumnWidth
    val showRowErrorMarkers = showDetailAction && validationOptions.showIndicator &&
        groupedErrors.byIndex.values.any { it.isNotEmpty() }
    val detailActionWidth = if (showDetailAction) RowDetailActionWidth +
        (if (showRowErrorMarkers) 28.dp else 0.dp) else 0.dp
    val tableActionWidth = (if (showActions) actionColumnWidth else 0.dp) +
        detailActionWidth
    val translator = props.state.jsonforms.i18n.translate
    fun rowDetailText(key: String, fallback: String, values: Map<String, Any?> = emptyMap(),
                      bundleKey: String = key) =
        translateRowDetailText(translator, props.state.jsonforms.i18n.locale, key, fallback,
            values, bundleKey)
    val errorTranslator = props.state.jsonforms.i18n.translateError ?: defaultErrorTranslator
    fun localizedCellErrors(errors: List<ErrorObject>) =
        errors.map { error ->
            error.copy(message = errorTranslator.translate(error, translator ?: defaultTranslator, CellUiSchema))
        }
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
    val editDetailsText = rowDetailText("rowDetail.edit", "Edit details")
    val viewDetailsText = rowDetailText("rowDetail.view", "View details")
    val geometryKey = listOf(stateProps.path, stateProps.schema, control.options["columnDefs"],
        control.options["agGridOptions"])
    val resizedWidths = remember(geometryKey) { mutableStateMapOf<String, Float>() }
    val measuredWidths = remember(geometryKey) { mutableStateMapOf<String, Float>() }
    val tableScroll = rememberScrollState()
    val tableMinimumWidth = (
        32f + columns.sumOf { column ->
            (resizedWidths[column.name] ?: column.initialWidth() ?: maxOf(column.minWidth ?: 0f, 160f)).toDouble()
        }.toFloat() + 12f * (columns.size - 1 + if (showTableActions && columns.isNotEmpty()) 1 else 0).coerceAtLeast(0) +
            if (showTableActions) tableActionWidth.value else 0f
    ).dp

    val tableContent: @Composable (Modifier) -> Unit = { tableModifier ->
    Card(modifier = tableModifier) {
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
                    if (agGridRequested) {
                        Text(
                            text = translateRendererText(translator, props.state.jsonforms.i18n.locale,
                                "array.agGridFallback", "AG Grid is unavailable here; showing an array table.",
                                arrayRendererLocaleBundles),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (stateProps.label.isNotBlank()) {
                            Text(
                                text = computeLabel(stateProps.label, stateProps.required,
                                    hideRequiredAsterisk),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
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
                    visibleDescription?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    stateProps.errors.takeIf { it.isNotBlank() }?.let { errors ->
                        Text(
                            text = errors,
                            modifier = Modifier.semantics { error(errors) },
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
                if (panelRowDetail != null) TextButton(
                    enabled = !pendingEditor && !dialogOpen,
                    onClick = { panelVisible = !panelVisible },
                ) {
                    Text(rowDetailText(if (panelVisible) "rowDetail.hide" else "rowDetail.show",
                        if (panelVisible) "Hide details" else "Show details"))
                }
                Box {
                ActionTooltip(addText.tooltip) {
                    FilledIconButton(
                        enabled = canAdd && !dialogOpen,
                        modifier = Modifier.testTag("table-add-item").focusRequester(addFocusRequester)
                            .onFocusChanged { addFocused = it.isFocused }
                            .semantics {
                                if (accessibleState.isNotBlank()) stateDescription = accessibleState
                                if (stateProps.errors.isNotBlank()) error(stateProps.errors)
                            },
                        onClick = {
                            if (!canAdd || dialogOpen) return@FilledIconButton
                            if (addBranchChoices != null) {
                                addBranchSnapshot = itemsArray
                                addBranchExpanded = true
                                return@FilledIconButton
                            }
                            pendingAddedIndex = items.size
                            val snapshot = storedValue
                            val item = createDefaultValue(stateProps.schema, stateProps.rootSchema)
                            props.dispatch(UpdateAction(stateProps.path, updater = { current ->
                                if (!canAdd || !stateProps.enabled || stateProps.readonly) current else
                                    appendArrayItemIfAllowed(current, snapshot, item,
                                        stateProps.arraySchema.maxItems, restrict)
                            }))
                        },
                    ) {
                        Icon(AddIcon, contentDescription = addText.ariaLabel)
                    }
                }
                DropdownMenu(
                    expanded = addBranchExpanded && canAdd && !dialogOpen,
                    onDismissRequest = { addBranchExpanded = false; addBranchSnapshot = null },
                ) {
                    addBranchChoices?.forEachIndexed { index, choice ->
                        val label = choice.title ?: translateRendererText(
                            translator, props.state.jsonforms.i18n.locale,
                            "array.branchOption", "Option {index}", arrayRendererLocaleBundles,
                            mapOf("index" to index + 1),
                        ).replace("{index}", (index + 1).toString())
                        DropdownMenuItem(
                            text = { Text(label) },
                            modifier = Modifier.testTag("add-branch-$index"),
                            onClick = {
                                addBranchExpanded = false
                                addBranchSnapshot = null
                                if (!canAdd || dialogOpen) return@DropdownMenuItem
                                pendingAddedIndex = items.size
                                val snapshot = storedValue
                                val initial = extractDefaults(choice.schema, stateProps.rootSchema)
                                    as? JsonObject ?: JsonObject(emptyMap())
                                val item = JsonObject(initial +
                                    (choice.discriminatorField to choice.discriminatorValue))
                                props.dispatch(UpdateAction(stateProps.path, updater = { current ->
                                    if (!canAdd || !stateProps.enabled || stateProps.readonly) current else
                                        appendArrayItemIfAllowed(current, snapshot, item,
                                            stateProps.arraySchema.maxItems, restrict)
                                }))
                            },
                        )
                    }
                }
                }
            }

            HorizontalDivider()

            if (renderAsTable && columns.isNotEmpty()) {
                TableScrollRow(tableMinimumWidth, tableScroll, "table-header-scroll") {
                    columns.forEach { column ->
                        Row(
                            modifier = columnModifier(column, resizedWidths[column.name])
                                .onSizeChanged { size ->
                                    measuredWidths[column.name] = size.width.toFloat()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f).clipToBounds(),
                                text = tableColumnHeaderLabel(
                                    column.title, column.bindingName, stateProps.schema,
                                    control.options, stateProps.config,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TableColumnResizeHandle(
                                column = column,
                                measuredPixels = measuredWidths[column.name],
                                resizedWidth = resizedWidths[column.name],
                                translator = translator,
                                onResize = { resizedWidths[column.name] = it },
                            )
                        }
                    }
                    if (showTableActions) Box(modifier = Modifier.width(tableActionWidth))
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

            visibleIndices.forEach { index ->
                key(rowIds[index]) {
                if (index > pageWindow.start) HorizontalDivider()
                val itemPath = composePaths(stateProps.path, index.toString())

                if (renderAsTable) {
                    TableScrollRow(
                        tableMinimumWidth, tableScroll, "table-row-scroll-$index",
                        isCurrent = panelRowDetail != null && panelVisible && panelSelection?.index == index,
                        currentLabel = rowDetailText("rowDetail.currentRow", "Current detail row"),
                        onSelect = if (panelRowDetail != null) {{ selectPanel(index) }} else null,
                    ) {
                        columns.forEach { column ->
                            val cellPath = if (column.bindingName == null) {
                                itemPath
                            } else {
                                composePaths(itemPath, column.bindingName)
                            }
                            Box(modifier = columnModifier(column, resizedWidths[column.name])
                                .clipToBounds()
                                .then(if (panelRowDetail != null) Modifier.onFocusChanged {
                                    if (it.hasFocus) selectPanel(index)
                                } else Modifier)) {
                                val cellId = "${stateProps.id}-$index-${column.name}"
                                val context = TesterContext(rootSchema = stateProps.rootSchema, config = props.config)
                                val cellJson = ((control.options["cells"] as? JsonObject)
                                    ?.get(column.name) as? JsonObject)
                                // §18.24 keeps OTP passwords as ordinary masked inputs inside cells.
                                val cellPresentation = if (cellJson?.get("variant") == JsonPrimitive("otp"))
                                    cellJson - "variant" else cellJson
                                val cellUiSchema = if (cellJson == null) CellUiSchema
                                    else CellUiSchema.copy(options = cellPresentation ?: emptyMap())
                                val cellOptions = resolveArrayTableCellOptions(control.options, column.name)
                                val boundValue = if (column.bindingName == null) items[index]
                                    else (items[index] as? JsonObject)?.get(column.bindingName)
                                val cellSchema = arrayColumnSchemaForRow(
                                    column, stateProps.schema, stateProps.rootSchema, boundValue, items[index])
                                if (cellSchema == null) {
                                    // This authored column belongs to another object alternative.
                                    // Preserve any imported value as read-only JSON for inspection.
                                    if (boundValue != null) Text(boundValue.toString())
                                } else {
                                val complex = isComplexTableCellSchema(cellSchema, boundValue)
                                var bestCellRank = -1
                                var cellOwnsValidationFeedback = false
                                props.cells.forEach { entry ->
                                    val rank = entry.tester(cellUiSchema, cellSchema, context)
                                    if (rank > bestCellRank) {
                                        bestCellRank = rank
                                        cellOwnsValidationFeedback = entry.ownsValidationFeedback
                                    }
                                }
                                val hasCellRenderer = bestCellRank >= 0
                                // A registered cell editor owns its compact form unless an authored presentation overrides it.
                                val presented = (complex && column.bindingName != null && !hasCellRenderer) ||
                                    cellOptions?.summary != null ||
                                    cellOptions?.detail != null
                                if (presented) {
                                    val labelSummary = cellOptions?.summary as? LabelElement
                                    val interpolated = (labelSummary?.options?.get("interpolate") as? JsonPrimitive)
                                        ?.booleanOrNull == true
                                    val resolvedLabel = if (labelSummary != null && interpolated)
                                        resolveTableLabelSummary(
                                            deriveLabelForUISchemaElement(labelSummary,
                                                translator ?: defaultTranslator).orEmpty(),
                                            labelSummary.options["textParams"] as? JsonObject,
                                            props.state.jsonforms.core.data, items[index], stateProps.config,
                                        ) else null
                                    val cellJson = ((control.options["cells"] as? JsonObject)
                                        ?.get(column.name) as? JsonObject)
                                    val cellActions = resolveCompositeDialogActions(
                                        cellJson, control.options, stateProps.config)
                                    val clearValue = boundValue?.takeIf { it is JsonObject || it is JsonArray }
                                    val clearField = column.bindingName?.takeIf {
                                        showActions && !dialogOpen && cellActions.clearable && clearValue != null &&
                                            (labelSummary == null || cellOptions?.detail != null)
                                    }
                                    val clearLabel = clearField?.let {
                                        val fallback = if (props.state.jsonforms.i18n.locale
                                                ?.replace('_', '-')?.substringBefore('-')
                                                ?.equals("bg", ignoreCase = true) == true)
                                            "Изчисти ${column.title}" else "Clear ${column.title}"
                                        translator?.translate("composite.clear", fallback,
                                            mapOf("label" to column.title)) ?: fallback
                                    }
                                    val typeMarker = if (resolveArrayTableCellTypeIndicator(
                                            cellJson, control.options, stateProps.config)) when (boundValue) {
                                        is JsonObject -> "{}"
                                        is JsonArray -> "[]"
                                        else -> null
                                    } else null
                                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                                    val narrowCell = maxWidth < 160.dp
                                    Column(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (typeMarker != null) Text(typeMarker,
                                            modifier = Modifier.padding(end = 4.dp).clearAndSetSemantics {
                                                testTag = "composite-type-$index-${column.name}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Box(Modifier.weight(1f).clipToBounds()) {
                                            if (labelSummary != null && (!interpolated || resolvedLabel != null)) {
                                                JsonFormsDispatchRenderer(props.copy(
                                                    schema = cellSchema,
                                                    uischema = if (resolvedLabel == null) labelSummary else
                                                        labelSummary.copy(text = resolvedLabel, i18n = null,
                                                            options = emptyMap()),
                                                    path = cellPath, id = "$cellId-summary",
                                                ))
                                            } else {
                                                val previewTranslator = Translator { key, fallback, values ->
                                                    translateRendererText(translator,
                                                        props.state.jsonforms.i18n.locale, key, fallback.orEmpty(),
                                                        arrayRendererLocaleBundles, values)
                                                }
                                                val preview = arrayTableCellPreviewResult(boundValue,
                                                    cellOptions?.summary as? ControlElement,
                                                    previewTranslator, cellSchema)
                                                Text(
                                                    preview.text,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontStyle = if (preview.generated) FontStyle.Italic else FontStyle.Normal),
                                                    color = if (preview.generated) MaterialTheme.colorScheme.onSurfaceVariant
                                                        else MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                        }
                                        val cellValidation = resolveContainerValidationOptions(
                                            cellJson ?: emptyMap(), stateProps.config,
                                            defaultIndicator = true,
                                        )
                                        val cellErrors = tableCellErrors(
                                            groupedErrors.byIndex[index].orEmpty(), cellPath,
                                            cellOptions?.detail, labelSummary != null,
                                        )
                                        if (cellErrors.isNotEmpty() && cellValidation.showIndicator) {
                                            val count = cellErrors.size.takeIf { cellValidation.showCount }
                                            ErrorIndicator(localizedCellErrors(cellErrors), count,
                                                translateCompositeValidationIndicator(
                                                    props.state.jsonforms.i18n, column.title, count),
                                                includeMessageInName = true)
                                        }
                                        if ((cellOptions?.detail != null ||
                                                labelSummary == null && column.bindingName != null && complex) &&
                                            (stateProps.enabled || stateProps.readonly)) {
                                            val editLabel = translator?.translate("composite.edit",
                                                "Edit ${column.title}", mapOf("label" to column.title))
                                                ?: "Edit ${column.title}"
                                            ActionTooltip(editLabel) {
                                                OutlinedIconButton(enabled = !pendingEditor && !dialogOpen, onClick = {
                                                    if (pendingEditor || dialogOpen) return@OutlinedIconButton
                                                    rowDetailSession = null
                                                    cellDetailPending = false
                                                    cellDetailSession = CellDetailDraftSession(
                                                        index = index,
                                                        columnName = column.name,
                                                        field = column.bindingName,
                                                        title = column.title,
                                                        schema = cellSchema,
                                                        detail = cellOptions?.detail ?: CellUiSchema.copy(scope = "#"),
                                                        dialog = cellOptions?.dialog,
                                                        snapshot = itemsArray,
                                                        originalRow = items[index],
                                                        root = props.state.jsonforms.core.data,
                                                    )
                                                }) { Icon(EditCellIcon, contentDescription = editLabel) }
                                            }
                                        }
                                        if (!narrowCell && clearField != null && clearValue != null && clearLabel != null) {
                                            CompositeCellClearButton(clearLabel, !pendingEditor) {
                                                requestCellRemoval(index, clearField, clearValue)
                                            }
                                        }
                                    }
                                        if (narrowCell && clearField != null && clearValue != null && clearLabel != null) {
                                            CompositeCellClearButton(clearLabel, !pendingEditor) {
                                                requestCellRemoval(index, clearField, clearValue)
                                            }
                                        }
                                    }
                                    }
                                } else if (hasCellRenderer) {
                                    val cellValidation = resolveContainerValidationOptions(
                                        cellJson ?: emptyMap(), stateProps.config,
                                        defaultIndicator = true,
                                    )
                                    val cellErrors = tableCellErrors(
                                        groupedErrors.byIndex[index].orEmpty(), cellPath,
                                        detail = null, labelSummary = false,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f).clipToBounds()) {
                                            DispatchCell(ComposeJsonFormsCellProps(
                                                state = props.state,
                                                dispatch = props.dispatch,
                                                uischema = cellUiSchema,
                                                schema = cellSchema,
                                                rootSchema = stateProps.rootSchema,
                                                path = cellPath,
                                                enabled = backgroundCellsEnabled,
                                                renderers = props.renderers,
                                                cells = props.cells,
                                                id = cellId,
                                                config = props.config,
                                            ))
                                        }
                                        if (!cellOwnsValidationFeedback && cellErrors.isNotEmpty() &&
                                            cellValidation.showIndicator) {
                                            val count = cellErrors.size.takeIf { cellValidation.showCount }
                                            ErrorIndicator(localizedCellErrors(cellErrors), count,
                                                translateCompositeValidationIndicator(
                                                    props.state.jsonforms.i18n, column.title, count),
                                                includeMessageInName = true)
                                        }
                                    }
                                } else {
                                    val cellJson = ((control.options["cells"] as? JsonObject)
                                        ?.get(column.name) as? JsonObject)
                                    val cellValidation = resolveContainerValidationOptions(
                                        cellJson ?: emptyMap(), stateProps.config,
                                        defaultIndicator = true,
                                    )
                                    val cellErrors = tableCellErrors(
                                        groupedErrors.byIndex[index].orEmpty(), cellPath,
                                        detail = null, labelSummary = false,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f).clipToBounds()) {
                                            JsonFormsDispatchRenderer(props.copy(
                                                schema = cellSchema,
                                                rootSchema = stateProps.rootSchema,
                                                uischema = cellUiSchema,
                                                path = cellPath,
                                                enabled = backgroundCellsEnabled,
                                                id = cellId,
                                            ))
                                        }
                                        if (cellErrors.isNotEmpty() && cellValidation.showIndicator) {
                                            val count = cellErrors.size.takeIf { cellValidation.showCount }
                                            ErrorIndicator(localizedCellErrors(cellErrors), count,
                                                translateCompositeValidationIndicator(
                                                    props.state.jsonforms.i18n, column.title, count),
                                                includeMessageInName = true)
                                        }
                                    }
                                }
                                }
                            }
                        }

                        if (showTableActions) {
                            Row(Modifier.width(tableActionWidth), verticalAlignment = Alignment.CenterVertically) {
                                if (showDetailAction) {
                                    val detailFocus = remember(stateProps.path, index) { FocusRequester() }
                                    TextButton(
                                        modifier = Modifier.width(RowDetailActionWidth).focusRequester(detailFocus),
                                        enabled = !pendingEditor && !dialogOpen,
                                        onClick = {
                                            rowDetailActiveFocus = detailFocus
                                            revealRowDetail(index)
                                        },
                                    ) { Text(if (stateProps.readonly) viewDetailsText else editDetailsText) }
                                    if (showRowErrorMarkers) {
                                        val rowErrors = groupedErrors.byIndex[index].orEmpty()
                                        if (rowErrors.isNotEmpty()) {
                                            val count = rowErrors.size
                                            ErrorIndicator(rowErrors, count,
                                                translateRowDetailValidationIndicator(
                                                    props.state.jsonforms.i18n, index + 1, count))
                                        }
                                    }
                                }
                                if (showActions) ArrayItemActions(
                                modifier = Modifier.width(actionColumnWidth),
                                index = index,
                                lastIndex = items.lastIndex,
                                showSortButtons = showSortButtons,
                                canMove = backgroundActionsEnabled,
                                upTooltip = upText.tooltip,
                                upAriaLabel = upText.ariaLabel,
                                downTooltip = downText.tooltip,
                                downAriaLabel = downText.ariaLabel,
                                removeTooltip = removeText.tooltip,
                                removeAriaLabel = removeText.ariaLabel,
                                canRemove = !dialogOpen && canRemove && canRemoveAt(index),
                                onMoveUp = { move(index, index - 1) },
                                onMoveDown = { move(index, index + 1) },
                                onRemove = { requestRemoval(index) },
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Item ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (showActions) {
                                ArrayItemActions(
                                    index = index,
                                    lastIndex = items.lastIndex,
                                    showSortButtons = showSortButtons,
                                    canMove = backgroundActionsEnabled,
                                    upTooltip = upText.tooltip,
                                    upAriaLabel = upText.ariaLabel,
                                    downTooltip = downText.tooltip,
                                    downAriaLabel = downText.ariaLabel,
                                    removeTooltip = removeText.tooltip,
                                    removeAriaLabel = removeText.ariaLabel,
                                    canRemove = !dialogOpen && canRemove && canRemoveAt(index),
                                    onMoveUp = { move(index, index - 1) },
                                    onMoveDown = { move(index, index + 1) },
                                    onRemove = { requestRemoval(index) },
                                )
                            }
                        }
                        JsonFormsDispatchRenderer(
                            props = props.copy(
                                schema = stateProps.schema,
                                rootSchema = stateProps.rootSchema,
                                uischema = CellUiSchema,
                                path = itemPath,
                                enabled = stateProps.enabled,
                                id = "${stateProps.id}-$index",
                            ),
                        )
                    }
                }
                }
            }
            MaterialCollectionPager(
                pagination, pageWindow, pageSize, items.size, errorPages, translator,
                canNavigate = !pendingEditor && !dialogOpen,
                onGoToFirstError = firstErrorIndex?.let { index ->
                    {
                        if (!pendingEditor && !dialogOpen) {
                            currentPage = index / pageSize + 1
                            revealRowDetail(index)
                        }
                    }
                },
                onChange = { requestedPage, requestedSize ->
                    if (pendingEditor || dialogOpen) return@MaterialCollectionPager
                    currentPage = if (requestedSize == pageSize) requestedPage else
                        pageForFirstVisibleItem(pageWindow.start, requestedSize)
                    pageSize = requestedSize
                },
            )
        }
    }
    }
    if (panelRowDetail != null && panelVisible) {
        val resizeLabel = rowDetailText("rowDetail.resize", "Resize table and details")
        val detail: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                ArrayRowDetailPanel(
                    props, stateProps, control, panelRowDetail,
                    panelSelection?.index, panelSelectionGeneration, items,
                )
            }
        }
        if (panelRowDetail.placement == ArrayRowDetailPlacement.Bottom) {
            TableBottomSplitPane(
                identity = stateProps.path to stateProps.schema,
                resizeLabel = resizeLabel,
                resizable = panelRowDetail.resizable,
                tableHeightDp = panelBottomHeight,
                onTableHeightChange = { panelBottomHeight = it },
                table = { modifier -> tableContent(modifier) },
                detail = detail,
            )
        } else {
            ListDetailSplitPane(
                identity = stateProps.path to stateProps.schema,
                resizeLabel = resizeLabel,
                resizable = panelRowDetail.resizable,
                fraction = panelRightFraction,
                onFractionChange = { panelRightFraction = it },
                list = { modifier ->
                    Box(modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
                        tableContent(Modifier.fillMaxWidth())
                    }
                },
                detail = { modifier ->
                    Box(modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
                        detail(Modifier.fillMaxWidth())
                    }
                },
            )
        }
    } else tableContent(Modifier.fillMaxWidth())
    val openDetail = rowDetailSession?.takeIf { dialogRowDetail != null && it.snapshot === itemsArray }
    if (openDetail != null) {
        val itemPath = composePaths(stateProps.path, openDetail.index.toString())
        val pointer = "#/" + itemPath.split('.').joinToString("/") { JsonPointer.encode(it) }
        val detailUiSchema = remember(stateProps.schema, stateProps.rootSchema, control.scope,
            control.options["rowDetail"], stateProps.uischemas, itemPath) {
            dialogRowDetail?.detail
                ?: findMatchingUISchema(stateProps.uischemas)(stateProps.schema, control.scope, itemPath)
                ?: Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema)
        }
        val title = rowDetailText("rowDetail.title", "Item ${openDetail.index + 1} details",
            mapOf("index" to openDetail.index + 1))
        val applyLabel = rowDetailText("rowDetail.apply", "Apply")
        val cancelLabel = if (stateProps.readonly) rowDetailText("rowDetail.cancel", "Close",
            bundleKey = "rowDetail.close")
            else rowDetailText("rowDetail.cancel", "Cancel")
        val dialogOptions = resolveDetailDialogOptions(dialogRowDetail?.dialog, stateProps.config)
        rowDetailDialog(
            title,
            {
                IsolatedDetailDraftForm(
                    props = props.copy(rootSchema = stateProps.rootSchema, enabled = showActions),
                    rootData = openDetail.root,
                    positionSchema = stateProps.schema,
                    positionPath = itemPath,
                    detail = detailUiSchema,
                    onChange = { changed ->
                        if (rowDetailSession === openDetail) rowDetailSession = openDetail.copy(root = changed)
                    },
                    onPendingChange = { pending ->
                        if (rowDetailSession?.snapshot === openDetail.snapshot &&
                            rowDetailSession?.index == openDetail.index) rowDetailPending = pending
                    },
                )
            },
            applyLabel,
            cancelLabel,
            null,
            null,
            showActions && !rowDetailPending,
            dialogOptions,
            rowDetailText("rowDetail.maximize", "Maximize"),
            rowDetailText("rowDetail.restore", "Restore"),
            rowDetailText("rowDetail.close", "Close"),
            rowDetailText("rowDetail.resizeDialog", "Resize dialog"),
            {
                val current = rowDetailSession
                if (current != null && current.snapshot === itemsArray && showActions && !rowDetailPending &&
                    current.index in items.indices && items[current.index] === current.original) {
                    val edited = JsonPointer.resolve(current.root, pointer)
                    if (edited != null && edited != current.original) {
                        props.dispatch(UpdateAction(stateProps.path, updater = { existing ->
                            if (existing !== current.snapshot) existing
                            else JsonArray(existing.mapIndexed { index, value ->
                                if (index == current.index) edited else value
                            })
                        }))
                    }
                }
                rowDetailSession = null
                rowDetailReturnFocus = rowDetailActiveFocus
            },
            {
                rowDetailSession = null
                rowDetailReturnFocus = rowDetailActiveFocus
            },
        )
    }
    val openCell = cellDetailSession?.takeIf { it.snapshot === itemsArray && it.index in items.indices }
    if (openCell != null && pendingCellRemoval == null) {
        val liveCellOptions = ((control.options["cells"] as? JsonObject)
            ?.get(openCell.columnName) as? JsonObject)
        val actions = resolveCompositeDialogActions(liveCellOptions,
            control.options, stateProps.config)
        val itemPath = composePaths(stateProps.path, openCell.index.toString())
        val cellPath = openCell.field?.let { composePaths(itemPath, it) } ?: itemPath
        val rowPointer = "#/" + itemPath.split('.').joinToString("/") { JsonPointer.encode(it) }
        val title = translator?.translate("composite.dialog.title", "${openCell.title} details",
            mapOf("label" to openCell.title)) ?: "${openCell.title} details"
        fun actionLabel(explicit: String?, key: String, fallback: String): String =
            if (explicit != null) translator?.translate(explicit, explicit, emptyMap()) ?: explicit
            else translateRendererText(translator, props.state.jsonforms.i18n.locale,
                key, fallback, arrayRendererLocaleBundles)
        val applyLabel = actionLabel(actions.okLabel, "composite.apply", "Apply")
        val cancelLabel = actionLabel(actions.cancelLabel,
            if (stateProps.readonly) "composite.close" else "composite.cancel",
            if (stateProps.readonly) "Close" else "Cancel")
        val draftValue = JsonPath.get(openCell.root, cellPath)
        val emptyValue = when (draftValue) {
            is JsonObject -> JsonObject(emptyMap())
            is JsonArray -> JsonArray(emptyList())
            else -> null
        }
        val canClearDraft = showActions && actions.clearable && openCell.field != null &&
            emptyValue != null && canEmptyCompositeDraft(draftValue, openCell.schema,
                stateProps.rootSchema, restrict, props.state.jsonforms.core.validator,
                openCell.root, cellPath)
        val canRemoveDraft = showActions && actions.clearable && openCell.field != null &&
            draftValue != null
        fun changeDraft(value: JsonElement?) {
            val current = cellDetailSession
            if (current == null || current.revision != openCell.revision ||
                current.snapshot !== itemsArray || !showActions || !actions.clearable ||
                current.field == null) return
            val currentValue = JsonPath.get(current.root, cellPath)
            if (value == null && currentValue == null) return
            if (value != null && !canEmptyCompositeDraft(currentValue, current.schema,
                    stateProps.rootSchema, restrict, props.state.jsonforms.core.validator,
                    current.root, cellPath)) return
            cellDetailPending = false
            cellDetailSession = current.copy(
                root = JsonPath.set(current.root, cellPath, value, stateProps.rootSchema),
                revision = current.revision + 1,
            )
        }
        rowDetailDialog(
            title,
            {
                Column {
                    if (actions.showEmpty && canClearDraft ||
                        actions.showRemove && canRemoveDraft) {
                        Row {
                            if (actions.showEmpty && canClearDraft) {
                                val label = actionLabel(actions.emptyLabel,
                                    "composite.empty", "Clear")
                                ActionTooltip(actionLabel(null, "composite.emptyTooltip",
                                    "Clear contents, keeping the object or array.")) {
                                    TextButton(enabled = draftValue != emptyValue,
                                        onClick = { changeDraft(emptyValue) }) { Text(label) }
                                }
                            }
                            if (actions.showRemove && canRemoveDraft) {
                                val label = actionLabel(actions.removeLabel,
                                    "composite.remove", "Remove")
                                ActionTooltip(actionLabel(null, "composite.removeTooltip",
                                    "Remove this value from the form data.")) {
                                    TextButton(onClick = { changeDraft(null) }) {
                                        Text(label, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    key(openCell.revision) {
                        IsolatedDetailDraftForm(
                            props = props.copy(rootSchema = stateProps.rootSchema, enabled = showActions),
                            rootData = openCell.root,
                            positionSchema = openCell.schema,
                            positionPath = cellPath,
                            itemPath = itemPath,
                            detail = openCell.detail,
                            onChange = { changed ->
                                if (cellDetailSession === openCell) cellDetailSession = openCell.copy(root = changed)
                            },
                            onPendingChange = { pending ->
                                if (cellDetailSession?.snapshot === openCell.snapshot &&
                                    cellDetailSession?.index == openCell.index &&
                                    cellDetailSession?.field == openCell.field &&
                                    cellDetailSession?.revision == openCell.revision) cellDetailPending = pending
                            },
                        )
                    }
                }
            },
            applyLabel,
            cancelLabel,
            actionLabel(null, "composite.applyTooltip", "Apply changes and close."),
            actionLabel(null, "composite.cancelTooltip", "Discard changes and close."),
            showActions && !cellDetailPending,
            resolveDetailDialogOptions(openCell.dialog, stateProps.config),
            rowDetailText("rowDetail.maximize", "Maximize"),
            rowDetailText("rowDetail.restore", "Restore"),
            rowDetailText("rowDetail.close", "Close"),
            rowDetailText("rowDetail.resizeDialog", "Resize dialog"),
            {
                val current = cellDetailSession
                if (current != null && current.snapshot === itemsArray && showActions && !cellDetailPending &&
                    current.index in items.indices && items[current.index] === current.originalRow) {
                    val editedRow = JsonPointer.resolve(current.root, rowPointer)
                    val replacement = if (current.field == null) editedRow else {
                        val originalObject = current.originalRow as? JsonObject
                        val editedObject = editedRow as? JsonObject
                        if (originalObject == null || editedObject == null) null else {
                            val value = editedObject[current.field]
                            JsonObject(originalObject.toMutableMap().apply {
                                if (value == null) remove(current.field) else put(current.field, value)
                            })
                        }
                    }
                    if (replacement != null && replacement != current.originalRow) {
                        val oldCell = current.field?.let { (current.originalRow as? JsonObject)?.get(it) }
                        val newCell = current.field?.let { (replacement as? JsonObject)?.get(it) }
                        val removingComposite = current.field != null &&
                            (oldCell is JsonObject || oldCell is JsonArray) &&
                            (newCell == null || newCell == JsonNull)
                        val decision = if (removingComposite) resolveConfirmationDecision(
                            ConfirmationOperation.Delete, "compositeCell", listOf(oldCell),
                            control.options, stateProps.config,
                        ) else null
                        if (decision?.required == true) {
                            pendingCellRemoval = PendingCompositeCellRemoval(
                                current.index, requireNotNull(current.field), current.snapshot,
                                current.originalRow as JsonObject, requireNotNull(oldCell),
                                replacement as JsonObject,
                            )
                        } else {
                            props.dispatch(UpdateAction(stateProps.path, updater = { existing ->
                                if (existing !== current.snapshot) existing
                                else JsonArray(existing.mapIndexed { index, value ->
                                    if (index == current.index) replacement else value
                                })
                            }))
                        }
                    }
                }
                if (pendingCellRemoval == null) cellDetailSession = null
            },
            { cellDetailSession = null },
        )
    }
    val request = pendingRemoval?.takeIf {
        canRemove && it.snapshot === itemsArray && it.index in items.indices && canRemoveAt(it.index)
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
    val cellRemoval = pendingCellRemoval?.takeIf { request ->
        showActions && !pendingEditor && request.snapshot === itemsArray &&
            request.index in items.indices && items[request.index] === request.originalRow &&
            (items[request.index] as? JsonObject)?.get(request.field) === request.value
    }
    if (cellRemoval != null) {
        fun translated(key: String, fallback: String) =
            translateConfirmationText(props.state.jsonforms.i18n, key, fallback)
        confirmationDialog(
            translated("confirm.delete.title", "Delete this value?"),
            translated("confirm.delete.message", "The value will be removed. This cannot be undone from here."),
            translated("confirm.accept", "Yes"),
            translated("confirm.decline", "No"),
            {
                if (pendingCellRemoval === cellRemoval) removeCell(cellRemoval)
                if (pendingCellRemoval === cellRemoval) pendingCellRemoval = null
            },
            { if (pendingCellRemoval === cellRemoval) pendingCellRemoval = null },
        )
    }
}

private data class PendingTableRemoval(val index: Int, val snapshot: JsonArray)
private data class PendingCompositeCellRemoval(
    val index: Int,
    val field: String,
    val snapshot: JsonArray,
    val originalRow: JsonObject,
    val value: JsonElement,
    val replacementRow: JsonObject? = null,
)
private data class RowDetailDraftSession(
    val index: Int,
    val snapshot: JsonArray,
    val original: JsonElement,
    val root: JsonElement,
)
private data class CellDetailDraftSession(
    val index: Int,
    val columnName: String,
    val field: String?,
    val title: String,
    val schema: JsonSchema,
    val detail: UiSchemaElement,
    val dialog: JsonObject?,
    val snapshot: JsonArray,
    val originalRow: JsonElement,
    val root: JsonElement,
    val revision: Int = 0,
)
private data class PendingPanelChange(val result: JsonArray, val selectedIndex: Int?)

@Composable
private fun ArrayRowDetailPanel(
    props: ComposeJsonFormsRendererProps,
    stateProps: StatePropsOfArrayControl,
    control: ControlElement,
    rowDetail: ArrayRowDetailOptions,
    selectedIndex: Int?,
    selectionGeneration: Int,
    items: List<JsonElement>,
) {
    val translator = props.state.jsonforms.i18n.translate
    fun rowDetailText(key: String, fallback: String) =
        translateRowDetailText(translator, props.state.jsonforms.i18n.locale, key, fallback)
    if (selectedIndex == null || selectedIndex !in items.indices) {
        Text(
            rowDetailText("rowDetail.noSelection", "No selection"),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        return
    }
    val itemPath = composePaths(stateProps.path, selectedIndex.toString())
    val detail = remember(stateProps.schema, stateProps.rootSchema, control.scope,
        rowDetail, stateProps.uischemas, itemPath) {
        rowDetail.detail
            ?: findMatchingUISchema(stateProps.uischemas)(stateProps.schema, control.scope, itemPath)
            ?: Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema)
    }
    key(selectionGeneration) {
        JsonFormsItemScope(items[selectedIndex]) {
            JsonFormsDispatchRenderer(props.copy(
                schema = stateProps.schema,
                rootSchema = stateProps.rootSchema,
                uischema = detail,
                path = itemPath,
                enabled = stateProps.enabled,
                id = "${stateProps.id}-row-detail-$selectionGeneration",
            ))
        }
    }
}

@Composable
private fun ArrayItemActions(
    index: Int,
    lastIndex: Int,
    showSortButtons: Boolean,
    canMove: Boolean,
    upTooltip: String,
    upAriaLabel: String,
    downTooltip: String,
    downAriaLabel: String,
    removeTooltip: String,
    removeAriaLabel: String,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSortButtons) {
            ActionTooltip(upTooltip) {
                OutlinedIconButton(enabled = canMove && index > 0, onClick = onMoveUp) {
                    Icon(ChevronUpIcon, contentDescription = upAriaLabel)
                }
            }
            ActionTooltip(downTooltip) {
                OutlinedIconButton(enabled = canMove && index < lastIndex, onClick = onMoveDown) {
                    Icon(ChevronDownIcon, contentDescription = downAriaLabel)
                }
            }
        }
        ActionTooltip(removeTooltip) {
            FilledIconButton(
                enabled = canRemove,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                onClick = onRemove,
            ) {
                Icon(DeleteIcon, contentDescription = removeAriaLabel)
            }
        }
    }
}

private data class ArrayColumn(
    val name: String,
    val bindingName: String?,
    val title: String,
    val schema: JsonSchema,
    val alternativeOnly: Boolean = false,
    val width: Float? = null,
    val minWidth: Float? = null,
    val maxWidth: Float? = null,
)

private fun ArrayColumn.initialWidth(): Float? {
    val minimum = minWidth ?: 0f
    val maximum = maxWidth?.coerceAtLeast(minimum)
    return width?.coerceAtLeast(minimum)?.let { width ->
        maximum?.let(width::coerceAtMost) ?: width
    }
}

private fun RowScope.columnModifier(column: ArrayColumn, resizedWidth: Float?): Modifier {
    val fixed = resizedWidth ?: column.initialWidth()
    val minimum = maxOf(column.minWidth ?: 0f, 160f)
        .coerceAtMost(column.maxWidth ?: Float.MAX_VALUE)
    val maximum = column.maxWidth?.coerceAtLeast(minimum)
    return if (fixed != null) Modifier.width(fixed.dp)
    else Modifier.weight(1f).widthIn(
        min = minimum.dp,
        max = maximum?.dp ?: Dp.Infinity,
    )
}

@Composable
private fun TableScrollRow(
    minimumWidth: Dp,
    scrollState: ScrollState,
    tag: String,
    isCurrent: Boolean = false,
    currentLabel: String = "Current detail row",
    onSelect: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewportWidth = maxWidth
        Box(Modifier.fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .horizontalScroll(scrollState)
            .then(if (onSelect != null) Modifier.clickable(onClick = onSelect) else Modifier)
            .semantics {
                if (onSelect != null) selected = isCurrent
                if (isCurrent) {
                    stateDescription = currentLabel
                }
            }
            .testTag(tag)) {
            Row(
                modifier = Modifier.width(maxOf(viewportWidth, minimumWidth))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
private fun TableColumnResizeHandle(
    column: ArrayColumn,
    measuredPixels: Float?,
    resizedWidth: Float?,
    translator: com.chobantonov.jsonfroms.core.store.Translator?,
    onResize: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val minimum = column.minWidth ?: 80f
    val maximum = (column.maxWidth ?: 1200f).coerceAtLeast(minimum)
    val measured = measuredPixels?.let { with(density) { it.toDp().value } }
    val current = (resizedWidth ?: measured ?: column.initialWidth() ?: 160f).coerceIn(minimum, maximum)
    val directionSign = if (direction == LayoutDirection.Ltr) 1f else -1f
    fun resizeTo(width: Float) { onResize(width.coerceIn(minimum, maximum)) }
    val dragState = rememberDraggableState { delta ->
        resizeTo(current + directionSign * with(density) { delta.toDp().value })
    }
    val title = column.title.ifBlank { column.name.ifBlank { "Value" } }
    val label = translator?.translate("array.columnResize", "Resize $title column", mapOf("column" to title))
        ?: "Resize $title column"
    val state = translator?.translate("array.columnWidthState",
        "${current.toInt()} px, minimum ${minimum.toInt()} px, maximum ${maximum.toInt()} px",
        mapOf("current" to current.toInt(), "min" to minimum.toInt(), "max" to maximum.toInt()))
        ?: "${current.toInt()} px, minimum ${minimum.toInt()} px, maximum ${maximum.toInt()} px"
    Box(
        Modifier.width(16.dp).height(32.dp)
            .draggable(dragState, Orientation.Horizontal)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> resizeTo(current - directionSign * 16f)
                    Key.DirectionRight -> resizeTo(current + directionSign * 16f)
                    Key.MoveHome -> resizeTo(minimum)
                    Key.MoveEnd -> resizeTo(maximum)
                    else -> return@onKeyEvent false
                }
                true
            }
            .semantics {
                contentDescription = label
                stateDescription = state
                progressBarRangeInfo = ProgressBarRangeInfo(current, minimum..maximum)
                setProgress { requested -> resizeTo(requested); true }
            }
            .focusable()
            .testTag("table-column-resize-${column.name}"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

private fun arrayColumns(
    itemSchema: JsonSchema,
    rootSchema: JsonSchema,
    definitions: List<ArrayTableColumnDef>? = null,
): List<ArrayColumn> {
    val selectable = selectableAlternativeObjectBranches(itemSchema, rootSchema)
    val alternativeColumns = if (definitions != null || selectable != null)
        alternativeObjectColumns(itemSchema, rootSchema) else null
    val properties = (itemSchema["properties"] as? JsonObject)
        ?: if (selectable != null) alternativeColumns?.properties
        else commonAlternativeProperties(itemSchema, rootSchema)
    if (definitions != null) return definitions.mapNotNull { definition ->
        if (definition.wholeRow) return@mapNotNull ArrayColumn(
            name = definition.field,
            bindingName = null,
            title = definition.title ?: startCase(definition.field),
            schema = itemSchema,
            width = definition.width,
            minWidth = definition.minWidth,
            maxWidth = definition.maxWidth,
        )
        val value = (properties ?: alternativeColumns?.properties)
            ?.get(definition.field) as? JsonObject ?: return@mapNotNull null
        val fallback = JsonSchema(value)
        val schema = if (selectable != null && definition.field == selectable.first().discriminatorField)
            fallback else Resolve.schema(
            itemSchema,
            "#/properties/${JsonPointer.encode(definition.field)}",
            rootSchema,
        ) ?: fallback
        ArrayColumn(
            name = definition.field,
            bindingName = definition.field,
            title = definition.title ?: (schema["title"] as? JsonPrimitive)?.content ?: startCase(definition.field),
            schema = schema,
            alternativeOnly = definition.field in alternativeColumns?.branchSpecific.orEmpty(),
            width = definition.width,
            minWidth = definition.minWidth,
            maxWidth = definition.maxWidth,
        )
    }
    if (properties.isNullOrEmpty()) {
        return listOf(ArrayColumn(name = "", bindingName = null, title = "", schema = itemSchema))
    }

    return properties.mapNotNull { (name, value) ->
        val fallback = (value as? JsonObject)?.let(::JsonSchema) ?: return@mapNotNull null
        val schema = if (selectable != null) fallback else Resolve.schema(
            itemSchema,
            "#/properties/${JsonPointer.encode(name)}",
            rootSchema,
        ) ?: fallback
        ArrayColumn(
            name = name,
            bindingName = name,
            title = (schema["title"] as? JsonPrimitive)?.content ?: startCase(name),
            schema = schema,
            alternativeOnly = name in alternativeColumns?.branchSpecific.orEmpty(),
        )
    }.ifEmpty {
        listOf(ArrayColumn(name = "", bindingName = null, title = "", schema = itemSchema))
    }
}

private fun arrayColumnSchemaForRow(
    column: ArrayColumn,
    itemSchema: JsonSchema,
    rootSchema: JsonSchema,
    value: JsonElement?,
    row: JsonElement,
): JsonSchema? {
    val field = column.bindingName ?: return column.schema
    val scope = "#/properties/${JsonPointer.encode(field)}"
    val branch = activeEditingSchemaForScopedProperty(itemSchema, scope, rootSchema, value, row)
        ?: return column.schema.takeUnless { column.alternativeOnly }
    return Resolve.schema(branch, scope, rootSchema) ?: column.schema
}

internal fun shouldRenderArrayItemsAsTable(
    itemSchema: JsonSchema,
    rootSchema: JsonSchema,
): Boolean = arrayColumns(itemSchema, rootSchema).all { column ->
    !isComplexTableCellSchema(column.schema)
}

/** Composed object/array values need the compact summary and delegated detail editor. */
private fun isComplexTableCellSchema(schema: JsonSchema, value: JsonElement? = null): Boolean {
    if (deriveTypes(schema).any { it == "object" || it == "array" }) return true
    return listOf("oneOf", "anyOf").any { keyword ->
        val branches = schema[keyword] as? JsonArray ?: return@any false
        val schemas = branches.mapNotNull(JsonElement::asJsonSchemaOrNull)
        schemas.isNotEmpty() && schemas.size == branches.size && (
            schemas.all { isComplexTableCellSchema(it) } || when (value) {
                is JsonObject -> schemas.any { branchCanBeType(it, "object") }
                is JsonArray -> schemas.any { branchCanBeType(it, "array") }
                else -> false
            }
        )
    }
}

private fun branchCanBeType(schema: JsonSchema, type: String): Boolean {
    if (type in deriveTypes(schema)) return true
    return listOf("oneOf", "anyOf", "allOf").any { keyword ->
        (schema[keyword] as? JsonArray)?.any { branch ->
            branch.asJsonSchemaOrNull()?.let { branchCanBeType(it, type) } == true
        } == true
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
    scope = "",
    label = BooleanControlLabel(false),
)

@Composable
private fun CompositeCellClearButton(label: String, enabled: Boolean, onClear: () -> Unit) {
    ActionTooltip(label) {
        OutlinedIconButton(enabled = enabled, onClick = onClear) {
            Icon(DeleteIcon, contentDescription = label)
        }
    }
}

private val DeleteActionColumnWidth = 52.dp
private val SortAndDeleteActionColumnWidth = 156.dp
private val RowDetailActionWidth = 116.dp

private val EditCellIcon: ImageVector by lazy {
    lineIcon("Edit cell") {
        moveTo(4f, 20f)
        lineTo(8f, 19f)
        lineTo(19f, 8f)
        lineTo(16f, 5f)
        lineTo(5f, 16f)
        close()
        moveTo(14f, 7f)
        lineTo(17f, 10f)
    }
}

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

private val defaultArrayControlTester = rankWith(
    3,
    or(isObjectArrayControl, isPrimitiveArrayControl, isMixedArrayControl),
)
private val explicitTableArrayTester = rankWith(
    5,
    and(optionIs("table", true), or(isObjectArrayControl, isPrimitiveArrayControl, isMixedArrayControl)),
)
private val authoredColumnDefsArrayTester = rankWith(
    5,
    and({ ui, _, _ -> (ui as? ControlElement)?.options?.get("columnDefs") is JsonArray },
        or(isObjectArrayControl, isPrimitiveArrayControl, isMixedArrayControl)),
)
private val agGridArrayFallbackTester = rankWith(
    5,
    and(optionIs("variant", "ag-grid"), or(isObjectArrayControl, isPrimitiveArrayControl, isMixedArrayControl)),
)
val materialArrayControlTester: RankedTester = { ui, schema, context ->
    maxOf(defaultArrayControlTester(ui, schema, context), explicitTableArrayTester(ui, schema, context),
        authoredColumnDefsArrayTester(ui, schema, context), agGridArrayFallbackTester(ui, schema, context))
}
