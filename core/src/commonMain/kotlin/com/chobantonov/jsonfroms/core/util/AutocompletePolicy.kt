package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Searchable finite choices are opt-in; only JSON or host booleans count. */
fun resolveAutocomplete(option: Any?, config: Map<String, Any?>): Boolean =
    autocompleteBoolean(option) ?: autocompleteBoolean(config["autocomplete"]) ?: false

private fun autocompleteBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is JsonPrimitive -> value.takeUnless(JsonPrimitive::isString)?.booleanOrNull
    else -> null
}
