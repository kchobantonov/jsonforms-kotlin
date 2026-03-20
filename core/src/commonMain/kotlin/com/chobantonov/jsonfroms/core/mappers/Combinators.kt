package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.util.resolveSchema
import kotlinx.serialization.json.JsonPrimitive

data class CombinatorSubSchemaRenderInfo(
    val schema: JsonSchema,
    val uischema: UiSchemaElement,
    val label: String,
)

enum class CombinatorKeyword {
    anyOf,
    oneOf,
    allOf,
}

fun createCombinatorRenderInfos(
    combinatorSubSchemas: List<JsonSchema>,
    rootSchema: JsonSchema,
    keyword: CombinatorKeyword,
    control: ControlElement,
    path: String,
    uischemas: List<JsonFormsUISchemaRegistryEntry> = emptyList(),
): List<CombinatorSubSchemaRenderInfo> =
    combinatorSubSchemas.mapIndexed { index, subSchema ->
        val resolvedSubSchema = (subSchema["\$ref"] as? JsonPrimitive)?.content?.let {
            resolveSchema(rootSchema, it, rootSchema)
        }
        val schema = resolvedSubSchema ?: subSchema
        val resolvedUiSchema = uischemas
            .maxByOrNull { it.tester(schema, control.scope, path) }
            ?.takeIf { it.tester(schema, control.scope, path) >= 0 }
            ?.uischema
            ?: Generate.uiSchema(schema, rootSchema = rootSchema)
        val label = (subSchema["title"] as? JsonPrimitive)?.content
            ?: (resolvedSubSchema?.get("title") as? JsonPrimitive)?.content
            ?: "${keyword.name}-$index"
        CombinatorSubSchemaRenderInfo(
            schema = schema,
            uischema = resolvedUiSchema,
            label = label,
        )
    }
