package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Shared top-level array action flags. Element options override the host config, including false. */
data class ArrayActionPolicy(val disableAdd: Boolean, val disableRemove: Boolean)

fun resolveArrayActionPolicy(options: JsonOptions, config: Map<String, Any?>): ArrayActionPolicy =
    ArrayActionPolicy(
        disableAdd = arrayActionFlag("disableAdd", options, config),
        disableRemove = arrayActionFlag("disableRemove", options, config),
    )

private fun arrayActionFlag(name: String, options: JsonOptions, config: Map<String, Any?>): Boolean =
    (options[name] as? JsonPrimitive)?.booleanOrNull ?: when (val configured = config[name]) {
        is Boolean -> configured
        is JsonPrimitive -> configured.booleanOrNull
        else -> null
    } ?: false
