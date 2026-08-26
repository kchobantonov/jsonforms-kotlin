package com.chobantonov.jsonfroms.cupertino.renderers.complex

import androidx.compose.runtime.Composable
import com.chobantonov.jsonfroms.compose.ComposeJsonFormsRendererProps
import com.chobantonov.jsonfroms.compose.JsonFormsDispatchRenderer
import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.Layout
import com.chobantonov.jsonfroms.core.util.isLayout
import kotlinx.serialization.json.JsonObject

@Composable
fun CombinatorProperties(
    props: ComposeJsonFormsRendererProps,
    schema: JsonSchema,
    combinatorKeyword: String,
) {
    val otherProps = schemaWithoutCombinatorProperties(schema, combinatorKeyword) ?: return
    val foundUiSchema = Generate.uiSchema(otherProps, layoutType = "VerticalLayout", rootSchema = props.rootSchema)
    if (!isLayout(foundUiSchema) || foundUiSchema !is Layout || foundUiSchema.elements.isEmpty()) return
    JsonFormsDispatchRenderer(props.copy(schema = otherProps, uischema = foundUiSchema))
}

internal fun schemaWithoutCombinatorProperties(schema: JsonSchema, combinatorKeyword: String): JsonSchema? {
    val otherProps = JsonSchema(JsonObject(schema.toJsonObject().filterKeys { it != combinatorKeyword }))
    return otherProps.takeIf { !(it["properties"] as? JsonObject).isNullOrEmpty() }
}
