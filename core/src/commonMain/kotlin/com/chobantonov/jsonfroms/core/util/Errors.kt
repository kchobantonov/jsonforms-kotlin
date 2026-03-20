package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.validation.ErrorObject

fun getInvalidProperty(error: ErrorObject): String? =
    when (error.keyword) {
        "required", "dependencies", "dependentRequired" ->
            error.params["missingProperty"] ?: extractFirstPropertyFromCollection(error.message)

        "additionalProperties" ->
            error.params["additionalProperty"] ?: lastPathSegment(error.instancePath)

        else -> null
    }

fun getControlPath(error: ErrorObject): String {
    var controlPath = error.dataPath ?: error.instancePath
    controlPath = controlPath.replace('/', '.')

    val invalidProperty = getInvalidProperty(error)
    if (invalidProperty != null && !controlPath.endsWith(invalidProperty)) {
        controlPath = if (controlPath.isBlank()) {
            invalidProperty
        } else {
            "$controlPath.$invalidProperty"
        }
    }

    controlPath = controlPath.removePrefix(".")
    return decodeDotSeparatedPath(controlPath)
}

fun errorsAt(
    instancePath: String,
    schema: JsonSchema,
    matchPath: (String) -> Boolean,
): (List<ErrorObject>) -> List<ErrorObject> = { errors ->
    errors.filter { error ->
        val controlPath = getControlPath(error)
        matchPath(controlPath) || controlPath == instancePath
    }
}

fun formatErrorMessage(errors: List<String>?): String =
    errors?.joinToString("\n").orEmpty()

private fun decodeDotSeparatedPath(path: String): String =
    path
        .split('.')
        .filter { it.isNotEmpty() }
        .joinToString(".") { JsonPointer.decode(it) }

private fun lastPathSegment(instancePath: String): String? =
    instancePath
        .split('/')
        .lastOrNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let(JsonPointer::decode)

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
