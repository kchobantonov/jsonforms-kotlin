package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val groupRendererLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "group.label" to JsonPrimitive("Група"),
        "group.expanded" to JsonPrimitive("Разгъната"),
        "group.collapsed" to JsonPrimitive("Свита"),
        "group.dataIndicator" to JsonPrimitive("Съдържа данни"),
        "group.validationIndicator" to JsonPrimitive("Съдържа грешки"),
        "group.validationIndicatorOne" to JsonPrimitive("1 грешка"),
        "group.validationIndicatorMany" to JsonPrimitive("{count} грешки"),
    )),
)

fun translateGroupRendererText(i18n: JsonFormsI18nState, key: String, documentedDefault: String): String =
    translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
        groupRendererLocaleBundles)

fun translateGroupValidationIndicator(i18n: JsonFormsI18nState, count: Int?): String {
    val bundleKey = when (count) {
        null -> "group.validationIndicator"
        1 -> "group.validationIndicatorOne"
        else -> "group.validationIndicatorMany"
    }
    val documentedDefault = when (count) {
        null -> "Contains errors"
        1 -> "1 error"
        else -> "$count errors"
    }
    return translateRendererText(i18n.translate, i18n.locale, "group.validationIndicator",
        documentedDefault, groupRendererLocaleBundles,
        values = mapOf("count" to count), bundleKey = bundleKey)
        .replace("{count}", count?.toString() ?: "")
}
