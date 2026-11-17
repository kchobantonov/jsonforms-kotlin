package com.chobantonov.jsonfroms.core.i18n

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState

val tupleLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "tuple.configuration" to JsonPrimitive("Конфигурацията на кортежа не се поддържа"),
        "tuple.title" to JsonPrimitive("Кортеж"),
        "tuple.position" to JsonPrimitive("Елемент {position}"),
        "tuple.additionalItems" to JsonPrimitive("Допълнителни елементи"),
        "tuple.add" to JsonPrimitive("Добави елемент"),
        "tuple.edit" to JsonPrimitive("Редактирай {label}"),
        "tuple.delete" to JsonPrimitive("Изтрий {label}"),
        "tuple.invalidDetail" to JsonPrimitive("Неподдържано оформление на кортеж"),
        "tuple.invalidPosition" to JsonPrimitive("Неподдържана схема на елемент"),
        "tuple.invalidCurrentValue" to JsonPrimitive("Невалидна текуща стойност"),
        "tuple.validationIndicator" to JsonPrimitive("Съдържа грешки"),
        "tuple.validationIndicatorOne" to JsonPrimitive("1 грешка"),
        "tuple.validationIndicatorMany" to JsonPrimitive("{count} грешки"),
        "composite.summary.unset" to JsonPrimitive("Не е зададено"),
        "composite.summary.details" to JsonPrimitive("Подробности"),
        "composite.summary.item" to JsonPrimitive("{count} елемент"),
        "composite.summary.items" to JsonPrimitive("{count} елемента"),
        "composite.summary.more" to JsonPrimitive("(+{count} още)"),
        "composite.apply" to JsonPrimitive("Приложи"),
        "composite.cancel" to JsonPrimitive("Отказ"),
        "composite.empty" to JsonPrimitive("Изчисти"),
    )),
)

fun translateTupleRendererText(
    i18n: JsonFormsI18nState,
    key: String,
    documentedDefault: String,
    values: Map<String, Any?> = emptyMap(),
): String = values.entries.fold(
    translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
        if (key.startsWith("confirm.")) confirmationLocaleBundles else tupleLocaleBundles, values)
) { text, (name, value) -> text.replace("{$name}", value?.toString().orEmpty()) }

fun translateTupleValidationIndicator(i18n: JsonFormsI18nState, count: Int?): String {
    val bundleKey = when (count) {
        null -> "tuple.validationIndicator"
        1 -> "tuple.validationIndicatorOne"
        else -> "tuple.validationIndicatorMany"
    }
    val documentedDefault = when (count) {
        null -> "Contains errors"
        1 -> "1 error"
        else -> "$count errors"
    }
    return translateRendererText(i18n.translate, i18n.locale, "tuple.validationIndicator",
        documentedDefault, tupleLocaleBundles,
        values = mapOf("count" to count), bundleKey = bundleKey)
        .replace("{count}", count?.toString() ?: "")
}
