package com.chobantonov.jsonfroms.core.mappers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** Distinguishes a missing string from imported JSON of the wrong type. */
fun invalidStringStoredValue(data: JsonElement, dataProvided: Boolean): JsonElement? =
    data.takeIf { dataProvided && (it as? JsonPrimitive)?.isString != true }
