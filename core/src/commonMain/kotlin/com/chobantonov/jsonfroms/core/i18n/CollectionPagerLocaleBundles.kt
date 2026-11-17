package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val collectionPagerLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "collection.pageSize" to JsonPrimitive("Размер на страница"),
        "collection.previous" to JsonPrimitive("Предишна страница"),
        "collection.next" to JsonPrimitive("Следваща страница"),
        "collection.page" to JsonPrimitive("{current} от {pages}"),
        "collection.firstError" to JsonPrimitive("Към първата грешка"),
        "collection.errorPageOne" to JsonPrimitive("Страница {page}: 1 грешка"),
        "collection.errorPageMany" to JsonPrimitive("Страница {page}: {count} грешки"),
    )),
)

fun translateCollectionPagerText(
    translator: Translator?,
    locale: String?,
    key: String,
    documentedDefault: String,
    values: Map<String, Any?> = emptyMap(),
): String = values.entries.fold(
    translateRendererText(translator, locale, key, documentedDefault,
        collectionPagerLocaleBundles, values)
) { result, (name, value) -> result.replace("{$name}", value?.toString().orEmpty()) }

fun translateCollectionErrorPage(
    translator: Translator?,
    locale: String?,
    page: Int,
    count: Int,
): String = translateRendererText(translator, locale, "collection.errorPage",
    "Page $page: $count ${if (count == 1) "error" else "errors"}",
    collectionPagerLocaleBundles,
    values = mapOf("page" to page, "count" to count),
    bundleKey = if (count == 1) "collection.errorPageOne" else "collection.errorPageMany")
    .replace("{page}", page.toString())
    .replace("{count}", count.toString())
