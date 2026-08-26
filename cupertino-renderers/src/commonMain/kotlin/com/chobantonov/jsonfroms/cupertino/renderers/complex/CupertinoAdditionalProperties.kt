package com.chobantonov.jsonfroms.cupertino.renderers.complex

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsActionIcons
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.compose.LocalPreserveEmptyValue
import com.chobantonov.jsonfroms.compose.preserveDynamicValueOnClear
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.mappers.StatePropsOfControlWithDetail
import com.chobantonov.jsonfroms.core.mappers.additionalPropertyKeys
import com.chobantonov.jsonfroms.core.mappers.additionalPropertySchema
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.supportsAdditionalProperties
import com.chobantonov.jsonfroms.core.mappers.validateAdditionalPropertyName
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.StringControlLabel
import com.chobantonov.jsonfroms.core.util.composePaths
import com.chobantonov.jsonfroms.cupertino.renderers.ActionTooltip
import com.chobantonov.jsonfroms.cupertino.renderers.util.LocalCupertinoColors
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
    val interactive = controlProps.enabled && !controlProps.readonly
    val restrict = (controlProps.uischema.options["restrict"] as? JsonPrimitive)?.booleanOrNull
        ?: controlProps.config["restrict"] as? Boolean ?: false
    val canAdd = interactive && (!restrict || schema.maxProperties == null || data.size < schema.maxProperties!!)
    val canRemove = interactive && (!restrict || schema.minProperties == null || data.size > schema.minProperties!!)
    var newName by remember(controlProps.path) { mutableStateOf("") }
    var addError by remember(controlProps.path) { mutableStateOf<String?>(null) }
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
                        enabled = canAdd,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.accent,
                            contentColor = androidx.compose.ui.graphics.Color.White,
                        ),
                        onClick = {
                            val name = newName.trim()
                            val error = validateAdditionalPropertyName(
                                schema, name, data.keys, controlProps.config, controlProps.rootSchema,
                            )
                            val propertySchema = additionalPropertySchema(schema, name, controlProps.rootSchema)
                            if (error != null || propertySchema == null) addError = text.error(error, name)
                            else {
                                updateObject(props, controlProps.path) { current ->
                                    JsonObject(current + (name to createDefaultValue(propertySchema, controlProps.rootSchema)))
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
        keys.forEach { key -> CupertinoAdditionalProperty(props, controlProps, data, key, canRemove, text) }
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
    text: AdditionalPropertiesText,
) {
    val schema = additionalPropertySchema(controlProps.schema, propertyName, controlProps.rootSchema) ?: return
    val colors = LocalCupertinoColors.current
    val interactive = controlProps.enabled && !controlProps.readonly
    var renaming by remember(controlProps.path, propertyName) { mutableStateOf(false) }
    var renamedValue by remember(controlProps.path, propertyName) { mutableStateOf(propertyName) }
    var renameError by remember(controlProps.path, propertyName) { mutableStateOf<String?>(null) }
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
                    IconButton(onClick = {
                        val name = renamedValue.trim()
                        val error = validateAdditionalPropertyName(
                            controlProps.schema, name, data.keys - propertyName, controlProps.config, controlProps.rootSchema,
                        )
                        if (error != null || additionalPropertySchema(controlProps.schema, name, controlProps.rootSchema) == null) {
                            renameError = text.error(error, name)
                        } else {
                            updateObject(props, controlProps.path) { current ->
                                val renamed = linkedMapOf<String, JsonElement>()
                                current.forEach { (key, value) -> renamed[if (key == propertyName) name else key] = value }
                                JsonObject(renamed)
                            }
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
                Box(Modifier.weight(1f)) {
                    CupertinoAdditionalPropertyValue(props, controlProps, schema, propertyName)
                }
                if (interactive) ActionTooltip(text.rename.tooltip) {
                    IconButton(onClick = { renaming = true }) {
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
                        onClick = { updateObject(props, controlProps.path) { JsonObject(it - propertyName) } },
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
}

@Composable
private fun CupertinoAdditionalPropertyValue(
    props: ComposeJsonFormsRendererProps,
    controlProps: StatePropsOfControlWithDetail,
    schema: com.chobantonov.jsonfroms.core.models.JsonSchema,
    propertyName: String,
) {
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
        translator?.translate("$prefix.$key", fallback, values + extra) ?: fallback
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
            translated(key, error ?: "Property name '$propertyName' is invalid", mapOf("propertyName" to propertyName))
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
