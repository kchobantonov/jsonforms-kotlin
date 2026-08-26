package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrBoolean
import com.chobantonov.jsonfroms.core.util.Resolve
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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

fun additionalPropertySchema(
    schema: JsonSchema,
    propertyName: String,
    rootSchema: JsonSchema = schema,
): JsonSchema? {
    val matchingPattern = schema.patternProperties.orEmpty().entries.firstOrNull { (pattern, _) ->
        runCatching { Regex(pattern).containsMatchIn(propertyName) }.getOrDefault(false)
    }?.value
    val selected = matchingPattern ?: when (val additional = schema.additionalProperties) {
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
): String? {
    if (propertyName.isBlank()) return "Property name is required"
    if ('.' in propertyName) return "Property names containing '.' are not supported"
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
