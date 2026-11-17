package com.chobantonov.jsonfroms.core.validation

import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.util.JsonPointer
import io.github.optimumcode.json.schema.FormatBehavior
import io.github.optimumcode.json.schema.JsonSchemaLoader
import io.github.optimumcode.json.schema.SchemaOption
import io.github.optimumcode.json.schema.JsonSchema as OptimumCodeSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val LatestSchemaUri = "https://json-schema.org/draft/2020-12/schema"
private const val DraftSevenSchemaUri = "http://json-schema.org/draft-07/schema#"

private val genericSchemaUris = setOf(
    "http://json-schema.org/schema",
    "http://json-schema.org/schema#",
    "https://json-schema.org/schema",
    "https://json-schema.org/schema#",
)

@OptIn(io.github.optimumcode.json.schema.ExperimentalApi::class)
class OptimumCodeJsonSchemaValidator : JsonSchemaValidator {
    override fun compile(schema: JsonSchema): ValidateFunction {
        val normalizedSchema = schema.normalizeGenericSchemaUri()
        val rawSchema = normalizedSchema.toJsonObject()
        fun load(rootData: JsonElement?): OptimumCodeSchema = JsonSchemaLoader.create()
            .withCustomFormat("iso-time", isoTimeFormatValidator)
            .withCustomFormat("iso-date-time", isoDateTimeFormatValidator)
            .withCustomFormat("cron", cronFormatValidator)
            .withCustomFormat("color", colorFormatValidator)
            .withExtensions(temporalComparisonKeywords.map { keyword ->
                TemporalFormatAssertionFactory(keyword, rawSchema, rootData)
            })
            .withSchemaOption(SchemaOption.FORMAT_BEHAVIOR_OPTION,
                FormatBehavior.ANNOTATION_AND_ASSERTION)
            .fromJsonElement(rawSchema)
        val delegate = try {
            load(null)
        } catch (exception: Exception) {
            return SchemaCompilationFailure(schema, exception)
        }
        val dynamicBounds = rawSchema.hasDynamicTemporalBound()

        return object : ValidateFunction {
            override val schema: JsonSchema = schema

            override fun validate(data: JsonData): List<ErrorObject> {
                val active = if (dynamicBounds) try {
                    load(data)
                } catch (exception: Exception) {
                    return SchemaCompilationFailure(schema, exception).validate(data)
                } else delegate
                val errors = mutableListOf<io.github.optimumcode.json.schema.ValidationError>()
                active.validate(data, io.github.optimumcode.json.schema.ErrorCollector { error ->
                    errors += error
                })
                return errors.flatMap { error ->
                    error.toErrorObject().expandMissingProperties(rawSchema, data)
                }.withoutDuplicateModernTupleTailErrors().withAggregateAnyOfErrors(rawSchema)
                    .withSchemaErrorMessages(rawSchema)
            }
        }
    }
}

private fun List<ErrorObject>.withSchemaErrorMessages(root: JsonObject): List<ErrorObject> = map { error ->
    val keyword = error.keyword ?: return@map error
    val parent = error.schemaPath?.substringBeforeLast('/') ?: return@map error
    val schema = JsonPointer.resolve(root, parent) as? JsonObject ?: return@map error
    val messages = schema["errorMessage"] as? JsonObject ?: return@map error
    val custom = (messages[keyword] as? JsonPrimitive)?.contentOrNull ?: return@map error
    error.copy(message = custom, params = error.params +
        ("i18nKey" to "error.errorMessage.$custom"))
}

private fun JsonSchema.normalizeGenericSchemaUri(): JsonSchema {
    if (schemaUri != null && schemaUri !in genericSchemaUris) return this
    val raw = toJsonObject()
    val usesLegacyItems = raw.containsLegacyPositionalItems()
    if (schemaUri == null && !usesLegacyItems) return this
    val inferred = if (usesLegacyItems) DraftSevenSchemaUri else LatestSchemaUri
    return JsonSchema(JsonObject(raw + ("\$schema" to JsonPrimitive(inferred))))
}

private fun JsonElement.containsLegacyPositionalItems(): Boolean {
    val schema = this as? JsonObject ?: return false
    if (schema["items"] is JsonArray && schema["prefixItems"] == null) return true

    val singleSchemas = listOf(
        "items", "additionalItems", "additionalProperties", "unevaluatedItems",
        "unevaluatedProperties", "contains", "propertyNames", "not", "if", "then", "else",
    )
    val schemaArrays = listOf("prefixItems", "allOf", "anyOf", "oneOf")
    val schemaMaps = listOf("properties", "patternProperties", "definitions", "\$defs", "dependentSchemas")
    return singleSchemas.any { key -> schema[key]?.containsLegacyPositionalItems() == true } ||
        schemaArrays.any { key -> (schema[key] as? JsonArray)?.any { it.containsLegacyPositionalItems() } == true } ||
        schemaMaps.any { key -> (schema[key] as? JsonObject)?.values?.any { it.containsLegacyPositionalItems() } == true }
}

private data class FailedPrefixPosition(
    val schemaParentPath: String,
    val index: Int,
    val instancePath: String,
)

// The validator may apply a 2020-12 items tail schema to a prefix value when
// that value fails prefixItems. Keep the prefix error and actual tail errors.
private fun List<ErrorObject>.withoutDuplicateModernTupleTailErrors(): List<ErrorObject> {
    val prefixMarker = Regex("""(?:^|/)prefixItems/(\d+)(?=/|$)""")
    val failedPrefixes = flatMap { error ->
        val schemaPath = error.schemaPath ?: return@flatMap emptyList()
        prefixMarker.findAll(schemaPath).map { match ->
            FailedPrefixPosition(
                schemaParentPath = schemaPath.substring(0, match.range.first),
                index = match.groupValues[1].toInt(),
                instancePath = error.instancePath,
            )
        }.toList()
    }
    if (failedPrefixes.isEmpty()) return this

    return filterNot { error ->
        val schemaPath = error.schemaPath ?: return@filterNot false
        failedPrefixes.any { failedPrefix ->
            val tailSchemaPath = failedPrefix.schemaParentPath + "/items"
            (schemaPath == tailSchemaPath || schemaPath.startsWith(tailSchemaPath + "/")) &&
                failedPrefix.instancePath.commonPointerPrefix(error.instancePath).any {
                    it.toIntOrNull() == failedPrefix.index
                }
        }
    }
}

// The backend emits the failing branch errors for anyOf but omits the keyword error
// that Ajv reports after those branches. Keep the branch details and add one
// aggregate error per failed anyOf instance.
private fun List<ErrorObject>.withAggregateAnyOfErrors(schema: JsonObject): List<ErrorObject> {
    val lastBranchError = mutableMapOf<Pair<String, String>, Int>()
    forEachIndexed { index, error ->
        val segments = error.schemaPath?.trim('/')?.split('/') ?: return@forEachIndexed
        for (position in 0 until segments.lastIndex) {
            if (segments[position] != "anyOf") continue
            val branchIndex = segments[position + 1].toIntOrNull() ?: continue
            val keywordPath = "/" + segments.take(position + 1).joinToString("/")
            val branches = JsonPointer.resolve(schema, keywordPath) as? JsonArray ?: continue
            if (branchIndex !in branches.indices) continue
            lastBranchError[error.instancePath to keywordPath] = index
        }
    }
    val missing = lastBranchError.filterKeys { (instancePath, keywordPath) ->
        none { it.keyword == "anyOf" && it.instancePath == instancePath &&
            it.schemaPath == keywordPath }
    }
    if (missing.isEmpty()) return this
    return buildList {
        this@withAggregateAnyOfErrors.forEachIndexed { index, error ->
            add(error)
            missing.forEach { (key, lastIndex) ->
                if (lastIndex == index) add(ErrorObject(
                    instancePath = key.first,
                    dataPath = key.first,
                    schemaPath = key.second,
                    keyword = "anyOf",
                    message = "must match a schema in anyOf",
                ))
            }
        }
    }
}

private fun String.commonPointerPrefix(other: String): List<String> =
    split('/').drop(1).zip(other.split('/').drop(1))
        .takeWhile { (left, right) -> left == right }
        .map { (left, _) -> left }

private class SchemaCompilationFailure(
    override val schema: JsonSchema,
    exception: Exception,
) : ValidateFunction {
    private val temporalKeywordPath = (exception as? TemporalBoundSchemaException)?.keywordPath
    private val error = ErrorObject(
        schemaPath = temporalKeywordPath ?: if (schema.schemaUri == null) "" else "/\$schema",
        keyword = temporalKeywordPath?.substringAfterLast('/') ?: "\$schema",
        message = buildString {
            append("Unable to compile JSON Schema")
            exception.message?.takeIf(String::isNotBlank)?.let {
                append(": ")
                append(it)
            }
        },
        params = schema.schemaUri?.let { mapOf("schemaUri" to it) }.orEmpty(),
    )

    override fun validate(data: JsonData): List<ErrorObject> = listOf(error)
}

private fun io.github.optimumcode.json.schema.ValidationError.toErrorObject(): ErrorObject {
    val instancePath = objectPath.toString()
    val schemaPath = schemaPath.toString()
    val schemaSegments = schemaPath.trim('/').split('/')
    val keyword = when {
        schemaSegments.size >= 2 && schemaSegments[schemaSegments.lastIndex - 1] in
            setOf("dependencies", "dependentRequired") -> schemaSegments[schemaSegments.lastIndex - 1]
        else -> schemaSegments.last()
    }
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

/** The validator groups missing names into one error; controls need one routed error per name. */
private fun ErrorObject.expandMissingProperties(schema: JsonObject, data: JsonElement): List<ErrorObject> {
    if (keyword !in setOf("required", "dependencies", "dependentRequired")) return listOf(this)
    val declared = JsonPointer.resolve(schema, schemaPath) as? JsonArray ?: return listOf(this)
    val instance = JsonPointer.resolve(data, instancePath) as? JsonObject ?: return listOf(this)
    val missing = declared.mapNotNull { (it as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.contentOrNull }
        .filterNot(instance::containsKey).distinct()
    if (missing.isEmpty()) return listOf(this)
    val bracketedNames = Regex("""\[[^\]]*\]""")
    return missing.map { property ->
        copy(
            message = if (bracketedNames.containsMatchIn(message))
                message.replaceFirst(bracketedNames, "[$property]") else message,
            params = params + ("missingProperty" to property),
        )
    }
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
