package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.HorizontalLayout
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.models.UiSchemaElement
import com.chobantonov.jsonfroms.core.models.VerticalLayout
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator

/** Structural layouts with no effective descendants do not occupy a parent slot. */
fun effectiveLayoutChildren(
    elements: List<UiSchemaElement>,
    data: JsonData,
    validator: JsonSchemaValidator,
    path: String?,
    config: Map<String, Any?>,
): List<UiSchemaElement> = elements.filter { child ->
    Runtime.isVisible(child, data, validator, path, config) && when (child) {
        is HorizontalLayout -> effectiveLayoutChildren(child.elements, data, validator, path, config).isNotEmpty()
        is VerticalLayout -> effectiveLayoutChildren(child.elements, data, validator, path, config).isNotEmpty()
        else -> true
    }
}
