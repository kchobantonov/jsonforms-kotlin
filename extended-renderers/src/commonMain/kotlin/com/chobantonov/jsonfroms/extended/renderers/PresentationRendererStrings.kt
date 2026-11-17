package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val presentationLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "link.urlRefused" to JsonPrimitive("Адресът на връзката е отказан от правилата за URL адреси"),
        "link.invalidTarget" to JsonPrimitive("Целта на връзката не се поддържа"),
        "button.actionScriptConflict" to JsonPrimitive("Действието на бутона има предимство пред скрипта"),
        "button.scriptUnsupported" to JsonPrimitive("Изпълнението на скрипт за бутона не е достъпно в тази среда"),
        "button.evaluationDisabled" to JsonPrimitive("Изпълнението на скрипт за бутона е изключено"),
        "action.unhandled" to JsonPrimitive("Няма регистриран обработчик за това действие."),
        "button.running" to JsonPrimitive("Изпълнява се"),
        "button.pendingEdit" to JsonPrimitive("Завърши текущата редакция, преди да изпълниш действието."),
        "image.noSource" to JsonPrimitive("ImageView изисква src или scope"),
        "image.scopeNotString" to JsonPrimitive("Полето за изображение не допуска текстов източник"),
        "image.nonStringSource" to JsonPrimitive("Данните за източника на изображението не са текст"),
        "image.urlRefused" to JsonPrimitive("Източникът на изображението е отказан от правилата за URL адреси"),
        "image.missingAlt" to JsonPrimitive("Липсва алтернативен текст за изображението"),
        "image.loadFailed" to JsonPrimitive("Източникът на изображението не може да се зареди"),
        "image.blocked" to JsonPrimitive("Изображението е блокирано"),
        "image.unavailable" to JsonPrimitive("Изображението не е достъпно"),
        "image.retry" to JsonPrimitive("Опитай отново"),
    )),
)
