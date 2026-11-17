package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val tableValidationLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "composite.validationIndicator" to JsonPrimitive("{label} съдържа грешки"),
        "composite.validationIndicatorOne" to JsonPrimitive("{label}: 1 грешка"),
        "composite.validationIndicatorMany" to JsonPrimitive("{label}: {count} грешки"),
        "rowDetail.validationIndicatorOne" to JsonPrimitive("Елемент {index}: 1 грешка"),
        "rowDetail.validationIndicatorMany" to JsonPrimitive("Елемент {index}: {count} грешки"),
    )),
)

fun translateCompositeValidationIndicator(i18n: JsonFormsI18nState, label: String, count: Int?): String {
    val bundleKey = when (count) {
        null -> "composite.validationIndicator"
        1 -> "composite.validationIndicatorOne"
        else -> "composite.validationIndicatorMany"
    }
    val documentedDefault = if (count == null) "$label contains errors"
        else "$label: $count ${if (count == 1) "error" else "errors"}"
    return translateRendererText(i18n.translate, i18n.locale, "composite.validationIndicator",
        documentedDefault, tableValidationLocaleBundles,
        values = mapOf("label" to label, "count" to count), bundleKey = bundleKey)
        .replace("{count}", count?.toString() ?: "")
        .replace("{label}", label)
}

fun translateRowDetailValidationIndicator(i18n: JsonFormsI18nState, index: Int, count: Int): String {
    val bundleKey = if (count == 1) "rowDetail.validationIndicatorOne"
        else "rowDetail.validationIndicatorMany"
    val documentedDefault = "Item $index: $count ${if (count == 1) "error" else "errors"}"
    return translateRendererText(i18n.translate, i18n.locale, "rowDetail.validationIndicator",
        documentedDefault, tableValidationLocaleBundles,
        values = mapOf("index" to index, "count" to count), bundleKey = bundleKey)
        .replace("{index}", index.toString())
        .replace("{count}", count.toString())
}
