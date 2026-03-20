package com.chobantonov.jsonfroms.core.generators

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import kotlinx.serialization.json.JsonElement

object Generate {
    fun jsonSchema(
        instance: JsonElement,
        options: JsonSchemaGenerationOptions = JsonSchemaGenerationOptions(),
    ): JsonSchema = generateJsonSchema(instance, options)

    fun uiSchema(
        jsonSchema: JsonSchema,
        layoutType: String = "VerticalLayout",
        prefix: String = "#",
        rootSchema: JsonSchema = jsonSchema,
    ): UiSchemaElement = generateDefaultUISchema(jsonSchema, layoutType, prefix, rootSchema)

    fun controlElement(ref: String): ControlElement = createControlElement(ref)
}
