package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrBoolean
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrSchemas
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object JsonPath {
    fun get(root: JsonElement?, path: String): JsonElement? {
        if (root == null || path.isBlank()) {
            return root
        }

        var current: JsonElement? = root
        parse(path).forEach { segment ->
            current = when (val container = current) {
                is JsonObject -> container[segment]
                is JsonArray -> segment.toArrayIndex()?.let(container::getOrNull)
                else -> null
            }
        }
        return current
    }

    fun set(
        root: JsonElement?,
        path: String,
        value: JsonElement?,
        schema: JsonSchema? = null,
    ): JsonElement {
        val segments = parse(path)
        if (segments.isEmpty()) {
            return value ?: JsonNull
        }

        return setAt(
            current = root ?: containerFor(schema),
            segments = segments,
            value = value,
            schema = schema,
            rootSchema = schema,
        )
    }

    private fun setAt(
        current: JsonElement?,
        segments: List<String>,
        value: JsonElement?,
        schema: JsonSchema?,
        rootSchema: JsonSchema?,
    ): JsonElement {
        val segment = segments.first()
        val remaining = segments.drop(1)
        val resolvedSchema = resolveSchema(schema, "", rootSchema) ?: schema
        val isArray = current is JsonArray || (current !is JsonObject && resolvedSchema?.let(::deriveTypes)?.contains("array") == true)

        return if (isArray) {
            val index = requireNotNull(segment.toArrayIndex()) {
                "Array path segment must be a non-negative integer: $segment"
            }
            val source = current as? JsonArray ?: JsonArray(emptyList())
            val mutable = source.toMutableList()
            while (mutable.size <= index) {
                mutable += JsonNull
            }
            val childSchema = arrayItemSchema(resolvedSchema, index, rootSchema)

            if (remaining.isEmpty()) {
                if (value == null) {
                    if (index in mutable.indices) {
                        mutable.removeAt(index)
                    }
                } else {
                    mutable[index] = value
                }
            } else {
                mutable[index] = setAt(mutable.getOrNull(index), remaining, value, childSchema, rootSchema)
            }

            buildJsonArray {
                mutable.forEach(::add)
            }
        } else {
            val source = current as? JsonObject ?: JsonObject(emptyMap())
            val childSchema = objectPropertySchema(resolvedSchema, segment)
            val updatedValue = if (remaining.isEmpty()) {
                value
            } else {
                setAt(source[segment], remaining, value, childSchema, rootSchema)
            }

            buildJsonObject {
                source.forEach { (key, existing) ->
                    if (key == segment) {
                        if (updatedValue != null) {
                            put(key, updatedValue)
                        }
                    } else {
                        put(key, existing)
                    }
                }
                if (segment !in source && updatedValue != null) {
                    put(segment, updatedValue)
                }
            }
        }
    }

    internal fun parse(path: String): List<String> =
        path.split('.').filter(String::isNotEmpty)

    private fun String.toArrayIndex(): Int? =
        takeIf { it.matches(Regex("(?:0|[1-9][0-9]*)")) }?.toIntOrNull()

    private fun containerFor(schema: JsonSchema?): JsonElement =
        if (schema?.let(::deriveTypes)?.contains("array") == true) {
            JsonArray(emptyList())
        } else {
            JsonObject(emptyMap())
        }

    private fun objectPropertySchema(
        schema: JsonSchema?,
        property: String,
    ): JsonSchema? =
        schema?.properties?.get(property)
            ?: (schema?.additionalProperties as? JsonSchemaOrBoolean.SchemaValue)?.schema

    private fun arrayItemSchema(
        schema: JsonSchema?,
        index: Int,
        rootSchema: JsonSchema?,
    ): JsonSchema? {
        schema ?: return null
        schema.prefixItems?.getOrNull(index)?.let { return resolveSchema(it, "", rootSchema) ?: it }
        return when (val items = schema.items) {
            is JsonSchemaOrSchemas.Single -> resolveSchema(items.schema, "", rootSchema) ?: items.schema
            is JsonSchemaOrSchemas.Multiple -> items.schemas.getOrNull(index)
            null -> null
        }
    }
}
