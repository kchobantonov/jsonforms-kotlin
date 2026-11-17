package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Exact dotted keys take precedence over a nested spelling of the same catalog key. */
fun lookupRendererCatalogString(catalog: JsonObject, key: String): String? {
    if (key in catalog) return catalog[key].catalogString()
    var value: JsonElement = catalog
    for (part in key.split('.')) {
        value = (value as? JsonObject)?.get(part) ?: return null
    }
    return value.catalogString()
}

private fun JsonElement?.catalogString(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/** The host translator receives the active renderer bundle string as its default. */
fun translateRendererText(
    translator: Translator?,
    locale: String?,
    key: String,
    documentedDefault: String,
    localeBundles: Map<String, JsonObject>,
    values: Map<String, Any?> = emptyMap(),
    i18nKeyPrefix: String? = null,
    bundleKey: String = key,
): String {
    val normalized = locale?.replace('_', '-')?.lowercase()?.takeIf(String::isNotBlank)
    val language = normalized?.substringBefore('-')
    val fallback = listOfNotNull(normalized, language).distinct().firstNotNullOfOrNull { tag ->
        localeBundles.entries.firstOrNull { it.key.replace('_', '-').lowercase() == tag }
            ?.value?.let { lookupRendererCatalogString(it, bundleKey) }
    } ?: documentedDefault
    return translateScopedText(translator, i18nKeyPrefix, key, fallback, values)
}
