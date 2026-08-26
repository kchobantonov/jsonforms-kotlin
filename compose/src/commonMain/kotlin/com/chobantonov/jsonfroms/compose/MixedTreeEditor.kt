package com.chobantonov.jsonfroms.compose

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.core.mappers.MixedTreeNode

data class MixedNavigationContext(
    val rootPath: String,
    val detailPath: String,
    val select: (String) -> Unit,
)

val LocalMixedNavigationContext = compositionLocalOf<MixedNavigationContext?> { null }

data class MixedTreeColors(
    val container: Color,
    val border: Color,
    val selectedContainer: Color,
    val text: Color,
    val secondaryText: Color,
    val danger: Color,
)

@Composable
fun MixedTreeEditor(
    root: MixedTreeNode,
    selectedPath: String,
    colors: MixedTreeColors,
    renameLabel: String,
    deleteLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    searchLabel: String,
    clearSearchLabel: String,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (MixedTreeNode, String) -> Unit,
    onDelete: (MixedTreeNode) -> Unit,
    detail: @Composable (MixedTreeNode) -> Unit,
) {
    val selected = root.find(selectedPath) ?: root
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 640.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TreePane(root, selected.path, colors, renameLabel, deleteLabel, confirmLabel, cancelLabel, searchLabel, clearSearchLabel, showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives, onSelect, onRename, onDelete)
                DetailPane(selected, colors, detail)
            }
        } else {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.width(280.dp)) {
                    TreePane(root, selected.path, colors, renameLabel, deleteLabel, confirmLabel, cancelLabel, searchLabel, clearSearchLabel, showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives, onSelect, onRename, onDelete)
                }
                Box(Modifier.weight(1f)) { DetailPane(selected, colors, detail) }
            }
        }
    }
}

@Composable
private fun TreePane(
    root: MixedTreeNode,
    selectedPath: String,
    colors: MixedTreeColors,
    renameLabel: String,
    deleteLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    searchLabel: String,
    clearSearchLabel: String,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (MixedTreeNode, String) -> Unit,
    onDelete: (MixedTreeNode) -> Unit,
) {
    val expanded = remember(root.path) { mutableStateMapOf(root.path to true) }
    var query by remember(root.path) { mutableStateOf("") }
    val filtered = remember(root, query) { root.filter(query) }
    Surface(
        color = colors.container,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 560.dp).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(searchLabel) },
                leadingIcon = { Icon(JsonFormsActionIcons.Search, contentDescription = null, modifier = Modifier.size(17.dp)) },
                trailingIcon = if (query.isNotEmpty()) {{
                    ActionIcon(JsonFormsActionIcons.Clear, clearSearchLabel, colors.secondaryText) { query = "" }
                }} else null,
            )
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                filtered?.let {
                    TreeNodeRow(
                        node = it,
                        depth = 0,
                        selectedPath = selectedPath,
                        expanded = expanded,
                        forceExpanded = query.isNotBlank(),
                        colors = colors,
                        renameLabel = renameLabel,
                        deleteLabel = deleteLabel,
                        confirmLabel = confirmLabel,
                        cancelLabel = cancelLabel,
                        showPrimitives = showPrimitives,
                        showPrimitivesLabel = showPrimitivesLabel,
                        hidePrimitivesLabel = hidePrimitivesLabel,
                        onTogglePrimitives = onTogglePrimitives,
                        onSelect = onSelect,
                        onRename = onRename,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun TreeNodeRow(
    node: MixedTreeNode,
    depth: Int,
    selectedPath: String,
    expanded: MutableMap<String, Boolean>,
    forceExpanded: Boolean,
    colors: MixedTreeColors,
    renameLabel: String,
    deleteLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (MixedTreeNode, String) -> Unit,
    onDelete: (MixedTreeNode) -> Unit,
) {
    var editing by remember(node.path) { mutableStateOf(false) }
    var draft by remember(node.path, node.label) { mutableStateOf(node.label) }
    val hasChildren = node.children.isNotEmpty()
    val isExpanded = forceExpanded || expanded[node.path] ?: (depth < 2)
    var hovered by remember(node.path) { mutableStateOf(false) }
    val alwaysShowActions = LocalInputModeManager.current.inputMode == InputMode.Touch
    val showActions = alwaysShowActions || hovered
    Row(
        Modifier.fillMaxWidth().padding(start = (depth * 14).dp)
            .pointerInput(node.path) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> hovered = true
                            PointerEventType.Exit -> hovered = false
                        }
                    }
                }
            }
            .clickable { onSelect(node.path) }
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = if (node.path == selectedPath) colors.selectedContainer else Color.Transparent,
            shape = RoundedCornerShape(7.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasChildren) {
                    Icon(
                        JsonFormsActionIcons.ChevronRight,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(22.dp).rotate(if (isExpanded) 90f else 0f)
                            .clickable { expanded[node.path] = !isExpanded },
                    )
                } else {
                    Text("•", Modifier.width(22.dp), color = colors.secondaryText)
                }
                if (editing) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    ActionIcon(JsonFormsActionIcons.Confirm, confirmLabel, colors.text) {
                        onRename(node, draft)
                        editing = false
                    }
                    ActionIcon(JsonFormsActionIcons.Cancel, cancelLabel, colors.secondaryText) {
                        draft = node.label
                        editing = false
                    }
                } else {
                    Text(
                        node.type.treeGlyph(),
                        modifier = Modifier.width(26.dp),
                        color = colors.secondaryText,
                    )
                    Text(
                        node.label,
                        modifier = Modifier.weight(1f),
                        color = colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (depth == 0) {
                        ActionIcon(
                            if (showPrimitives) JsonFormsActionIcons.Visibility else JsonFormsActionIcons.VisibilityOff,
                            if (showPrimitives) hidePrimitivesLabel else showPrimitivesLabel,
                            colors.secondaryText,
                            onTogglePrimitives,
                        )
                    }
                    Row(Modifier.graphicsLayer { alpha = if (showActions) 1f else 0f }) {
                        if (node.canRename) {
                            ActionIcon(JsonFormsActionIcons.Rename, renameLabel, colors.secondaryText) { editing = true }
                        }
                        if (node.canDelete) {
                            ActionIcon(JsonFormsActionIcons.Delete, deleteLabel, colors.danger) { onDelete(node) }
                        }
                    }
                }
            }
        }
    }
    if (hasChildren && isExpanded) {
        node.children.forEach { child ->
            TreeNodeRow(
                child,
                depth + 1,
                selectedPath,
                expanded,
                forceExpanded,
                colors,
                renameLabel,
                deleteLabel,
                confirmLabel,
                cancelLabel,
                showPrimitives,
                showPrimitivesLabel,
                hidePrimitivesLabel,
                onTogglePrimitives,
                onSelect,
                onRename,
                onDelete,
            )
        }
    }
}

@Composable
private fun DetailPane(
    node: MixedTreeNode,
    colors: MixedTreeColors,
    detail: @Composable (MixedTreeNode) -> Unit,
) {
    Surface(
        color = colors.container,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(node.label, color = colors.text)
            detail(node)
        }
    }
}

private fun MixedTreeNode.find(path: String): MixedTreeNode? {
    if (this.path == path) return this
    return children.firstNotNullOfOrNull { it.find(path) }
}

private fun MixedTreeNode.filter(query: String): MixedTreeNode? {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    val filteredChildren = children.mapNotNull { it.filter(normalized) }
    return if (label.contains(normalized, ignoreCase = true) || filteredChildren.isNotEmpty()) {
        copy(children = filteredChildren)
    } else {
        null
    }
}

private fun com.chobantonov.jsonfroms.core.models.JsonSchemaType.treeGlyph(): String = when (this) {
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Object -> "{}"
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Array -> "[]"
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.String -> "\"\""
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Boolean -> "◉"
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Integer,
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Number,
    -> "#"
    com.chobantonov.jsonfroms.core.models.JsonSchemaType.Null -> "∅"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
        }
    }
}
