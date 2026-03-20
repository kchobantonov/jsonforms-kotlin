package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.asJsonSchemaOrNull
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.util.deriveTypes
import com.chobantonov.jsonfroms.core.util.doubleValueOrNull
import com.chobantonov.jsonfroms.core.util.hasType
import com.chobantonov.jsonfroms.core.util.intValueOrNull
import com.chobantonov.jsonfroms.core.util.isEnumSchema
import com.chobantonov.jsonfroms.core.util.isOneOfEnumSchema
import com.chobantonov.jsonfroms.core.util.resolveSchema
import com.chobantonov.jsonfroms.core.util.resolveSchemaElement
import com.chobantonov.jsonfroms.core.util.stringContentOrNull
import com.chobantonov.jsonfroms.core.util.booleanValueOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

const val NOT_APPLICABLE: Int = -1
const val NotApplicable: Int = -1

data class TesterContext(
    val rootSchema: JsonSchema? = null,
    val config: Map<String, Any?> = emptyMap(),
)

typealias Tester = (UiSchemaElement, JsonSchema?, TesterContext) -> Boolean
typealias RankedTester = (UiSchemaElement, JsonSchema?, TesterContext) -> Int
typealias UiSchemaTester = (JsonSchema?, String, String) -> Int

fun isControl(uiSchema: UiSchemaElement?): Boolean = uiSchema is ControlElement

fun rankWith(
    rank: Int,
    tester: Tester,
): RankedTester = { uiSchema, schema, context ->
    if (tester(uiSchema, schema, context)) {
        rank
    } else {
        NOT_APPLICABLE
    }
}

fun withIncreasedRank(
    by: Int,
    rankedTester: RankedTester,
): RankedTester = { uiSchema, schema, context ->
    val rank = rankedTester(uiSchema, schema, context)
    if (rank == NOT_APPLICABLE) {
        NOT_APPLICABLE
    } else {
        rank + by
    }
}

fun uiTypeIs(expected: String): Tester = { uiSchema, _, _ ->
    uiSchema.type == expected
}

fun optionIs(
    optionName: String,
    optionValue: Any?,
): Tester = { uiSchema, _, _ ->
    val options: JsonOptions = uiSchema.options
    optionEquals(options[optionName], optionValue)
}

fun hasOption(optionName: String): Tester = { uiSchema, _, _ ->
    uiSchema.options.containsKey(optionName)
}

fun scopeEndsWith(expected: String): Tester = { uiSchema, _, _ ->
    uiSchema is ControlElement && expected.isNotEmpty() && uiSchema.scope.endsWith(expected)
}

fun scopeEndIs(expected: String): Tester = { uiSchema, _, _ ->
    uiSchema is ControlElement &&
        expected.isNotEmpty() &&
        uiSchema.scope.split('/').lastOrNull() == expected
}

fun and(vararg testers: Tester): Tester = { uiSchema, schema, context ->
    testers.all { it(uiSchema, schema, context) }
}

fun or(vararg testers: Tester): Tester = { uiSchema, schema, context ->
    testers.any { it(uiSchema, schema, context) }
}

fun schemaMatches(predicate: (JsonSchema) -> Boolean): Tester = { uiSchema, schema, context ->
    val resolved = when {
        uiSchema is ControlElement -> resolveControlSchema(uiSchema, schema, context)
        schema != null -> schema
        else -> null
    }
    resolved?.let(predicate) ?: false
}

fun schemaMatches(predicate: (JsonSchema, JsonSchema) -> Boolean): Tester = { uiSchema, schema, context ->
    val resolved = when {
        uiSchema is ControlElement -> resolveControlSchema(uiSchema, schema, context)
        schema != null -> schema
        else -> null
    }
    val root = context.rootSchema ?: schema
    if (resolved != null && root != null) {
        predicate(resolved, root)
    } else {
        false
    }
}

fun schemaSubPathMatches(
    subPath: String,
    predicate: (JsonSchema, JsonSchema) -> Boolean,
): Tester = { uiSchema, schema, context ->
    val resolved = when {
        uiSchema is ControlElement -> resolveControlSchema(uiSchema, schema, context)
        schema != null -> schema
        else -> null
    }
    val root = context.rootSchema ?: schema
    val subSchema = resolveSchema(resolved, subPath, root)
    if (subSchema != null && root != null) {
        predicate(subSchema, root)
    } else {
        false
    }
}

fun schemaTypeIs(expectedType: String): Tester =
    schemaMatches { schema -> hasType(schema, expectedType) }

fun formatIs(expectedFormat: String): Tester =
    schemaMatches { schema ->
        val format = schema["format"] as? JsonPrimitive
        format?.stringContentOrNull() == expectedFormat
    }

val isBooleanControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("boolean"))
val isObjectControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("object"))
val isAllOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("allOf") })
val isAnyOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("anyOf") })
val isOneOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("oneOf") })
val isEnumControl: Tester = and(uiTypeIs("Control"), schemaMatches(::isEnumSchema))
val isOneOfEnumControl: Tester = and(uiTypeIs("Control"), schemaMatches(::isOneOfEnumSchema))
val isIntegerControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("integer"))
val isNumberControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("number"))
val isStringControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("string"))
val isMultiLineControl: Tester = and(uiTypeIs("Control"), optionIs("multi", true))
val isDateControl: Tester = and(uiTypeIs("Control"), or(formatIs("date"), optionIs("format", "date")))
val isTimeControl: Tester = and(uiTypeIs("Control"), or(formatIs("time"), optionIs("format", "time")))
val isDateTimeControl: Tester = and(uiTypeIs("Control"), or(formatIs("date-time"), optionIs("format", "date-time")))

val isObjectArray: Tester = and(
    schemaMatches { schema, rootSchema ->
        hasType(schema, "array") && resolveSchemaElement(schema, "items", rootSchema) !is JsonArray
    },
    schemaSubPathMatches("items") { schema, rootSchema ->
        val resolvedSchema = resolveReferencedSchema(schema, rootSchema)
        hasType(resolvedSchema, "object")
    },
)

val isObjectArrayControl: Tester = and(uiTypeIs("Control"), isObjectArray)
val isArrayObjectControl: Tester = isObjectArrayControl

val isPrimitiveArrayControl: Tester = and(
    uiTypeIs("Control"),
    schemaMatches { schema, rootSchema ->
        deriveTypes(schema).isNotEmpty() && resolveSchemaElement(schema, "items", rootSchema) !is JsonArray
    },
    schemaSubPathMatches("items") { schema, rootSchema ->
        val resolvedSchema = resolveReferencedSchema(schema, rootSchema)
        val types = deriveTypes(resolvedSchema)
        types.size == 1 && types.first() in setOf("integer", "number", "boolean", "string")
    },
)

val isRangeControl: Tester = and(
    uiTypeIs("Control"),
    or(schemaTypeIs("number"), schemaTypeIs("integer")),
    schemaMatches { schema ->
        schema.containsKey("maximum") && schema.containsKey("minimum") && schema.containsKey("default")
    },
    optionIs("slider", true),
)

val isNumberFormatControl: Tester = and(
    uiTypeIs("Control"),
    schemaTypeIs("integer"),
    optionIs("format", true),
)

fun isObjectArrayWithNesting(
    uiSchema: UiSchemaElement,
    schema: JsonSchema?,
    context: TesterContext,
): Boolean {
    if (!uiTypeIs("Control")(uiSchema, schema, context)) {
        return false
    }
    val control = uiSchema as? ControlElement ?: return false
    val rootSchema = context.rootSchema ?: schema ?: return false
    val resolvedSchema = resolveSchema(schema, control.scope, rootSchema) ?: return false
    var objectDepth = 0
    val items = resolvedSchema["items"] ?: return false

    if (traverse(items, rootSchema) { value ->
            if (value == schema) {
                false
            } else if (value.containsKey("\$ref")) {
                false
            } else if (value.containsKey("anyOf") || value.containsKey("allOf")) {
                true
            } else if (value.containsKey("oneOf") && !isOneOfEnumSchema(value)) {
                true
            } else if (hasType(value, "object")) {
                objectDepth += 1
                objectDepth == 2
            } else {
                hasType(value, "array")
            }
        }) {
        return true
    }

    val detail = uiSchema.options["detail"]
    return when (detail) {
        is JsonPrimitive -> detail.content.uppercase() != "DEFAULT"
        is JsonObject -> detail["type"] is JsonPrimitive
        else -> false
    }
}

fun isCategorization(category: UiSchemaElement): Boolean = category is CategorizationElement

fun isCategory(uiSchema: UiSchemaElement): Boolean = uiSchema is CategoryElement

fun hasCategory(categorization: CategorizationElement): Boolean {
    if (categorization.elements.isEmpty()) {
        return false
    }
    return categorization.elements.all { element ->
        if (element is CategorizationElement) {
            hasCategory(element)
        } else {
            isCategory(element)
        }
    }
}

fun categorizationHasCategory(uiSchema: UiSchemaElement): Boolean =
    (uiSchema as? CategorizationElement)?.let(::hasCategory) ?: false

fun not(tester: Tester): Tester = { uiSchema, schema, context ->
    !tester(uiSchema, schema, context)
}

private fun resolveControlSchema(
    control: ControlElement,
    schema: JsonSchema?,
    context: TesterContext,
): JsonSchema? {
    val root = context.rootSchema ?: schema ?: return null
    return resolveSchema(schema ?: root, control.scope, root)
}

private fun optionEquals(
    option: JsonElement?,
    expected: Any?,
): Boolean = when (expected) {
    is JsonElement -> option == expected
    is String -> (option as? JsonPrimitive)?.stringContentOrNull() == expected
    is Boolean -> (option as? JsonPrimitive)?.booleanValueOrNull() == expected
    is Int -> (option as? JsonPrimitive)?.intValueOrNull() == expected
    is Double -> (option as? JsonPrimitive)?.doubleValueOrNull() == expected
    else -> option == null && expected == null
}

private fun resolveReferencedSchema(
    schema: JsonSchema,
    rootSchema: JsonSchema,
): JsonSchema =
    resolveSchema(schema, (schema["\$ref"] as? JsonPrimitive)?.stringContentOrNull(), rootSchema) ?: schema

private fun traverse(
    element: JsonElement,
    rootSchema: JsonSchema,
    predicate: (JsonSchema) -> Boolean,
): Boolean = when (element) {
    is JsonArray -> element.any { traverse(it, rootSchema, predicate) }
    is JsonObject -> {
        val schemaElement = JsonSchema(element)
        if (predicate(schemaElement)) {
            true
        } else {
            val referenced = resolveReferencedSchema(schemaElement, rootSchema)
            if (referenced != schemaElement && traverse(referenced.toJsonObject(), rootSchema, predicate)) {
                true
            } else {
                val items = element["items"]
                val properties = element["properties"] as? JsonObject
                (items != null && traverse(items, rootSchema, predicate)) ||
                    properties?.values?.any { traverse(it, rootSchema, predicate) } == true
            }
        }
    }

    else -> false
}
