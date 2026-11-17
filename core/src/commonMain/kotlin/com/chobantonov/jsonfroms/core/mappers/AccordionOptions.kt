package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Explicit local false overrides the shared expansion default. */
fun resolveAccordionCollapsed(options: JsonOptions, config: Map<String, Any?>): Boolean {
    val local = options["collapsed"]
    val defaults = config["jsonformsExtended"]
    val global = when (defaults) {
        is Map<*, *> -> defaults["collapsed"]
        is JsonObject -> defaults["collapsed"]
        else -> null
    }
    val value = if (options.containsKey("collapsed")) local else global
    return when (value) {
        is Boolean -> value
        is JsonPrimitive -> if (value.isString) false else value.content.toBooleanStrictOrNull() ?: false
        else -> false
    }
}
