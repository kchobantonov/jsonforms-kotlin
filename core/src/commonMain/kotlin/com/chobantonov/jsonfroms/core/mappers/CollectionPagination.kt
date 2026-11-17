package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.JsonOptions
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class CollectionPaginationOptions(
    val enabled: Boolean,
    val pageSize: Int,
    val pageSizeOptions: List<Int>,
)

/** Resolve one complete pagination value; fields never merge across option levels. */
fun resolveCollectionPagination(
    options: JsonOptions,
    config: Map<String, Any?>,
    kind: String = "array",
    defaultEnabled: Boolean = true,
): CollectionPaginationOptions {
    fun member(value: Any?, key: String): Any? = when (value) {
        is JsonObject -> value[key]
        is Map<*, *> -> value[key]
        else -> null
    }
    fun supportedShape(value: Any?): Boolean = when (value) {
        is Boolean, is JsonObject, is Map<*, *> -> true
        is JsonPrimitive -> !value.isString && value.booleanOrNull != null
        else -> false
    }
    val local = options["pagination"].takeIf(::supportedShape)
    val scoped = member(member(config["jsonformsExtended"], kind), "pagination")
        .takeIf(::supportedShape)
    val value = local ?: scoped ?: defaultEnabled
    fun positiveInteger(value: Any?): Int? {
        val number = when (value) {
            is Number -> value.toDouble()
            is JsonPrimitive -> if (value.isString) null else value.content.toDoubleOrNull()
            else -> null
        } ?: return null
        return number.takeIf { it.isFinite() && it >= 1.0 && it <= Int.MAX_VALUE && it == it.toInt().toDouble() }
            ?.toInt()
    }
    val size = positiveInteger(member(value, "pageSize")) ?: 5
    val defaultChoices = listOf(5, 10, 25, 50)
    val configuredChoices = when (val choices = member(value, "pageSizeOptions")) {
        is JsonArray -> choices.toList()
        is List<*> -> choices
        else -> defaultChoices
    }
    val validChoices = configuredChoices.mapNotNull(::positiveInteger)
    val choices = ((validChoices.takeIf { it.isNotEmpty() } ?: defaultChoices) + size)
        .distinct().sorted()
    val enabled = when (value) {
        is Boolean -> value
        is JsonPrimitive -> value.booleanOrNull ?: defaultEnabled
        else -> true
    }
    return CollectionPaginationOptions(enabled, size, choices)
}

/** Apply only explicitly authored AG Grid pagination fields to portable settings. */
fun applyAgGridPaginationOverrides(
    portable: CollectionPaginationOptions,
    nativeOptions: JsonObject?,
): CollectionPaginationOptions {
    if (nativeOptions == null) return portable
    fun positiveInteger(value: kotlinx.serialization.json.JsonElement?): Int? {
        val primitive = (value as? JsonPrimitive)?.takeUnless { it.isString } ?: return null
        val number = primitive.content.toDoubleOrNull() ?: return null
        return number.takeIf {
            it.isFinite() && it >= 1.0 && it <= Int.MAX_VALUE && it == it.toInt().toDouble()
        }?.toInt()
    }
    val enabled = (nativeOptions["pagination"] as? JsonPrimitive)
        ?.takeUnless { it.isString }?.booleanOrNull ?: portable.enabled
    val size = positiveInteger(nativeOptions["paginationPageSize"]) ?: portable.pageSize
    val choices = when (val selector = nativeOptions["paginationPageSizeSelector"]) {
        is JsonArray -> (selector.mapNotNull(::positiveInteger) + size).distinct().sorted()
        is JsonPrimitive -> when (selector.takeUnless { it.isString }?.booleanOrNull) {
            false -> listOf(size)
            true -> (listOf(5, 10, 25, 50) + size).distinct().sorted()
            null -> portable.pageSizeOptions + size
        }
        else -> portable.pageSizeOptions + size
    }.distinct().sorted()
    return CollectionPaginationOptions(enabled, size, choices)
}

data class CollectionPageWindow(
    val page: Int,
    val pages: Int,
    val start: Int,
    val endExclusive: Int,
)

fun collectionPageWindow(total: Int, requestedPage: Int, size: Int, enabled: Boolean): CollectionPageWindow {
    val safeSize = size.coerceAtLeast(1)
    val pages = if (total <= 0) 1 else (total - 1) / safeSize + 1
    val page = if (enabled) requestedPage.coerceIn(1, pages) else 1
    val start = if (enabled) (page - 1) * safeSize else 0
    return CollectionPageWindow(page, pages, start, if (enabled) minOf(total, start + safeSize) else total)
}

fun pageForFirstVisibleItem(firstVisibleIndex: Int, nextSize: Int): Int =
    firstVisibleIndex / nextSize.coerceAtLeast(1) + 1
