package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.models.CategorizationElement
import com.chobantonov.jsonfroms.examples.ExampleDescription
import kotlinx.serialization.json.JsonPrimitive

val categorizationStepperUiSchema = CategorizationElement(
    label = categorizationUiSchema.label,
    i18n = categorizationUiSchema.i18n,
    rule = categorizationUiSchema.rule,
    options = mapOf("variant" to JsonPrimitive("stepper")),
    elements = categorizationUiSchema.elements,
)

val categorizationStepperExample = ExampleDescription(
    name = "categorizationstepper",
    label = "Categorization (Stepper)",
    data = categorizationData,
    schema = categorizationSchema,
    uischema = categorizationStepperUiSchema,
)
