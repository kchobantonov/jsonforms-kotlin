package com.chobantonov.jsonfroms.core.mappers

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.core.models.CategoryElement
import com.chobantonov.jsonfroms.core.models.JsonData
import com.chobantonov.jsonfroms.core.util.Runtime
import com.chobantonov.jsonfroms.core.validation.JsonSchemaValidator

fun visibleCategories(
    categorization: CategorizationElement,
    data: JsonData,
    validator: JsonSchemaValidator,
    config: Map<String, Any?> = emptyMap(),
    path: String? = null,
): List<CategoryElement> = categorization.elements
    .filterIsInstance<CategoryElement>()
    .filter { category -> Runtime.isVisible(category, data, validator, path, config) }
