package com.chobantonov.jsonfroms.core.mappers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Distinguishes a missing number from imported JSON of the wrong type. */
fun invalidNumericStoredValue(data: JsonElement, dataProvided: Boolean): JsonElement? {
    if (!dataProvided) return null
    val isNumber = data is JsonPrimitive && data !== JsonNull && !data.isString && data.booleanOrNull == null
    return data.takeUnless { isNumber }
}
