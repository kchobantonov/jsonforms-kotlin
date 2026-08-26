package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.*
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.mappers.*
import com.chobantonov.jsonfroms.core.models.BooleanControlLabel
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isMixedControl
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.material.renderers.translatedText
import com.chobantonov.jsonfroms.material.renderers.translatedActionText
import com.chobantonov.jsonfroms.material.renderers.ActionTooltip
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

val MaterialMixedRenderer = ComposeJsonFormsRenderer { MaterialMixedRenderer(it) }

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
        if (current != null && !mixedTypeSupportsData(current, inferred) && !invalidNumericDraft && inferred in types) {
            selected = inferred
        }
    }
    val prompt = translatedText(
        props.state.jsonforms.i18n.translate, controlProps.i18nKeyPrefix, "selectType", "Select a type",
        mapOf("control" to controlProps.label),
    )
    val interactive = controlProps.enabled && !controlProps.readonly
    val openText = translatedActionText(
        props.state.jsonforms.i18n.translate,
        controlProps.i18nKeyPrefix,
        "openTooltip",
        "openAriaLabel",
        "Open",
        "Open details",
        mapOf("control" to controlProps.label),
    )
    val clearLabel = props.state.jsonforms.i18n.translate?.translate("clear", "Clear value", emptyMap()) ?: "Clear value"
    val searchLabel = props.state.jsonforms.i18n.translate?.translate("searchTree", "Search tree…", emptyMap()) ?: "Search tree…"
    val clearSearchLabel = props.state.jsonforms.i18n.translate?.translate("clearSearch", "Clear search", emptyMap()) ?: "Clear search"
    val expandLabel = props.state.jsonforms.i18n.translate?.translate("expand", "Expand", emptyMap()) ?: "Expand"
    val collapseLabel = props.state.jsonforms.i18n.translate?.translate("collapse", "Collapse", emptyMap()) ?: "Collapse"
    val showPrimitivesLabel = props.state.jsonforms.i18n.translate?.translate("showPrimitives", "Show primitive values", emptyMap()) ?: "Show primitive values"
    val hidePrimitivesLabel = props.state.jsonforms.i18n.translate?.translate("hidePrimitives", "Hide primitive values", emptyMap()) ?: "Hide primitive values"
    val navigation = LocalMixedNavigationContext.current
    val complex = shouldOpenMixedDetails(selected)
    val updateType: (JsonSchemaType) -> Unit = { type ->
        selected = type
        props.dispatch(UpdateAction(
            path = controlProps.path,
            updater = { createDefaultValue(concreteMixedSchema(controlProps.schema, type), controlProps.rootSchema) },
        ))
        if (shouldOpenMixedDetails(type)) {
            navigation?.select(controlProps.path)
        }
    }
    val clearType: () -> Unit = {
        selected = null
        props.dispatch(UpdateAction(
            path = controlProps.path,
            updater = { JsonNull },
        ))
    }

    when {
        complex && navigation == null -> {
            var selectedPath by remember(controlProps.path) { mutableStateOf(controlProps.path) }
            var expanded by remember(controlProps.path, selected) {
                mutableStateOf(shouldOpenMixedDetails(selected))
            }
            var showPrimitives by remember(controlProps.path) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, onSelect = updateType, onClear = clearType, clearLabel = clearLabel)
                    Spacer(Modifier.weight(1f))
                    ActionTooltip(if (expanded) collapseLabel else expandLabel) { IconButton(onClick = { expanded = !expanded }) {
                        Icon(JsonFormsActionIcons.ChevronRight, if (expanded) collapseLabel else expandLabel, Modifier.rotate(if (expanded) 90f else 0f))
                    } }
                }
                if (expanded) {
                    MaterialMixedTree(
                        props, control,
                        buildMixedTree(
                            controlProps.data, controlProps.schema, controlProps.rootSchema, controlProps.path,
                            controlProps.label.ifBlank { "Root" }, includePrimitives = showPrimitives,
                        ),
                        selectedPath,
                        { selectedPath = it },
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
                MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, onSelect = updateType, onClear = clearType, clearLabel = clearLabel)
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
                        types, selected, prompt, controlProps.label, interactive,
                        modifier = Modifier.width(180.dp), onSelect = updateType,
                        onClear = clearType, clearLabel = clearLabel,
                    )
                }
                MaterialMixedValue(props, control, controlProps, selected)
            }
        }
        else -> {
            Row(
                Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.Bottom,
            ) {
                MaterialTypeSelector(types, selected, prompt, controlProps.label, interactive, onSelect = updateType, onClear = clearType, clearLabel = clearLabel)
                Box(Modifier.weight(1f)) {
                    MaterialMixedValue(props, control, controlProps, selected)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialTypeSelector(
    types: List<JsonSchemaType>, selected: JsonSchemaType?, prompt: String, label: String,
    interactive: Boolean, modifier: Modifier = Modifier.width(180.dp), onSelect: (JsonSchemaType) -> Unit,
    onClear: () -> Unit, clearLabel: String,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded && interactive, { expanded = it && interactive }, modifier) {
        OutlinedTextField(
            value = selected?.displayName() ?: prompt, onValueChange = {},
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, interactive),
            readOnly = true, enabled = interactive, label = { Text(label.ifBlank { "Type" }) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) {
                        ActionTooltip(clearLabel) { IconButton(onClick = onClear, enabled = interactive, modifier = Modifier.size(32.dp)) {
                            Icon(JsonFormsActionIcons.Clear, clearLabel, modifier = Modifier.size(16.dp))
                        } }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                }
            },
        )
        ExposedDropdownMenu(expanded && interactive, { expanded = false }) {
            types.forEach { type ->
                DropdownMenuItem({ Text(type.displayName()) }, { onSelect(type); expanded = false })
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
            uischema = control.copy(scope = "", label = BooleanControlLabel(false)), path = controlProps.path,
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
    searchLabel: String,
    clearSearchLabel: String,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val translate = props.state.jsonforms.i18n.translate
    fun text(key: String, fallback: String): String = translate?.translate(key, fallback, emptyMap()) ?: fallback
    MixedTreeEditor(
        root, selectedPath,
        MixedTreeColors(
            Color.Transparent, scheme.outlineVariant, scheme.secondaryContainer,
            scheme.onSurface, scheme.onSurfaceVariant, scheme.error,
        ),
        text("rename", "Rename"), text("remove", "Delete"), text("confirm", "Confirm"), text("cancel", "Cancel"), searchLabel, clearSearchLabel,
        showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives,
        onSelect,
        { node, draft ->
            val parent = node.parentPath
            val oldName = node.key
            val name = draft.trim()
            if (parent != null && oldName != null && name.isNotBlank() && '.' !in name) {
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
            props.dispatch(UpdateAction(path = node.path, updater = { null }))
            onSelect(node.parentPath ?: root.path)
        },
    ) { node ->
        CompositionLocalProvider(LocalMixedNavigationContext provides MixedNavigationContext(root.path, node.path, onSelect)) {
            JsonFormsDispatchRenderer(props.copy(
                schema = node.schema,
                uischema = control.copy(scope = "", label = BooleanControlLabel(false)),
                path = node.path,
                id = "${props.id}-mixed-detail-${node.path}",
            ))
        }
    }
}

val materialMixedControlTester: RankedTester = rankWith(20, isMixedControl)
