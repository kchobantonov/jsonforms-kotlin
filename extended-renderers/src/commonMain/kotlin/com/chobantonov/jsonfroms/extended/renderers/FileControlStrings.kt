package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Built-in file copy is keyed by renderer contract IDs, not English source text. */
internal val fileControlLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "file.attachment" to JsonPrimitive("Прикачен файл"),
        "file.empty" to JsonPrimitive("Няма файл"),
        "file.remove" to JsonPrimitive("Премахни"),
        "file.clearAll" to JsonPrimitive("Премахни всички"),
        "file.select" to JsonPrimitive("Избери"),
        "file.add" to JsonPrimitive("Добави"),
        "file.addAttachment" to JsonPrimitive("Добави прикачен файл"),
        "file.replace" to JsonPrimitive("Замени"),
        "file.clear" to JsonPrimitive("Изчисти"),
        "file.choose" to JsonPrimitive("Избери файл"),
        "file.manualValue" to JsonPrimitive("Данни за файл или URL"),
        "file.duplicate" to JsonPrimitive("Този файл вече е прикачен"),
        "file.countLimit" to JsonPrimitive("Достигнат е максималният брой прикачени файлове"),
        "file.sizeRejected" to JsonPrimitive("Избраният файл не отговаря на ограничението за размер"),
        "file.readFailed" to JsonPrimitive("Избраният файл не може да бъде прочетен"),
        "file.sizeInvalid" to JsonPrimitive("Избраният файл нарушава ограничението за размер"),
    )),
)

internal fun fileCountLocaleBundles(count: Int): Map<String, JsonObject> =
    fileControlLocaleBundles.mapValues { (_, catalog) ->
        val sizeInvalid = "$count избрани файла нарушават ограниченията за размер"
        val errorCount = "$count грешки"
        JsonObject(catalog + mapOf(
            "file.sizeInvalid" to JsonPrimitive(sizeInvalid),
            "file.errorCount" to JsonPrimitive(errorCount),
        ))
    }
