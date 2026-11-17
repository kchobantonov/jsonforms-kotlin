package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.util.resolveSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** A positional array's described prefix and the rule for indices after it. */
data class TupleSchema(
    val prefix: List<JsonElement>,
    val tail: TupleTail,
) {
    val fixedCount: Int get() = prefix.size

    fun isFixed(index: Int): Boolean = index >= 0 && index < fixedCount

    fun canRemoveTrailing(index: Int, arraySize: Int): Boolean =
        index >= fixedCount && index < arraySize

    fun schemaAt(index: Int): JsonSchema? = when {
        index < 0 -> null
        index < fixedCount -> prefix[index].asRenderableSchema()
        else -> when (tail) {
            TupleTail.Forbidden -> null
            TupleTail.Unconstrained -> unconstrainedMixedSchema("Item ${index + 1}")
            is TupleTail.Typed -> tail.schema
        }
    }
}

const val TupleDiagnosticReporterConfigKey = "tupleDiagnosticReporter"

data class TupleDiagnostic(val code: String, val path: String, val scope: String? = null)

fun interface TupleDiagnosticReporter {
    fun report(diagnostic: TupleDiagnostic)
}

fun tupleVariantDiagnosticCode(schema: JsonSchema): String =
    if (schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Array))
        "variant.unsupported" else "variant.inapplicable"

sealed interface TuplePositionScope {
    data class Whole(val index: Int) : TuplePositionScope
    data class Nested(val index: Int) : TuplePositionScope
    data object Invalid : TuplePositionScope
    data object Other : TuplePositionScope
}

fun resolveTuplePositionScope(scope: String, tuple: TupleSchema): TuplePositionScope {
    val suffix = when {
        scope.startsWith("#/items/") -> scope.removePrefix("#/items/")
        scope.startsWith("#/prefixItems/") -> scope.removePrefix("#/prefixItems/")
        else -> return TuplePositionScope.Other
    }
    val indexText = suffix.substringBefore('/')
    val index = indexText.toIntOrNull() ?: return TuplePositionScope.Invalid
    if (index !in 0 until tuple.fixedCount || indexText != index.toString()) return TuplePositionScope.Invalid
    return if ('/' in suffix) TuplePositionScope.Nested(index) else TuplePositionScope.Whole(index)
}

data class TuplePresentation(val vertical: Boolean, val showBorder: Boolean)

/** Local tuple options override only the tuple namespace, never generic layout config. */
fun resolveTuplePresentation(options: JsonOptions, config: Map<String, Any?>): TuplePresentation {
    val tupleConfig = (config["jsonformsExtended"] as? Map<*, *>)?.get("tuple") as? Map<*, *>
    fun value(key: String, fallback: Boolean): Boolean =
        (options[key] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
            ?: (tupleConfig?.get(key) as? Boolean ?: fallback)
    return TuplePresentation(vertical = value("vertical", false), showBorder = value("showBorder", true))
}

fun isComplexTuplePosition(schema: JsonSchema, rootSchema: JsonSchema): Boolean {
    val resolved = (schema["\$ref"] as? JsonPrimitive)?.contentOrNull?.let {
        resolveSchema(rootSchema, it, rootSchema)
    } ?: schema
    return resolved.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Object) ||
        resolved.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.Array)
}

/** Returns null when a missing prefix has no supported, unambiguous initial value. */
fun tupleInitialValue(schema: JsonSchema, rootSchema: JsonSchema): JsonElement? {
    val resolved = (schema["\$ref"] as? JsonPrimitive)?.contentOrNull?.let {
        resolveSchema(rootSchema, it, rootSchema)
    } ?: schema
    resolved["default"]?.let { return it }
    if (resolved.typeValue !is JsonSchemaTypeValue.Single) return null
    return doCreateDefaultValue(resolved, rootSchema)
}

sealed interface TupleTail {
    data object Forbidden : TupleTail
    data object Unconstrained : TupleTail
    data class Typed(val schema: JsonSchema) : TupleTail
}

/**
 * Draft-07 uses array-valued items plus additionalItems. Draft 2020-12 uses
 * prefixItems plus items. A missing tail keyword allows trailing values.
 */
fun resolveTupleSchema(schema: JsonSchema, explicitVariant: Boolean = false): TupleSchema? {
    val modernPrefix = schema["prefixItems"] as? JsonArray
    val legacyPrefix = schema["items"] as? JsonArray
    val prefix = modernPrefix ?: legacyPrefix
    if (prefix == null) {
        if (!explicitVariant || schema.typeValue != JsonSchemaTypeValue.Single(JsonSchemaType.Array)) return null
        val item = schema["items"] as? JsonObject ?: return null
        val count = schema.minItems?.takeIf { it >= 0 && it == schema.maxItems } ?: return null
        return TupleSchema(List(count) { item }, TupleTail.Forbidden)
    }
    val tailKeyword = if (modernPrefix != null) schema["items"] else schema["additionalItems"]
    val tail = when (tailKeyword) {
        is JsonObject -> TupleTail.Typed(JsonSchema(tailKeyword))
        is JsonPrimitive -> when (tailKeyword.takeUnless { it.isString }?.booleanOrNull) {
            false -> TupleTail.Forbidden
            else -> TupleTail.Unconstrained
        }
        else -> TupleTail.Unconstrained
    }
    return TupleSchema(prefix.toList(), tail)
}

private fun JsonElement.asRenderableSchema(): JsonSchema? = when (this) {
    is JsonObject -> JsonSchema(this)
    is JsonPrimitive -> when (takeUnless { isString }?.booleanOrNull) {
        true -> JsonSchema()
        false -> JsonSchema(JsonObject(mapOf("not" to JsonObject(emptyMap()))))
        null -> null
    }
    else -> null
}
