package com.chobantonov.jsonfroms.cupertino.renderers.additional

import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText

import com.chobantonov.jsonfroms.core.i18n.translateArrayValidationIndicator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.focusable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.JsonFormsItemScope
import com.chobantonov.jsonfroms.compose.rememberInitialControlFocusRequester
import com.chobantonov.jsonfroms.compose.strictControlBooleanOption
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.material.renderers.ActionText
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.material.renderers.layouts.resolveChildLabelProp
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfArrayControl
import com.chobantonov.jsonfroms.core.i18n.translateControlRequired
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.collectionPageWindow
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.ContainsRemovalGuard
import com.chobantonov.jsonfroms.core.mappers.groupArrayErrors
import com.chobantonov.jsonfroms.core.mappers.pageForFirstVisibleItem
import com.chobantonov.jsonfroms.core.mappers.resolveCollectionPagination
import com.chobantonov.jsonfroms.core.mappers.resolveContainerValidationOptions
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.util.resolveDetailUiSchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.additional.CollectionArrayReference
import com.chobantonov.jsonfroms.material.renderers.additional.ListDetailSelection
import com.chobantonov.jsonfroms.material.renderers.additional.ListDetailSplitPane
import com.chobantonov.jsonfroms.material.renderers.additional.computeItemLabel
import com.chobantonov.jsonfroms.material.renderers.additional.reconcileListDetailSelection
import com.chobantonov.jsonfroms.material.renderers.additional.materialListWithDetailTester
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive

val CupertinoListWithDetailRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    CupertinoListWithDetailRenderer(props)
}

@Composable
fun CupertinoListWithDetailRenderer(props: ComposeJsonFormsRendererProps) {
    val listWithDetail = props.uischema as? ListWithDetailElement
        ?: error("CupertinoListWithDetailRenderer can only render ListWithDetail elements")
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

    val itemsArray = stateProps.data as? JsonArray ?: JsonArray(emptyList())
    val items = itemsArray.toList()
    val validationOptions = resolveContainerValidationOptions(
        listWithDetail.options, stateProps.config, defaultIndicator = true,
    )
    val hideSummaryValidation = (listWithDetail.options["hideArraySummaryValidation"] as? JsonPrimitive)
        ?.booleanOrNull ?: (stateProps.config["hideArraySummaryValidation"] as? Boolean ?: false)
    val arrayErrors = remember(stateProps.childErrors, stateProps.path) {
        groupArrayErrors(stateProps.childErrors, stateProps.path)
    }
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
    val upText = translatedActionText(translator, prefix, "up", "upAriaLabel", "Move up", values = values)
    val downText = translatedActionText(translator, prefix, "down", "downAriaLabel", "Move down", values = values)
    fun booleanOption(name: String, default: Boolean): Boolean =
        (listWithDetail.options[name] as? JsonPrimitive)?.booleanOrNull
            ?: when (val global = stateProps.config[name]) {
                is Boolean -> global
                is JsonPrimitive -> global.booleanOrNull ?: default
                else -> default
            }
    val showSortButtons = booleanOption("showSortButtons", false)
    val restrict = booleanOption("restrict", true)
    val actionPolicy = resolveArrayActionPolicy(listWithDetail.options, stateProps.config)
    val canAdd = actionsEnabled && !actionPolicy.disableAdd &&
        (!restrict || stateProps.arraySchema.maxItems?.let { items.size < it } != false)
    val appliedOptions = stateProps.config + listWithDetail.options
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
    val noDataText = translatedText(translator, prefix, "noDataMessage", "No data", values)
    val noSelectionText = translatedText(translator, prefix, "noSelectionMessage", "No selection", values)
    val childLabelProp = resolveChildLabelProp(listWithDetail, stateProps.schema)
    val pagination = resolveCollectionPagination(
        listWithDetail.options, stateProps.config, defaultEnabled = false,
    )
    var pageSize by remember(stateProps.schema, stateProps.path) { mutableStateOf(pagination.pageSize) }
    var currentPage by remember(stateProps.schema, stateProps.path) { mutableStateOf(1) }
    LaunchedEffect(pagination) {
        pageSize = pagination.pageSize
        currentPage = 1
    }
    val pageWindow = collectionPageWindow(items.size, currentPage, pageSize, pagination.enabled)
    val visibleIndices = pageWindow.start until pageWindow.endExclusive
    val errorPages = if (pagination.enabled) arrayErrors.byIndex.entries
        .groupBy({ it.key / pageSize + 1 }, { it.value.size })
        .mapValues { (_, counts) -> counts.sum() }
        .filterKeys { it != pageWindow.page }
    else emptyMap()
    var selection by remember(stateProps.schema, stateProps.path) { mutableStateOf<ListDetailSelection?>(null) }
    var selectionGeneration by remember(stateProps.schema, stateProps.path) { mutableStateOf(0) }
    val selectedIndex = selection?.index
    var previousItems by remember(stateProps.schema, stateProps.path) {
        mutableStateOf(itemsArray, referentialEqualityPolicy())
    }
    var pendingLocalChange by remember(stateProps.path) { mutableStateOf<PendingListChange?>(null) }
    var pendingRemoval by remember(stateProps.path) { mutableStateOf<PendingListRemoval?>(null) }
    LaunchedEffect(CollectionArrayReference(itemsArray), canRemove, props.state.jsonforms.core.data) {
        if (pendingRemoval?.let { !canRemove || it.snapshot !== itemsArray || !canRemoveAt(it.index) } == true)
            pendingRemoval = null
        if (itemsArray !== previousItems) {
            val local = pendingLocalChange
            selection = if (local != null && itemsArray == local.result) {
                if (pagination.enabled && local.addedIndex != null)
                    currentPage = local.addedIndex / pageSize + 1
                local.selectedIndex?.takeIf { it in itemsArray.indices }
                    ?.let { ListDetailSelection(it, itemsArray[it]) }
            } else {
                reconcileListDetailSelection(selection, previousItems, itemsArray)
            }
            pendingLocalChange = null
            previousItems = itemsArray
        }
        currentPage = currentPage.coerceAtMost(
            collectionPageWindow(itemsArray.size, currentPage, pageSize, pagination.enabled).pages,
        )
        if (selection?.index?.let { it !in itemsArray.indices } == true) selection = null
    }
    fun remove(index: Int, snapshot: JsonArray) {
        if (!canRemove || itemsArray !== snapshot || index !in snapshot.indices || !canRemoveAt(index)) return
        val result = JsonArray(snapshot.filterIndexed { position, _ -> position != index })
        pendingLocalChange = PendingListChange(result, selectedIndex.afterRemoval(index))
        props.dispatch(UpdateAction(path = stateProps.path, updater = { current ->
            if (current === snapshot) result else current
        }))
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CupertinoText(
                        computeLabel(
                            stateProps.label,
                            stateProps.required,
                            hideRequiredAsterisk,
                        ),
                    )
                    if (arrayErrors.descendants.isNotEmpty() &&
                        validationOptions.showIndicator && !hideSummaryValidation
                    ) {
                        val count = arrayErrors.descendants.size.takeIf { validationOptions.showCount }
                        CupertinoListErrorIndicator(
                            arrayErrors.descendants,
                            count,
                            translateArrayValidationIndicator(props.state.jsonforms.i18n, count),
                        )
                    }
                }
                visibleDescription?.let {
                    CupertinoText(it)
                }
                stateProps.errors.takeIf { it.isNotBlank() }?.let {
                    CupertinoText(it, modifier = Modifier.semantics { error(it) },
                        color = LocalCupertinoColors.current.destructive)
                }
            }
            ActionTooltip(addText.tooltip) {
                CupertinoButton(
                    enabled = canAdd,
                    colors = CupertinoButtonDefaults.filledButtonColors(),
                    modifier = Modifier.focusRequester(addFocusRequester)
                        .onFocusChanged { addFocused = it.isFocused }
                        .semantics {
                            if (accessibleState.isNotBlank()) stateDescription = accessibleState
                            if (stateProps.errors.isNotBlank()) error(stateProps.errors)
                        },
                    onClick = {
                        if (!canAdd) return@CupertinoButton
                        val initial = createDefaultValue(stateProps.schema, stateProps.rootSchema)
                        selectionGeneration++
                        pendingLocalChange = PendingListChange(JsonArray(items + initial), items.size, items.size)
                        dispatchProps.addItem(
                            stateProps.path,
                            initial,
                        )()
                    },
                ) { Icon(AddIcon, contentDescription = addText.ariaLabel) }
            }
        }

        ListDetailSplitPane(
            identity = stateProps.schema to stateProps.path,
            dividerColor = LocalCupertinoColors.current.separator,
            resizeLabel = translatedText(
                translator, prefix, "resizeAriaLabel", "Resize list and detail panes", values,
            ),
            list = { listModifier ->
                val onSelect: (Int) -> Unit = { index ->
                    if (selection?.index != index) selectionGeneration++
                    selection = ListDetailSelection(index, itemsArray[index])
                }
            val remove: (Int) -> Unit = { index ->
                if (canRemove && index in items.indices && canRemoveAt(index)) {
                    val snapshot = itemsArray
                    val decision = resolveConfirmationDecision(
                        ConfirmationOperation.Delete, "arrayLayout", listOf(items[index]),
                        listWithDetail.options, stateProps.config,
                    )
                    if (decision.required) pendingRemoval = PendingListRemoval(index, snapshot)
                    else remove(index, snapshot)
                }
            }
            val move: (Int, Int) -> Unit = { from, to ->
                if (actionsEnabled && from in items.indices && to in items.indices) {
                    val result = items.toMutableList().apply { add(to, removeAt(from)) }
                    val nextSelectedIndex = selectedIndex.swap(from, to)
                    pendingLocalChange = PendingListChange(JsonArray(result), nextSelectedIndex)
                    if (to < from) dispatchProps.moveUp?.invoke(stateProps.path, from)?.invoke()
                    else dispatchProps.moveDown?.invoke(stateProps.path, from)?.invoke()
                    selection = selection?.let { selected ->
                        nextSelectedIndex?.let { selected.copy(index = it) }
                    }
                }
            }
                Column(listModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ItemList(
                        Modifier.fillMaxWidth(), items, visibleIndices, childLabelProp, selectedIndex,
                        actionsEnabled, { index -> canRemove && canRemoveAt(index) },
                        showSortButtons, upText, downText, noDataText,
                        removeText, arrayErrors.byIndex, validationOptions.showIndicator,
                        validationOptions.showCount, props.state.jsonforms.i18n,
                        onSelect = onSelect, onRemove = remove, onMove = move,
                    )
                    CupertinoCollectionPager(
                        pagination, pageWindow, pageSize, items.size, errorPages, translator,
                        onChange = { requestedPage, requestedSize ->
                            currentPage = if (requestedSize == pageSize) requestedPage else
                                pageForFirstVisibleItem(pageWindow.start, requestedSize)
                            pageSize = requestedSize
                        },
                    )
                }
            },
            detail = { detailModifier ->
                Box(detailModifier) {
                    DetailPane(props, stateProps, listWithDetail, selectedIndex, selectionGeneration, items, noSelectionText)
                }
            },
        )
    }
    val request = pendingRemoval?.takeIf {
        canRemove && it.snapshot === itemsArray && it.index in items.indices && canRemoveAt(it.index)
    }
    if (request != null) {
        fun translated(key: String, fallback: String) =
            translateConfirmationText(props.state.jsonforms.i18n, key, fallback)
        CupertinoAlertDialog(
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
}

private data class PendingListRemoval(val index: Int, val snapshot: JsonArray)
private data class PendingListChange(val result: JsonArray, val selectedIndex: Int?, val addedIndex: Int? = null)

@Composable
private fun ItemList(
    modifier: Modifier,
    items: List<JsonElement>,
    visibleIndices: IntRange,
    childLabelProp: String?,
    selectedIndex: Int?,
    actionsEnabled: Boolean,
    canRemove: (Int) -> Boolean,
    showSortButtons: Boolean,
    upText: ActionText,
    downText: ActionText,
    noDataText: String,
    removeText: ActionText,
    errorsByIndex: Map<Int, List<ErrorObject>>,
    showIndicator: Boolean,
    showCount: Boolean,
    i18n: JsonFormsI18nState,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    Box(modifier.heightIn(max = 384.dp)
        .border(1.dp, LocalCupertinoColors.current.separator, RoundedCornerShape(10.dp))) {
        if (items.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CupertinoText(noDataText, color = LocalCupertinoColors.current.secondaryLabel)
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(6.dp).verticalScroll(rememberScrollState())
                    .focusable()
                    .testTag("list-detail-navigation-scroll"),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                visibleIndices.forEach { index ->
                    val item = items[index]
                    val selected = selectedIndex == index
                    Box(Modifier.fillMaxWidth().background(
                        if (selected) LocalCupertinoColors.current.secondaryBackground
                        else Color.Transparent, RoundedCornerShape(8.dp))) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).background(
                                LocalCupertinoColors.current.tertiaryBackground, CircleShape),
                                contentAlignment = Alignment.Center) {
                                CupertinoText((index + 1).toString())
                            }
                            CupertinoText(
                                computeItemLabel(item, index, childLabelProp),
                                modifier = Modifier.weight(1f).clickable { onSelect(index) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val itemErrors = errorsByIndex[index].orEmpty()
                            if (showIndicator && itemErrors.isNotEmpty()) {
                                val count = itemErrors.size.takeIf { showCount }
                                CupertinoListErrorIndicator(
                                    itemErrors,
                                    count,
                                    translateArrayValidationIndicator(i18n, count, index + 1),
                                )
                            }
                            if (showSortButtons) {
                                ActionTooltip(upText.tooltip) {
                                    CupertinoButton(
                                        enabled = actionsEnabled && index > 0,
                                        colors = CupertinoButtonDefaults.plainButtonColors(),
                                        onClick = { onMove(index, index - 1) },
                                    ) { Icon(ChevronUpIcon, contentDescription = upText.ariaLabel) }
                                }
                                ActionTooltip(downText.tooltip) {
                                    CupertinoButton(
                                        enabled = actionsEnabled && index < items.lastIndex,
                                        colors = CupertinoButtonDefaults.plainButtonColors(),
                                        onClick = { onMove(index, index + 1) },
                                    ) { Icon(ChevronDownIcon, contentDescription = downText.ariaLabel) }
                                }
                            }
                            ActionTooltip(removeText.tooltip) {
                                CupertinoButton(
                                    enabled = canRemove(index),
                                    colors = CupertinoButtonDefaults.plainButtonColors(),
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
    selectionGeneration: Int,
    items: List<JsonElement>,
    noSelectionText: String,
) {
    if (selectedIndex == null || selectedIndex !in items.indices) {
        CupertinoText(noSelectionText)
        return
    }
    val itemPath = composePaths(stateProps.path, selectedIndex.toString())
    val itemUiSchema = remember(stateProps.schema, stateProps.rootSchema, listWithDetail.scope,
        listWithDetail.options, stateProps.uischemas, itemPath) {
        runCatching {
            resolveDetailUiSchema(listWithDetail,
                registered = { findMatchingUISchema(stateProps.uischemas)(
                    stateProps.schema, listWithDetail.scope, itemPath,
                ) },
                generated = { Generate.uiSchema(stateProps.schema, rootSchema = stateProps.rootSchema) },
            )
        }.getOrElse { ControlElement(scope = "#") }
    }
    key(selectionGeneration) {
        JsonFormsItemScope(items[selectedIndex]) {
            JsonFormsDispatchRenderer(
                props.copy(
                    schema = stateProps.schema,
                    rootSchema = stateProps.rootSchema,
                    uischema = itemUiSchema,
                    path = itemPath,
                    enabled = stateProps.enabled,
                    id = "${stateProps.id}-detail-$selectionGeneration",
                ),
            )
        }
    }
}

private fun Int?.afterRemoval(removedIndex: Int): Int? = when {
    this == null -> null
    this > removedIndex -> this - 1
    this == removedIndex -> null
    else -> this
}

private fun Int?.swap(first: Int, second: Int): Int? = when (this) {
    first -> second
    second -> first
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
private val ChevronUpIcon = lineIcon("Move up") {
    moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f)
}
private val ChevronDownIcon = lineIcon("Move down") {
    moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
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

val cupertinoListWithDetailTester = materialListWithDetailTester
