package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import com.chobantonov.jsonfroms.core.models.JsonSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** Applies the cell, collection, then global required-marker setting to a table header. */
fun tableColumnHeaderLabel(
    title: String,
    field: String?,
    itemSchema: JsonSchema,
    options: JsonOptions,
    config: Map<String, Any?>,
): String {
    if (field == null || title.isBlank()) return title
    val required = (itemSchema["required"] as? JsonArray)
        ?.any { (it as? JsonPrimitive)?.contentOrNull == field } == true
    if (!required) return title

    val cells = options["cells"] as? JsonObject
    val cell = cells?.get(field) as? JsonObject
    val hide = cell?.get("hideRequiredAsterisk").asBooleanOrNull()
        ?: options["hideRequiredAsterisk"].asBooleanOrNull()
        ?: config["hideRequiredAsterisk"].asBooleanOrNull()
        ?: false
    return if (hide) title else "$title*"
}

private fun Any?.asBooleanOrNull(): Boolean? = when (this) {
    is Boolean -> this
    is JsonPrimitive -> takeUnless { isString }?.booleanOrNull
    else -> null
}
