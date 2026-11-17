package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.JsonPointer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Makes an extracted schema's root-local references resolve inside its own form or validator. */
fun rebaseLocalSchemaReferences(schema: JsonSchema, rootSchema: JsonSchema): JsonSchema {
    val existing = schema.value["definitions"] as? JsonObject ?: JsonObject(emptyMap())
    var key = "__jsonforms_root"
    var suffix = 1
    while (key in existing) key = "__jsonforms_root_${suffix++}"
    val prefix = "#/definitions/${JsonPointer.encode(key)}"

    fun rewrite(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (name, value) ->
            when {
                name in setOf("default", "enum", "const") -> value
                name == "\$ref" && value is JsonPrimitive && value.isString -> {
                    val reference = value.content
                    when {
                        reference == "#" -> JsonPrimitive(prefix)
                        reference.startsWith("#/") -> JsonPrimitive(prefix + reference.removePrefix("#"))
                        else -> value
                    }
                }
                else -> rewrite(value)
            }
        })
        is JsonArray -> JsonArray(element.map(::rewrite))
        else -> element
    }

    val rewritten = rewrite(schema.value) as JsonObject
    val definitions = (rewritten["definitions"] as? JsonObject ?: JsonObject(emptyMap())) +
        (key to rewrite(rootSchema.value))
    val dialect = schema.value["\$schema"] ?: rootSchema.value["\$schema"]
    val result = rewritten + ("definitions" to JsonObject(definitions)) +
        (dialect?.let { mapOf("\$schema" to it) } ?: emptyMap())
    return JsonSchema(JsonObject(result))
}
