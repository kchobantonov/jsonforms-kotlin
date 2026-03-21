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
    val otherProps = JsonSchema(
        JsonObject(schema.toJsonObject().filterKeys { it != combinatorKeyword }),
    )
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
