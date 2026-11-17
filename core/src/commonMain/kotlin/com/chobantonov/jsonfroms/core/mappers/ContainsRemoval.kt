package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.JsonPath
import com.chobantonov.jsonfroms.core.validation.ValidateFunction
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** A discrete Delete may not turn a satisfied contains minimum into a violation. */
class ContainsRemovalGuard(
    schema: JsonSchema,
    private val validator: ValidateFunction?,
    private val rootData: JsonElement,
    private val arrayPath: String,
    private val items: JsonArray,
) {
    private val applicable = schema["contains"] != null && validator != null
    private val target = pathSegments(arrayPath)
    private fun hasContainsFailure(active: ValidateFunction, data: JsonElement): Boolean =
        active.validate(data).any { error ->
            errorPathSegments(error) == target && error.keyword in setOf("contains", "minContains")
        }
    private val currentFails = applicable && hasContainsFailure(requireNotNull(validator), rootData)

    fun canRemove(index: Int): Boolean {
        if (index !in items.indices) return true
        val next = JsonArray(items.filterIndexed { position, _ -> position != index })
        return canReplaceWith(next)
    }

    /** Bulk removal follows the same satisfied-minimum rule as a single Delete. */
    fun canReplaceWith(next: JsonArray): Boolean {
        val active = validator ?: return true
        if (!applicable || currentFails) return true
        return !hasContainsFailure(active, JsonPath.set(rootData, arrayPath, next, active.schema))
    }
}

fun canRemoveWithoutBreakingContains(
    schema: JsonSchema,
    validator: ValidateFunction?,
    rootData: JsonElement,
    arrayPath: String,
    items: JsonArray,
    index: Int,
): Boolean = ContainsRemovalGuard(schema, validator, rootData, arrayPath, items).canRemove(index)
