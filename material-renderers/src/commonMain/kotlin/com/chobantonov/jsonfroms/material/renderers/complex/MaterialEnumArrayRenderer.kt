package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapStateToMultiEnumControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.schemaMatches
import com.chobantonov.jsonfroms.core.testers.schemaSubPathMatches
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.hasType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

val MaterialEnumArrayRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialEnumArrayRenderer(props)
}

@Composable
fun MaterialEnumArrayRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialEnumArrayRenderer can only render Control elements")
    val controlProps = mapStateToMultiEnumControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = control,
            own = OwnPropsOfRenderer(
                uischema = props.uischema,
                schema = props.schema,
                enabled = props.enabled,
                path = props.path,
                renderers = props.renderers,
                cells = props.cells,
            ),
        ),
    )

    if (!controlProps.visible) {
        return
    }

    val selectedValues = (controlProps.data as? JsonArray).orEmpty()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = controlProps.label,
            style = MaterialTheme.typography.titleMedium,
        )
        controlProps.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        controlProps.options.forEach { option ->
            val checked = selectedValues.contains(option.value)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = checked,
                    enabled = controlProps.enabled && !controlProps.readonly,
                    onCheckedChange = { newValue ->
                        props.dispatch(
                            UpdateAction(
                                path = controlProps.path,
                                updater = { current ->
                                    val items = (current as? JsonArray)?.toMutableList() ?: mutableListOf()
                                    if (newValue) {
                                        if (!items.contains(option.value)) {
                                            items += option.value
                                        }
                                    } else {
                                        items.removeAll { it == option.value }
                                    }
                                    JsonArray(items)
                                },
                            ),
                        )
                    },
                )
                Text(option.label)
            }
        }
        controlProps.errors.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun hasOneOfItems(schema: com.chobantonov.jsonfroms.core.models.JsonSchema): Boolean =
    schema.oneOf?.isNotEmpty() == true && schema.oneOf.orEmpty().all { it["const"] != null }

private fun hasEnumItems(schema: com.chobantonov.jsonfroms.core.models.JsonSchema): Boolean =
    hasType(schema, "string") && schema["enum"] is JsonArray

val materialEnumArrayRendererTester: RankedTester = rankWith(
    5,
    and(
        uiTypeIs("Control"),
        schemaMatches { schema ->
            hasType(schema, "array") &&
                schema["uniqueItems"] == JsonPrimitive(true) &&
                schema["items"] !is JsonArray
        },
        schemaSubPathMatches("items") { schema, rootSchema ->
            val resolved = (schema["\$ref"] as? JsonPrimitive)?.content?.let {
                com.chobantonov.jsonfroms.core.util.resolveSchema(rootSchema, it, rootSchema)
            } ?: schema
            hasOneOfItems(resolved) || hasEnumItems(resolved)
        },
    ),
)
