package com.chobantonov.jsonfroms.core.util

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Resolves the authored element option before the host setting and the extended-spec default. */
fun resolveRestrict(option: Any?, config: Map<String, Any?>): Boolean =
    restrictBoolean(option) ?: restrictBoolean(config["restrict"]) ?: true

private fun restrictBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is JsonPrimitive -> value.booleanOrNull
    else -> null
}
