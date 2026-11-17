package com.chobantonov.jsonfroms.core.testers

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.ScopedUiSchemaElement
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull

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
        uiSchema is ScopedUiSchemaElement -> resolveScopedSchema(uiSchema, schema, context)
        schema != null -> schema
        else -> null
    }
    resolved?.let(predicate) ?: false
}

fun schemaMatches(predicate: (JsonSchema, JsonSchema) -> Boolean): Tester = { uiSchema, schema, context ->
    val resolved = when {
        uiSchema is ScopedUiSchemaElement -> resolveScopedSchema(uiSchema, schema, context)
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
        uiSchema is ScopedUiSchemaElement -> resolveScopedSchema(uiSchema, schema, context)
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

val isBooleanControl: Tester = and(uiTypeIs("Control"), or(schemaTypeIs("boolean"), composedScalarTypeIs(JsonSchemaType.Boolean)))
val isMixedControl: Tester = and(
    uiTypeIs("Control"),
    schemaMatches { schema -> schema.typeValue is com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue.Multiple },
)
val isObjectControl: Tester = and(uiTypeIs("Control"), schemaTypeIs("object"))
val isAllOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("allOf") })
val isAnyOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("anyOf") })
val isOneOfControl: Tester = and(uiTypeIs("Control"), schemaMatches { schema -> schema.containsKey("oneOf") })
/** A scalar's validation alternatives do not describe separate editing forms. */
val isSingleScalarEditorCombinator: Tester = and(
    uiTypeIs("Control"),
    schemaMatches { schema, root -> singleScalarEditorType(schema, root) != null },
)

private fun composedScalarTypeIs(type: JsonSchemaType): Tester =
    schemaMatches { schema, root -> singleScalarEditorType(schema, root) == type }

private fun singleScalarEditorType(schema: JsonSchema, root: JsonSchema): JsonSchemaType? {
    val keywords = listOf("oneOf", "anyOf", "allOf").filter(schema::containsKey)
    if (keywords.size != 1) return null
    val keyword = keywords.single()
    val branches = schema[keyword] as? JsonArray ?: return null
    if (branches.isEmpty()) return null
    val outerType = (schema.typeValue as? JsonSchemaTypeValue.Single)?.value
    if (outerType != null && outerType !in SCALAR_EDITOR_TYPES) return null
    if (branches.all { it is JsonObject && it.keys.all(SCALAR_BRANCH_KEYWORDS::contains) }) {
        return outerType
    }
    // Only allOf can establish one type through a referenced branch without
    // turning distinct alternatives into an incorrectly merged editor.
    if (keyword != "allOf") return null
    val resolved = branches.map { branch ->
        val objectBranch = branch as? JsonObject ?: return null
        val ref = (objectBranch["\$ref"] as? JsonPrimitive)?.stringContentOrNull()
        if (ref != null) {
            if (objectBranch.keys != setOf("\$ref")) return null
            resolveSchema(root, ref, root) ?: return null
        } else JsonSchema(objectBranch)
    }
    if (resolved.any { branch ->
            branch.keys.any { it != "type" && it !in SCALAR_BRANCH_KEYWORDS }
        }) return null
    val types = resolved.mapNotNull { (it.typeValue as? JsonSchemaTypeValue.Single)?.value }.distinct()
    val type = outerType ?: types.singleOrNull()
    return type?.takeIf { it in SCALAR_EDITOR_TYPES && types.all { branchType -> branchType == it } }
}

private val SCALAR_EDITOR_TYPES = setOf(
    JsonSchemaType.String, JsonSchemaType.Number, JsonSchemaType.Integer, JsonSchemaType.Boolean,
)

private val SCALAR_BRANCH_KEYWORDS = setOf(
    "title", "description", "default", "examples", "readOnly", "writeOnly", "deprecated", "\$comment",
    "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf",
    "minLength", "maxLength", "pattern",
)
val isEnumControl: Tester = and(uiTypeIs("Control"), schemaMatches(::isEnumSchema))
val isOneOfEnumControl: Tester = and(uiTypeIs("Control"), schemaMatches(::isOneOfEnumSchema))
val isIntegerControl: Tester = and(uiTypeIs("Control"), or(schemaTypeIs("integer"), composedScalarTypeIs(JsonSchemaType.Integer)))
val isNumberControl: Tester = and(uiTypeIs("Control"), or(schemaTypeIs("number"), composedScalarTypeIs(JsonSchemaType.Number)))
val isStringControl: Tester = and(uiTypeIs("Control"), or(schemaTypeIs("string"), composedScalarTypeIs(JsonSchemaType.String)))
/** Compact plain text cells must leave specialized string presentations to their own renderers. */
val plainStringCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    if (control != null && schema != null &&
        schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) &&
        schema.ref == null && schema.format == null && schema.enumValues == null && schema.const == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null) 1 else NOT_APPLICABLE
}
/** Numeric cells use the ordinary editor only for unambiguous scalar schemas. */
val plainNumericCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    val type = schema?.typeValue
    if (control != null && schema != null &&
        (type == JsonSchemaTypeValue.Single(JsonSchemaType.Integer) ||
            type == JsonSchemaTypeValue.Single(JsonSchemaType.Number)) &&
        schema.ref == null && schema.format == null && schema.enumValues == null && schema.const == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null &&
        control.options["slider"] != JsonPrimitive(true)) 2 else NOT_APPLICABLE
}
/** An explicit enum keeps its typed option values in the compact selector. */
val plainEnumCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    if (control != null && schema != null && schema.enumValues != null &&
        schema.ref == null && schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null) 103 else NOT_APPLICABLE
}
/** Scalar const choices use a compact selector; Boolean agreements keep their toggle. */
val plainConstCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    val constant = schema?.const as? JsonPrimitive
    if (control != null && schema != null && constant != null && constant != JsonNull &&
        (constant.isString || constant.booleanOrNull == null) &&
        schema.ref == null && schema.enumValues == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null) 104 else NOT_APPLICABLE
}
/** Const-only oneOf choices retain their labels and typed values in compact cells. */
val plainOneOfEnumCellTester: RankedTester = { ui, schema, context ->
    val control = ui as? ControlElement
    if (control != null && schema != null && isOneOfEnumControl(ui, schema, context) &&
        schema.ref == null && schema.enumValues == null && schema.const == null &&
        schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null) 105 else NOT_APPLICABLE
}
/** An authored radio presentation outranks the compact choice selector. */
val radioChoiceCellTester: RankedTester = { ui, schema, context ->
    val control = ui as? ControlElement
    if (control != null && schema != null && schema.ref == null &&
        control.options["format"] == JsonPrimitive("radio") &&
        control.options["variant"] == null &&
        (isEnumControl(ui, schema, context) || isOneOfEnumControl(ui, schema, context))) 120
    else NOT_APPLICABLE
}
/** Opt-in bounded numeric sliders retain their family widget in table cells. */
val sliderCellTester: RankedTester = { ui, schema, context ->
    val control = ui as? ControlElement
    if (control != null && schema != null && isRangeControl(ui, schema, context) &&
        schema.ref == null && schema.enumValues == null && schema.const == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["format"] == null && control.options["variant"] == null) 110
    else NOT_APPLICABLE
}
/** Temporal string cells reuse their picker and draft editor without a repeated field frame. */
val plainTemporalCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    val schemaFormat = schema?.format
    val uiFormat = (control?.options?.get("format") as? JsonPrimitive)
        ?.takeIf(JsonPrimitive::isString)?.content
    val formats = setOf("date", "time", "date-time")
    val selectedFormat = schemaFormat ?: uiFormat
    if (control != null && schema != null &&
        schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) &&
        selectedFormat in formats && (schemaFormat == null || uiFormat == null || schemaFormat == uiFormat) &&
        schema.ref == null && schema.enumValues == null && schema.const == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null &&
        control.options["variant"] == null) 104 else NOT_APPLICABLE
}
/** Compact password cells keep masking; OTP boxes are for standalone controls. */
val plainPasswordCellTester: RankedTester = { ui, schema, _ ->
    val control = ui as? ControlElement
    val schemaFormat = schema?.format
    val uiFormat = (control?.options?.get("format") as? JsonPrimitive)
        ?.takeIf(JsonPrimitive::isString)?.content
    val variant = control?.options?.get("variant")
    if (control != null && schema != null &&
        schema.typeValue == JsonSchemaTypeValue.Single(JsonSchemaType.String) &&
        (schemaFormat == "password" || uiFormat == "password") &&
        (schemaFormat == null || uiFormat == null || schemaFormat == uiFormat) &&
        (variant == null || variant == JsonPrimitive("otp")) &&
        schema.ref == null && schema.enumValues == null && schema.const == null &&
        schema.oneOf == null && schema.anyOf == null && schema.allOf == null) 104
    else NOT_APPLICABLE
}
val isMultiLineControl: Tester = and(uiTypeIs("Control"), optionIs("multi", true))
val isPasswordControl: Tester = and(isStringControl, or(formatIs("password"), optionIs("format", "password")))
val isDateControl: Tester = and(isStringControl, or(formatIs("date"), optionIs("format", "date")))
val isTimeControl: Tester = and(isStringControl, or(formatIs("time"), optionIs("format", "time")))
val isDateTimeControl: Tester = and(isStringControl, or(formatIs("date-time"), optionIs("format", "date-time")))

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

val isMixedArrayControl: Tester = and(
    uiTypeIs("Control"),
    schemaTypeIs("array"),
    or(
        schemaSubPathMatches("items") { schema, _ ->
            schema.typeValue is com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue.Multiple ||
                listOf("oneOf", "anyOf", "allOf").any(schema::containsKey)
        },
        schemaMatches { schema -> (schema["items"] as? JsonPrimitive)?.booleanValueOrNull() == false },
    ),
)

val isRangeControl: Tester = and(
    uiTypeIs("Control"),
    or(schemaTypeIs("number"), schemaTypeIs("integer")),
    schemaMatches { schema ->
        schema.containsKey("maximum") && schema.containsKey("minimum") && schema.containsKey("default")
    },
    { ui, _, _ -> ui.options["slider"] == JsonPrimitive(true) },
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
            } else if (value.containsKey("allOf")) {
                true
            } else if ((value.containsKey("anyOf") ||
                    value.containsKey("oneOf") && !isOneOfEnumSchema(value)) &&
                !com.chobantonov.jsonfroms.core.util.hasFlatAlternativeObjectProperties(value, rootSchema)) {
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

private fun resolveScopedSchema(
    uiSchema: ScopedUiSchemaElement,
    schema: JsonSchema?,
    context: TesterContext,
): JsonSchema? {
    val root = context.rootSchema ?: schema ?: return null
    return resolveSchema(schema ?: root, uiSchema.scope, root)
}

private fun optionEquals(
    option: JsonElement?,
    expected: Any?,
): Boolean = when (expected) {
    is JsonElement -> option == expected
    is String -> (option as? JsonPrimitive)?.stringContentOrNull() == expected
    is Boolean -> (option as? JsonPrimitive)?.takeUnless { it.isString }?.booleanValueOrNull() == expected
    is Number -> (option as? JsonPrimitive)?.takeUnless { it.isString }
        ?.doubleValueOrNull() == expected.toDouble()
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
