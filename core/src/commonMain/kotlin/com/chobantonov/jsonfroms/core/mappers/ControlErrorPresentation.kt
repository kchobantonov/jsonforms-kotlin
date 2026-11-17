package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Filters only inline control text. Structured errors and validity remain unchanged. */
internal fun filterControlErrorsBeforeTouch(
    errors: List<ErrorObject>,
    options: Map<String, Any?>,
): List<ErrorObject> {
    val enabled = when (val value = options["enableFilterErrorsBeforeTouch"]) {
        is Boolean -> value
        is JsonPrimitive -> !value.isString && value.booleanOrNull == true
        else -> false
    }
    if (!enabled) return errors

    val keywords = when (val value = options["filterErrorKeywordsBeforeTouch"]) {
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull }
        is List<*> -> value.filterIsInstance<String>()
        else -> emptyList()
    }.toSet()
    if (keywords.isEmpty()) return emptyList()
    return errors.filterNot { it.keyword in keywords }
}
