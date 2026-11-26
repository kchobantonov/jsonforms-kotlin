package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText
import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateMixedRendererText
import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText
import com.chobantonov.jsonfroms.core.i18n.translateMixedTypeName
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.*
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporter
import com.chobantonov.jsonfroms.core.diagnostics.UiDiagnosticReporterConfigKey
import com.chobantonov.jsonfroms.core.mappers.*
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isMixedControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.withIncreasedRank
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.cupertino.renderers.controls.CupertinoDropdownChevron
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.slapps.cupertino.CupertinoButton
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.CupertinoTextField
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive
import com.slapps.cupertino.CupertinoButtonDefaults
import com.slapps.cupertino.CupertinoDropdownMenu
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.MenuPickerAction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

val CupertinoMixedRenderer = ComposeJsonFormsRenderer { CupertinoMixedRenderer(it) }

@OptIn(ExperimentalCupertinoApi::class)
@Composable
fun CupertinoMixedRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("CupertinoMixedRenderer can only render Control elements")
    val controlProps = mapStateToControlProps(
        props.state,
        OwnPropsOfControl(controlUiSchema = control, own = OwnPropsOfRenderer(
            uischema = control, schema = props.schema, enabled = props.enabled, path = props.path,
            renderers = props.renderers, cells = props.cells,
        )),
    )
    if (!controlProps.visible) return
    val types = mixedSchemaTypes(controlProps.schema)
    if (types.isEmpty()) return
    val inferred = jsonDataType(controlProps.data)
    var selected by remember(controlProps.path, controlProps.schema, controlProps.dataProvided) {
        mutableStateOf(if (controlProps.dataProvided) inferred.takeIf(types::contains) ?: types.first() else null)
    }
    LaunchedEffect(inferred, types) {
        val current = selected
        val invalidNumericDraft = current in listOf(JsonSchemaType.Integer, JsonSchemaType.Number) &&
            inferred == JsonSchemaType.String
        val numericEditorIntent = isNumericMixedType(current) && isNumericMixedType(inferred)
        if (current != null && !mixedTypeSupportsData(current, inferred) && !invalidNumericDraft &&
            !numericEditorIntent && inferred in types) {
            selected = inferred
        }
    }
    val i18n = props.state.jsonforms.i18n
    fun text(key: String, fallback: String, scoped: Boolean = true): String =
        translateMixedRendererText(i18n, controlProps.i18nKeyPrefix.takeIf { scoped }, key,
            fallback, if (scoped) mapOf("control" to controlProps.label) else emptyMap())
    val prompt = text("selectType", "Select a type")
    val interactive = controlProps.enabled && !controlProps.readonly
    val colors = LocalCupertinoColors.current
    val openTooltip = text("openTooltip", "Open")
    val openAriaLabel = text("openAriaLabel", "Open details")
    val clearLabel = translateControlClear(props.state.jsonforms.i18n)
    val typeClearable = resolvedControlBooleanOption(control.options, props.config, "clearable") != false
    val typeSelectionKey = "${controlProps.path}.__mixed_type"
    val searchLabel = text("searchTree", "Search tree…", scoped = false)
    val clearSearchLabel = text("clearSearch", "Clear search", scoped = false)
    val expandLabel = text("expand", "Expand", scoped = false)
    val collapseLabel = text("collapse", "Collapse", scoped = false)
    val showPrimitivesLabel = text("showPrimitives", "Show primitive values", scoped = false)
    val hidePrimitivesLabel = text("hidePrimitives", "Hide primitive values", scoped = false)
    val navigation = LocalMixedNavigationContext.current
    val complex = shouldOpenMixedDetails(selected)
    val data = props.state.jsonforms.core.data
    var pendingTypeChange by remember(controlProps.path, controlProps.schema, control) {
        mutableStateOf<PendingMixedTypeChange?>(null)
    }
    LaunchedEffect(data, interactive) {
        if (!interactive || pendingTypeChange?.let {
                data !== it.root || JsonPath.get(data, controlProps.path) !== it.snapshot
            } == true) pendingTypeChange = null
    }
    fun applyType(target: JsonSchemaType?, snapshot: JsonElement?, rootSnapshot: JsonElement) {
        if (!interactive || data !== rootSnapshot || JsonPath.get(data, controlProps.path) !== snapshot) return
        if (target != null && target !in types) return
        if (isNumericMixedEditorSwitch(selected, target, snapshot)) {
            selected = target
            return
        }
        selected = target
        props.dispatch(UpdateAction(path = controlProps.path, updater = { current ->
            if (current !== snapshot) current
            else if (target == null) JsonNull
            else createDefaultValue(concreteMixedSchema(controlProps.schema, target), controlProps.rootSchema)
        }))
        if (shouldOpenMixedDetails(target)) navigation?.select(controlProps.path)
    }
    fun requestType(target: JsonSchemaType?) {
        if (!interactive || target == selected) return
        val snapshot = JsonPath.get(data, controlProps.path)
        if (isNumericMixedEditorSwitch(selected, target, snapshot)) {
            applyType(target, snapshot, data)
            return
        }
        val decision = resolveConfirmationDecision(
            ConfirmationOperation.TypeChange, "mixed", listOf(snapshot), control.options, props.config,
        )
        if (decision.required) pendingTypeChange = PendingMixedTypeChange(target, snapshot, data)
        else applyType(target, snapshot, data)
    }
    val updateType: (JsonSchemaType) -> Unit = { requestType(it) }
    val clearType: () -> Unit = { requestType(null) }

    when {
        complex && navigation == null -> {
            var selectedPath by remember(controlProps.path) { mutableStateOf(controlProps.path) }
            val expansion = resolveMixedExpansionOptions(control.options, props.config)
            val reporter = props.config[UiDiagnosticReporterConfigKey] as? UiDiagnosticReporter
            LaunchedEffect(reporter, expansion.diagnostic) {
                expansion.diagnostic?.let {
                    reporter?.report(it.copy(details = it.details + ("path" to controlProps.path)))
                }
            }
            var expanded by remember(controlProps.path) { mutableStateOf(!expansion.collapsed) }
            LaunchedEffect(expansion.collapsed) { expanded = !expansion.collapsed }
            val isExpanded = !expansion.collapsible || expanded
            val stateText = translateGroupRendererText(i18n,
                if (isExpanded) "group.expanded" else "group.collapsed",
                if (isExpanded) "Expanded" else "Collapsed")
            var showPrimitives by remember(controlProps.path) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CupertinoTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                    Spacer(Modifier.weight(1f))
                    if (expansion.collapsible) {
                        ActionTooltip(if (isExpanded) collapseLabel else expandLabel) {
                            CupertinoButton(
                                onClick = { expanded = !expanded },
                                colors = CupertinoButtonDefaults.plainButtonColors(),
                                modifier = Modifier.semantics {
                                    stateDescription = stateText
                                    contentDescription = if (isExpanded) collapseLabel else expandLabel
                                },
                            ) {
                                CupertinoDropdownChevron(isExpanded, colors.secondaryLabel)
                            }
                        }
                    }
                }
                if (isExpanded) {
                    CupertinoMixedTree(
                        props, control,
                        buildMixedTree(
                            controlProps.data, controlProps.schema, controlProps.rootSchema, controlProps.path,
                            controlProps.label.ifBlank { text("root", "Root", scoped = false) }, includePrimitives = showPrimitives,
                        ),
                        selectedPath,
                        { selectedPath = it },
                        interactive,
                        searchLabel,
                        clearSearchLabel,
                        showPrimitives,
                        showPrimitivesLabel,
                        hidePrimitivesLabel,
                        { showPrimitives = !showPrimitives },
                    )
                }
            }
        }
        complex && navigation != null && navigation.detailPath != controlProps.path -> {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.Bottom) {
                CupertinoTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                ActionTooltip(openTooltip) {
                    IconButton(onClick = { navigation.select(controlProps.path) }) {
                        Icon(
                            JsonFormsActionIcons.ChevronRight,
                            contentDescription = openAriaLabel,
                            tint = colors.accent,
                        )
                    }
                }
            }
        }
        complex && navigation?.detailPath == controlProps.path -> {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (navigation.rootPath != controlProps.path) {
                    CupertinoTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                }
                CupertinoMixedValue(props, control, controlProps, selected)
            }
        }
        else -> {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.Bottom) {
                CupertinoTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                Box(Modifier.weight(1f)) {
                    CupertinoMixedValue(props, control, controlProps, selected)
                }
            }
        }
    }
    val typeRequest = pendingTypeChange?.takeIf {
        interactive && data === it.root && JsonPath.get(data, controlProps.path) === it.snapshot
    }
    if (typeRequest != null) CupertinoAlertDialog(
        onDismissRequest = { if (pendingTypeChange === typeRequest) pendingTypeChange = null },
        title = { CupertinoText(translateConfirmationText(i18n, "confirm.typeChange.title", "Change this type?")) },
        message = { CupertinoText(translateConfirmationText(i18n, "confirm.typeChange.message", "The current value will be replaced.")) },
        buttons = {
            cancel(onClick = { if (pendingTypeChange === typeRequest) pendingTypeChange = null }) {
                CupertinoText(translateConfirmationText(i18n, "confirm.decline", "No"))
            }
            destructive(onClick = {
                if (pendingTypeChange === typeRequest) applyType(typeRequest.target, typeRequest.snapshot, typeRequest.root)
                if (pendingTypeChange === typeRequest) pendingTypeChange = null
            }) { CupertinoText(translateConfirmationText(i18n, "confirm.accept", "Yes")) }
        },
    )
}

private data class PendingMixedTypeChange(
    val target: JsonSchemaType?, val snapshot: JsonElement?, val root: JsonElement,
)

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoTypeSelector(
    types: List<JsonSchemaType>, selected: JsonSchemaType?, prompt: String, label: String,
    interactive: Boolean, clearable: Boolean, selectionKey: String,
    modifier: Modifier = Modifier.width(180.dp), onSelect: (JsonSchemaType) -> Unit,
    onClear: () -> Unit, clearLabel: String, i18n: JsonFormsI18nState,
) {
    val colors = LocalCupertinoColors.current
    var expanded by remember { mutableStateOf(false) }
    val clearAffordance = rememberClearAffordanceState(selectionKey)
    val canClear = interactive && clearable && selected != null
    val clearVisible = clearAffordance.isVisible()
    Column(modifier) {
        Text(label.ifBlank { translateMixedRendererText(i18n, null, "type", "Type") },
            color = colors.secondaryLabel)
        Box {
            Row(
                Modifier.fillMaxWidth().border(1.dp, colors.controlBorder, RoundedCornerShape(10.dp))
                    .clickable(interactive) { expanded = !expanded }
                    .then(clearAffordance.fieldModifier)
                    .pointerInput(clearAffordance) {
                        awaitPointerEventScope {
                            while (true) if (awaitPointerEvent().type == PointerEventType.Press)
                                clearAffordance.selectField()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected?.let { translateMixedTypeName(i18n, it) } ?: prompt, Modifier.weight(1f),
                    color = if (interactive) colors.label else colors.secondaryLabel,
                )
                if (canClear) {
                    if (clearVisible) ActionTooltip(clearLabel) {
                        IconButton(onClick = onClear, modifier = clearAffordance.clearModifier(28.dp)) {
                            Icon(JsonFormsActionIcons.Clear, clearLabel, tint = colors.secondaryLabel, modifier = Modifier.size(15.dp))
                        }
                    } else Spacer(Modifier.size(28.dp))
                }
                CupertinoDropdownChevron(expanded, colors.secondaryLabel)
            }
            CupertinoDropdownMenu(expanded && interactive, { expanded = false }) {
                types.forEach { type ->
                    MenuPickerAction(
                        isSelected = type == selected,
                        onClick = { onSelect(type); expanded = false },
                        title = { Text(translateMixedTypeName(i18n, type)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CupertinoMixedValue(
    props: ComposeJsonFormsRendererProps,
    control: ControlElement,
    controlProps: StatePropsOfControl,
    selected: JsonSchemaType?,
) {
    if (selected == null || selected == JsonSchemaType.Null) return
    val concreteSchema = concreteMixedSchema(controlProps.schema, selected)
    CompositionLocalProvider(LocalPreserveEmptyValue provides true) {
        JsonFormsDispatchRenderer(props.copy(
            schema = concreteSchema,
            uischema = mixedControlForType(control, selected).copy(scope = "", label = BooleanControlLabel(false)), path = controlProps.path,
            enabled = controlProps.enabled, id = "${controlProps.id}-mixed-${selected.name}",
        ))
    }
}

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoMixedTree(
    props: ComposeJsonFormsRendererProps,
    control: ControlElement,
    root: MixedTreeNode,
    selectedPath: String,
    onSelect: (String) -> Unit,
    interactive: Boolean,
    searchLabel: String,
    clearSearchLabel: String,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
) {
    val colors = LocalCupertinoColors.current
    fun text(key: String, fallback: String): String = translateMixedRendererText(
        props.state.jsonforms.i18n, null, key, fallback)
    val data = props.state.jsonforms.core.data
    var pendingDelete by remember(root.path) { mutableStateOf<Pair<MixedTreeNode, JsonElement>?>(null) }
    LaunchedEffect(data, interactive) {
        if (!interactive || pendingDelete?.let { JsonPath.get(data, it.first.path) !== it.second } == true) {
            pendingDelete = null
        }
    }
    fun delete(node: MixedTreeNode, value: JsonElement) {
        if (!interactive || JsonPath.get(data, node.path) !== value) return
        props.dispatch(UpdateAction(path = node.path, updater = { current -> if (current === value) null else current }))
        onSelect(node.parentPath ?: root.path)
    }
    MixedTreeEditor(
        root, selectedPath,
        MixedTreeColors(
            Color.Transparent, colors.controlBorder, colors.tertiaryBackground,
            colors.label, colors.secondaryLabel, colors.destructive,
        ),
        MixedTreeComponents(
            text = { value, modifier, color, maxLines ->
                CupertinoText(value, modifier = modifier, color = color, maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis)
            },
            input = { value, onChange, modifier, enabled, placeholder ->
                Box(modifier) {
                    CupertinoTextField(value = value, onValueChange = onChange,
                        modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
                        placeholder = placeholder?.let { { CupertinoText(it) } })
                }
            },
            action = { icon, label, tint, onClick ->
                ActionTooltip(label) {
                    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
                        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
                    }
                }
            },
        ),
        text("rename", "Rename"), text("remove", "Delete"), text("confirm", "Confirm"), text("cancel", "Cancel"), interactive, searchLabel, clearSearchLabel,
        showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives,
        onSelect,
        { node, draft ->
            val parent = node.parentPath
            val oldName = node.key
            val name = draft.trim()
            if (interactive && parent != null && oldName != null && name.isNotBlank() && '.' !in name) {
                props.dispatch(UpdateAction(path = parent, updater = { value ->
                    val source = value as? JsonObject ?: return@UpdateAction value
                    if (name != oldName && name in source) return@UpdateAction value
                    JsonObject(linkedMapOf<String, JsonElement>().apply {
                        source.forEach { (key, item) -> put(if (key == oldName) name else key, item) }
                    })
                }))
                onSelect(if (parent.isBlank()) name else "$parent.$name")
            }
        },
        { node ->
            val value = JsonPath.get(data, node.path)
            if (interactive && node.canDelete && value != null) {
                val decision = resolveConfirmationDecision(
                    ConfirmationOperation.Delete, "mixed", listOf(value), control.options, props.config,
                )
                if (decision.required) pendingDelete = node to value else delete(node, value)
            }
        },
    ) { node ->
        CompositionLocalProvider(LocalMixedNavigationContext provides MixedNavigationContext(root.path, node.path, onSelect)) {
            JsonFormsDispatchRenderer(props.copy(
                schema = node.schema,
                uischema = (if (node.path == root.path) mixedControlForType(control, node.type) else control)
                    .copy(scope = "", label = BooleanControlLabel(false)),
                path = node.path,
                id = "${props.id}-mixed-detail-${node.path}",
            ))
        }
    }
    val request = pendingDelete?.takeIf { interactive && JsonPath.get(data, it.first.path) === it.second }
    if (request != null) CupertinoAlertDialog(
        onDismissRequest = { if (pendingDelete === request) pendingDelete = null },
        title = { CupertinoText(translateConfirmationText(props.state.jsonforms.i18n, "confirm.delete.title", "Delete this value?")) },
        message = { CupertinoText(translateConfirmationText(props.state.jsonforms.i18n, "confirm.delete.message", "The value will be removed. This cannot be undone from here.")) },
        buttons = {
            cancel(onClick = { if (pendingDelete === request) pendingDelete = null }) {
                CupertinoText(translateConfirmationText(props.state.jsonforms.i18n, "confirm.decline", "No"))
            }
            destructive(onClick = {
                if (pendingDelete === request) delete(request.first, request.second)
                if (pendingDelete === request) pendingDelete = null
            }) { CupertinoText(translateConfirmationText(props.state.jsonforms.i18n, "confirm.accept", "Yes")) }
        },
    )
}

val cupertinoMixedControlTester: RankedTester = withIncreasedRank(100, rankWith(20, isMixedControl))
