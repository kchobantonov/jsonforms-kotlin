package com.chobantonov.jsonfroms.material.renderers.complex

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.isLayout
import kotlinx.serialization.json.JsonObject

@Composable
fun CombinatorProperties(
    props: ComposeJsonFormsRendererProps,
    schema: JsonSchema,
    combinatorKeyword: String,
) {
    val otherProps = schemaWithoutCombinatorProperties(schema, combinatorKeyword) ?: return

    val foundUiSchema = Generate.uiSchema(
        jsonSchema = otherProps,
        layoutType = "VerticalLayout",
        rootSchema = props.rootSchema,
    )
    val isLayoutWithElements = isLayout(foundUiSchema) && when (foundUiSchema) {
        is com.chobantonov.jsonfroms.core.models.Layout -> foundUiSchema.elements.isNotEmpty()
        else -> false
    }

    if (!isLayoutWithElements) {
        return
    }

    JsonFormsDispatchRenderer(
        props = props.copy(
            schema = otherProps,
            uischema = foundUiSchema,
        ),
    )
}

/**
 * Returns the non-combinator part of a schema only when it contains properties that can be
 * rendered. Dispatching an empty remainder would fall through to UnknownRenderer.
 */
internal fun schemaWithoutCombinatorProperties(
    schema: JsonSchema,
    combinatorKeyword: String,
): JsonSchema? {
    val otherProps = JsonSchema(
        JsonObject(schema.toJsonObject().filterKeys { it != combinatorKeyword }),
    )
    val properties = otherProps["properties"] as? JsonObject
    return otherProps.takeIf { !properties.isNullOrEmpty() }
}
