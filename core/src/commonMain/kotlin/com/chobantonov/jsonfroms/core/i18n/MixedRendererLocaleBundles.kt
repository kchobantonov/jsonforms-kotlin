package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import com.chobantonov.jsonfroms.core.mappers.displayName
import com.chobantonov.jsonfroms.core.models.JsonSchemaType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val mixedRendererLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "mixed.selectType" to JsonPrimitive("Избери тип"),
        "mixed.openTooltip" to JsonPrimitive("Отвори"),
        "mixed.openAriaLabel" to JsonPrimitive("Отвори подробностите"),
        "mixed.searchTree" to JsonPrimitive("Търси в дървото…"),
        "mixed.clearSearch" to JsonPrimitive("Изчисти търсенето"),
        "mixed.expand" to JsonPrimitive("Разгъни"),
        "mixed.collapse" to JsonPrimitive("Свий"),
        "mixed.showPrimitives" to JsonPrimitive("Покажи простите стойности"),
        "mixed.hidePrimitives" to JsonPrimitive("Скрий простите стойности"),
        "mixed.root" to JsonPrimitive("Корен"),
        "mixed.type" to JsonPrimitive("Тип"),
        "mixed.type.string" to JsonPrimitive("Низ"),
        "mixed.type.number" to JsonPrimitive("Число"),
        "mixed.type.integer" to JsonPrimitive("Цяло число"),
        "mixed.type.object" to JsonPrimitive("Обект"),
        "mixed.type.array" to JsonPrimitive("Масив"),
        "mixed.type.boolean" to JsonPrimitive("Булева стойност"),
        "mixed.type.null" to JsonPrimitive("Нула"),
        "mixed.rename" to JsonPrimitive("Преименувай"),
        "mixed.remove" to JsonPrimitive("Изтрий"),
        "mixed.confirm" to JsonPrimitive("Потвърди"),
        "mixed.cancel" to JsonPrimitive("Отказ"),
    )),
)

fun translateMixedRendererText(
    i18n: JsonFormsI18nState,
    i18nKeyPrefix: String?,
    key: String,
    documentedDefault: String,
    values: Map<String, Any?> = emptyMap(),
): String = translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
    mixedRendererLocaleBundles, values, i18nKeyPrefix, bundleKey = "mixed.$key")

fun translateMixedTypeName(i18n: JsonFormsI18nState, type: JsonSchemaType): String =
    translateMixedRendererText(i18n, null, "type.${type.name.lowercase()}", type.displayName())
