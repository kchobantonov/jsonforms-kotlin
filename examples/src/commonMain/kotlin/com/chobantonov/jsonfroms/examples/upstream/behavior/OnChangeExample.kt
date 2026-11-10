package com.chobantonov.jsonfroms.examples.upstream.behavior

import com.chobantonov.jsonfroms.core.actions.UpdateAction
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.reducers.Middleware
import com.chobantonov.jsonfroms.core.util.getControlPath
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private val touchedProperties = mutableMapOf(
    "name" to false,
    "description" to false,
)

val onChangeSchema = JsonSchema(
    buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("name") {
                put("type", "string")
                put("minLength", 1)
            }
            putJsonObject("description") {
                put("type", "string")
                put("minLength", 1)
            }
        }
        putJsonObject("required") {
        }
    }.let { schema ->
        buildJsonObject {
            schema.forEach { (key, value) ->
                put(key, value)
            }
            put("required", kotlinx.serialization.json.buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("name"))
                add(kotlinx.serialization.json.JsonPrimitive("description"))
            })
        }
    },
)

val onChangeMiddleware = Middleware { state, action, defaultReducer ->
    val nextState = defaultReducer(state, action)
    if (action is UpdateAction) {
        val data = nextState.data as? JsonObject
        data?.keys?.forEach { key ->
            touchedProperties[key] = true
        }

        val filteredErrors = nextState.errors.filter { error ->
            val topLevelProperty = getControlPath(error).substringBefore('.', missingDelimiterValue = getControlPath(error))
            touchedProperties[topLevelProperty] == true
        }

        if (filteredErrors.size < nextState.errors.size) {
            nextState.copy(errors = filteredErrors)
        } else {
            nextState
        }
    } else {
        nextState
    }
}

val onChangeData = buildJsonObject {}

val onChangeExample = ExampleDescription(
    name = "onChange",
    label = "On Change Listener",
    data = onChangeData,
    schema = onChangeSchema,
    uischema = null,
    middleware = onChangeMiddleware,
)
