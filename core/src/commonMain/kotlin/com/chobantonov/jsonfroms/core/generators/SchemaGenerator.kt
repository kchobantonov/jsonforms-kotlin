package com.chobantonov.jsonfroms.core.generators

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.buildJsonSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val ADDITIONAL_PROPERTIES = "additionalProperties"
private const val REQUIRED_PROPERTIES = "required"

typealias RequiredPropertiesProvider = (Map<String, JsonSchema>) -> List<String>

data class JsonSchemaGenerationOptions(
    val additionalProperties: Boolean = true,
    val requiredProperties: RequiredPropertiesProvider = { it.keys.toList() },
)

fun generateJsonSchema(
    instance: JsonElement,
    options: JsonSchemaGenerationOptions = JsonSchemaGenerationOptions(),
): JsonSchema = SchemaGenerator(options).property(instance)

private class SchemaGenerator(
    private val options: JsonSchemaGenerationOptions,
) {
    fun schemaObject(data: JsonObject): JsonSchema {
        val properties = properties(data)
        val required = options.requiredProperties(properties)
        return buildJsonSchema {
            put("type", "object")
            put("properties", JsonObject(properties.mapValues { it.value.toJsonObject() }))
            put(ADDITIONAL_PROPERTIES, options.additionalProperties)
            if (required.isNotEmpty()) {
                put(REQUIRED_PROPERTIES, buildJsonArray { required.forEach { add(JsonPrimitive(it)) } })
            }
        }
    }

    private fun properties(data: JsonObject): Map<String, JsonSchema> =
        data.mapValues { (_, value) -> property(value) }

    fun property(data: JsonElement): JsonSchema =
        when (data) {
            JsonNull -> simpleType("null")
            is JsonObject -> schemaObjectOrArray(data)
            is JsonArray -> schemaArray(data)
            is JsonPrimitive -> when {
                data.isString -> simpleType("string")
                data.booleanOrNullCompat() != null -> simpleType("boolean")
                data.intOrNullCompat() != null -> simpleType("integer")
                data.doubleOrNullCompat() != null -> simpleType("number")
                else -> buildJsonSchema { }
            }
        }

    private fun schemaObjectOrArray(data: JsonElement): JsonSchema =
        when (data) {
            is JsonArray -> schemaArray(data)
            is JsonObject -> schemaObject(data)
            else -> buildJsonSchema { }
        }

    private fun schemaArray(data: JsonArray): JsonSchema {
        if (data.isEmpty()) {
            return buildJsonSchema {
                put("type", "array")
                put("items", buildJsonObject { })
            }
        }

        val uniqueProperties = data
            .map(::property)
            .distinctBy { it.toString() }

        return if (uniqueProperties.size == 1) {
            buildJsonSchema {
                put("type", "array")
                put("items", uniqueProperties.first().toJsonObject())
            }
        } else {
            buildJsonSchema {
                put("type", "array")
                put("items", buildJsonObject {
                    put("oneOf", buildJsonArray { uniqueProperties.forEach { add(it.toJsonObject()) } })
                })
            }
        }
    }

    private fun simpleType(type: String): JsonSchema = buildJsonSchema {
        put("type", type)
    }
}

private fun JsonPrimitive.booleanOrNullCompat(): Boolean? =
    when (content.lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }

private fun JsonPrimitive.intOrNullCompat(): Int? = content.toIntOrNull()

private fun JsonPrimitive.doubleOrNullCompat(): Double? = content.toDoubleOrNull()
