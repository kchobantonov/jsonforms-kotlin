package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.generators.Generate
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.JsonFormsUISchemaRegistryEntry
import com.chobantonov.jsonfroms.core.reducers.findMatchingUISchema
import com.chobantonov.jsonfroms.core.util.resolveDetailUiSchema
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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
 * A fully valid branch wins when available. Otherwise non-structural errors, for example a
 * string format error, do not prevent a branch from being selected so an invalid value remains
 * editable. Root definitions are included for nested local
 * references while the root's own validation constraints are not applied to the branch value.
 * If every branch has structural errors, immediate fields still determine the branch so invalid
 * descendants can be repaired without switching the surrounding editor.
 */
fun findFittingSchemaIndex(
    combinatorSubSchemas: List<JsonSchema>,
    data: JsonData,
    validator: JsonSchemaValidator,
    rootSchema: JsonSchema? = null,
): Int? {
    val branchErrors = combinatorSubSchemas.map { subSchema ->
        val validationSchema = if (rootSchema == null) subSchema else
            validationSchemaForCombinatorBranch(subSchema, rootSchema)
        validator.validate(validationSchema, data)
    }
    val valid = branchErrors.indexOfFirst { it.isEmpty() }
    if (valid >= 0) return valid
    val structuralErrors = branchErrors.map { errors ->
        errors.filter { error ->
            error.keyword == "\$schema" || error.keyword in structuralValidationKeywords
        }
    }
    val exact = structuralErrors.indexOfFirst { it.isEmpty() }
    if (exact >= 0) return exact
    // A bad descendant must not switch the surrounding object to a different branch. Keep a
    // branch whose own value and immediate fields still match when no branch matches exactly.
    return structuralErrors.indexOfFirst { errors ->
        errors.none { error -> error.instancePath.count { it == '/' } <= 1 }
    }.takeIf { it >= 0 }
}

/** Give a branch its root reference definitions without applying root validation constraints. */
fun validationSchemaForCombinatorBranch(
    subSchema: JsonSchema,
    rootSchema: JsonSchema,
): JsonSchema {
    val context = mutableMapOf<String, JsonElement>()
    listOf("\$schema", "\$id").forEach { key -> rootSchema[key]?.let { context[key] = it } }
    listOf("definitions", "\$defs").forEach { key ->
        val rootEntries = rootSchema[key] as? JsonObject
        val branchEntries = subSchema[key] as? JsonObject
        if (rootEntries != null || branchEntries != null) {
            context[key] = JsonObject(rootEntries.orEmpty() + branchEntries.orEmpty())
        }
    }
    return JsonSchema(JsonObject(context + subSchema.value.filterKeys {
        it != "definitions" && it != "\$defs"
    }))
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
        val resolvedUiSchema = resolveDetailUiSchema(control,
            registered = { findMatchingUISchema(uischemas)(schema, control.scope, path) },
            generated = { Generate.uiSchema(schema, rootSchema = rootSchema) },
        )
        val label = (subSchema["title"] as? JsonPrimitive)?.content
            ?: (resolvedSubSchema?.get("title") as? JsonPrimitive)?.content
            ?: "${keyword.name}-$index"
        CombinatorSubSchemaRenderInfo(
            schema = schema,
            uischema = resolvedUiSchema,
            label = label,
        )
    }
