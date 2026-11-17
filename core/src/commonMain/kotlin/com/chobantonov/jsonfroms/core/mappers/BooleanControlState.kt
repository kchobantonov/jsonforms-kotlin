package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.util.JsonPath
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class BooleanControlState(
    val value: Boolean?,
    val hasValue: Boolean,
    val clearValue: JsonElement?,
    val invalidValue: JsonElement? = null,
)

fun booleanControlState(root: JsonElement, path: String): BooleanControlState {
    val stored = JsonPath.get(root, path)
    val parentPath = path.substringBeforeLast('.', missingDelimiterValue = "")
    val inArray = JsonPath.get(root, parentPath) is JsonArray
    val value = (stored as? JsonPrimitive)?.takeUnless(JsonPrimitive::isString)?.booleanOrNull
    return BooleanControlState(
        value = value,
        hasValue = stored != null,
        clearValue = if (inArray) JsonPrimitive(false) else null,
        invalidValue = stored.takeIf { value == null },
    )
}
