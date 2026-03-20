package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import io.github.optimumcode.json.schema.JsonSchema as OptimumCodeSchema

class OptimumCodeJsonSchemaValidator : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction {
        val delegate = OptimumCodeSchema.fromJsonElement(schema.toJsonObject())

        return object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonData): List<ErrorObject> {
                val errors = mutableListOf<io.github.optimumcode.json.schema.ValidationError>()
                delegate.validate(data, io.github.optimumcode.json.schema.ErrorCollector { error ->
                    errors += error
                })
                return errors.map { error -> error.toErrorObject() }
            }
        }
    }
}

private fun io.github.optimumcode.json.schema.ValidationError.toErrorObject(): ErrorObject {
    val instancePath = objectPath.toString()
    val schemaPath = schemaPath.toString()
    val keyword = schemaPath.substringAfterLast('/', missingDelimiterValue = schemaPath)
    val params = details.toMutableMap()

    when (keyword) {
        "required", "dependencies", "dependentRequired" -> {
            extractFirstPropertyFromCollection(message)?.let { property ->
                if ("missingProperty" !in params) {
                    params["missingProperty"] = property
                }
            }
        }

        "additionalProperties" -> {
            objectPath.toString()
                .substringAfterLast('/', missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }
                ?.let { property ->
                    if ("additionalProperty" !in params) {
                        params["additionalProperty"] = property
                    }
                }
        }
    }

    return ErrorObject(
        instancePath = instancePath,
        dataPath = instancePath,
        schemaPath = schemaPath,
        keyword = keyword,
        message = message,
        params = params,
        absoluteKeywordLocation = absoluteLocation?.toString(),
    )
}

private fun extractFirstPropertyFromCollection(message: String): String? {
    val collectionMatch = Regex("""\[(.*?)\]""").find(message)?.groupValues?.getOrNull(1)
    val first = collectionMatch
        ?.split(',')
        ?.firstOrNull()
        ?.trim()
        ?.removePrefix("'")
        ?.removeSuffix("'")

    if (!first.isNullOrBlank()) {
        return first
    }

    val singleQuoted = Regex("""'([^']+)'""").find(message)?.groupValues?.getOrNull(1)
    return singleQuoted?.takeIf { it.isNotBlank() }
}
