package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.util.JsonPointer
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

data class ConditionalObjectFields(
    val activeSchema: JsonSchema,
    val reservedSchema: JsonSchema,
    val inactiveProperties: Set<String>,
    val diagnostic: ConditionalFieldDiagnostic? = null,
)

enum class ConditionalFieldDiagnostic {
    DiscoveryDepth,
    ConditionCompilationFailure,
}

fun conditionalFieldsEnabled(control: ControlElement, config: Map<String, Any?>): Boolean {
    val local = (control.options["conditionalFields"] as? JsonPrimitive)
        ?.takeUnless { it.isString }?.booleanOrNull
    val extended = when (val value = config["jsonformsExtended"]) {
        is JsonObject -> (value["conditionalFields"] as? JsonPrimitive)
            ?.takeUnless { it.isString }?.booleanOrNull
        is Map<*, *> -> when (val option = value["conditionalFields"]) {
            is Boolean -> option
            is JsonPrimitive -> option.takeUnless { it.isString }?.booleanOrNull
            else -> null
        }
        else -> null
    }
    return local ?: extended ?: false
}

/** Projects only object presentation; callers keep the original schema in the validator. */
fun projectConditionalObjectFields(
    schema: JsonSchema,
    rootSchema: JsonSchema,
    data: JsonObject,
    validator: JsonSchemaValidator,
): ConditionalObjectFields {
    val active = linkedMapOf<String, JsonElement>()
    val reserved = linkedMapOf<String, JsonElement>()
    val activeRequired = linkedSetOf<String>()
    var diagnostic: ConditionalFieldDiagnostic? = null

    fun walk(branch: JsonObject, isActive: Boolean, depth: Int) {
        if (depth >= 16) {
            diagnostic = ConditionalFieldDiagnostic.DiscoveryDepth
            return
        }
        val properties = (branch["properties"] as? JsonObject).orEmpty()
        properties.forEach { (name, field) ->
            if (name !in reserved) reserved[name] = field
            if (isActive) {
                active[name] = active[name]?.let { existing -> combinePropertySchemas(existing, field) } ?: field
            }
        }
        if (isActive) requiredNames(JsonSchema(branch)).forEach(activeRequired::add)
        (branch["allOf"] as? JsonArray)?.forEach { child ->
            (child as? JsonObject)?.let { walk(it, isActive, depth + 1) }
        }
        val condition = branch["if"]
        if (condition != null) {
            val matches = if (isActive) {
                when (condition) {
                    is JsonObject -> {
                        val conditionSchema = JsonSchema(JsonObject(buildMap {
                            rootSchema["\$schema"]?.let { put("\$schema", it) }
                            rootSchema["definitions"]?.let { put("definitions", it) }
                            rootSchema["\$defs"]?.let { put("\$defs", it) }
                            putAll(condition)
                        }))
                        val errors = validator.validate(conditionSchema, data)
                        if (errors.any { it.keyword == "\$schema" }) {
                            diagnostic = ConditionalFieldDiagnostic.ConditionCompilationFailure
                            null
                        } else errors.isEmpty()
                    }
                    is JsonPrimitive -> condition.takeUnless { it.isString }?.booleanOrNull.also {
                        if (it == null) diagnostic = ConditionalFieldDiagnostic.ConditionCompilationFailure
                    }
                    else -> {
                        diagnostic = ConditionalFieldDiagnostic.ConditionCompilationFailure
                        null
                    }
                }
            } else null
            (branch["then"] as? JsonObject)?.let { walk(it, isActive && matches == true, depth + 1) }
            (branch["else"] as? JsonObject)?.let { walk(it, isActive && matches == false, depth + 1) }
        }
        for (keyword in listOf("dependencies", "dependentSchemas")) {
            val dependencies = branch[keyword] as? JsonObject ?: continue
            dependencies.forEach { (trigger, dependent) ->
                when (dependent) {
                    is JsonObject -> walk(dependent, isActive && data.containsKey(trigger), depth + 1)
                    is JsonArray -> if (isActive && data.containsKey(trigger)) dependent.forEach { value ->
                        (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(activeRequired::add)
                    }
                    else -> Unit
                }
            }
        }
        (branch["dependentRequired"] as? JsonObject)?.forEach { (trigger, value) ->
            if (isActive && data.containsKey(trigger)) (value as? JsonArray)?.forEach { item ->
                (item as? JsonPrimitive)?.takeIf { it.isString }?.content?.let(activeRequired::add)
            }
        }
    }

    walk(schema.toJsonObject(), true, 0)

    val activeSchema = withObjectFields(schema, active, activeRequired)
    val reservedSchema = withObjectFields(schema, reserved, activeRequired)
    return ConditionalObjectFields(activeSchema, reservedSchema, reserved.keys - active.keys, diagnostic)
}

private fun requiredNames(schema: JsonSchema): List<String> =
    (schema["required"] as? JsonArray).orEmpty().mapNotNull { value ->
        (value as? JsonPrimitive)?.takeIf { it.isString }?.content
    }

private fun combinePropertySchemas(first: JsonElement, second: JsonElement): JsonElement {
    if (first == second) return first
    val facets = mutableListOf<JsonObject>()
    fun collect(value: JsonElement) {
        val objectValue = value as? JsonObject ?: return
        facets += objectValue
        (objectValue["allOf"] as? JsonArray)?.forEach(::collect)
    }
    collect(first)
    collect(second)
    // Keep the full conjunction for validation, and repeat only safe editor-facing facets.
    val hints = linkedMapOf<String, JsonElement>()
    fun uniqueString(key: String): JsonElement? = facets.mapNotNull { value ->
        (value[key] as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)
    }.distinct().singleOrNull()
    uniqueString("type")?.let { hints["type"] = it }
    uniqueString("format")?.let { hints["format"] = it }
    facets.mapNotNull { (it["maxLength"] as? JsonPrimitive)?.intOrNull }
        .minOrNull()?.let { hints["maxLength"] = JsonPrimitive(it) }
    facets.mapNotNull { (it["minLength"] as? JsonPrimitive)?.intOrNull }
        .maxOrNull()?.let { hints["minLength"] = JsonPrimitive(it) }
    if (facets.any { (it["readOnly"] as? JsonPrimitive)
            ?.takeUnless { value -> value.isString }?.booleanOrNull == true })
        hints["readOnly"] = JsonPrimitive(true)
    return JsonObject(hints + ("allOf" to JsonArray(listOf(first, second))))
}

private fun withObjectFields(
    schema: JsonSchema,
    fields: Map<String, JsonElement>,
    required: Set<String>,
): JsonSchema = JsonSchema(JsonObject(schema.value + mapOf(
    "properties" to JsonObject(fields),
    "required" to JsonArray(required.map(::JsonPrimitive)),
)))

/** Keeps authored containers and their order while omitting inactive direct child controls. */
fun omitInactiveObjectControls(uiSchema: UiSchemaElement, inactive: Set<String>): UiSchemaElement {
    if (inactive.isEmpty()) return uiSchema
    fun filtered(elements: List<UiSchemaElement>): List<UiSchemaElement> = elements.mapNotNull { element ->
        if (element is ControlElement && directPropertyName(element.scope) in inactive) null
        else omitInactiveObjectControls(element, inactive)
    }
    return when (uiSchema) {
        is VerticalLayout -> uiSchema.copy(elements = filtered(uiSchema.elements))
        is HorizontalLayout -> uiSchema.copy(elements = filtered(uiSchema.elements))
        is GroupElement -> uiSchema.copy(elements = filtered(uiSchema.elements))
        is CategoryElement -> uiSchema.copy(elements = filtered(uiSchema.elements))
        is CategorizationElement -> uiSchema.copy(elements = filtered(uiSchema.elements))
        else -> uiSchema
    }
}

private fun directPropertyName(scope: String): String? {
    if (!scope.startsWith("#/properties/")) return null
    val encoded = scope.removePrefix("#/properties/")
    if ('/' in encoded) return null
    return JsonPointer.decode(encoded)
}
