package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectArrayControl
import com.chobantonov.jsonfroms.core.testers.isPrimitiveArrayControl
import com.chobantonov.jsonfroms.core.testers.or
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.composePaths
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val MaterialArrayControlRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayControlRenderer(props)
}

@Composable
fun MaterialArrayControlRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialArrayControlRenderer can only render Control elements")
    val stateProps = mapStateToArrayControlProps(
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
                uischemas = props.state.jsonforms.uischemas,
            ),
        ),
    )
    val dispatchProps = mapDispatchToArrayControlProps(
        dispatch = object : com.chobantonov.jsonfroms.core.store.Dispatch<com.chobantonov.jsonfroms.core.actions.JsonFormsAction> {
            override fun dispatch(action: com.chobantonov.jsonfroms.core.actions.JsonFormsAction): com.chobantonov.jsonfroms.core.actions.JsonFormsAction {
                props.dispatch(action)
                return action
            }
        },
    )

    if (!stateProps.visible) {
        return
    }

    val items = (stateProps.data as? JsonArray).orEmpty()
    val itemUiSchema = findMatchingUISchema(stateProps.uischemas)(
        stateProps.schema,
        "${control.scope}/items",
        stateProps.path,
    ) ?: Generate.uiSchema(
        jsonSchema = stateProps.schema,
        rootSchema = stateProps.rootSchema,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (stateProps.label.isNotBlank()) {
                    Text(
                        text = stateProps.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                stateProps.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                stateProps.errors.takeIf { it.isNotBlank() }?.let { errors ->
                    Text(
                        text = errors,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(
                enabled = stateProps.enabled && !stateProps.readonly,
                onClick = {
                    dispatchProps.addItem(stateProps.path, createNewArrayItem(stateProps.schema))()
                },
            ) {
                Text("Add")
            }
        }

        items.forEachIndexed { index, _ ->
            val itemPath = composePaths(stateProps.path, index.toString())
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Item ${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                enabled = stateProps.enabled && !stateProps.readonly && index > 0,
                                onClick = {
                                    dispatchProps.moveUp?.invoke(stateProps.path, index)?.invoke()
                                },
                            ) {
                                Text("Up")
                            }
                            IconButton(
                                enabled = stateProps.enabled && !stateProps.readonly && index < items.lastIndex,
                                onClick = {
                                    dispatchProps.moveDown?.invoke(stateProps.path, index)?.invoke()
                                },
                            ) {
                                Text("Down")
                            }
                            OutlinedButton(
                                enabled = stateProps.enabled && !stateProps.readonly,
                                onClick = {
                                    dispatchProps.removeItems?.invoke(stateProps.path, listOf(index))?.invoke()
                                },
                            ) {
                                Text("Remove")
                            }
                        }
                    }

                    JsonFormsDispatchRenderer(
                        props = props.copy(
                            schema = stateProps.schema,
                            rootSchema = stateProps.rootSchema,
                            uischema = itemUiSchema,
                            path = itemPath,
                            enabled = stateProps.enabled,
                            id = "${stateProps.id}-$index",
                        ),
                    )
                }
            }
        }
    }
}

private fun createNewArrayItem(
    itemSchema: com.chobantonov.jsonfroms.core.models.JsonSchema,
): JsonElement = when ((itemSchema["type"] as? JsonPrimitive)?.content) {
    "string" -> JsonPrimitive("")
    "integer", "number" -> JsonPrimitive(0)
    "boolean" -> JsonPrimitive(false)
    "object" -> JsonObject(emptyMap())
    "array" -> JsonArray(emptyList())
    else -> JsonNull
}

val materialArrayControlTester: RankedTester = rankWith(
    3,
    or(isObjectArrayControl, isPrimitiveArrayControl),
)
