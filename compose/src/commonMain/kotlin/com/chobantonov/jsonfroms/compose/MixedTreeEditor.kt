package com.chobantonov.jsonfroms.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
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

/** Family-owned text, input and action widgets; tree state and traversal remain shared. */
data class MixedTreeComponents(
    val text: @Composable (String, Modifier, Color, Int) -> Unit,
    val input: @Composable (String, (String) -> Unit, Modifier, Boolean, String?) -> Unit,
    val action: @Composable (ImageVector, String, Color, () -> Unit) -> Unit,
)

private val LocalMixedTreeComponents = compositionLocalOf<MixedTreeComponents> {
    error("MixedTreeEditor requires family components")
}

@Composable
fun MixedTreeEditor(
    root: MixedTreeNode,
    selectedPath: String,
    colors: MixedTreeColors,
    components: MixedTreeComponents,
    renameLabel: String,
    deleteLabel: String,
    confirmLabel: String,
    cancelLabel: String,
    interactive: Boolean,
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
    CompositionLocalProvider(LocalMixedTreeComponents provides components) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 640.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TreePane(root, selected.path, colors, renameLabel, deleteLabel, confirmLabel, cancelLabel, interactive, searchLabel, clearSearchLabel, showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives, onSelect, onRename, onDelete)
                    DetailPane(selected, colors, detail)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.width(280.dp)) {
                        TreePane(root, selected.path, colors, renameLabel, deleteLabel, confirmLabel, cancelLabel, interactive, searchLabel, clearSearchLabel, showPrimitives, showPrimitivesLabel, hidePrimitivesLabel, onTogglePrimitives, onSelect, onRename, onDelete)
                    }
                    Box(Modifier.weight(1f)) { DetailPane(selected, colors, detail) }
                }
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
    interactive: Boolean,
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
    val components = LocalMixedTreeComponents.current
    val expanded = remember(root.path) { mutableStateMapOf(root.path to true) }
    var query by remember(root.path) { mutableStateOf("") }
    val filtered = remember(root, query) { root.filter(query) }
    MixedTreeSurface(colors.container, colors.border, 10.dp) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 560.dp).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(JsonFormsActionIcons.Search, contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.secondaryText), modifier = Modifier.size(17.dp))
                components.input(query, { query = it }, Modifier.weight(1f), true, searchLabel)
                if (query.isNotEmpty()) ActionIcon(JsonFormsActionIcons.Clear, clearSearchLabel,
                    colors.secondaryText) { query = "" }
            }
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
                        interactive = interactive,
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
    interactive: Boolean,
    showPrimitives: Boolean,
    showPrimitivesLabel: String,
    hidePrimitivesLabel: String,
    onTogglePrimitives: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (MixedTreeNode, String) -> Unit,
    onDelete: (MixedTreeNode) -> Unit,
) {
    val components = LocalMixedTreeComponents.current
    var editing by remember(node.path) { mutableStateOf(false) }
    var draft by remember(node.path, node.label) { mutableStateOf(node.label) }
    val hasChildren = node.children.isNotEmpty()
    val isExpanded = forceExpanded || expanded[node.path] ?: (depth < 2)
    var hovered by remember(node.path) { mutableStateOf(false) }
    val alwaysShowActions = LocalInputModeManager.current.inputMode == InputMode.Touch
    val showActions = alwaysShowActions || hovered
    Row(
        Modifier.fillMaxWidth().testTag("mixed-tree-row-${node.path}")
            .padding(start = (depth * 14).dp)
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
        MixedTreeSurface(
            if (node.path == selectedPath) colors.selectedContainer else Color.Transparent,
            null, 7.dp, Modifier.weight(1f),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasChildren) {
                    Image(
                        JsonFormsActionIcons.ChevronRight,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.secondaryText),
                        modifier = Modifier.size(22.dp).rotate(if (isExpanded) 90f else 0f)
                            .clickable { expanded[node.path] = !isExpanded },
                    )
                } else {
                    components.text("•", Modifier.width(22.dp), colors.secondaryText, 1)
                }
                if (editing) {
                    components.input(draft, { if (interactive) draft = it }, Modifier.weight(1f), interactive, null)
                    ActionIcon(JsonFormsActionIcons.Confirm, confirmLabel, colors.text) {
                        if (interactive) onRename(node, draft)
                        editing = false
                    }
                    ActionIcon(JsonFormsActionIcons.Cancel, cancelLabel, colors.secondaryText) {
                        draft = node.label
                        editing = false
                    }
                } else {
                    components.text(node.type.treeGlyph(), Modifier.width(26.dp), colors.secondaryText, 1)
                    components.text(node.label, Modifier.weight(1f), colors.text, 1)
                    if (depth == 0) {
                        ActionIcon(
                            if (showPrimitives) JsonFormsActionIcons.Visibility else JsonFormsActionIcons.VisibilityOff,
                            if (showPrimitives) hidePrimitivesLabel else showPrimitivesLabel,
                            colors.secondaryText,
                            onTogglePrimitives,
                        )
                    }
                    Row(Modifier.graphicsLayer { alpha = if (showActions) 1f else 0f }) {
                        if (interactive && node.canRename) {
                            ActionIcon(JsonFormsActionIcons.Rename, renameLabel, colors.secondaryText) { editing = true }
                        }
                        if (interactive && node.canDelete) {
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
                interactive,
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
    val components = LocalMixedTreeComponents.current
    MixedTreeSurface(colors.container, colors.border, 10.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            components.text(node.label, Modifier, colors.text, Int.MAX_VALUE)
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

@Composable
private fun ActionIcon(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    LocalMixedTreeComponents.current.action(icon, label, tint, onClick)
}

@Composable
private fun MixedTreeSurface(
    color: Color,
    borderColor: Color?,
    radius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    val border = if (borderColor == null) Modifier else Modifier.border(BorderStroke(1.dp, borderColor), shape)
    Box(modifier.background(color, shape).then(border), content = { content() })
}
