package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.JsonFormsI18nState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Built-in confirmation copy shared by Material and Cupertino renderers. */
val confirmationLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "confirm.typeChange.title" to JsonPrimitive("Промяна на типа?"),
        "confirm.typeChange.message" to JsonPrimitive("Текущата стойност ще бъде заменена."),
        "confirm.delete.title" to JsonPrimitive("Изтриване на стойността?"),
        "confirm.delete.message" to JsonPrimitive("Стойността ще бъде премахната. Това действие не може да се отмени тук."),
        "confirm.accept" to JsonPrimitive("Да"),
        "confirm.decline" to JsonPrimitive("Не"),
    )),
)

fun translateConfirmationText(
    i18n: JsonFormsI18nState,
    key: String,
    documentedDefault: String,
): String = translateRendererText(i18n.translate, i18n.locale, key, documentedDefault,
    confirmationLocaleBundles)
