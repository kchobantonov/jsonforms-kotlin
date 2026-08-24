package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlinx.serialization.json.JsonPrimitive

private val structuralValidationKeywords = setOf(
    "required",
    "additionalProperties",
    "type",
    "enum",
    "const",
)

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

/**
 * Finds the first combinator branch which structurally matches [data].
 *
 * Non-structural errors, for example a string format error, do not prevent a branch from being
 * selected. This mirrors JSON Forms' combinator selection and keeps the renderer stable while a
 * user edits a value inside the selected branch.
 */
fun findFittingSchemaIndex(
    combinatorSubSchemas: List<JsonSchema>,
    data: JsonData,
    validator: JsonSchemaValidator,
): Int? = combinatorSubSchemas.indexOfFirst { subSchema ->
    val errors = validator.validate(subSchema, data)
    errors.none { error ->
        error.keyword == "\$schema" || error.keyword in structuralValidationKeywords
    }
}.takeIf { it >= 0 }

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
