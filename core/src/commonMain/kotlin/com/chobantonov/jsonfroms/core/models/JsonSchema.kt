package com.chobantonov.jsonfroms.core.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class JsonSchemaType {
    String,
    Number,
    Integer,
    Object,
    Array,
    Boolean,
    Null,
}

sealed interface JsonSchemaTypeValue {
    data class Single(
        val value: JsonSchemaType,
    ) : JsonSchemaTypeValue

    data class Multiple(
        val values: List<JsonSchemaType>,
    ) : JsonSchemaTypeValue
}

sealed interface JsonSchemaOrBoolean {
    data class SchemaValue(
        val schema: JsonSchema,
    ) : JsonSchemaOrBoolean

    data class BooleanValue(
        val value: Boolean,
    ) : JsonSchemaOrBoolean
}

sealed interface JsonSchemaOrSchemas {
    data class Single(
        val schema: JsonSchema,
    ) : JsonSchemaOrSchemas

    data class Multiple(
        val schemas: List<JsonSchema>,
    ) : JsonSchemaOrSchemas
}

/**
 * Rich schema wrapper used throughout the port.
 *
 * It preserves the full raw JSON object for compatibility while exposing
 * typed accessors for commonly used JSON Schema keywords across draft 4/7/2020-12.
 */
data class JsonSchema(
    val value: JsonObject = JsonObject(emptyMap()),
) : Map<String, JsonElement> by value {
    val ref: String? get() = string("\$ref")
    val id: String? get() = string("id")
    val draftId: String? get() = string("\$id")
    val schemaUri: String? get() = string("\$schema")
    val comment: String? get() = string("\$comment")
    val anchor: String? get() = string("\$anchor")
    val dynamicAnchor: String? get() = string("\$dynamicAnchor")
    val dynamicRef: String? get() = string("\$dynamicRef")
    val recursiveRef: String? get() = string("\$recursiveRef")
    val recursiveAnchor: Boolean? get() = boolean("\$recursiveAnchor")

    val title: String? get() = string("title")
    val description: String? get() = string("description")
    val default: JsonElement? get() = value["default"]
    val examples: List<JsonElement>? get() = array("examples")?.toList()
    val enumValues: List<JsonElement>? get() = array("enum")?.toList()
    val const: JsonElement? get() = value["const"]
    val format: String? get() = string("format")

    val typeValue: JsonSchemaTypeValue?
        get() = when (val element = value["type"]) {
            is JsonPrimitive -> element.stringContentOrNull()?.let(::schemaTypeFromString)?.let(JsonSchemaTypeValue::Single)
            is JsonArray -> {
                val types = element.mapNotNull { (it as? JsonPrimitive)?.stringContentOrNull()?.let(::schemaTypeFromString) }
                if (types.isEmpty()) null else JsonSchemaTypeValue.Multiple(types)
            }
            else -> null
        }

    val multipleOf: Double? get() = number("multipleOf")
    val maximum: Double? get() = number("maximum")
    val exclusiveMaximumNumber: Double? get() = number("exclusiveMaximum")
    val exclusiveMaximumBoolean: Boolean? get() = boolean("exclusiveMaximum")
    val minimum: Double? get() = number("minimum")
    val exclusiveMinimumNumber: Double? get() = number("exclusiveMinimum")
    val exclusiveMinimumBoolean: Boolean? get() = boolean("exclusiveMinimum")

    val maxLength: Int? get() = int("maxLength")
    val minLength: Int? get() = int("minLength")
    val pattern: String? get() = string("pattern")

    val additionalItems: JsonSchemaOrBoolean? get() = schemaOrBoolean("additionalItems")
    val items: JsonSchemaOrSchemas? get() = schemaOrSchemas("items")
    val prefixItems: List<JsonSchema>? get() = schemaList("prefixItems")
    val unevaluatedItems: JsonSchemaOrBoolean? get() = schemaOrBoolean("unevaluatedItems")
    val contains: JsonSchema? get() = schema("contains")
    val minContains: Int? get() = int("minContains")
    val maxContains: Int? get() = int("maxContains")
    val maxItems: Int? get() = int("maxItems")
    val minItems: Int? get() = int("minItems")
    val uniqueItems: Boolean? get() = boolean("uniqueItems")

    val maxProperties: Int? get() = int("maxProperties")
    val minProperties: Int? get() = int("minProperties")
    val required: List<String>? get() = stringList("required")
    val additionalProperties: JsonSchemaOrBoolean? get() = schemaOrBoolean("additionalProperties")
    val unevaluatedProperties: JsonSchemaOrBoolean? get() = schemaOrBoolean("unevaluatedProperties")
    val properties: Map<String, JsonSchema>? get() = schemaMap("properties")
    val patternProperties: Map<String, JsonSchema>? get() = schemaMap("patternProperties")
    val propertyNames: JsonSchema? get() = schema("propertyNames")
    val dependencies: Map<String, JsonElement>? get() = objectValue("dependencies")?.toMap()
    val dependentRequired: Map<String, List<String>>? get() = stringListMap("dependentRequired")
    val dependentSchemas: Map<String, JsonSchema>? get() = schemaMap("dependentSchemas")
    val definitions: Map<String, JsonSchema>? get() = schemaMap("definitions")
    val defs: Map<String, JsonSchema>? get() = schemaMap("\$defs")

    val allOf: List<JsonSchema>? get() = schemaList("allOf")
    val anyOf: List<JsonSchema>? get() = schemaList("anyOf")
    val oneOf: List<JsonSchema>? get() = schemaList("oneOf")
    val not: JsonSchema? get() = schema("not")
    val ifSchema: JsonSchema? get() = schema("if")
    val thenSchema: JsonSchema? get() = schema("then")
    val elseSchema: JsonSchema? get() = schema("else")

    val readOnly: Boolean? get() = boolean("readOnly")
    val writeOnly: Boolean? get() = boolean("writeOnly")
    val contentEncoding: String? get() = string("contentEncoding")
    val contentMediaType: String? get() = string("contentMediaType")
    val contentSchema: JsonSchema? get() = schema("contentSchema")
    val errorMessage: JsonElement? get() = value["errorMessage"]

    fun toJsonObject(): JsonObject = value

    private fun schema(key: String): JsonSchema? = value[key].asJsonSchemaOrNull()

    private fun schemaList(key: String): List<JsonSchema>? =
        (value[key] as? JsonArray)?.mapNotNull { it.asJsonSchemaOrNull() }

    private fun schemaMap(key: String): Map<String, JsonSchema>? =
        objectValue(key)?.mapValues { (_, element) -> JsonSchema(element as JsonObject) }

    private fun schemaOrBoolean(key: String): JsonSchemaOrBoolean? =
        when (val element = value[key]) {
            is JsonObject -> JsonSchemaOrBoolean.SchemaValue(JsonSchema(element))
            is JsonPrimitive -> element.booleanValueOrNull()?.let { JsonSchemaOrBoolean.BooleanValue(it) }
            else -> null
        }

    private fun schemaOrSchemas(key: String): JsonSchemaOrSchemas? =
        when (val element = value[key]) {
            is JsonObject -> JsonSchemaOrSchemas.Single(JsonSchema(element))
            is JsonArray -> JsonSchemaOrSchemas.Multiple(element.mapNotNull { it.asJsonSchemaOrNull() })
            else -> null
        }

    private fun objectValue(key: String): JsonObject? = value[key] as? JsonObject

    private fun string(key: String): String? = (value[key] as? JsonPrimitive)?.stringContentOrNull()

    private fun boolean(key: String): Boolean? = (value[key] as? JsonPrimitive)?.booleanValueOrNull()

    private fun int(key: String): Int? = (value[key] as? JsonPrimitive)?.intValueOrNull()

    private fun number(key: String): Double? = (value[key] as? JsonPrimitive)?.doubleValueOrNull()

    private fun array(key: String): JsonArray? = value[key] as? JsonArray

    private fun stringList(key: String): List<String>? =
        array(key)?.mapNotNull { (it as? JsonPrimitive)?.stringContentOrNull() }

    private fun stringListMap(key: String): Map<String, List<String>>? =
        objectValue(key)?.mapValues { (_, element) ->
            (element as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.stringContentOrNull() }.orEmpty()
        }
}

typealias JsonSchema4 = JsonSchema
typealias JsonSchema7 = JsonSchema
typealias JsonSchema202012 = JsonSchema

private fun schemaTypeFromString(value: String): JsonSchemaType? =
    when (value) {
        "string" -> JsonSchemaType.String
        "number" -> JsonSchemaType.Number
        "integer" -> JsonSchemaType.Integer
        "object" -> JsonSchemaType.Object
        "array" -> JsonSchemaType.Array
        "boolean" -> JsonSchemaType.Boolean
        "null" -> JsonSchemaType.Null
        else -> null
    }

private fun JsonPrimitive.stringContentOrNull(): String? =
    if (isString) {
        content
    } else {
        when (content.lowercase()) {
            "true", "false", "null" -> null
            else -> content
        }
    }

private fun JsonPrimitive.booleanValueOrNull(): Boolean? =
    when (content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

private fun JsonPrimitive.intValueOrNull(): Int? = content.toIntOrNull()

private fun JsonPrimitive.doubleValueOrNull(): Double? = content.toDoubleOrNull()
