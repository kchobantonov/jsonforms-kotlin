package com.chobantonov.jsonfroms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.UnknownUiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsRendererRegistryEntry
import com.chobantonov.jsonfroms.core.util.JsonPath
import kotlinx.serialization.json.JsonElement

/** Runs an item detail against a private copy of the full document. */
@Composable
fun IsolatedDetailDraftForm(
    props: ComposeJsonFormsRendererProps,
    rootData: JsonElement,
    positionSchema: JsonSchema,
    positionPath: String,
    itemPath: String = positionPath,
    detail: UiSchemaElement,
    onChange: (JsonElement) -> Unit,
    onPendingChange: (Boolean) -> Unit = {},
) {
    val latestPendingCallback = rememberUpdatedState(onPendingChange)
    val marker = remember { UnknownUiSchemaElement(type = "TupleDraftRoot") }
    val bridge = remember(marker, positionSchema, positionPath, itemPath, detail, props.rootSchema, props.renderers) {
        JsonFormsRendererRegistryEntry(
            tester = { ui, _, _ -> if (ui === marker) 10_000 else -1 },
            renderer = ComposeJsonFormsRenderer { draftProps ->
                val pending = useJsonForms().controller.hasPendingOwnedWork
                LaunchedEffect(pending) { latestPendingCallback.value(pending) }
                val child: @Composable () -> Unit = {
                    JsonFormsDispatchRenderer(draftProps.copy(
                        schema = positionSchema,
                        rootSchema = props.rootSchema,
                        uischema = detail,
                        path = positionPath,
                        renderers = props.renderers,
                        id = null,
                    ))
                }
                val item = JsonPath.get(draftProps.state.jsonforms.core.data, itemPath)
                JsonFormsItemScope(item, child)
            },
        )
    }
    val renderers = remember(bridge, props.renderers) { listOf(bridge) + props.renderers }
    JsonForms(
        data = rootData,
        schema = props.rootSchema,
        uischema = marker,
        renderers = renderers,
        cells = props.cells,
        config = props.config,
        uischemas = props.state.jsonforms.uischemas,
        readonly = !props.enabled,
        i18n = props.state.jsonforms.i18n,
        onChange = { onChange(it.data) },
    )
}

@Composable
fun TupleDraftForm(
    props: ComposeJsonFormsRendererProps,
    rootData: JsonElement,
    positionSchema: JsonSchema,
    positionPath: String,
    detail: UiSchemaElement,
    onChange: (JsonElement) -> Unit,
) = IsolatedDetailDraftForm(
    props = props, rootData = rootData, positionSchema = positionSchema,
    positionPath = positionPath, detail = detail, onChange = onChange,
)
