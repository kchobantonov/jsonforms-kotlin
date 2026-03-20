package com.chobantonov.jsonfroms.core.store

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.validation.ErrorObject

fun interface Translator {
    fun translate(
        id: String,
        defaultMessage: String?,
        values: Map<String, Any?>,
    ): String?
}

fun interface ErrorTranslator {
    fun translate(
        error: ErrorObject,
        translator: Translator,
        uiSchema: UiSchemaElement?,
    ): String
}

data class JsonFormsI18nState(
    val locale: String? = null,
    val translate: Translator? = null,
    val translateError: ErrorTranslator? = null,
)

typealias I18nJsonSchema = JsonSchema
