package com.chobantonov.jsonfroms.extended.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.actions.Actions
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.composeWithUi
import com.chobantonov.jsonfroms.core.util.resolveSchema
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

const val ExtendedFilePickerConfigKey = "extendedFilePicker"

private val isFileSchema: (JsonSchema) -> Boolean = { schema ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) && (
        (schema["contentEncoding"] as? JsonPrimitive)?.contentOrNull == "base64" ||
            schema.format == "binary" || schema.format == "byte"
        )
}

private val isFileArraySchema: (JsonSchema, JsonSchema) -> Boolean = { schema, root ->
    schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Array) &&
        (schema["items"] as? JsonObject)?.let { item ->
            resolveSchema(JsonSchema(item), null, root)?.let(isFileSchema)
        } == true
}

private val scalarFileTester = rankWith(2, and(uiTypeIs("Control"), schemaMatches(isFileSchema)))
private val arrayFileTester = rankWith(7, and(uiTypeIs("Control"), schemaMatches(isFileArraySchema)))

val fileControlTester: RankedTester = { ui, schema, context ->
    maxOf(scalarFileTester(ui, schema, context), arrayFileTester(ui, schema, context))
}

private fun resolvedControlSchema(control: ControlElement, rootSchema: JsonSchema): JsonSchema =
    resolveSchema(rootSchema, control.scope.removePrefix("#/"), rootSchema)
        ?: rootSchema[control.scope.substringAfterLast('/')].asJsonSchemaOrNull()
        ?: rootSchema

fun interface ExtendedFilePicker {
    fun pick(schema: JsonSchema, options: JsonOptions, onSelected: (JsonElement) -> Unit)
}

@Composable
internal fun FileControl(props: ComposeJsonFormsRendererProps, textRenderer: ComposeJsonFormsRenderer) {
    val control = props.uischema as? ControlElement ?: return
    val picker = props.config[ExtendedFilePickerConfigKey] as? ExtendedFilePicker
    val editable = props.enabled && (control.options["readonly"] as? JsonPrimitive)?.booleanOrNull != true
    val schema = resolvedControlSchema(control, props.rootSchema)
    val path = composeWithUi(control, props.path)
    val multiple = schema.typeValue == com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue.Single(
        com.chobantonov.jsonfroms.core.models.JsonSchemaType.Array,
    )
    val pickerSchema = if (multiple) {
        (schema["items"] as? JsonObject)?.let { resolveSchema(JsonSchema(it), null, props.rootSchema) } ?: schema
    } else schema
    val current = com.chobantonov.jsonfroms.core.util.JsonPath.get(props.state.jsonforms.core.data, path)
    var manualValue by remember(path) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (multiple) {
            (current as? JsonArray)?.forEachIndexed { index, attachment ->
                val name = (attachment as? JsonPrimitive)?.contentOrNull
                    ?.substringAfter(";filename=", "")?.substringBefore(';')?.takeIf(String::isNotEmpty)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name ?: "Attachment ${index + 1}", Modifier.weight(1f))
                    OutlinedButton(enabled = editable, onClick = {
                        props.dispatch(Actions.update(path = path, updater = { old ->
                            JsonArray((old as? JsonArray).orEmpty().filterIndexed { itemIndex, _ -> itemIndex != index })
                        }))
                    }) { Text("Remove") }
                }
            }
        } else {
            textRenderer.Render(props)
        }
        if (picker != null) {
            OutlinedButton(
                enabled = editable,
                onClick = {
                    picker.pick(
                        pickerSchema,
                        props.config.filterValues { it is JsonElement }.mapValues { it.value as JsonElement } + control.options,
                    ) { value ->
                        props.dispatch(Actions.update(path = path, updater = { old ->
                            if (multiple) JsonArray((old as? JsonArray).orEmpty() + value) else value
                        }))
                    }
                },
            ) { Text("Choose file") }
        } else if (multiple) {
            OutlinedTextField(
                value = manualValue,
                onValueChange = { manualValue = it },
                label = { Text("File data or URL") },
                enabled = editable,
            )
            OutlinedButton(enabled = editable && manualValue.isNotBlank(), onClick = {
                val value = JsonPrimitive(manualValue)
                props.dispatch(Actions.update(path = path, updater = { old ->
                    JsonArray((old as? JsonArray).orEmpty() + value)
                }))
                manualValue = ""
            }) { Text("Add attachment") }
        }
    }
}
