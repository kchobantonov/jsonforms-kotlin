package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.mappers.CombinatorSubSchemaRenderInfo
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControl
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.util.JsonPath
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal data class PendingBranchChange(
    val index: Int,
    val root: JsonElement,
    val value: JsonElement?,
)

internal data class BranchChangeController(
    val pending: PendingBranchChange?,
    val interactive: Boolean,
    val request: (Int) -> Unit,
    val clear: () -> Unit,
    val confirm: (PendingBranchChange) -> Unit,
    val dismiss: (PendingBranchChange) -> Unit,
)

internal fun enclosingOneOfProperties(value: JsonElement?, schema: JsonSchema): JsonObject? {
    val source = value as? JsonObject ?: return null
    val declared = schema["properties"] as? JsonObject ?: return null
    return JsonObject(source.filterKeys { it in declared })
}

internal fun clearOneOfBranch(value: JsonElement?, schema: JsonSchema): JsonElement? =
    enclosingOneOfProperties(value, schema)?.takeIf { it.isNotEmpty() }

internal fun discardedOneOfBranchValue(value: JsonElement?, schema: JsonSchema): JsonElement? {
    val source = value as? JsonObject ?: return value
    val declared = schema["properties"] as? JsonObject ?: return value
    val discarded = JsonObject(source.filterKeys { it !in declared })
    return discarded.takeUnless { it.isEmpty() && source.isNotEmpty() }
}

@Composable
internal fun rememberBranchChangeController(
    props: ComposeJsonFormsRendererProps,
    state: StatePropsOfControl,
    control: ControlElement,
    catalogId: String,
    renderInfos: List<CombinatorSubSchemaRenderInfo>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
): BranchChangeController {
    val root = props.state.jsonforms.core.data
    val interactive = state.enabled && !state.readonly
    var pending by remember(state.path, state.schema, control, catalogId) {
        mutableStateOf<PendingBranchChange?>(null)
    }
    LaunchedEffect(root, interactive) {
        if (!interactive || pending?.root !== root) pending = null
    }
    fun apply(request: PendingBranchChange) {
        if (!interactive || root !== request.root || request.index !in -1 until renderInfos.size ||
            JsonPath.get(root, state.path) !== request.value) return
        props.dispatch(UpdateAction(path = state.path, updater = { current ->
            if (current !== request.value) current
            else if (request.index == -1) clearOneOfBranch(current, state.schema)
            else {
                val generated = createDefaultValue(renderInfos[request.index].schema, state.rootSchema)
                val preserved = enclosingOneOfProperties(current, state.schema)
                if (generated is JsonObject && preserved != null) JsonObject(generated + preserved)
                else generated
            }
        }))
        onSelected(request.index)
    }
    return BranchChangeController(
        pending = pending?.takeIf { interactive && it.root === root &&
            it.index in -1 until renderInfos.size && JsonPath.get(root, state.path) === it.value },
        interactive = interactive,
        request = { index ->
            if (interactive && index in renderInfos.indices && index != selectedIndex) {
                val value = JsonPath.get(root, state.path)
                val request = PendingBranchChange(index, root, value)
                val discarded = if (catalogId == "oneOf") {
                    if (selectedIndex == -1) emptyList()
                    else listOf(discardedOneOfBranchValue(value, state.schema))
                } else listOf(value)
                val decision = resolveConfirmationDecision(
                    ConfirmationOperation.BranchChange, catalogId, discarded,
                    control.options, state.config,
                )
                if (decision.required) pending = request else apply(request)
            }
        },
        clear = {
            if (catalogId == "oneOf" && interactive && selectedIndex in renderInfos.indices &&
                JsonPath.get(root, state.path) != null) {
                val value = JsonPath.get(root, state.path)
                val request = PendingBranchChange(-1, root, value)
                val decision = resolveConfirmationDecision(
                    ConfirmationOperation.BranchChange, catalogId,
                    listOf(discardedOneOfBranchValue(value, state.schema)),
                    control.options, state.config,
                )
                if (decision.required) pending = request else apply(request)
            }
        },
        confirm = { request ->
            if (pending === request) apply(request)
            if (pending === request) pending = null
        },
        dismiss = { request -> if (pending === request) pending = null },
    )
}

@Composable
internal fun BranchChangeDialog(
    controller: BranchChangeController,
    i18n: JsonFormsI18nState,
    dialog: @Composable (String, String, String, String, () -> Unit, () -> Unit) -> Unit,
) {
    val request = controller.pending ?: return
    fun text(key: String, fallback: String): String =
        i18n.translate?.translate(key, fallback, emptyMap()) ?: fallback
    dialog(
        text("confirm.branchChange.title", "Change this branch?"),
        text("confirm.branchChange.message", "The current value will be replaced."),
        text("confirm.accept", "Yes"),
        text("confirm.decline", "No"),
        { controller.confirm(request) },
        { controller.dismiss(request) },
    )
}

@Composable
internal fun MaterialBranchConfirmationDialog(
    title: String, message: String, accept: String, decline: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(accept) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(decline) } },
    )
}
