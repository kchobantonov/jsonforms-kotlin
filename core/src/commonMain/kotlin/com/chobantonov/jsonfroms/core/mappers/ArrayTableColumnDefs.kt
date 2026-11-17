package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class ArrayTableColumnDef(
    val field: String,
    val wholeRow: Boolean,
    val title: String?,
    val width: Float?,
    val minWidth: Float?,
    val maxWidth: Float?,
)

/** Null means automatic schema columns; an empty list is an explicit actions-only table. */
fun resolveArrayTableColumnDefs(options: JsonOptions, agGridRequested: Boolean): List<ArrayTableColumnDef>? {
    val native = (options["agGridOptions"] as? JsonObject)
        ?.takeIf { agGridRequested && "columnDefs" in it }
        ?.get("columnDefs")
    val source = native ?: options["columnDefs"] ?: return null
    val entries = source as? JsonArray ?: return emptyList()
    val seen = mutableSetOf<String>()
    return entries.mapNotNull { entry ->
        val definition = entry as? JsonObject ?: return@mapNotNull null
        val field = (definition["field"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
            ?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        if (!seen.add(field)) return@mapNotNull null
        fun positivePixels(name: String): Float? {
            val raw = (definition[name] as? JsonPrimitive)?.takeUnless { it.isString }
                ?.content?.toDoubleOrNull() ?: return null
            return raw.takeIf { it.isFinite() && it > 0 && it <= Float.MAX_VALUE }?.toFloat()
        }
        ArrayTableColumnDef(
            field = field,
            wholeRow = (definition["scope"] as? JsonPrimitive)?.contentOrNull == "#",
            title = (definition["headerName"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull,
            width = positivePixels("width"),
            minWidth = positivePixels("minWidth"),
            maxWidth = positivePixels("maxWidth"),
        )
    }
}
