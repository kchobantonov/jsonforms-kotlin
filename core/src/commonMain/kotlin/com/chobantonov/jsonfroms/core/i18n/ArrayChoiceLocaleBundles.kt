package com.chobantonov.jsonfroms.core.i18n

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

val arrayChoiceLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "arrayChoice.add" to JsonPrimitive("Добави избор"),
        "arrayChoice.remove" to JsonPrimitive("Премахни избор"),
        "arrayChoice.replace" to JsonPrimitive("Смени избор"),
        "arrayChoice.current" to JsonPrimitive("Текуща стойност"),
        "arrayChoice.outsideChoices" to JsonPrimitive("Текуща стойност извън опциите"),
        "arrayChoice.search" to JsonPrimitive("Търси избор"),
        "arrayChoice.noResults" to JsonPrimitive("Няма съвпадения"),
        "arrayChoice.token" to JsonPrimitive("Нов елемент"),
    )),
)
