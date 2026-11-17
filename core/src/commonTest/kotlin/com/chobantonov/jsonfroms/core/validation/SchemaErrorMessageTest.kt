package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.i18n.defaultErrorTranslator
import com.chobantonov.jsonfroms.core.i18n.getCombinedErrorMessage
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.store.ErrorTranslator
import com.chobantonov.jsonfroms.core.store.Translator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class SchemaErrorMessageTest {
    private val schema = JsonSchema(Json.parseToJsonElement("""
        {"type":"object","properties":{"certifications":{"type":"array","items":{
          "type":"object","properties":{"name":{"type":"string","minLength":1,
            "errorMessage":{"minLength":"certificateNameRequired"}}}}}}}
    """).jsonObject)
    private val data = Json.parseToJsonElement("""{"certifications":[{"name":""},{"name":""}]}""")

    @Test
    fun preservesIndividualKeywordPathsAndProvidesBusinessMessageKey() {
        val errors = OptimumCodeJsonSchemaValidator().validate(schema, data)
        assertEquals(listOf("/certifications/0/name", "/certifications/1/name"),
            errors.map(ErrorObject::instancePath))
        assertEquals(listOf("minLength", "minLength"), errors.map(ErrorObject::keyword))
        assertEquals(listOf("certificateNameRequired", "certificateNameRequired"),
            errors.map(ErrorObject::message))
        assertEquals(listOf("error.errorMessage.certificateNameRequired"),
            errors.mapNotNull { it.params["i18nKey"] }.distinct())
    }

    @Test
    fun defaultTranslatorUsesCatalogAndFallsBackToSchemaMessage() {
        val error = OptimumCodeJsonSchemaValidator().validate(schema, data).first()
        val bulgarian = Translator { key, fallback, _ ->
            if (key == "error.errorMessage.certificateNameRequired")
                "Въведете името, отпечатано на сертификата." else fallback
        }
        assertEquals("Въведете името, отпечатано на сертификата.",
            defaultErrorTranslator.translate(error, bulgarian, null))
        assertEquals("certificateNameRequired",
            defaultErrorTranslator.translate(error, Translator { _, fallback, _ -> fallback }, null))
    }

    @Test
    fun controlCustomAndKeywordOverridesPrecedeSchemaMessage() {
        val error = OptimumCodeJsonSchemaValidator().validate(schema, data).first()
        val control = ControlElement("#/properties/certifications/items/properties/name", i18n = "certificate.name")
        fun translate(values: Map<String, String>) = Translator { key, fallback, _ -> values[key] ?: fallback }
        fun message(values: Map<String, String>, callback: ErrorTranslator = defaultErrorTranslator) =
            getCombinedErrorMessage(listOf(error), callback, translate(values),
                uiSchema = control, path = "certifications.0.name")

        assertEquals("All name errors", message(mapOf(
            "certificate.name.error.custom" to "All name errors",
            "certificate.name.error.minLength" to "Field keyword",
        )))
        assertEquals("Field keyword", message(mapOf(
            "certificate.name.error.minLength" to "Field keyword",
            "error.minLength" to "Global keyword",
        )))
        assertEquals("Global keyword", message(mapOf("error.minLength" to "Global keyword")))
        assertEquals("Business wording", message(mapOf(
            "error.errorMessage.certificateNameRequired" to "Business wording",
        )))
        val callback = ErrorTranslator { _, _, _ -> "Host callback" }
        assertEquals("Host callback", message(mapOf("error.minLength" to "Global keyword"), callback))
        assertEquals("All name errors", message(mapOf(
            "certificate.name.error.custom" to "All name errors",
        ), callback))
    }
}
