package com.chobantonov.jsonfroms.material.renderers.layouts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRenderer
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfControl
import com.chobantonov.jsonfroms.core.mappers.OwnPropsOfRenderer
import com.chobantonov.jsonfroms.core.mappers.computeLabel
import com.chobantonov.jsonfroms.core.mappers.createDefaultValue
import com.chobantonov.jsonfroms.core.mappers.mapDispatchToArrayControlProps
import com.chobantonov.jsonfroms.core.mappers.mapStateToArrayControlProps
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.isObjectArrayWithNesting
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

val MaterialArrayLayoutRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialArrayLayoutRenderer(props)
}

@Composable
fun MaterialArrayLayoutRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val control = props.uischema as? ControlElement
        ?: error("MaterialArrayLayoutRenderer can only render Control elements")
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
        dispatch = Dispatch { action ->
            props.dispatch(action)
            action
        },
    )

    if (!stateProps.visible) {
        return
    }

    val items = (stateProps.data as? JsonArray).orEmpty()
    var expandedPath by remember(stateProps.path) { mutableStateOf<String?>(null) }
    val detailUiSchema = Generate.uiSchema(
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
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = computeLabel(
                        stateProps.label,
                        stateProps.required,
                        stateProps.config["hideRequiredAsterisk"] as? Boolean ?: false,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
                stateProps.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                stateProps.errors.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Button(
                enabled = stateProps.enabled && !stateProps.readonly,
                onClick = {
                    dispatchProps.addItem(
                        stateProps.path,
                        createDefaultValue(stateProps.arraySchema, stateProps.rootSchema),
                    )()
                },
            ) {
                Text("Add")
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        items.forEachIndexed { index, item ->
            val itemPath = composePaths(stateProps.path, index.toString())
            val isExpanded = expandedPath == itemPath
            val title = computeChildLabel(item, index, stateProps.config["elementLabelProp"] as? String)

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedPath = if (isExpanded) null else itemPath }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        OutlinedButton(
                            enabled = stateProps.enabled && !stateProps.readonly,
                            onClick = {
                                dispatchProps.removeItems?.invoke(stateProps.path, listOf(index))?.invoke()
                                if (expandedPath == itemPath) {
                                    expandedPath = null
                                }
                            },
                        ) {
                            Text("Remove")
                        }
                    }

                    if (isExpanded) {
                        JsonFormsDispatchRenderer(
                            props = props.copy(
                                schema = stateProps.schema,
                                rootSchema = stateProps.rootSchema,
                                uischema = detailUiSchema,
                                path = itemPath,
                                enabled = stateProps.enabled,
                                id = "${stateProps.id}-layout-$index",
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun computeChildLabel(
    item: kotlinx.serialization.json.JsonElement,
    index: Int,
    childLabelProp: String?,
): String {
    val propName = childLabelProp?.takeIf { it.isNotBlank() }
    val propValue = propName?.let { Resolve.data(item, it) } as? JsonPrimitive
    return propValue?.content?.takeIf { it.isNotBlank() } ?: "Item ${index + 1}"
}

val materialArrayLayoutTester: RankedTester = rankWith(
    4,
    { uiSchema, schema, context -> isObjectArrayWithNesting(uiSchema, schema, context) },
)
