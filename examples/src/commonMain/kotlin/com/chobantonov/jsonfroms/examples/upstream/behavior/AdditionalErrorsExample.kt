package com.chobantonov.jsonfroms.examples.upstream.behavior

import com.chobantonov.jsonfroms.core.validation.ErrorObject
import com.chobantonov.jsonfroms.examples.ExampleAction
import com.chobantonov.jsonfroms.examples.ExampleDescription
import com.chobantonov.jsonfroms.examples.upstreamAdditionalErrorsExample

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
    data = upstreamAdditionalErrorsExample.data,
    schema = upstreamAdditionalErrorsExample.schema,
    uischema = upstreamAdditionalErrorsExample.uischema,
    actions = additionalErrorsActions,
)
