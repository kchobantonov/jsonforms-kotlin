package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import com.chobantonov.jsonfroms.core.models.JsonSchemaTypeValue
import com.chobantonov.jsonfroms.core.models.JsonSchemaOrSchemas
import com.chobantonov.jsonfroms.core.util.Resolve
import com.chobantonov.jsonfroms.core.util.composePaths
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

fun mixedSchemaTypes(schema: JsonSchema): List<JsonSchemaType> =
    (schema.typeValue as? JsonSchemaTypeValue.Multiple)?.values.orEmpty()

fun concreteMixedSchema(schema: JsonSchema, type: JsonSchemaType): JsonSchema {
    val values = schema.toJsonObject().toMutableMap()
    values["type"] = JsonPrimitive(type.jsonName())
    if (type == JsonSchemaType.Array && values["items"] == null) {
        values["items"] = unconstrainedMixedSchema("Item").toJsonObject()
    }
    if (type == JsonSchemaType.Object && values["additionalProperties"] == null) {
        values["additionalProperties"] = JsonPrimitive(true)
    }
    return JsonSchema(JsonObject(values))
}

fun jsonDataType(data: JsonElement): JsonSchemaType = when (data) {
    JsonNull -> JsonSchemaType.Null
    is JsonObject -> JsonSchemaType.Object
    is JsonArray -> JsonSchemaType.Array
    is JsonPrimitive -> when {
        data.isString -> JsonSchemaType.String
        data.booleanOrNull != null -> JsonSchemaType.Boolean
        data.longOrNull != null -> JsonSchemaType.Integer
        data.doubleOrNull != null -> JsonSchemaType.Number
        else -> JsonSchemaType.String
    }
}

fun JsonSchemaType.displayName(): String = jsonName().replaceFirstChar(Char::uppercase)

/**
 * Returns whether a value can still be edited using the explicitly selected mixed type.
 * A JSON integer is also a valid JSON number, so an integral draft must not silently
 * change a Number selection to Integer.
 */
fun mixedTypeSupportsData(selected: JsonSchemaType, dataType: JsonSchemaType): Boolean =
    selected == dataType || (selected == JsonSchemaType.Number && dataType == JsonSchemaType.Integer)

/** Container mixed values expose an editable detail panel as soon as they are selected. */
fun shouldOpenMixedDetails(type: JsonSchemaType?): Boolean =
    type == JsonSchemaType.Object || type == JsonSchemaType.Array

data class MixedTreeNode(
    val path: String,
    val label: String,
    val schema: JsonSchema,
    val type: JsonSchemaType,
    val parentPath: String? = null,
    val key: String? = null,
    val canRename: Boolean = false,
    val canDelete: Boolean = false,
    val children: List<MixedTreeNode> = emptyList(),
)

fun buildMixedTree(
    data: JsonElement,
    schema: JsonSchema,
    rootSchema: JsonSchema = schema,
    path: String = "",
    label: String = schema.title ?: "Root",
    includePrimitives: Boolean = true,
): MixedTreeNode = buildMixedTreeNode(
    data = data,
    schema = schema,
    rootSchema = rootSchema,
    path = path,
    label = label,
    includePrimitives = includePrimitives,
)

private fun buildMixedTreeNode(
    data: JsonElement,
    schema: JsonSchema,
    rootSchema: JsonSchema,
    path: String,
    label: String,
    parentPath: String? = null,
    key: String? = null,
    canRename: Boolean = false,
    canDelete: Boolean = false,
    includePrimitives: Boolean,
): MixedTreeNode {
    val resolved = Resolve.schema(schema, "", rootSchema) ?: schema
    val dataType = jsonDataType(data)
    val concrete = if (mixedSchemaTypes(resolved).contains(dataType)) {
        concreteMixedSchema(resolved, dataType)
    } else {
        resolved
    }
    val children = when (data) {
        is JsonObject -> data.entries.mapNotNull { (childKey, childData) ->
            if (!includePrimitives && childData !is JsonObject && childData !is JsonArray) return@mapNotNull null
            val declared = concrete.properties.orEmpty()[childKey]
            val childSchema = declared
                ?: additionalPropertySchema(concrete, childKey, rootSchema)
                ?: unconstrainedMixedSchema(childKey)
            buildMixedTreeNode(
                data = childData,
                schema = childSchema,
                rootSchema = rootSchema,
                path = composePaths(path, childKey),
                label = childKey,
                parentPath = path,
                key = childKey,
                canRename = declared == null,
                canDelete = true,
                includePrimitives = includePrimitives,
            )
        }
        is JsonArray -> data.mapIndexedNotNull { index, childData ->
            if (!includePrimitives && childData !is JsonObject && childData !is JsonArray) return@mapIndexedNotNull null
            buildMixedTreeNode(
                data = childData,
                schema = arrayItemSchema(concrete, index, rootSchema),
                rootSchema = rootSchema,
                path = composePaths(path, index.toString()),
                label = "Item ${index + 1}",
                parentPath = path,
                key = index.toString(),
                canDelete = true,
                includePrimitives = includePrimitives,
            )
        }
        else -> emptyList()
    }
    return MixedTreeNode(path, label, resolved, dataType, parentPath, key, canRename, canDelete, children)
}

private fun arrayItemSchema(schema: JsonSchema, index: Int, rootSchema: JsonSchema): JsonSchema {
    val selected = schema.prefixItems?.getOrNull(index) ?: when (val items = schema.items) {
        is JsonSchemaOrSchemas.Single -> items.schema
        is JsonSchemaOrSchemas.Multiple -> items.schemas.getOrNull(index)
        null -> null
    }
    return selected?.let { Resolve.schema(it, "", rootSchema) ?: it }
        ?: unconstrainedMixedSchema("Item ${index + 1}")
}

private fun unconstrainedMixedSchema(title: String): JsonSchema = JsonSchema(
    JsonObject(
        mapOf(
            "title" to JsonPrimitive(title),
            "type" to JsonArray(JsonSchemaType.entries.map { JsonPrimitive(it.jsonName()) }),
        ),
    ),
)

private fun JsonSchemaType.jsonName(): String = name.lowercase()
