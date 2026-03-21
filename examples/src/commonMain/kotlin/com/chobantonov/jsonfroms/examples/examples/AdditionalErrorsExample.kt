package com.chobantonov.jsonfroms.examples.examples

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.examples.ExampleAction
import com.chobantonov.jsonfroms.examples.ExampleDescription

private val additionalErrorsStore = mutableListOf<ErrorObject>()

val additionalErrorsActions = listOf(
    ExampleAction(
        label = "Add additional error",
        apply = { props ->
            additionalErrorsStore += ErrorObject(
                instancePath = "/personalData/age",
                message = "New error #${additionalErrorsStore.size + 1}",
                schemaPath = "",
                keyword = "",
                params = emptyMap(),
            )
            props.copy(additionalErrors = additionalErrorsStore.toList())
        },
    ),
)

val additionalErrorsExample = ExampleDescription(
    name = "additional-errors",
    label = "Additional errors",
    data = personData,
    schema = personSchema,
    uischema = personUiSchema,
    actions = additionalErrorsActions,
)
