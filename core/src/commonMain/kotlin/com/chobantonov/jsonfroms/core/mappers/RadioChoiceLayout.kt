package com.chobantonov.jsonfroms.core.mappers

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private fun choiceVertical(value: Any?): Boolean =
    when (value) {
        is Boolean -> value
        is JsonPrimitive -> !value.isString && value.booleanOrNull == true
        else -> false
    }

/** Local `vertical` wins; radio alone reads the nested extension default. */
fun radioChoiceVertical(config: Map<String, Any?>, options: Map<String, Any?>): Boolean {
    if ("vertical" in options) return choiceVertical(options["vertical"])
    val extended = config["jsonformsExtended"] as? Map<*, *>
    val radio = extended?.get("radio") as? Map<*, *>
    return choiceVertical(radio?.get("vertical"))
}

/** Automatic checkbox groups have no host orientation default. */
fun checkboxChoiceVertical(options: Map<String, Any?>): Boolean =
    choiceVertical(options["vertical"])
