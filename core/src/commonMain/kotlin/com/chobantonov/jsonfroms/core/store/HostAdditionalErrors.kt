package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonElement

/** Retain host errors only while the value at each error's JSON Pointer is unchanged. */
fun retainHostErrorsForUnchangedValues(
    previousData: JsonElement,
    nextData: JsonElement,
    errors: List<ErrorObject>,
): List<ErrorObject> = errors.filter { error ->
    JsonPointer.resolve(previousData, error.instancePath) == JsonPointer.resolve(nextData, error.instancePath)
}
