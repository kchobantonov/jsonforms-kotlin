package com.chobantonov.jsonfroms.extended.renderers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal val markupLabelLocaleBundles: Map<String, JsonObject> = mapOf(
    "bg" to JsonObject(mapOf(
        "markup.unsupported" to JsonPrimitive("Форматът за маркиране не се поддържа"),
        "markup.markdownDisabled" to JsonPrimitive("Markdown е деактивиран"),
        "markup.urlRefused" to JsonPrimitive("Връзката е блокирана от правилата за URL"),
        "markup.interpolationFailed" to JsonPrimitive("Някои стойности на етикета не могат да бъдат определени"),
    )),
)
