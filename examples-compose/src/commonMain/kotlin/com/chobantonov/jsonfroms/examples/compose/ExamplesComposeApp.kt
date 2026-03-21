package com.chobantonov.jsonfroms.examples.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chobantonov.jsonfroms.compose.JsonForms
import com.chobantonov.jsonfroms.core.reducers.defaultMiddleware
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.StateProps
import com.chobantonov.jsonfroms.examples.getBuiltInExamples
import com.chobantonov.jsonfroms.material.renderers.materialCells
import com.chobantonov.jsonfroms.material.renderers.materialRenderers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

@Composable
fun ExamplesComposeApp(
    examples: List<ExampleDescription> = getBuiltInExamples(),
) {
    MaterialTheme {
        if (examples.isEmpty()) {
            EmptyExamplesState()
            return@MaterialTheme
        }

        var selectedExampleName by remember(examples) { mutableStateOf(examples.first().name) }
        val selectedExample = examples.firstOrNull { it.name == selectedExampleName } ?: examples.first()
        var currentState by remember(selectedExample.name) {
            mutableStateOf(selectedExample.toStateProps())
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        ) {
            ExampleNavigation(
                examples = examples,
                selectedExample = selectedExample,
                onSelected = { example ->
                    selectedExampleName = example.name
                },
            )
            Spacer(modifier = Modifier.width(16.dp))
            ExampleSurface(
                example = selectedExample,
                state = currentState,
                onStateChange = { currentState = it },
            )
        }
    }
}

@Composable
private fun ExampleNavigation(
    examples: List<ExampleDescription>,
    selectedExample: ExampleDescription,
    onSelected: (ExampleDescription) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Examples",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            examples.forEach { example ->
                val selected = example.name == selectedExample.name
                val containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(containerColor, RoundedCornerShape(12.dp))
                        .clickable { onSelected(example) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = example.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExampleSurface(
    example: ExampleDescription,
    state: StateProps,
    onStateChange: (StateProps) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = example.label,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = example.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Form",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (state.actions.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.actions.forEach { action ->
                                FilledTonalButton(
                                    onClick = {
                                        onStateChange(action.apply(state))
                                    },
                                ) {
                                    Text(action.label)
                                }
                            }
                        }
                    }
                    JsonForms(
                        data = state.data,
                        schema = state.schema ?: example.schema,
                        uischema = state.uischema ?: example.uischema,
                        uischemas = state.uischemas,
                        renderers = materialRenderers,
                        cells = materialCells,
                        config = state.config,
                        readonly = state.readonly,
                        i18n = state.i18n ?: JsonFormsI18nState(),
                        additionalErrors = state.additionalErrors,
                        middleware = state.middleware ?: defaultMiddleware,
                        onChange = { change ->
                            onStateChange(state.copy(data = change.data))
                        },
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = prettyJson.encodeToString(state.data),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun ExampleDescription.toStateProps(): StateProps =
    StateProps(
        data = data,
        schema = schema,
        uischema = uischema,
        renderers = materialRenderers,
        cells = materialCells,
        config = config,
        uischemas = uischemas,
        additionalErrors = additionalErrors,
        actions = actions,
        middleware = middleware,
        i18n = i18n,
        readonly = readonly,
    )

@Composable
private fun EmptyExamplesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No examples registered",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add shared examples to the examples module to populate this app.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
