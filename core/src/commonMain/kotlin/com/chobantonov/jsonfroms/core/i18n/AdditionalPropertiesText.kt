package com.chobantonov.jsonfroms.core.i18n

import com.chobantonov.jsonfroms.core.store.Translator

private val additionalPropertiesSpecKeys = mapOf(
    "title" to "title",
    "propertyNameLabel" to "namePlaceholder",
    "renamePropertyNameLabel" to "renameTitle",
    "addTooltip" to "add",
    "addAriaLabel" to "addTo",
    "renameTooltip" to "rename",
    "renameAriaLabel" to "rename",
    "removeTooltip" to "delete",
    "removeAriaLabel" to "delete",
    "saveTooltip" to "renameConfirm",
    "saveAriaLabel" to "renameConfirm",
    "propertyAlreadyDefined" to "nameTaken",
    "propertyNameRequired" to "nameRequired",
    "propertyNameInvalid" to "nameInvalid",
)

/** Scoped renderer strings take precedence over the portable example's generic strings. */
fun translateAdditionalPropertiesText(
    translator: Translator?,
    prefix: String,
    key: String,
    fallback: String,
    values: Map<String, Any?> = emptyMap(),
): String {
    if (translator == null) return fallback
    translator.translate("$prefix.$key", null, values)?.let { return it }
    val specKey = additionalPropertiesSpecKeys[key] ?: return fallback
    return translator.translate("additionalProperties.$specKey", null, values) ?: fallback
}
