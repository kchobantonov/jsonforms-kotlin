package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.Internationalizable
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.store.Translator
import com.chobantonov.jsonfroms.core.util.formatErrorMessage
import com.chobantonov.jsonfroms.core.util.isInternationalized
import com.chobantonov.jsonfroms.core.validation.ErrorObject
import kotlinx.serialization.json.JsonPrimitive

val defaultTranslator: Translator = Translator { _, defaultMessage, _ -> defaultMessage }

val defaultErrorTranslator: ErrorTranslator = ErrorTranslator { error, translator, _ ->
    if (error.keyword == "required" && error.message.startsWith("missing required properties")) {
        translator.translate("is a required property", "is a required property", mapOf("error" to error))
            ?: "is a required property"
    } else {
        error.message
    }
}

fun getI18nKeyPrefixBySchema(
    schema: JsonSchema?,
    uiSchema: UiSchemaElement?,
): String? =
    if (isInternationalized(uiSchema)) {
        (uiSchema as? Internationalizable)?.i18n
    } else {
        (schema?.get("i18n") as? JsonPrimitive)?.content
    }

fun transformPathToI18nPrefix(path: String?): String =
    path
        ?.split('.')
        ?.filter { it.toIntOrNull() == null }
        ?.joinToString(".")
        ?.ifBlank { "root" }
        ?: "root"

fun getI18nKeyPrefix(
    schema: JsonSchema?,
    uiSchema: UiSchemaElement?,
    path: String?,
): String = getI18nKeyPrefixBySchema(schema, uiSchema) ?: transformPathToI18nPrefix(path)

fun getI18nKey(
    schema: JsonSchema?,
    uiSchema: UiSchemaElement?,
    path: String?,
    key: String,
): String = "${getI18nKeyPrefix(schema, uiSchema, path)}.$key"

fun getCombinedErrorMessage(
    errors: List<ErrorObject>,
    errorTranslator: ErrorTranslator,
    translator: Translator,
    schema: JsonSchema? = null,
    uiSchema: UiSchemaElement? = null,
    path: String? = null,
): String = formatErrorMessage(errors.map { error -> errorTranslator.translate(error, translator, uiSchema) })

fun deriveLabelForUISchemaElement(
    uiSchema: UiSchemaElement,
    translator: Translator,
): String? = when (uiSchema) {
    is LabelElement -> translator.translate(uiSchema.text, uiSchema.text, emptyMap()) ?: uiSchema.text
    is com.chobantonov.jsonfroms.core.models.GroupElement -> uiSchema.label
    is com.chobantonov.jsonfroms.core.models.CategoryElement -> uiSchema.label
    else -> null
}
