package com.chobantonov.jsonfroms.core.models

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

typealias JsonData = JsonElement
typealias JsonOptions = Map<String, JsonElement>

fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject

fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray

fun JsonElement?.asJsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

fun JsonElement?.asJsonSchemaOrNull(): JsonSchema? = (this as? JsonObject)?.let(::JsonSchema)

fun buildJsonSchema(builder: JsonObjectBuilder.() -> Unit): JsonSchema =
    JsonSchema(buildJsonObject(builder))
