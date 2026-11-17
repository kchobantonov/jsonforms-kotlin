package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val codeProfileLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "editor.monacoUnavailable" to JsonPrimitive("Редакторът Monaco не е наличен на този хост"),
    )),
)

internal fun templateLayoutLocaleBundles(language: String): Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "template.unsupportedLanguage" to JsonPrimitive("Езикът на шаблона не се поддържа: $language"),
        "template.engineUnavailable" to JsonPrimitive("Шаблонният двигател не е наличен на този хост: $language"),
        "template.evaluationDisabled" to JsonPrimitive("Изпълнението на скриптове в шаблони е деактивирано"),
    )),
)
