package com.chobantonov.jsonfroms.material.renderers.complex

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val arrayRendererLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "array.agGridFallback" to JsonPrimitive("AG Grid не е наличен тук; показва се таблица за масив."),
        "array.invalidCurrentValue" to JsonPrimitive("Невалидна текуща стойност"),
        "array.branchOption" to JsonPrimitive("Вариант {index}"),
        "composite.summary.unset" to JsonPrimitive("Не е зададено"),
        "composite.summary.emptyObject" to JsonPrimitive("Празен обект"),
        "composite.summary.unspecified" to JsonPrimitive("{label} не е посочено"),
        "composite.summary.details" to JsonPrimitive("Подробности"),
        "composite.summary.item" to JsonPrimitive("{count} елемент"),
        "composite.summary.items" to JsonPrimitive("{count} елемента"),
        "composite.summary.more" to JsonPrimitive("(+{count} още)"),
        "composite.apply" to JsonPrimitive("Приложи"),
        "composite.cancel" to JsonPrimitive("Отказ"),
        "composite.close" to JsonPrimitive("Затвори"),
        "composite.empty" to JsonPrimitive("Изчисти"),
        "composite.remove" to JsonPrimitive("Премахни"),
        "composite.applyTooltip" to JsonPrimitive("Приложи промените и затвори."),
        "composite.cancelTooltip" to JsonPrimitive("Отхвърли промените и затвори."),
        "composite.emptyTooltip" to JsonPrimitive("Изчисти съдържанието, като запазиш обекта или масива."),
        "composite.removeTooltip" to JsonPrimitive("Премахни тази стойност от данните на формуляра."),
    )),
)
