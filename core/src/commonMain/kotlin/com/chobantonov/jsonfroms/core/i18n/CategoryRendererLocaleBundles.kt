package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val categoryRendererLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "category.validationIndicator" to JsonPrimitive("Съдържа грешки"),
        "category.validationIndicatorOne" to JsonPrimitive("1 грешка"),
        "category.validationIndicatorMany" to JsonPrimitive("{count} грешки"),
    )),
)

fun translateCategoryValidationIndicator(i18n: JsonFormsI18nState, count: Int?): String {
    val bundleKey = when (count) {
        null -> "category.validationIndicator"
        1 -> "category.validationIndicatorOne"
        else -> "category.validationIndicatorMany"
    }
    val documentedDefault = when (count) {
        null -> "Contains errors"
        1 -> "1 error"
        else -> "$count errors"
    }
    return translateRendererText(i18n.translate, i18n.locale, "category.validationIndicator",
        documentedDefault, categoryRendererLocaleBundles,
        values = mapOf("count" to count), bundleKey = bundleKey)
        .replace("{count}", count?.toString() ?: "")
}
