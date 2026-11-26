package com.chobantonov.jsonfroms.material.renderers.complex

import com.chobantonov.jsonfroms.core.i18n.translateControlClear
import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText
import com.chobantonov.jsonfroms.core.i18n.translateMixedRendererText
import com.chobantonov.jsonfroms.core.i18n.translateGroupRendererText
import com.chobantonov.jsonfroms.core.i18n.translateMixedTypeName
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.material.renderers.ActionText
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

val MaterialMixedRenderer: ComposeJsonFormsRenderer = object : ComposeJsonFormsRenderer, ComposeUnknownRenderer {
    @Composable
    override fun Render(props: ComposeJsonFormsRendererProps) = MaterialMixedRenderer(props)

    @Composable
    override fun RenderUnknown(message: String) {
        Text(message, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MaterialMixedRenderer(props: ComposeJsonFormsRendererProps) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialMixedRenderer can only render Control elements")
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
    val openText = ActionText(text("openTooltip", "Open"), text("openAriaLabel", "Open details"))
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
                    MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                    Spacer(Modifier.weight(1f))
                    if (expansion.collapsible) {
                        ActionTooltip(if (isExpanded) collapseLabel else expandLabel) {
                            IconButton(
                                onClick = { expanded = !expanded },
                                modifier = Modifier.semantics { stateDescription = stateText },
                            ) {
                                Icon(
                                    JsonFormsActionIcons.ChevronRight,
                                    if (isExpanded) collapseLabel else expandLabel,
                                    Modifier.rotate(if (isExpanded) 90f else 0f),
                                )
                            }
                        }
                    }
                }
                if (isExpanded) {
                    MaterialMixedTree(
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
            Row(
                Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.Bottom,
            ) {
                MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                ActionTooltip(openText.tooltip) {
                    IconButton(onClick = { navigation.select(controlProps.path) }) {
                        Icon(JsonFormsActionIcons.ChevronRight, contentDescription = openText.ariaLabel)
                    }
                }
            }
        }
        complex && navigation?.detailPath == controlProps.path -> {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (navigation.rootPath != controlProps.path) {
                    MaterialTypeSelector(
                        types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey,
                        modifier = Modifier.width(180.dp), onSelect = updateType,
                        onClear = clearType, clearLabel = clearLabel, i18n = i18n,
                    )
                }
                MaterialMixedValue(props, control, controlProps, selected)
            }
        }
        else -> {
            Row(
                Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.Bottom,
            ) {
                MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, typeClearable, typeSelectionKey, onSelect = updateType, onClear = clearType, clearLabel = clearLabel, i18n = i18n)
                Box(Modifier.weight(1f)) {
                    MaterialMixedValue(props, control, controlProps, selected)
                }
            }
        }
    }
    val typeRequest = pendingTypeChange?.takeIf {
        interactive && data === it.root && JsonPath.get(data, controlProps.path) === it.snapshot
    }
    if (typeRequest != null) AlertDialog(
        onDismissRequest = { if (pendingTypeChange === typeRequest) pendingTypeChange = null },
        title = { Text(translateConfirmationText(i18n, "confirm.typeChange.title", "Change this type?")) },
        text = { Text(translateConfirmationText(i18n, "confirm.typeChange.message", "The current value will be replaced.")) },
        confirmButton = { TextButton(onClick = {
            if (pendingTypeChange === typeRequest) applyType(typeRequest.target, typeRequest.snapshot, typeRequest.root)
            if (pendingTypeChange === typeRequest) pendingTypeChange = null
        }) { Text(translateConfirmationText(i18n, "confirm.accept", "Yes")) } },
        dismissButton = { TextButton(onClick = { if (pendingTypeChange === typeRequest) pendingTypeChange = null }) {
            Text(translateConfirmationText(i18n, "confirm.decline", "No"))
        } },
    )
}

private data class PendingMixedTypeChange(
    val target: JsonSchemaType?, val snapshot: JsonElement?, val root: JsonElement,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialTypeSelector(
    types: List<JsonSchemaType>, selected: JsonSchemaType?, prompt: String, label: String,
    interactive: Boolean, clearable: Boolean, selectionKey: String,
    modifier: Modifier = Modifier.width(180.dp), onSelect: (JsonSchemaType) -> Unit,
    onClear: () -> Unit, clearLabel: String, i18n: JsonFormsI18nState,
) {
    var expanded by remember { mutableStateOf(false) }
    val clearAffordance = rememberClearAffordanceState(selectionKey)
    val canClear = interactive && clearable && selected != null
    var hovered by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    fun clearVisible(): Boolean = clearAffordance.isVisible() || hovered || focused
    ExposedDropdownMenuBox(expanded && interactive, {
        expanded = it && interactive
        if (expanded) clearAffordance.selectField()
    }, modifier.onFocusChanged { focused = it.hasFocus }.pointerInput(selectionKey) {
        awaitPointerEventScope {
            while (true) when (awaitPointerEvent().type) {
                PointerEventType.Enter -> hovered = true
                PointerEventType.Exit -> hovered = false
            }
        }
    }) {
        OutlinedTextField(
            value = selected?.let { translateMixedTypeName(i18n, it) } ?: prompt, onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, interactive),
            readOnly = true, enabled = interactive,
            label = { Text(label.ifBlank { translateMixedRendererText(i18n, null, "type", "Type") }) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canClear) {
                        ActionTooltip(clearLabel) {
                            IconButton(
                                onClick = { if (clearVisible()) onClear() },
                                modifier = clearAffordance.clearModifier(32.dp)
                                    .graphicsLayer { alpha = if (clearVisible()) 1f else 0f }
                                    .focusProperties { canFocus = clearVisible() }
                                    .clearAndSetSemantics {
                                        if (clearVisible()) {
                                            contentDescription = clearLabel
                                            onClick { onClear(); true }
                                        }
                                    },
                            ) { Icon(JsonFormsActionIcons.Clear, null, modifier = Modifier.size(16.dp)) }
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
        )
        ExposedDropdownMenu(expanded && interactive, { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem({ Text(translateMixedTypeName(i18n, type)) }, { onSelect(type); expanded = false })
            }
        }
    }
}

@Composable
private fun MaterialMixedValue(
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

@Composable
private fun MaterialMixedTree(
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
    val scheme = MaterialTheme.colorScheme
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
            Color.Transparent, scheme.outlineVariant, scheme.secondaryContainer,
            scheme.onSurface, scheme.onSurfaceVariant, scheme.error,
        ),
        MixedTreeComponents(
            text = { value, modifier, color, maxLines ->
                Text(value, modifier = modifier, color = color, maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis)
            },
            input = { value, onChange, modifier, enabled, placeholder ->
                OutlinedTextField(value = value, onValueChange = onChange, modifier = modifier,
                    enabled = enabled, singleLine = true,
                    placeholder = placeholder?.let { { Text(it) } })
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
    if (request != null) AlertDialog(
        onDismissRequest = { if (pendingDelete === request) pendingDelete = null },
        title = { Text(translateConfirmationText(props.state.jsonforms.i18n, "confirm.delete.title", "Delete this value?")) },
        text = { Text(translateConfirmationText(props.state.jsonforms.i18n, "confirm.delete.message", "The value will be removed. This cannot be undone from here.")) },
        confirmButton = { TextButton(onClick = {
            if (pendingDelete === request) delete(request.first, request.second)
            if (pendingDelete === request) pendingDelete = null
        }) { Text(translateConfirmationText(props.state.jsonforms.i18n, "confirm.accept", "Yes")) } },
        dismissButton = { TextButton(onClick = { if (pendingDelete === request) pendingDelete = null }) {
            Text(translateConfirmationText(props.state.jsonforms.i18n, "confirm.decline", "No"))
        } },
    )
}

val materialMixedControlTester: RankedTester = rankWith(20, isMixedControl)
