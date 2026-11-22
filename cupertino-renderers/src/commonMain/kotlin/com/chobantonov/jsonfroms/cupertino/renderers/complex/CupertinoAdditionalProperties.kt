package com.chobantonov.jsonfroms.cupertino.renderers.complex

import com.chobantonov.jsonfroms.core.i18n.translateConfirmationText

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.preserveDynamicValueOnClear
import com.chobantonov.jsonfroms.compose.rememberPendingEditOwner
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.confirmation.ConfirmationOperation
import com.chobantonov.jsonfroms.core.confirmation.resolveConfirmationDecision
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControlWithDetail
import com.chobantonov.jsonfroms.core.mappers.addAdditionalProperty
import com.chobantonov.jsonfroms.core.mappers.additionalPropertyKeys
import com.chobantonov.jsonfroms.core.mappers.additionalPropertySchema
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.collectionPageWindow
import com.chobantonov.jsonfroms.core.mappers.pageForFirstVisibleItem
import com.chobantonov.jsonfroms.core.mappers.preserveIsolatedAdditionalValue
import com.chobantonov.jsonfroms.core.mappers.rebaseLocalSchemaReferences
import com.chobantonov.jsonfroms.core.mappers.renameAdditionalProperty
import com.chobantonov.jsonfroms.core.mappers.resolveCollectionPagination
import com.chobantonov.jsonfroms.core.mappers.supportsAdditionalProperties
import com.chobantonov.jsonfroms.core.mappers.validateAdditionalPropertyName
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.store.getValidator
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.core.util.resolveRestrict
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.slapps.cupertino.CupertinoAlertDialog
import com.slapps.cupertino.CupertinoText
import com.slapps.cupertino.ExperimentalCupertinoApi
import com.slapps.cupertino.cancel
import com.slapps.cupertino.destructive
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
import com.chobantonov.jsonfroms.material.renderers.additional.MaterialCollectionPager
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@Composable
internal fun CupertinoAdditionalProperties(
    props: ComposeJsonFormsRendererProps,
    controlProps: StatePropsOfControlWithDetail,
) {
    val schema = controlProps.schema
    if (!supportsAdditionalProperties(schema, controlProps.config)) return
    val colors = LocalCupertinoColors.current
    val data = controlProps.data as? JsonObject ?: JsonObject(emptyMap())
    val keys = additionalPropertyKeys(schema, data)
    val localPagination = (controlProps.uischema.options["additionalProperties"] as? JsonObject)
        ?.get("pagination")
    val pagination = resolveCollectionPagination(
        localPagination?.let { mapOf("pagination" to it) } ?: emptyMap(),
        controlProps.config, kind = "additionalProperties",
    )
    var pageSize by remember(controlProps.schema, controlProps.path) { mutableStateOf(pagination.pageSize) }
    var currentPage by remember(controlProps.schema, controlProps.path) { mutableStateOf(1) }
    var previousKeys by remember(controlProps.schema, controlProps.path) { mutableStateOf(keys) }
    var pendingAddedKey by remember(controlProps.path) { mutableStateOf<String?>(null) }
    var pendingRenamedFocusKey by remember(controlProps.path) { mutableStateOf<String?>(null) }
    LaunchedEffect(pagination) {
        pageSize = pagination.pageSize
        currentPage = 1
    }
    LaunchedEffect(keys, pageSize, pagination.enabled) {
        val addedIndex = pendingAddedKey?.let(keys::indexOf) ?: -1
        if (pagination.enabled && addedIndex >= 0 && pendingAddedKey !in previousKeys)
            currentPage = addedIndex / pageSize + 1
        else currentPage = currentPage.coerceAtMost(
            collectionPageWindow(keys.size, currentPage, pageSize, pagination.enabled).pages,
        )
        pendingAddedKey = null
        previousKeys = keys
    }
    val window = collectionPageWindow(keys.size, currentPage, pageSize, pagination.enabled)
    val visibleKeys = keys.subList(window.start, window.endExclusive)
    val context = com.chobantonov.jsonfroms.compose.useJsonForms()
    val validator = getValidator(props.state)
    val pendingEditor = context.controller.hasPendingOwnedWork
    val errorsByKey = context.containerErrorIndex.countsByImmediateChild(controlProps.path)
    val errorPages = if (pagination.enabled) keys.withIndex()
        .filter { errorsByKey[it.value] != null }
        .groupBy({ it.index / pageSize + 1 }, { errorsByKey[it.value] ?: 0 })
        .mapValues { (_, counts) -> counts.sum() }
        .filterKeys { it != window.page }
    else emptyMap()
    val interactive = controlProps.enabled && !controlProps.readonly
    val restrict = resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config)
    val localAllowEmpty = (controlProps.uischema.options["allowEmptyPropertyNames"] as? JsonPrimitive)?.booleanOrNull
    val nameConfig = if (localAllowEmpty == null) controlProps.config else
        controlProps.config + ("allowEmptyPropertyNames" to localAllowEmpty)
    val canAdd = interactive && !pendingEditor && (!restrict || schema.maxProperties == null || data.size < schema.maxProperties!!)
    val canRemove = interactive && !pendingEditor && (!restrict || schema.minProperties == null || data.size > schema.minProperties!!)
    var newName by remember(controlProps.path) { mutableStateOf("") }
    var addError by remember(controlProps.path) { mutableStateOf<String?>(null) }
    LaunchedEffect(interactive, data) {
        newName = ""
        addError = null
    }
    val emptyDraftAllowed = newName.isNotEmpty() ||
        (validateAdditionalPropertyName(schema, "", data.keys, nameConfig, controlProps.rootSchema, validator) == null &&
            additionalPropertySchema(schema, "", controlProps.rootSchema) != null)
    val text = additionalPropertiesText(props, controlProps)

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text.title, style = MaterialTheme.typography.titleMedium, color = colors.label)
        if (interactive) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CupertinoPropertyNameField(newName, addError, text.propertyNameLabel, Modifier.weight(1f)) {
                    newName = it; addError = null
                }
                ActionTooltip(text.add.tooltip) {
                    FilledIconButton(
                        enabled = canAdd && emptyDraftAllowed,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.accent,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                        onClick = {
                            if (!canAdd) return@FilledIconButton
                            val name = newName
                            val error = validateAdditionalPropertyName(
                                schema, name, data.keys, nameConfig, controlProps.rootSchema, validator,
                            )
                            val propertySchema = additionalPropertySchema(schema, name, controlProps.rootSchema)
                            if (error != null || propertySchema == null) addError = text.error(error, name)
                            else {
                                pendingAddedKey = name
                                updateObject(props, controlProps.path) { current ->
                                    if (!controlProps.enabled || controlProps.readonly) current else
                                        addAdditionalProperty(current, data, name, schema,
                                            nameConfig, controlProps.rootSchema, restrict, validator)
                                }
                                newName = ""
                            }
                        },
                    ) {
                        Icon(JsonFormsActionIcons.Add, contentDescription = text.add.ariaLabel)
                    }
                }
            }
        }
        visibleKeys.forEach { key -> CupertinoAdditionalProperty(
            props, controlProps, data, key, canRemove, interactive && !pendingEditor,
            pendingRenamedFocusKey == key,
            onRenamed = { pendingRenamedFocusKey = it },
            onFocusHandled = { pendingRenamedFocusKey = null },
            nameConfig = nameConfig,
            validator = validator,
            text = text,
        ) }
        MaterialCollectionPager(
            pagination, window, pageSize, keys.size, errorPages,
            props.state.jsonforms.i18n.translate,
            canNavigate = !pendingEditor,
            onChange = { requestedPage, requestedSize ->
                if (pendingEditor) return@MaterialCollectionPager
                currentPage = if (requestedSize == pageSize) requestedPage else
                    pageForFirstVisibleItem(window.start, requestedSize)
                pageSize = requestedSize
            },
        )
    }
}

private fun additionalPropertiesTitle(schema: com.chobantonov.jsonfroms.core.models.JsonSchema): String =
    (schema.additionalProperties as? com.chobantonov.jsonfroms.core.models.JsonSchemaOrBoolean.SchemaValue)
        ?.schema?.title ?: "Additional Properties"

@Composable
private fun CupertinoAdditionalProperty(
    props: ComposeJsonFormsRendererProps,
    controlProps: StatePropsOfControlWithDetail,
    data: JsonObject,
    propertyName: String,
    canRemove: Boolean,
    canStartRename: Boolean,
    focusAfterRename: Boolean,
    onRenamed: (String) -> Unit,
    onFocusHandled: () -> Unit,
    nameConfig: Map<String, Any?>,
    validator: com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator,
    text: AdditionalPropertiesText,
) {
    val schema = additionalPropertySchema(controlProps.schema, propertyName, controlProps.rootSchema) ?: return
    val colors = LocalCupertinoColors.current
    val interactive = controlProps.enabled && !controlProps.readonly
    var renaming by remember(controlProps.path, propertyName) { mutableStateOf(false) }
    var renamedValue by remember(controlProps.path, propertyName) { mutableStateOf(propertyName) }
    var renameError by remember(controlProps.path, propertyName) { mutableStateOf<String?>(null) }
    var pendingRemoval by remember(controlProps.path, propertyName) { mutableStateOf<PendingPropertyRemoval?>(null) }
    val setRenamePending = rememberPendingEditOwner("additional-rename:${controlProps.path}:$propertyName")
    LaunchedEffect(renaming) { setRenamePending(renaming) }
    LaunchedEffect(interactive, data) {
        if (!interactive || renaming) {
            renaming = false
            renamedValue = propertyName
            renameError = null
        }
    }
    val valueFocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusAfterRename, renaming) {
        if (focusAfterRename && !renaming) {
            valueFocusRequester.requestFocus()
            onFocusHandled()
        }
    }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (renaming) {
            CupertinoPropertyNameField(renamedValue, renameError, text.renamePropertyNameLabel, Modifier.fillMaxWidth()) {
                renamedValue = it; renameError = null
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionTooltip(text.confirm.tooltip) {
                    IconButton(enabled = interactive, onClick = {
                        if (!interactive) return@IconButton
                        val name = renamedValue
                        val error = validateAdditionalPropertyName(
                            controlProps.schema, name, data.keys - propertyName, nameConfig, controlProps.rootSchema, validator,
                        )
                        if (error != null || additionalPropertySchema(controlProps.schema, name, controlProps.rootSchema) == null) {
                            renameError = text.error(error, name)
                        } else {
                            updateObject(props, controlProps.path) { current ->
                                if (!controlProps.enabled || controlProps.readonly) current else
                                    renameAdditionalProperty(current, data, propertyName, name,
                                        controlProps.schema, nameConfig, controlProps.rootSchema, validator)
                            }
                            onRenamed(name)
                            renaming = false
                        }
                    }) {
                        Icon(JsonFormsActionIcons.Confirm, text.confirm.ariaLabel, tint = colors.accent)
                    }
                }
                ActionTooltip(text.cancel.tooltip) {
                    IconButton(onClick = { renamedValue = propertyName; renameError = null; renaming = false }) {
                        Icon(JsonFormsActionIcons.Cancel, text.cancel.ariaLabel, tint = colors.secondaryLabel)
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f).focusRequester(valueFocusRequester)) {
                    CupertinoAdditionalPropertyValue(props, controlProps, schema, propertyName)
                }
                if (interactive) ActionTooltip(text.rename.tooltip) {
                    IconButton(enabled = canStartRename, onClick = { renaming = true }) {
                        Icon(
                            JsonFormsActionIcons.Rename,
                            text.rename.ariaLabel,
                            Modifier.size(18.dp),
                            tint = colors.accent,
                        )
                    }
                }
                if (interactive) ActionTooltip(text.remove.tooltip) {
                    IconButton(
                        enabled = canRemove,
                        onClick = {
                            val value = data[propertyName] ?: return@IconButton
                            val decision = resolveConfirmationDecision(
                                ConfirmationOperation.Delete, "additionalProperties", listOf(value),
                                controlProps.uischema.options, controlProps.config,
                            )
                            if (decision.required) pendingRemoval = PendingPropertyRemoval(propertyName, value, data)
                            else updateObject(props, controlProps.path) { current ->
                                if (current === data && current[propertyName] == value && !restrictRemoval(controlProps, current))
                                    JsonObject(current - propertyName) else current
                            }
                        },
                    ) {
                        Icon(
                            JsonFormsActionIcons.Delete,
                            text.remove.ariaLabel,
                            Modifier.size(18.dp),
                            tint = colors.destructive,
                        )
                    }
                }
            }
        }
        if (renaming) CupertinoAdditionalPropertyValue(props, controlProps, schema, propertyName)
    }
    SideEffect {
        if (pendingRemoval?.let { !canRemove || it.snapshot !== data || data[it.propertyName] != it.value } == true)
            pendingRemoval = null
    }
    val request = pendingRemoval?.takeIf { canRemove && it.snapshot === data && data[it.propertyName] == it.value }
    if (request != null) CupertinoRemoveDialog(
        props = props,
        onDismiss = { if (pendingRemoval === request) pendingRemoval = null },
        onConfirm = {
            if (pendingRemoval === request && canRemove && request.snapshot === data && data[request.propertyName] == request.value) {
                updateObject(props, controlProps.path) { current ->
                    if (current === request.snapshot && current[request.propertyName] == request.value && !restrictRemoval(controlProps, current))
                        JsonObject(current - request.propertyName) else current
                }
            }
            if (pendingRemoval === request) pendingRemoval = null
        },
    )
}

private data class PendingPropertyRemoval(val propertyName: String, val value: JsonElement, val snapshot: JsonObject)

@OptIn(ExperimentalCupertinoApi::class)
@Composable
private fun CupertinoRemoveDialog(props: ComposeJsonFormsRendererProps, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    fun translated(key: String, fallback: String) =
        translateConfirmationText(props.state.jsonforms.i18n, key, fallback)
    CupertinoAlertDialog(
        onDismissRequest = onDismiss,
        title = { CupertinoText(translated("confirm.delete.title", "Delete this value?")) },
        message = { CupertinoText(translated("confirm.delete.message", "The value will be removed. This cannot be undone from here.")) },
        buttons = {
            cancel(onClick = onDismiss) { CupertinoText(translated("confirm.decline", "No")) }
            destructive(onClick = onConfirm) { CupertinoText(translated("confirm.accept", "Yes")) }
        },
    )
}

private fun restrictRemoval(controlProps: StatePropsOfControlWithDetail, data: JsonObject): Boolean {
    if (!controlProps.enabled || controlProps.readonly) return true
    val restrict = resolveRestrict(controlProps.uischema.options["restrict"], controlProps.config)
    return restrict && controlProps.schema.minProperties?.let { data.size <= it } == true
}

@Composable
private fun CupertinoAdditionalPropertyValue(
    props: ComposeJsonFormsRendererProps,
    controlProps: StatePropsOfControlWithDetail,
    schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
    propertyName: String,
) {
    if (propertyName.isEmpty() || '.' in propertyName || propertyName.startsWith('[')) {
        val parentData = controlProps.data as? JsonObject ?: return
        val value = parentData[propertyName] ?: return
        val isolatedSchema = remember(schema, controlProps.rootSchema) {
            rebaseLocalSchemaReferences(schema, controlProps.rootSchema)
        }
        JsonForms(
            data = value,
            schema = isolatedSchema,
            uischema = ControlElement(scope = "#", label = StringControlLabel(propertyName)),
            renderers = props.renderers,
            cells = props.cells,
            config = controlProps.config,
            uischemas = props.state.jsonforms.uischemas,
            readonly = controlProps.readonly || !controlProps.enabled,
            validationMode = props.state.jsonforms.core.validationMode,
            i18n = props.state.jsonforms.i18n,
            onChange = { change ->
                val replacement = preserveIsolatedAdditionalValue(change.data, schema, controlProps.rootSchema)
                if (replacement != value) updateObject(props, controlProps.path) { current ->
                    if (current === parentData && current[propertyName] == value &&
                        controlProps.enabled && !controlProps.readonly
                    ) JsonObject(current + (propertyName to replacement)) else current
                }
            },
        )
        return
    }
    val path = composePaths(controlProps.path, propertyName)
    androidx.compose.runtime.CompositionLocalProvider(LocalPreserveEmptyValue provides true) {
        JsonFormsDispatchRenderer(
            props.copy(
                dispatch = preserveDynamicValueOnClear(
                    dispatch = props.dispatch,
                    path = path,
                    emptyValue = createDefaultValue(schema, controlProps.rootSchema),
                ),
                schema = schema,
                rootSchema = controlProps.rootSchema,
                uischema = ControlElement(scope = "#", label = StringControlLabel(propertyName)),
                path = path,
                enabled = controlProps.enabled,
                id = "${controlProps.id}-additional-$propertyName",
            ),
        )
    }
}

@Composable
private fun CupertinoPropertyNameField(
    value: String,
    error: String?,
    label: String,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    val colors = LocalCupertinoColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().border(
                1.dp, if (error == null) colors.controlBorder else colors.destructive, RoundedCornerShape(10.dp),
            ).padding(horizontal = 12.dp, vertical = 10.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.label),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, color = colors.secondaryLabel)
                inner()
            },
        )
        if (error != null) Text(error, style = MaterialTheme.typography.bodySmall, color = colors.destructive)
    }
}

private data class ActionText(val tooltip: String, val ariaLabel: String)

private data class AdditionalPropertiesText(
    val title: String,
    val propertyNameLabel: String,
    val renamePropertyNameLabel: String,
    val add: ActionText,
    val rename: ActionText,
    val remove: ActionText,
    val confirm: ActionText,
    val cancel: ActionText,
    val translateError: (String?, String) -> String,
) {
    fun error(error: String?, propertyName: String): String = translateError(error, propertyName)
}

private fun additionalPropertiesText(
    props: ComposeJsonFormsRendererProps,
    controlProps: StatePropsOfControlWithDetail,
): AdditionalPropertiesText {
    val translator = props.state.jsonforms.i18n.translate
    val prefix = controlProps.i18nKeyPrefix
        ?.takeIf(String::isNotBlank)
        ?.let { "$it.additionalProperties" }
        ?: "root.additionalProperties"
    val values = mapOf("control" to controlProps.label)
    fun translated(key: String, fallback: String, extra: Map<String, Any?> = emptyMap()): String =
        com.chobantonov.jsonfroms.core.i18n.translateAdditionalPropertiesText(
            translator, prefix, key, fallback, values + extra + ("label" to controlProps.label),
        )
    fun action(key: String, fallback: String) = ActionText(
        tooltip = translated("${key}Tooltip", fallback),
        ariaLabel = translated("${key}AriaLabel", "$fallback button"),
    )
    return AdditionalPropertiesText(
        title = translated("title", additionalPropertiesTitle(controlProps.schema)),
        propertyNameLabel = translated("propertyNameLabel", "Property name"),
        renamePropertyNameLabel = translated("renamePropertyNameLabel", "New property name"),
        add = action("add", "Add"),
        rename = action("rename", "Rename"),
        remove = action("remove", "Delete"),
        confirm = action("save", "Save"),
        cancel = action("cancel", "Cancel"),
        translateError = { error, propertyName ->
            val key = when {
                error?.contains("already exists") == true -> "propertyAlreadyDefined"
                error?.contains("required") == true -> "propertyNameRequired"
                else -> "propertyNameInvalid"
            }
            translated(key, error ?: "Property name '$propertyName' is invalid",
                mapOf("propertyName" to propertyName, "name" to propertyName))
        },
    )
}

private fun updateObject(
    props: ComposeJsonFormsRendererProps,
    path: String,
    update: (JsonObject) -> JsonObject,
) {
    props.dispatch(UpdateAction(path = path, updater = { update(it as? JsonObject ?: JsonObject(emptyMap())) }))
}
