package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val rowDetailLocaleBundles = mapOf(
    "bg" to JsonObject(mapOf(
        "rowDetail.edit" to JsonPrimitive("Редактиране на подробностите"),
        "rowDetail.view" to JsonPrimitive("Преглед на подробностите"),
        "rowDetail.show" to JsonPrimitive("Покажи подробностите"),
        "rowDetail.hide" to JsonPrimitive("Скрий подробностите"),
        "rowDetail.currentRow" to JsonPrimitive("Текущ ред с подробности"),
        "rowDetail.resize" to JsonPrimitive("Промени размера на таблицата и подробностите"),
        "rowDetail.title" to JsonPrimitive("Подробности за елемент {index}"),
        "rowDetail.apply" to JsonPrimitive("Приложи"),
        "rowDetail.cancel" to JsonPrimitive("Отказ"),
        "rowDetail.maximize" to JsonPrimitive("Увеличи"),
        "rowDetail.restore" to JsonPrimitive("Възстанови"),
        "rowDetail.close" to JsonPrimitive("Затвори"),
        "rowDetail.resizeDialog" to JsonPrimitive("Промени размера на диалога"),
        "rowDetail.noSelection" to JsonPrimitive("Няма избран елемент"),
    )),
)

fun translateRowDetailText(
    translator: Translator?,
    locale: String?,
    key: String,
    documentedDefault: String,
    values: Map<String, Any?> = emptyMap(),
    bundleKey: String = key,
): String = values.entries.fold(
    translateRendererText(translator, locale, key, documentedDefault,
        rowDetailLocaleBundles, values, bundleKey = bundleKey)
) { result, (name, value) -> result.replace("{$name}", value?.toString().orEmpty()) }
