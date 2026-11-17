package com.chobantonov.jsonfroms.compose

import com.chobantonov.jsonfroms.core.i18n.translateTupleValidationIndicator
import com.chobantonov.jsonfroms.core.i18n.translateTupleRendererText

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.defaultTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.mappers.TupleSchema
import com.chobantonov.jsonfroms.core.mappers.canEmptyCompositeDraft
import com.chobantonov.jsonfroms.core.mappers.resolveContainerValidationOptions
import com.chobantonov.jsonfroms.core.mappers.tupleInitialValue
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.toUiSchemaElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.getErrorAt
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.resolveArrayActionPolicy
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull

private data class TupleDraftSession(
    val original: JsonElement?,
    val root: JsonElement,
    val revision: Int = 0,
)

@Composable
fun TupleComplexPosition(
    props: ComposeJsonFormsRendererProps,
    arrayPath: String,
    rootSchema: JsonSchema,
    schema: JsonSchema,
    schemaPath: String,
    tuple: TupleSchema,
    index: Int,
    label: String,
    items: JsonArray,
    control: ControlElement,
    interactive: Boolean,
    summaryContent: @Composable (
        summary: String, indicatorLabel: String?, indicatorCount: Int?,
        ownErrorText: String?, editLabel: String, onEdit: () -> Unit,
    ) -> Unit,
    dialogContent: @Composable (
        title: String,
        content: @Composable () -> Unit,
        applyLabel: String,
        cancelLabel: String,
        clearLabel: String?,
        onClear: (() -> Unit)?,
        onApply: () -> Unit,
        onCancel: () -> Unit,
    ) -> Unit,
) {
    val path = composePaths(arrayPath, index.toString())
    val selected = findMatchingUISchema(props.state.jsonforms.uischemas)(schema, schemaPath, path)
    val selectedControl = selected as? ControlElement
    val detail: UiSchemaElement = when (selected) {
        is ControlElement -> (selected.options["detail"] as? JsonObject)
            ?.let { runCatching { it.toUiSchemaElement() }.getOrNull() }
            ?: Generate.uiSchema(schema, rootSchema = rootSchema)
        null -> Generate.uiSchema(schema, rootSchema = rootSchema)
        else -> selected
    }
    val translator = props.state.jsonforms.i18n.translate
    fun translated(key: String, fallback: String, values: Map<String, Any?> = emptyMap()): String =
        translateTupleRendererText(props.state.jsonforms.i18n, key, fallback, values)
    fun actionLabel(option: String, key: String, fallback: String): String {
        val selectedLabel = (selectedControl?.options?.get(option) as? JsonPrimitive)?.contentOrNull
        val parentLabel = (control.options[option] as? JsonPrimitive)?.contentOrNull
        return (selectedLabel ?: parentLabel)?.let { translated(it, it) } ?: translated(key, fallback)
    }
    val value = items.getOrNull(index)
    val summary = tuplePositionSummary(value, selectedControl?.options?.get("summary"), ::translated)
    var session by remember(path, schema, detail) { mutableStateOf<TupleDraftSession?>(null) }
    LaunchedEffect(value, session?.original) {
        if (session != null && session?.original != value) session = null
    }
    val editLabel = translated("tuple.edit", "Edit $label", mapOf("label" to label))
    val validationOptions = resolveContainerValidationOptions(
        control.options + selectedControl?.options.orEmpty(), props.config, defaultIndicator = true,
    )
    val errorIndex = useJsonForms().containerErrorIndex
    val scopes = listOf(emptyList<String>())
    val hasErrors = validationOptions.showIndicator && errorIndex.containsAny(scopes, path)
    val indicatorCount = if (hasErrors && validationOptions.showCount) {
        errorIndex.countAny(scopes, path)
    } else null
    val indicatorLabel = if (hasErrors) translateTupleValidationIndicator(
        props.state.jsonforms.i18n, indicatorCount) else null
    val ownErrorText = getCombinedErrorMessage(
        getErrorAt(path, schema)(props.state),
        props.state.jsonforms.i18n.translateError ?: defaultErrorTranslator,
        translator ?: defaultTranslator,
        schema, selectedControl ?: control, path,
    ).takeIf(String::isNotBlank)
    summaryContent(summary, indicatorLabel, indicatorCount, ownErrorText, editLabel) {
        if (interactive) session = TupleDraftSession(value, props.state.jsonforms.core.data)
    }
    val open = session
    if (open != null) {
        val pointer = if (path.isEmpty()) "#" else "#/" + path.split('.')
            .joinToString("/") { JsonPointer.encode(it) }
        val draftValue = JsonPointer.resolve(open.root, pointer)
        val empty = when (draftValue) {
            is JsonObject -> JsonObject(emptyMap())
            is JsonArray -> JsonArray(emptyList())
            else -> null
        }
        val showEmpty = (selectedControl?.options?.get("showEmptyButton") as? JsonPrimitive)?.booleanOrNull
            ?: (control.options["showEmptyButton"] as? JsonPrimitive)?.booleanOrNull
            ?: (props.config["showEmptyButton"] as? Boolean ?: false)
        val restrict = resolveRestrict(
            selectedControl?.options?.get("restrict") ?: control.options["restrict"], props.config)
        val disableRemove = resolveArrayActionPolicy(
            control.options + selectedControl?.options.orEmpty(), props.config).disableRemove
        val canClear = showEmpty && empty != null && draftValue != empty && interactive && !disableRemove &&
            canEmptyCompositeDraft(draftValue, schema, rootSchema, restrict,
                props.state.jsonforms.core.validator, open.root, path)
        dialogContent(
            label,
            {
                key(open.revision) {
                    TupleDraftForm(
                        props = props.copy(enabled = interactive, rootSchema = rootSchema),
                        rootData = open.root,
                        positionSchema = schema,
                        positionPath = path,
                        detail = detail,
                        onChange = { changed ->
                            if (session?.revision == open.revision) session = session?.copy(root = changed)
                        },
                    )
                }
            },
            actionLabel("okLabel", "composite.apply", "Apply"),
            actionLabel("cancelLabel", "composite.cancel", "Cancel"),
            if (showEmpty) actionLabel("emptyLabel", "composite.empty", "Clear") else null,
            if (canClear) {
                { val current = session
                    if (current != null && current.revision == open.revision && interactive &&
                        canEmptyCompositeDraft(draftValue, schema, rootSchema, restrict,
                            props.state.jsonforms.core.validator, current.root, path)) {
                        session = current.copy(
                            root = replaceTupleDraftValue(current.root, path.split('.'), requireNotNull(empty)),
                            revision = current.revision + 1,
                        )
                    }
                }
            } else null,
            {
                val current = session
                if (current != null && interactive && items.getOrNull(index) == current.original) {
                    val edited = JsonPointer.resolve(current.root, pointer)
                    if (edited != null && edited != current.original) {
                        props.dispatch(UpdateAction(arrayPath, updater = { existing ->
                            val values = (existing as? JsonArray)?.toMutableList() ?: mutableListOf()
                            if (values.getOrNull(index) != current.original) return@UpdateAction existing
                            if (index >= values.size) {
                                for (missing in values.size until index) {
                                    val initial = tuple.schemaAt(missing)?.let { tupleInitialValue(it, rootSchema) }
                                        ?: return@UpdateAction existing
                                    values += initial
                                }
                                values += edited
                            } else values[index] = edited
                            JsonArray(values)
                        }))
                    }
                }
                session = null
            },
            { session = null },
        )
    }
}

private fun replaceTupleDraftValue(root: JsonElement, segments: List<String>, value: JsonElement): JsonElement {
    if (segments.isEmpty()) return value
    val head = segments.first()
    val tail = segments.drop(1)
    return when (root) {
        is JsonObject -> root[head]?.let { child ->
            JsonObject(root + (head to replaceTupleDraftValue(child, tail, value)))
        } ?: root
        is JsonArray -> head.toIntOrNull()?.takeIf { it in root.indices }?.let { index ->
            JsonArray(root.mapIndexed { current, child ->
                if (current == index) replaceTupleDraftValue(child, tail, value) else child
            })
        } ?: root
        else -> root
    }
}

private fun tuplePositionSummary(
    value: JsonElement?,
    summaryOption: JsonElement?,
    translate: (String, String, Map<String, Any?>) -> String,
): String {
    if (value == null) return translate("composite.summary.unset", "Not set", emptyMap())
    val scope = ((summaryOption as? JsonObject)?.get("scope") as? JsonPrimitive)?.contentOrNull
    if (value is JsonArray) {
        val previews = value.mapNotNull { item ->
            val selected = resolveSummaryScope(item, scope ?: "#") as? JsonPrimitive
            selected?.takeUnless { it == JsonNull }?.contentOrNull?.takeIf(String::isNotBlank)
        }.take(2)
        if (previews.isNotEmpty()) {
            val remaining = value.size - previews.size
            val more = if (remaining > 0) " " + translate(
                "composite.summary.more", "(+$remaining more)", mapOf("count" to remaining),
            ) else ""
            return previews.joinToString(", ") + more
        }
        val key = if (value.size == 1) "composite.summary.item" else "composite.summary.items"
        val fallback = if (value.size == 1) "1 item" else "${value.size} items"
        return translate(key, fallback, mapOf("count" to value.size))
    }
    val selected = resolveSummaryScope(value, scope ?: "#") as? JsonPrimitive
    if (scope != null && selected != null && selected != JsonNull && selected.content.isNotBlank()) {
        return selected.content
    }
    return translate("composite.summary.details", "View details", emptyMap())
}

private fun resolveSummaryScope(value: JsonElement, scope: String): JsonElement? {
    if (scope == "#") return value
    if (!scope.startsWith("#/")) return null
    val parts = scope.removePrefix("#/").split('/')
    val dataParts = mutableListOf<String>()
    var index = 0
    while (index < parts.size) {
        if (parts[index] !in setOf("properties", "items", "prefixItems") || index + 1 >= parts.size) return null
        dataParts += parts[index + 1]
        index += 2
    }
    return JsonPointer.resolve(value, "#/" + dataParts.joinToString("/"))
}
