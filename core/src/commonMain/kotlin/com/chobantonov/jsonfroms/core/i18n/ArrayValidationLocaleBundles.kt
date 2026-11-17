package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val arrayValidationLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "array.validationIndicator" to JsonPrimitive("Съдържа грешки"),
        "array.validationIndicatorOne" to JsonPrimitive("1 грешка"),
        "array.validationIndicatorMany" to JsonPrimitive("{count} грешки"),
        "array.item.validationIndicator" to JsonPrimitive("Елемент {index}: съдържа грешки"),
        "array.item.validationIndicatorOne" to JsonPrimitive("Елемент {index}: 1 грешка"),
        "array.item.validationIndicatorMany" to JsonPrimitive("Елемент {index}: {count} грешки"),
    )),
)

/** Index is one-based for an item; null selects the array summary label. */
fun translateArrayValidationIndicator(i18n: JsonFormsI18nState, count: Int?, index: Int? = null): String {
    val key = if (index == null) "array.validationIndicator" else "array.item.validationIndicator"
    val bundleKey = when (count) {
        null -> key
        1 -> "${key}One"
        else -> "${key}Many"
    }
    val suffix = when (count) {
        null -> "Contains errors"
        1 -> "1 error"
        else -> "$count errors"
    }
    val documentedDefault = if (index == null) suffix else "Item $index: $suffix"
    val values = if (index == null) mapOf("count" to count)
        else mapOf("index" to index, "count" to count)
    return translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
        arrayValidationLocaleBundles, values = values, bundleKey = bundleKey)
        .replace("{index}", index?.toString() ?: "")
        .replace("{count}", count?.toString() ?: "")
}
