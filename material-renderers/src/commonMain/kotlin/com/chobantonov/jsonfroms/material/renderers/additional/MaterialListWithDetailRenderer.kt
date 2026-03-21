package com.chobantonov.jsonfroms.material.renderers.additional

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
import androidx.compose.runtime.LaunchedEffect
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
import com.chobantonov.jsonfroms.core.models.ListWithDetailElement
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.store.Dispatch
import com.chobantonov.jsonfroms.core.testers.RankedTester
import com.chobantonov.jsonfroms.core.testers.and
import com.chobantonov.jsonfroms.core.testers.isObjectArray
import com.chobantonov.jsonfroms.core.testers.rankWith
import com.chobantonov.jsonfroms.core.testers.uiTypeIs
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

val MaterialListWithDetailRenderer: ComposeJsonFormsRenderer = ComposeJsonFormsRenderer { props ->
    MaterialListWithDetailRenderer(props)
}

@Composable
fun MaterialListWithDetailRenderer(
    props: ComposeJsonFormsRendererProps,
) {
    val listWithDetail = props.uischema as? ListWithDetailElement
        ?: error("MaterialListWithDetailRenderer can only render ListWithDetail elements")
    val controlProxy = ControlElement(
        scope = listWithDetail.scope,
        i18n = listWithDetail.i18n,
        rule = listWithDetail.rule,
        options = listWithDetail.options,
    )
    val stateProps = mapStateToArrayControlProps(
        state = props.state,
        ownProps = OwnPropsOfControl(
            controlUiSchema = controlProxy,
            own = OwnPropsOfRenderer(
                uischema = controlProxy,
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

    var selectedIndex by remember(stateProps.schema, stateProps.path) { mutableStateOf<Int?>(null) }
    LaunchedEffect(stateProps.schema) {
        selectedIndex = null
    }

    val items = (stateProps.data as? JsonArray).orEmpty()
    val itemUiSchema = findMatchingUISchema(stateProps.uischemas)(
        stateProps.schema,
        listWithDetail.scope,
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
                    dispatchProps.addItem(
                        stateProps.path,
                        createDefaultValue(stateProps.arraySchema, stateProps.rootSchema),
                    )()
                },
            ) {
                Text("Add")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.35f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "No data",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    items.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = computeItemLabel(item, index, stateProps.config["elementLabelProp"] as? String),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                OutlinedButton(
                                    enabled = stateProps.enabled && !stateProps.readonly,
                                    onClick = {
                                        dispatchProps.removeItems?.invoke(stateProps.path, listOf(index))?.invoke()
                                        selectedIndex = when {
                                            selectedIndex == index -> null
                                            selectedIndex != null && selectedIndex!! > index -> selectedIndex!! - 1
                                            else -> selectedIndex
                                        }
                                    },
                                ) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(0.65f),
            ) {
                if (selectedIndex != null && selectedIndex in items.indices) {
                    JsonFormsDispatchRenderer(
                        props = props.copy(
                            schema = stateProps.schema,
                            rootSchema = stateProps.rootSchema,
                            uischema = itemUiSchema,
                            path = composePaths(stateProps.path, selectedIndex.toString()),
                            enabled = stateProps.enabled,
                            id = "${stateProps.id}-${selectedIndex}",
                        ),
                    )
                } else {
                    Text(
                        text = "No selection",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

private fun computeItemLabel(
    item: JsonElement,
    index: Int,
    childLabelProp: String?,
): String {
    val propName = childLabelProp?.takeIf { it.isNotBlank() }
    val propValue = propName?.let { Resolve.data(item, it) } as? JsonPrimitive
    return propValue?.content?.takeIf { it.isNotBlank() } ?: "Item ${index + 1}"
}

val materialListWithDetailTester: RankedTester = rankWith(
    4,
    and(uiTypeIs("ListWithDetail"), isObjectArray),
)
