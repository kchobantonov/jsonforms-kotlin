package com.chobantonov.jsonfroms.core.generators

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.GroupElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelElement
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.util.deriveTypes
import com.chobantonov.jsonfroms.core.util.resolveSchema
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun createControlElement(ref: String): ControlElement = ControlElement(scope = ref)

fun generateDefaultUISchema(
    jsonSchema: JsonSchema,
    layoutType: String = "VerticalLayout",
    prefix: String = "#",
    rootSchema: JsonSchema = jsonSchema,
): UiSchemaElement =
    wrapInLayoutIfNecessary(generateUiSchema(jsonSchema, prefix, "", layoutType, rootSchema), layoutType)

private fun generateUiSchema(
    jsonSchema: JsonSchema,
    currentRef: String,
    schemaName: String,
    layoutType: String,
    rootSchema: JsonSchema,
): UiSchemaElement {
    val ref = (jsonSchema["\$ref"] as? JsonPrimitive)?.content
    if (ref != null) {
        val resolved = resolveSchema(rootSchema, ref, rootSchema) ?: jsonSchema
        return generateUiSchema(resolved, currentRef, schemaName, layoutType, rootSchema)
    }

    if (isCombinator(jsonSchema)) {
        return createControlElement(currentRef)
    }

    val types = deriveTypes(jsonSchema)
    if (types.isEmpty() || types.size > 1) {
        return createControlElement(currentRef)
    }

    if (currentRef == "#" && types.first() == "object") {
        val elements = mutableListOf<UiSchemaElement>()
        val properties = jsonSchema["properties"] as? JsonObject
        if (properties != null && properties.size > 1 && schemaName.isNotBlank()) {
            if (layoutType == "Group") {
                // Group carries its label on the layout itself.
            } else {
                elements += LabelElement(text = startCase(schemaName))
            }
        }

        properties?.forEach { (propName, value) ->
            val refValue = "${currentRef}/properties/${encode(propName)}"
            val propertySchema = when (value) {
                is JsonObject -> {
                    val nestedRef = (value["\$ref"] as? JsonPrimitive)?.content
                    if (nestedRef != null) {
                        resolveSchema(rootSchema, nestedRef, rootSchema) ?: JsonSchema(value)
                    } else {
                        JsonSchema(value)
                    }
                }

                else -> null
            }
            if (propertySchema != null) {
                elements += generateUiSchema(propertySchema, refValue, propName, layoutType, rootSchema)
            }
        }

        return createLayout(layoutType, schemaName, elements)
    }

    return createControlElement(currentRef)
}

private fun createLayout(
    layoutType: String,
    schemaName: String,
    elements: List<UiSchemaElement>,
): UiSchemaElement =
    when (layoutType) {
        "Group" -> GroupElement(
            label = schemaName.takeIf { it.isNotBlank() }?.let(::startCase),
            elements = elements,
        )
        else -> VerticalLayout(elements = elements)
    }

private fun wrapInLayoutIfNecessary(
    uiSchema: UiSchemaElement,
    layoutType: String,
): UiSchemaElement =
    when (uiSchema) {
        is VerticalLayout, is GroupElement -> uiSchema
        else -> createLayout(layoutType, "", listOf(uiSchema))
    }

private fun isCombinator(jsonSchema: JsonSchema): Boolean =
    (jsonSchema["oneOf"] as? JsonArray)?.isNotEmpty() == true ||
        (jsonSchema["anyOf"] as? JsonArray)?.isNotEmpty() == true ||
        (jsonSchema["allOf"] as? JsonArray)?.isNotEmpty() == true

private fun encode(segment: String): String =
    segment.replace("~", "~0").replace("/", "~1")

private fun startCase(value: String): String =
    value
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
