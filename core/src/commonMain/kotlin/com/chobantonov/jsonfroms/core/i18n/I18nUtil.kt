package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
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

fun translateScopedText(
    translator: Translator?,
    i18nKeyPrefix: String?,
    key: String,
    fallback: String,
    values: Map<String, Any?> = emptyMap(),
): String {
    val id = i18nKeyPrefix?.takeIf(String::isNotBlank)?.let { "$it.$key" } ?: key
    return (translator ?: defaultTranslator).translate(id, fallback, values) ?: fallback
}

val defaultErrorTranslator: ErrorTranslator = ErrorTranslator { error, translator, _ ->
    val explicitKey = error.params["i18nKey"]
    if (explicitKey != null) {
        translator.translate(explicitKey, null, mapOf("error" to error))
            ?: translator.translate(error.message, null, mapOf("error" to error))
            ?: error.message
    } else if (error.keyword == "required" && error.message.startsWith("missing required properties")) {
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
): String {
    if (errors.isEmpty()) return ""
    val prefix = getI18nKeyPrefix(schema, uiSchema, path)
    val context = mapOf("schema" to schema, "uischema" to uiSchema, "path" to path, "errors" to errors)
    translator.translate("$prefix.error.custom", null, context)?.let { return it }
    return formatErrorMessage(errors.map { error ->
        translateErrorForPath(error, errorTranslator, translator, schema, uiSchema, path)
    })
}

fun translateErrorForPath(
    error: ErrorObject,
    errorTranslator: ErrorTranslator,
    translator: Translator,
    schema: JsonSchema? = null,
    uiSchema: UiSchemaElement? = null,
    path: String? = null,
): String {
    if (errorTranslator !== defaultErrorTranslator) return errorTranslator.translate(error, translator, uiSchema)
    val keyword = error.keyword
    if (keyword != null) {
        val prefix = getI18nKeyPrefix(schema, uiSchema, path)
        val context = mapOf("error" to error, "schema" to schema, "uischema" to uiSchema, "path" to path)
        translator.translate("$prefix.error.$keyword", null, context)?.let { return it }
        translator.translate("error.$keyword", null, context)?.let { return it }
    }
    return defaultErrorTranslator.translate(error, translator, uiSchema)
}

fun deriveLabelForUISchemaElement(
    uiSchema: UiSchemaElement,
    translator: Translator,
): String? = when (uiSchema) {
    is LabelElement -> translator.translate(
        uiSchema.i18n?.let { "$it.text" } ?: uiSchema.text,
        uiSchema.text,
        mapOf("uischema" to uiSchema),
    ) ?: uiSchema.text
    is GroupElement -> translateLayoutLabel(uiSchema.label, uiSchema.i18n, uiSchema, translator)
    is CategoryElement -> translateLayoutLabel(uiSchema.label, uiSchema.i18n, uiSchema, translator)
    else -> null
}

private fun translateLayoutLabel(
    label: String?,
    i18n: String?,
    uiSchema: UiSchemaElement,
    translator: Translator,
): String? {
    val translationKey = i18n?.let { "$it.label" } ?: label ?: return null
    return translator.translate(
        translationKey,
        label,
        mapOf("uischema" to uiSchema),
    ) ?: label
}
