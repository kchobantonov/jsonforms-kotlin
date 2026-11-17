package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrBoolean
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import com.chobantonov.jsonfroms.core.util.Resolve
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private val unconstrainedTypes = JsonArray(
    listOf("array", "boolean", "integer", "null", "number", "object", "string").map(::JsonPrimitive),
)

fun supportsAdditionalProperties(
    schema: JsonSchema,
    config: Map<String, Any?> = emptyMap(),
): Boolean = schema.patternProperties.orEmpty().isNotEmpty() || when (val additional = schema.additionalProperties) {
    is JsonSchemaOrBoolean.BooleanValue -> additional.value
    is JsonSchemaOrBoolean.SchemaValue -> true
    null -> config["allowAdditionalPropertiesIfMissing"] == true
}

fun additionalPropertyKeys(schema: JsonSchema, data: JsonObject): List<String> {
    val declared = schema.properties.orEmpty().keys
    return data.keys.filterNot(declared::contains)
}

/** Dynamic fields rendered by the additional-properties editor cover their own validation errors. */
fun additionalPropertyValidationScopes(
    schema: JsonSchema,
    data: JsonObject,
    config: Map<String, Any?> = emptyMap(),
    rootSchema: JsonSchema = schema,
): List<List<String>> = if (!supportsAdditionalProperties(schema, config)) emptyList() else
    additionalPropertyKeys(schema, data)
        .filter { additionalPropertySchema(schema, it, rootSchema) != null }
        .map(::listOf)

/** A cleared isolated editor keeps the literal key and uses the ordinary dynamic-value default. */
fun preserveIsolatedAdditionalValue(
    value: JsonElement,
    schema: JsonSchema,
    rootSchema: JsonSchema = schema,
): JsonElement {
    if (value != JsonNull) return value
    val allowsNull = when (val type = schema.typeValue) {
        is JsonSchemaTypeValue.Single -> type.value == JsonSchemaType.Null
        is JsonSchemaTypeValue.Multiple -> JsonSchemaType.Null in type.values
        null -> true
    }
    return if (allowsNull) value else createDefaultValue(schema, rootSchema)
}

/** Rechecks the complete object count and name when a pending Add action reaches the reducer. */
fun addAdditionalProperty(
    current: JsonObject,
    snapshot: JsonObject,
    name: String,
    schema: JsonSchema,
    config: Map<String, Any?> = emptyMap(),
    rootSchema: JsonSchema = schema,
    restrict: Boolean = true,
    validator: JsonSchemaValidator? = null,
): JsonObject {
    if (current !== snapshot || (restrict && schema.maxProperties?.let { current.size >= it } == true))
        return current
    if (validateAdditionalPropertyName(schema, name, current.keys, config, rootSchema, validator) != null)
        return current
    val propertySchema = additionalPropertySchema(schema, name, rootSchema) ?: return current
    return JsonObject(current + (name to createDefaultValue(propertySchema, rootSchema)))
}

/** Applies a rename only to the object the editor opened, without overwriting a newer key. */
fun renameAdditionalProperty(
    current: JsonObject,
    snapshot: JsonObject,
    oldName: String,
    newName: String,
    schema: JsonSchema,
    config: Map<String, Any?> = emptyMap(),
    rootSchema: JsonSchema = schema,
    validator: JsonSchemaValidator? = null,
): JsonObject {
    if (current !== snapshot || oldName !in current || oldName == newName) return current
    if (validateAdditionalPropertyName(schema, newName, current.keys - oldName, config, rootSchema, validator) != null ||
        additionalPropertySchema(schema, newName, rootSchema) == null) return current
    val renamed = linkedMapOf<String, kotlinx.serialization.json.JsonElement>()
    current.forEach { (name, value) -> renamed[if (name == oldName) newName else name] = value }
    return JsonObject(renamed)
}

fun additionalPropertySchema(
    schema: JsonSchema,
    propertyName: String,
    rootSchema: JsonSchema = schema,
): JsonSchema? {
    val matchingPatterns = schema.patternProperties.orEmpty().entries.filter { (pattern, _) ->
        runCatching { Regex(pattern).containsMatchIn(propertyName) }.getOrDefault(false)
    }.map { (_, patternSchema) -> Resolve.schema(patternSchema, "", rootSchema) ?: patternSchema }
    if (matchingPatterns.size > 1) return JsonSchema(JsonObject(mapOf(
        "allOf" to JsonArray(matchingPatterns.map { it.value }),
        "title" to JsonPrimitive(propertyName),
    )))
    val selected = matchingPatterns.singleOrNull() ?: when (val additional = schema.additionalProperties) {
        is JsonSchemaOrBoolean.SchemaValue -> additional.schema
        is JsonSchemaOrBoolean.BooleanValue -> if (additional.value) JsonSchema() else null
        null -> JsonSchema()
    } ?: return null
    val resolved = Resolve.schema(selected, "", rootSchema) ?: selected
    if (resolved.typeValue != null || resolved.enumValues != null || resolved.oneOf != null || resolved.anyOf != null) {
        return resolved.withTitle(propertyName)
    }
    return JsonSchema(JsonObject(resolved.value + ("type" to unconstrainedTypes))).withTitle(propertyName)
}

fun validateAdditionalPropertyName(
    schema: JsonSchema,
    propertyName: String,
    existingNames: Set<String>,
    config: Map<String, Any?> = emptyMap(),
    rootSchema: JsonSchema = schema,
    validator: JsonSchemaValidator? = null,
): String? {
    if (propertyName.isBlank() && config["allowEmptyPropertyNames"] != true)
        return "Property name is required"
    if (propertyName in existingNames) return "A property with this name already exists"

    val names = schema.propertyNames?.let { Resolve.schema(it, "", rootSchema) ?: it }
    names?.minLength?.let { if (propertyName.length < it) return "Property name must contain at least $it characters" }
    names?.maxLength?.let { if (propertyName.length > it) return "Property name must contain at most $it characters" }
    names?.pattern?.let { pattern ->
        val matches = runCatching { Regex(pattern).containsMatchIn(propertyName) }.getOrDefault(false)
        if (!matches) return "Property name must match $pattern"
    }
    names?.enumValues?.let { allowed ->
        if (JsonPrimitive(propertyName) !in allowed) return "Property name is not allowed"
    }
    names?.const?.let { allowed ->
        if (JsonPrimitive(propertyName) != allowed) return "Property name is not allowed"
    }
    if (names != null && validator != null) {
        val issues = runCatching {
            validator.compile(rebaseLocalSchemaReferences(names, rootSchema))
                .validate(JsonPrimitive(propertyName))
        }.getOrNull() ?: return "Property name is invalid"
        if (issues.isNotEmpty()) return "Property name is invalid"
    }

    val matchesPattern = schema.patternProperties.orEmpty().keys.any { pattern ->
        runCatching { Regex(pattern).containsMatchIn(propertyName) }.getOrDefault(false)
    }
    val additionalAllowed = when (val additional = schema.additionalProperties) {
        is JsonSchemaOrBoolean.BooleanValue -> additional.value
        is JsonSchemaOrBoolean.SchemaValue -> true
        null -> config["allowAdditionalPropertiesIfMissing"] == true
    }
    if (!matchesPattern && !additionalAllowed) return "Property name does not match an allowed pattern"
    return null
}

private fun JsonSchema.withTitle(title: String): JsonSchema =
    JsonSchema(JsonObject(value + ("title" to JsonPrimitive(title))))
