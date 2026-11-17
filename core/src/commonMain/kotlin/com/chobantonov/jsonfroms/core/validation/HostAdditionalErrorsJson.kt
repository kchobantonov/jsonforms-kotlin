package com.chobantonov.jsonfroms.core.validation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Decode the JSON array accepted by the Web Component's additional-errors attribute. */
fun decodeHostAdditionalErrors(text: String?): List<ErrorObject>? {
    if (text == null) return emptyList()
    val values = runCatching { Json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return null
    return runCatching { values.map { raw ->
        val value = raw as? JsonObject ?: error("additional errors must be objects")
        fun requiredString(key: String): String =
            (value[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: error("additional error $key must be a string")
        fun optionalString(key: String): String? = value[key]?.let { field ->
            (field as? JsonPrimitive)?.takeIf { it.isString }?.content
                ?: error("additional error $key must be a string")
        }
        val params = (value["params"] ?: JsonObject(emptyMap())).let { fields ->
            (fields as? JsonObject)?.mapValues { (_, field) ->
                (field as? JsonPrimitive)?.takeIf { it.isString }?.content
                    ?: error("additional error params must contain strings")
            } ?: error("additional error params must be an object")
        }
        ErrorObject(
            instancePath = optionalString("instancePath").orEmpty(),
            dataPath = optionalString("dataPath"),
            schemaPath = optionalString("schemaPath"),
            keyword = optionalString("keyword"),
            message = requiredString("message"),
            params = params,
            absoluteKeywordLocation = optionalString("absoluteKeywordLocation"),
        )
    } }.getOrNull()
}
