package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.json.JsonElement

@OptIn(ExperimentalTime::class)
fun convertDateToString(
    instant: Instant,
    format: String? = null,
): String {
    val dateTimeString = instant.toString()
    return when (format) {
        "date-time", null -> dateTimeString
        "date" -> dateTimeString.substringBefore('T')
        "time" -> dateTimeString.substringAfter('T').substringBefore('.').substringBefore('Z')
        else -> dateTimeString
    }
}

@OptIn(ExperimentalTime::class)
fun convertDateToString(
    format: String? = null,
): String = convertDateToString(Clock.System.now(), format)

fun convertToValidClassName(value: String): String =
    value.replace("#", "root").replace("/", "_")

fun composePaths(
    path1: String,
    path2: String,
): String {
    val first = if (path1.isNotEmpty() && path2.isNotEmpty() && !path2.startsWith("[")) {
        "$path1."
    } else {
        path1
    }
    return when {
        first.isEmpty() -> path2
        path2.isEmpty() -> first
        else -> first + path2
    }
}

fun toDataPathSegments(schemaPath: String): List<String> =
    schemaPath
        .replace(Regex("(anyOf|allOf|oneOf)/\\d+/"), "")
        .replace(Regex("(then|else)/"), "")
        .split('/')
        .map(JsonPointer::decode)
        .let { segments ->
            val startFromRoot = segments.firstOrNull() == "#" || segments.firstOrNull() == ""
            val startIndex = if (startFromRoot) 2 else 1
            segments.filterIndexed { index, _ -> index >= startIndex && (index - startIndex) % 2 == 0 }
        }

fun toDataPath(schemaPath: String): String = toDataPathSegments(schemaPath).joinToString(".")

object Resolve {
    fun schema(
        schema: JsonSchema?,
        schemaPath: String,
        rootSchema: JsonSchema,
    ): JsonSchema? = resolveSchema(schema, schemaPath, rootSchema)

    fun data(
        data: JsonElement?,
        path: String,
    ): JsonElement? = JsonPath.get(data, path)
}

object Paths {
    fun compose(
        path1: String,
        path2: String,
    ): String = composePaths(path1, path2)

    fun fromScoped(scopable: ControlElement): String =
        toDataPathSegments(scopable.scope).joinToString(".")
}

object Runtime {
    fun evalVisibility(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        path: String? = null,
        validator: JsonSchemaValidator,
        config: Any? = null,
    ): Boolean = RuleEvaluator.isVisible(uiSchema, data, validator, path, config)

    fun evalEnablement(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        path: String? = null,
        validator: JsonSchemaValidator,
        config: Any? = null,
    ): Boolean = RuleEvaluator.isEnabled(uiSchema, data, validator, path, config)

    fun evalReadonly(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        path: String? = null,
        validator: JsonSchemaValidator,
        config: Any? = null,
    ): Boolean = RuleEvaluator.isReadonly(uiSchema, data, validator, path, config)

    fun isEnabled(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = if (uiSchema.rule != null) evalEnablement(uiSchema, data, path, validator, config) else true

    fun isVisible(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = if (uiSchema.rule != null) evalVisibility(uiSchema, data, path, validator, config) else true

    fun isReadonly(
        uiSchema: UiSchemaElement,
        data: JsonElement,
        validator: JsonSchemaValidator,
        path: String? = null,
        config: Any? = null,
    ): Boolean = if (uiSchema.rule != null) evalReadonly(uiSchema, data, path, validator, config) else false
}
