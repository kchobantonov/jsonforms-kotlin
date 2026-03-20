package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema

data class ErrorObject(
    val instancePath: String = "",
    val dataPath: String? = null,
    val schemaPath: String? = null,
    val keyword: String? = null,
    val message: String,
    val params: Map<String, String> = emptyMap(),
    val absoluteKeywordLocation: String? = null,
)

typealias ValidationIssue = ErrorObject

interface ValidateFunction {
    val schema: JsonSchema

    fun validate(data: JsonData): List<ErrorObject>
}

interface JsonSchemaValidator {
    fun compile(schema: JsonSchema): ValidateFunction

    fun validate(
        schema: JsonSchema,
        data: JsonData,
    ): List<ErrorObject> = compile(schema).validate(data)
}
