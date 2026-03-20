package com.chobantonov.jsonfroms.core.util

import com.chobantonov.jsonfroms.core.models.ControlElement
import com.chobantonov.jsonfroms.core.models.JsonSchema
import com.chobantonov.jsonfroms.core.models.LabelDescription

object Helpers {
    fun createLabelDescriptionFrom(
        withLabel: ControlElement,
        schema: JsonSchema? = null,
    ): LabelDescription = com.chobantonov.jsonfroms.core.util.createLabelDescriptionFrom(withLabel, schema)

    fun convertToValidClassName(value: String): String =
        com.chobantonov.jsonfroms.core.util.convertToValidClassName(value)
}
