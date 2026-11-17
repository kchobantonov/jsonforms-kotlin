package com.chobantonov.jsonfroms.examples.compose

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal const val DemoServerErrorOwner = "demo-server"

internal fun replaceServerErrors(existing: List<ErrorObject>, params: JsonObject): List<ErrorObject> =
    clearServerErrors(existing) + serverErrorsFromAction(params)

internal fun clearServerErrors(existing: List<ErrorObject>): List<ErrorObject> =
    existing.filterNot { it.params["owner"] == DemoServerErrorOwner }

/** Decode the portable server-error payload of a registered demo action. */
internal fun serverErrorsFromAction(params: JsonObject): List<ErrorObject> =
    (params["errors"] as? JsonArray).orEmpty().mapNotNull { raw ->
        val value = raw as? JsonObject ?: return@mapNotNull null
        val message = (value["message"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
        val fields = (value["params"] as? JsonObject).orEmpty().mapNotNull { (key, field) ->
            (field as? JsonPrimitive)?.contentOrNull?.let { key to it }
        }.toMap()
        ErrorObject(
            instancePath = (value["instancePath"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            schemaPath = (value["schemaPath"] as? JsonPrimitive)?.contentOrNull,
            keyword = (value["keyword"] as? JsonPrimitive)?.contentOrNull,
            message = message,
            params = fields + ("owner" to DemoServerErrorOwner),
        )
    }
